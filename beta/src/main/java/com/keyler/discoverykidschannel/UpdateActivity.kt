/*
 ESTO TIENE LICENCIA GNU LICENSE APACHE

 NO ELIMINAR NINGUN COMENTARIO AGREGADO
*/
package com.keyler.discoverykidschannel

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * UpdateActivity — Release 2007.4.3.0
 *
 * Pantalla dedicada del Actualizador. Reemplaza por completo el flujo viejo
 * de AlertDialogs (confirmación → "descargando en segundo plano" →
 * resultado) que vivía repartido entre AppUpdater y SettingsActivity.
 *
 * SettingsActivity ya no llama a AppUpdater.checkForUpdate() directamente:
 * ahora solo abre esta Activity (startActivity), y es UpdateActivity quien
 * hace la consulta, muestra el resultado, pide confirmación, descarga con
 * una ProgressBar en vivo, y abre el instalador del sistema al terminar.
 *
 * Estados de pantalla (un solo layout, se muestra/oculta cada grupo de vistas
 * según el estado — no hay AlertDialogs en ningún paso):
 *   1. CHECKING     — "Buscando actualizaciones…" + barra fina indeterminada.
 *   2. UP_TO_DATE    — mensaje + renglón "Comprobar actualizaciones".
 *   3. AVAILABLE     — versión encontrada + renglones "Descargar" / "Más tarde".
 *   4. DOWNLOADING   — barra fina determinada (0–100%) + porcentaje en texto.
 *   5. INSTALLING    — se abrió el instalador del sistema (Intent ACTION_VIEW);
 *                      esta Activity se queda en un estado neutro de fondo,
 *                      el usuario confirma la instalación fuera de la app.
 *   6. ERROR         — mensaje de error + renglones "Reintentar" / "Volver".
 *
 * Instalación automática: por restricción de seguridad de Android (8+), no
 * se puede instalar un APK sin que el usuario confirme en la pantalla del
 * sistema ("Instalar apps desconocidas" la primera vez, y la pantalla de
 * instalación en cada APK). Por eso, al terminar la descarga, esta Activity
 * abre el instalador automáticamente (vía AppUpdater.downloadAndInstall,
 * mismo mecanismo de siempre con FileProvider) — es el paso máximo de
 * automatización posible; lo que sigue después ya es 100% del sistema.
 *
 * Se accede únicamente desde Configuración → "Buscar actualizaciones".
 *
 * Release 2007.4.4.0 — el progreso de descarga ya no viene de
 * DownloadManager.Query sino directo del callback onProgress de
 * AppUpdater.downloadAndInstall() (ahora con OkHttp), que además informa
 * bytes descargados/totales — se muestran como "X MB de Y MB" debajo de
 * la barra. Rediseño de activity_update.xml para que coincida con el
 * lenguaje visual de SettingsTheme/activity_settings (header idéntico,
 * ícono centrado, botones planos en vez de Button de ancho completo).
 *
 * Preview 2008.4.5.0.50 — REDISEÑO 2: se calca la pantalla nativa
 * "Configuración → Sistema → Actualización del sistema" de Android. El
 * bloque de estado (ícono + título + subtítulos) ahora queda alineado a la
 * izquierda en vez de centrado, la ProgressBar grande se reemplaza por una
 * barra fina debajo del título (visible solo en CHECKING/DOWNLOADING), y
 * los Button planos se reemplazan por renglones de lista clickeables con
 * flecha ">" (itemUpdatePrimary / itemUpdateSecondary), igual a como se ve
 * "Comprobar actualizaciones" en la captura de referencia. El estado se
 * sigue manejando igual que antes (mismo enum, mismos callbacks de
 * AppUpdater) — lo único que cambió es qué vistas se muestran y cómo.
 */
class UpdateActivity : AppCompatActivity() {

    private enum class UpdateScreenState {
        CHECKING, UP_TO_DATE, AVAILABLE, DOWNLOADING, INSTALLING, ERROR
    }

    private lateinit var imgUpdateIcon: View
    private lateinit var progressThin: ProgressBar
    private lateinit var txtTitle: TextView
    private lateinit var txtSubtitle1: TextView
    private lateinit var txtSubtitle2: TextView
    private lateinit var dividerAfterPrimary: View
    private lateinit var itemPrimary: View
    private lateinit var txtPrimaryLabel: TextView
    private lateinit var itemSecondary: View
    private lateinit var txtSecondaryLabel: TextView

