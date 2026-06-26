/*
 ESTO TIENE LICENCIA GNU LICENSE APACHE

 NO ELIMINAR NINGUN COMENTARIO AGREGADO
*/
package com.keyler.discoverykidschannel

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * AppUpdater — Preview 2006.4.1.0.21
 *
 * Actualizador integrado: consulta los releases publicados en GitHub
 * (`keylerperales07-commits/DiscoveryKidsChannel`), compara la versión del
 * candidato contra la instalada (`versionName`, vía PackageManager) y, si hay
 * una más nueva, descarga el `.apk` adjunto usando `DownloadManager` y abre
 * el instalador del sistema al terminar.
 *
 * Accesible únicamente desde Configuración → "Buscar actualizaciones"
 * (sección "Actualizaciones"). No corre automáticamente al iniciar la
 * app: siempre es una acción explícita del usuario.
 *
 * Preview 4.1.0.21 — NUEVO: "Habilitar versiones Preview" en Configuración
 * (desactivado por defecto, ver SettingsManager.isPreviewUpdatesEnabled()).
 *   - Desactivado (default): solo se considera el último release ESTABLE
 *     (no marcado `prerelease` en GitHub) — equivalente a `/releases/latest`.
 *   - Activado: se considera el release más reciente de TODOS, sea estable
 *     o Preview (`prerelease: true`), permitiendo instalar Previews desde
 *     el Actualizador.
 *
 * Flujo:
 *   1. checkForUpdate() — hilo en background, GET a la API de GitHub Releases.
 *      Con Preview updates desactivado pide `/releases/latest` (GitHub ya
 *      excluye prereleases ahí). Activado, pide `/releases` (lista completa,
 *      ordenada por fecha de creación descendente) y toma el primer elemento.
 *   2. Parsea `tag_name` y la URL del primer asset `.apk` en
 *      `assets[].browser_download_url`.
 *   3. Compara `tag_name` (sin el prefijo "v" si lo tuviera) contra
 *      `versionName` actual con compareVersions() — comparación numérica
 *      por segmento (no alfabética), compatible con el esquema
 *      YYYY.MAJOR.MINOR.PATCH del proyecto.
 *   4. Si hay versión nueva: AlertDialog de confirmación → downloadAndInstall()
 *      vía DownloadManager → BroadcastReceiver de ACTION_DOWNLOAD_COMPLETE →
 *      Intent ACTION_VIEW con FileProvider hacia el instalador de paquetes.
 *   5. Si no hay versión nueva, o si falla la consulta (sin red, repo sin
 *      releases, etc.), se informa con un AlertDialog simple — nunca se deja
 *      a la app en un estado roto ni se reintenta sola.
 *
 * Nota: requiere el permiso INTERNET (declarado en AndroidManifest.xml) y,
 * para instalar el APK descargado, REQUEST_INSTALL_PACKAGES — en Android 8+
 * el usuario debe habilitar "Instalar apps desconocidas" para esta app la
 * primera vez; el sistema muestra esa pantalla automáticamente al intentar
 * abrir el instalador si el permiso no está concedido.
 */
object AppUpdater {

    private const val TAG = "AppUpdater"

    private const val GITHUB_OWNER = "keylerperales07-commits"
    private const val GITHUB_REPO = "DiscoveryKidsChannel"
    private const val GITHUB_API_LATEST_RELEASE =
        "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    private const val GITHUB_API_ALL_RELEASES =
        "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases"

    /** Callback simple para que el caller (SettingsActivity) sepa cuándo la consulta terminó. */
    interface CheckCallback {
        fun onUpToDate()
        fun onUpdateAvailable(remoteVersion: String, apkUrl: String, releaseNotesUrl: String)
        fun onError(message: String)
    }

    // ── Paso 1: consultar el release candidato ──────────────────────────────
    fun checkForUpdate(context: Context, callback: CheckCallback) {
        Thread {
            try {
                val previewUpdatesEnabled = SettingsManager.isPreviewUpdatesEnabled(context)

                val release: JSONObject? = if (previewUpdatesEnabled) {
                    // Lista completa de releases, ordenada por GitHub del más
                    // reciente al más antiguo: el primero es el candidato,
                    // sea estable o Preview (prerelease).
                    val json = httpGet(GITHUB_API_ALL_RELEASES)
                    val releases = org.json.JSONArray(json)
                    if (releases.length() > 0) releases.getJSONObject(0) else null
                } else {
                    // Solo el último release ESTABLE — GitHub ya excluye
                    // prereleases y drafts en este endpoint.
                    JSONObject(httpGet(GITHUB_API_LATEST_RELEASE))
                }

                if (release == null) {
                    runOnUi(context) {
                        callback.onError("El repositorio no tiene releases publicados todavía.")
                    }
                    return@Thread
                }

                val tagName = release.getString("tag_name")
                val remoteVersion = tagName.removePrefix("v").removePrefix("V")
                val htmlUrl = release.optString("html_url", "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/latest")

                val assets = release.optJSONArray("assets")
                var apkUrl: String? = null
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.getString("browser_download_url")
                            break
                        }
                    }
                }

