/*
 ESTO TIENE LICENCIA GNU LICENSE APACHE

 NO ELIMINAR NINGUN COMENTARIO AGREGADO
*/
package com.keyler.discoverykidschannel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * AppUpdater — Release 2007.4.3.0
 *
 * Actualizador integrado: consulta los releases publicados en GitHub
 * (`keylerperales07-commits/DiscoveryKidsChannel`), compara la versión del
 * candidato contra la instalada (`versionName`, vía PackageManager) y, si hay
 * una más nueva, descarga el `.apk` adjunto usando OkHttp (con progreso en
 * vivo) y abre el instalador del sistema al terminar.
 *
 * Accesible únicamente desde Configuración → "Buscar actualizaciones"
 * (sección "Actualizaciones"). No corre automáticamente al iniciar la
 * app: siempre es una acción explícita del usuario.
 *
 * Preview 4.1.0.21 — "Habilitar versiones Preview" en Configuración
 * (desactivado por defecto, ver SettingsManager.isPreviewUpdatesEnabled()).
 *   - Desactivado (default): solo se considera el último release ESTABLE
 *     (no marcado `prerelease` en GitHub) — equivalente a `/releases/latest`.
 *   - Activado: se considera el release más reciente de TODOS, sea estable
 *     o Preview (`prerelease: true`), permitiendo instalar Previews desde
 *     el Actualizador.
 *
 * Release 2006.4.2.1 — BUG FIX: el Actualizador siempre creía estar al día
 * sin importar el tag publicado en GitHub. Causa: Keyler etiqueta los
 * releases con el esquema corto MAJOR.MINOR.PATCH[.BUILD] (ej. "v4.2.1"),
 * pero la comparación usaba el versionName completo de la app, que incluye
 * el segmento de Era al inicio (ej. "2006.4.2.0") — comparar ambos tal cual
 * hacía que el primer segmento del tag (4) se comparara contra el de Era
 * (2006), perdiendo siempre. Ver currentVersionName() para el detalle.
 *
 * Release 2007.4.3.0 — NUEVO: se reemplazó el flujo basado en AlertDialog
 * (confirmación, "descargando en segundo plano", resultado) por
 * `UpdateActivity`, una pantalla dedicada con barra de progreso en vivo.
 * AppUpdater ya no muestra ningún diálogo de descarga/instalación — esa
 * responsabilidad pasó por completo a UpdateActivity. Lo que se mantiene
 * acá es solo la consulta a GitHub (checkForUpdate) y la descarga con
 * progreso (downloadAndInstall con onProgress). showUpdateAvailableDialog()
 * y showInfoDialog() quedan removidos: SettingsActivity ahora navega
 * directamente a UpdateActivity en vez de mostrar esos diálogos.
 *
 * Release 2007.4.4.0 — CAMBIO: la descarga del APK dejó de usar
 * `DownloadManager` y ahora se hace con OkHttp (downloadAndInstall lee el
 * `ResponseBody` en un loop manual, escribiendo a un `FileOutputStream` en
 * el mismo hilo de descarga). Con DownloadManager, saber cuándo terminaba
 * la descarga dependía de un `BroadcastReceiver` (ACTION_DOWNLOAD_COMPLETE)
 * más un `Thread` separado sondeando `DownloadManager.Query` cada 300 ms
 * para el progreso — dos mecanismos distintos y una condición de carrera
 * posible entre ambos. Con OkHttp todo pasa en un solo lugar: se lee el
 * body en chunks, se reporta el progreso en cada chunk, y apenas el loop
 * de lectura termina se sabe con certeza que la descarga está completa
 * (no hace falta un receiver aparte). Requiere la dependencia
 * `com.squareup.okhttp3:okhttp` en build.gradle (no incluida en este
 * paquete de fuentes — agregarla si todavía no está).
 *
 * Nota sobre la Era: a partir de esta release el segmento de Era del
 * versionName pasa de 2006 a 2007 (ej. "2007.4.3.0"). currentVersionName()
 * sigue funcionando igual sin cambios — descarta el primer segmento sin
 * importar su valor, así que el cambio de Era no requiere tocar este archivo.
 *
 * Flujo:
 *   1. checkForUpdate() — hilo en background, GET a la API de GitHub Releases.
 *      Con Preview updates desactivado pide `/releases/latest` (GitHub ya
 *      excluye prereleases ahí). Activado, pide `/releases` (lista completa,
 *      ordenada por fecha de creación descendente) y toma el primer elemento.
 *   2. Parsea `tag_name` y la URL del primer asset `.apk` en
 *      `assets[].browser_download_url`.
 *   3. Compara `tag_name` (sin el prefijo "v" si lo tuviera) contra
 *      `versionName` actual (sin el segmento de Era) con compareVersions() —
 *      comparación numérica por segmento (no alfabética), en el mismo
 *      esquema corto MAJOR.MINOR.PATCH[.BUILD] que usan los tags de GitHub.
 *   4. Si hay versión nueva: SettingsActivity lanza UpdateActivity con los
 *      datos del release (versión, apkUrl) → UpdateActivity llama a
 *      downloadAndInstall() con un callback de progreso → OkHttp descarga el
 *      `.apk` en un loop de lectura manual (reporta % y bytes en cada bloque)
 *      → al terminar el loop, Intent ACTION_VIEW con FileProvider hacia
 *      el instalador de paquetes.
 *   5. Si no hay versión nueva, o si falla la consulta (sin red, repo sin
 *      releases, etc.), UpdateActivity muestra el estado correspondiente en
 *      pantalla — nunca se deja a la app en un estado roto ni se reintenta sola.
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
                    runOnUi {
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

                runOnUi {
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
                runOnUi {
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
        val raw = packageInfo.versionName?.substringBefore("-") ?: "0"

        // Release 2006.4.2.1 — BUG FIX: Keyler etiqueta los releases en GitHub
        // con el esquema corto MAJOR.MINOR.PATCH[.BUILD] (ej. tag "v4.2.1"),
        // mientras que versionName usa el esquema completo con el segmento de
        // Era al inicio: YYYY.MAJOR.MINOR.PATCH[.BUILD] (ej. "2006.4.2.0").
        // Comparar ambos tal cual hacía que compareVersions() comparara el
        // segmento de Era (2006) contra el primer segmento del tag (4), y
        // como 4 < 2006 el Actualizador SIEMPRE creía estar ya en la última
        // versión sin importar qué tag hubiera publicado en GitHub.
        //
        // Se descarta el primer segmento (la Era, fija para todo el esquema
        // de versionado del proyecto) para que la comparación quede en el
        // mismo formato corto que usan los tags: MAJOR.MINOR.PATCH[.BUILD].
        val segments = raw.split(".")
        return if (segments.size > 1) segments.drop(1).joinToString(".") else raw
    }

    /**
     * Compara dos versiones con esquema "N.N.N..." (cantidad de segmentos libre).
     * Comparación numérica segmento por segmento, no alfabética (necesario
     * porque "4.10" debe ser mayor que "4.9", no menor).
     *
     * Release 2006.4.2.1: ambos lados llegan ya en formato corto
     * MAJOR.MINOR.PATCH[.BUILD] — remote desde el tag de GitHub (sin "v"),
     * local desde currentVersionName() (que descarta el segmento de Era
     * inicial del versionName completo). Ver currentVersionName() para el
     * detalle de por qué era necesario ese descarte.
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

    // ── Paso 2: descargar el APK (OkHttp) y abrir el instalador ─────────────

    private const val DOWNLOAD_FILE_NAME = "DiscoveryKidsChannel-update.apk"

    /** Cliente único reutilizado entre descargas; timeouts generosos por el tamaño del .apk. */
    private val downloadHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Release 2007.4.4.0 — descarga el .apk con OkHttp en vez de DownloadManager.
     *
     * Todo corre en un único hilo: se abre el `ResponseBody` del release,
     * se lee en bloques de 8 KB escribiéndolos directo a un `FileOutputStream`,
     * y se reporta el progreso (porcentaje + bytes) en cada bloque vía
     * onProgress. Cuando el loop de lectura termina sin excepciones, la
     * descarga está garantizada completa — no hace falta un
     * BroadcastReceiver ni sondear un estado aparte como con DownloadManager.
     */
    fun downloadAndInstall(
        context: Context,
        apkUrl: String,
        onStarted: () -> Unit = {},
        onProgress: (percent: Int, bytesDownloaded: Long, bytesTotal: Long) -> Unit = { _, _, _ -> },
        onCompleted: () -> Unit = {},
        onFailed: (String) -> Unit = {}
    ) {
        val appContext = context.applicationContext
        Thread {
            var output: FileOutputStream? = null
            try {
                // Limpia una descarga anterior incompleta/vieja del mismo nombre, si existe.
                val downloadDir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                val apkFile = File(downloadDir, DOWNLOAD_FILE_NAME)
                if (apkFile.exists()) apkFile.delete()

                runOnUi { onStarted() }

                val request = Request.Builder().url(apkUrl).build()
                downloadHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("HTTP ${response.code} al descargar el APK")
                    }
                    val body = response.body ?: throw IllegalStateException("Respuesta vacía del servidor")
                    val bytesTotal = body.contentLength() // -1 si el servidor no manda Content-Length
                    var bytesDownloaded = 0L
                    var lastReportedPercent = -1

                    output = FileOutputStream(apkFile)
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output!!.write(buffer, 0, read)
                            bytesDownloaded += read

                            if (bytesTotal > 0) {
                                val percent = ((bytesDownloaded * 100L) / bytesTotal).toInt().coerceIn(0, 100)
                                if (percent != lastReportedPercent) {
                                    lastReportedPercent = percent
                                    runOnUi { onProgress(percent, bytesDownloaded, bytesTotal) }
                                }
                            } else {
                                // Sin Content-Length no se puede calcular %, pero igual
                                // se informan los bytes para que la UI muestre algo.
                                runOnUi { onProgress(-1, bytesDownloaded, -1L) }
                            }
                        }
                    }
                    output?.flush()
                }

                // El loop de lectura terminó sin tirar excepción: la descarga está completa.
                runOnUi { onCompleted() }
                openInstaller(appContext, apkFile)
            } catch (e: Exception) {
                Log.e(TAG, "Error descargando el APK con OkHttp", e)
                runOnUi {
                    onFailed("La descarga falló. Revisá tu conexión e intentá de nuevo.")
                }
            } finally {
                output?.close()
            }
        }.start()
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

    private fun runOnUi(action: () -> Unit) {
        android.os.Handler(android.os.Looper.getMainLooper()).post(action)
    }
}