    private var pendingApkUrl: String? = null
    private var pendingRemoteVersion: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.SettingsTheme)
        setContentView(R.layout.activity_update)

        bindViews()
        render(UpdateScreenState.CHECKING)
        startCheck()
    }

    private fun bindViews() {
        findViewById<android.widget.ImageButton>(R.id.btnUpdateBack).setOnClickListener { finish() }

        imgUpdateIcon = findViewById(R.id.imgUpdateIcon)
        progressThin = findViewById(R.id.progressThin)
        txtTitle = findViewById(R.id.txtUpdateTitle)
        txtSubtitle1 = findViewById(R.id.txtUpdateSubtitle1)
        txtSubtitle2 = findViewById(R.id.txtUpdateSubtitle2)
        dividerAfterPrimary = findViewById(R.id.dividerAfterPrimary)
        itemPrimary = findViewById(R.id.itemUpdatePrimary)
        txtPrimaryLabel = findViewById(R.id.txtUpdatePrimaryLabel)
        itemSecondary = findViewById(R.id.itemUpdateSecondary)
        txtSecondaryLabel = findViewById(R.id.txtUpdateSecondaryLabel)
    }

    /** "Versión instalada: 2008.4.5.0" — primera línea de subtítulo, siempre visible. */
    private fun installedVersionLine(): String {
        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "?"
        } catch (e: Exception) {
            "?"
        }
        return "Versión instalada: $versionName"
    }

    // ── Paso 1: consultar GitHub ─────────────────────────────────────────────
    private fun startCheck() {
        render(UpdateScreenState.CHECKING)
        AppUpdater.checkForUpdate(this, object : AppUpdater.CheckCallback {
            override fun onUpToDate() {
                render(UpdateScreenState.UP_TO_DATE)
            }

            override fun onUpdateAvailable(remoteVersion: String, apkUrl: String, releaseNotesUrl: String) {
                pendingRemoteVersion = remoteVersion
                pendingApkUrl = apkUrl
                render(UpdateScreenState.AVAILABLE)
            }

            override fun onError(message: String) {
                txtSubtitle2.text = message
                render(UpdateScreenState.ERROR)
            }
        })
    }

    // ── Paso 2: descargar (OkHttp) + instalar ────────────────────────────────
    private fun startDownload() {
        val apkUrl = pendingApkUrl ?: return
        render(UpdateScreenState.DOWNLOADING)

        AppUpdater.downloadAndInstall(
            context = this,
            apkUrl = apkUrl,
            onProgress = { percent, bytesDownloaded, bytesTotal ->
                if (percent >= 0) {
                    progressThin.isIndeterminate = false
                    progressThin.progress = percent
                    txtSubtitle2.text = "$percent% – ${formatBytesProgress(bytesDownloaded, bytesTotal)}"
                } else {
                    // Sin Content-Length no se puede calcular %: barra indeterminada.
                    progressThin.isIndeterminate = true
                    txtSubtitle2.text = "Descargando… ${formatBytesProgress(bytesDownloaded, bytesTotal)}"
                }
            },
            onCompleted = {
                render(UpdateScreenState.INSTALLING)
            },
            onFailed = { message ->
                txtSubtitle2.text = message
                render(UpdateScreenState.ERROR)
            }
        )
    }

    /** Formatea "12.3 MB de 45.0 MB" (o solo "12.3 MB descargados" si no hay total). */
    private fun formatBytesProgress(downloaded: Long, total: Long): String {
        val downloadedMb = downloaded / (1024.0 * 1024.0)
        return if (total > 0) {
            val totalMb = total / (1024.0 * 1024.0)
            String.format("%.1f MB de %.1f MB", downloadedMb, totalMb)
        } else {
            String.format("%.1f MB descargados", downloadedMb)
        }
    }

    // ── Render de estados ────────────────────────────────────────────────────
    private fun render(state: UpdateScreenState) {
        // Por defecto todo oculto; cada estado prende lo que necesita.
        progressThin.visibility = View.GONE
        itemPrimary.visibility = View.GONE
        itemSecondary.visibility = View.GONE
        dividerAfterPrimary.visibility = View.GONE
        txtSubtitle1.text = installedVersionLine()

        when (state) {
            UpdateScreenState.CHECKING -> {
                txtTitle.text = "Buscando actualizaciones…"
                txtSubtitle2.text = "Consultando GitHub, un momento."
                progressThin.isIndeterminate = true
                progressThin.visibility = View.VISIBLE
                // Sin renglón de acción mientras se busca — igual que la
                // pantalla nativa de Android, que oculta "Comprobar
                // actualizaciones" mientras la comprobación está en curso.
            }

            UpdateScreenState.UP_TO_DATE -> {
                txtTitle.text = "Tu app está actualizada"
                txtSubtitle2.text = "Tenés instalada la última versión disponible."
                txtPrimaryLabel.text = "Comprobar actualizaciones"
                itemPrimary.setOnClickListener { startCheck() }
                itemPrimary.visibility = View.VISIBLE
            }

            UpdateScreenState.AVAILABLE -> {
                val version = pendingRemoteVersion.orEmpty()
                txtTitle.text = "Actualización disponible"
                txtSubtitle2.text = "Nueva versión disponible: $version"
                txtPrimaryLabel.text = "Descargar actualización"
                itemPrimary.setOnClickListener { startDownload() }
                itemPrimary.visibility = View.VISIBLE
                dividerAfterPrimary.visibility = View.VISIBLE
                txtSecondaryLabel.text = "Más tarde"
                itemSecondary.setOnClickListener { finish() }
                itemSecondary.visibility = View.VISIBLE
            }

            UpdateScreenState.DOWNLOADING -> {
                txtTitle.text = "Descargando actualización…"
                txtSubtitle2.text = "0% – calculando…"
                progressThin.isIndeterminate = false
                progressThin.progress = 0
                progressThin.visibility = View.VISIBLE
                // Sin renglón de acción mientras descarga: no cerrar la
                // pantalla hasta que termine.
            }

            UpdateScreenState.INSTALLING -> {
                txtTitle.text = "Descarga completa"
                txtSubtitle2.text = "Se abrió el instalador del sistema. Seguí los pasos en esa pantalla para terminar."
                txtPrimaryLabel.text = "Cerrar"
                itemPrimary.setOnClickListener { finish() }
                itemPrimary.visibility = View.VISIBLE
            }

            UpdateScreenState.ERROR -> {
                txtTitle.text = "No se pudo completar"
                txtPrimaryLabel.text = "Reintentar"
                itemPrimary.setOnClickListener { startCheck() }
                itemPrimary.visibility = View.VISIBLE
                dividerAfterPrimary.visibility = View.VISIBLE
                txtSecondaryLabel.text = "Volver"
                itemSecondary.setOnClickListener { finish() }
                itemSecondary.visibility = View.VISIBLE
            }
        }
    }
}