                val localVersion = currentVersionName(context)

                runOnUi(context) {
                    if (apkUrl == null) {
                        // Hay un release más nuevo pero sin .apk adjunto: no hay nada para descargar.
                        callback.onError("El último release ($remoteVersion) no tiene un archivo .apk adjunto en GitHub.")
                        return@runOnUi
                    }
                    if (compareVersions(remoteVersion, localVersion) > 0) {
                        callback.onUpdateAvailable(remoteVersion, apkUrl, htmlUrl)
                    } else {
                        callback.onUpToDate()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error consultando actualizaciones", e)
                runOnUi(context) {
                    callback.onError("No se pudo conectar con GitHub. Revisá tu conexión a internet e intentá de nuevo.")
                }
            }
        }.start()
    }

    private fun httpGet(urlString: String): String {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IllegalStateException("HTTP $responseCode al consultar $urlString")
            }
            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                return reader.readText()
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun BufferedReader.readText(): String {
        val sb = StringBuilder()
        var line: String?
        while (true) {
            line = readLine() ?: break
            sb.append(line)
        }
        return sb.toString()
    }

    private fun currentVersionName(context: Context): String {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        // El versionName puede traer sufijo "-preview" (ej. 2006.4.2.0.20-preview);
        // se descarta para la comparación numérica por segmento.
        return packageInfo.versionName?.substringBefore("-") ?: "0"
    }

    /**
     * Compara dos versiones con esquema "N.N.N..." (cantidad de segmentos libre,
     * compatible con YYYY.MAJOR.MINOR.PATCH y también con YYYY.MAJOR.MINOR.PATCH.BUILD
     * de las Preview). Comparación numérica segmento por segmento, no alfabética
     * (necesario porque "4.10" debe ser mayor que "4.9", no menor).
     *
     * @return positivo si remote > local, 0 si son iguales, negativo si remote < local.
     */
    internal fun compareVersions(remote: String, local: String): Int {
        val remoteParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
        val localParts = local.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r != l) return r - l
        }
        return 0
    }

    // ── Paso 2: descargar el APK y abrir el instalador ──────────────────────

    private const val DOWNLOAD_FILE_NAME = "DiscoveryKidsChannel-update.apk"

    fun downloadAndInstall(context: Context, apkUrl: String, onStarted: () -> Unit = {}, onFailed: (String) -> Unit = {}) {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

            // Limpia una descarga anterior incompleta/vieja del mismo nombre, si existe.
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val existingFile = File(downloadDir, DOWNLOAD_FILE_NAME)
            if (existingFile.exists()) existingFile.delete()

            val request = DownloadManager.Request(Uri.parse(apkUrl))
                .setTitle("Discovery Kids LA — Actualización")
                .setDescription("Descargando nueva versión…")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, DOWNLOAD_FILE_NAME)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadId = downloadManager.enqueue(request)
            registerInstallReceiver(context, downloadManager, downloadId, existingFile)
            onStarted()
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando la descarga del APK", e)
            onFailed("No se pudo iniciar la descarga. Intentá de nuevo más tarde.")
        }
    }

    private fun registerInstallReceiver(
        context: Context,
        downloadManager: DownloadManager,
        downloadId: Long,
        apkFile: File
    ) {
        val appContext = context.applicationContext
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (completedId != downloadId) return

                appContext.unregisterReceiver(this)

                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                var success = false
                if (cursor != null && cursor.moveToFirst()) {
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    if (statusIndex >= 0 && cursor.getInt(statusIndex) == DownloadManager.STATUS_SUCCESSFUL) {
                        success = true
                    }
                    cursor.close()
                }

                if (success && apkFile.exists()) {
                    openInstaller(appContext, apkFile)
                } else {
                    Log.e(TAG, "La descarga del APK falló o el archivo no existe")
                }
            }
        }

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            appContext.registerReceiver(receiver, filter)
        }
    }

    private fun openInstaller(context: Context, apkFile: File) {
        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(installIntent)
    }

    // ── Helpers de UI ────────────────────────────────────────────────────────

    /** Diálogo de confirmación cuando hay una versión nueva disponible. */
    fun showUpdateAvailableDialog(context: Context, remoteVersion: String, apkUrl: String) {
        AlertDialog.Builder(context)
            .setTitle("Actualización disponible")
            .setMessage("Hay una nueva versión disponible: $remoteVersion\n\n¿Querés descargarla e instalarla ahora?")
            .setPositiveButton("Descargar") { _, _ ->
                downloadAndInstall(
                    context,
                    apkUrl,
                    onStarted = {
                        showInfoDialog(context, "Descargando…", "La descarga continúa en segundo plano. Te avisaremos cuando esté lista para instalar.")
                    },
                    onFailed = { message -> showInfoDialog(context, "Error", message) }
                )
            }
            .setNegativeButton("Más tarde", null)
            .show()
    }

    fun showInfoDialog(context: Context, title: String, message: String) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Entendido", null)
            .show()
    }

    private fun runOnUi(context: Context, action: () -> Unit) {
        android.os.Handler(android.os.Looper.getMainLooper()).post(action)
    }
}
