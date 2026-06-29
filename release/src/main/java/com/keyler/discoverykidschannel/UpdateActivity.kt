/*
 ESTO TIENE LICENCIA GNU LICENSE APACHE

 NO ELIMINAR NINGUN COMENTARIO AGREGADO
*/
package com.keyler.discoverykidschannel

import android.os.Bundle
import android.view.View
import android.widget.Button
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
 *   1. CHECKING     — "Buscando actualizaciones…" + ProgressBar indeterminada.
 *   2. UP_TO_DATE    — mensaje + botón "Volver".
 *   3. AVAILABLE     — versión encontrada + botones "Descargar" / "Más tarde".
 *   4. DOWNLOADING   — ProgressBar determinada (0–100%) + porcentaje en texto.
 *   5. INSTALLING    — se abrió el instalador del sistema (Intent ACTION_VIEW);
 *                      esta Activity se queda en un estado neutro de fondo,
 *                      el usuario confirma la instalación fuera de la app.
 *   6. ERROR         — mensaje de error + botones "Reintentar" / "Volver".
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
 */
class UpdateActivity : AppCompatActivity() {

    private enum class UpdateScreenState {
        CHECKING, UP_TO_DATE, AVAILABLE, DOWNLOADING, INSTALLING, ERROR
    }

    private lateinit var progressIndeterminate: ProgressBar
    private lateinit var progressDownload: ProgressBar
    private lateinit var txtTitle: TextView
    private lateinit var txtMessage: TextView
    private lateinit var txtPercent: TextView
    private lateinit var btnPrimary: Button
    private lateinit var btnSecondary: Button

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

        progressIndeterminate = findViewById(R.id.progressIndeterminate)
        progressDownload = findViewById(R.id.progressDownload)
        txtTitle = findViewById(R.id.txtUpdateTitle)
        txtMessage = findViewById(R.id.txtUpdateMessage)
        txtPercent = findViewById(R.id.txtUpdatePercent)
        btnPrimary = findViewById(R.id.btnUpdatePrimary)
        btnSecondary = findViewById(R.id.btnUpdateSecondary)
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
                txtMessage.text = message
                render(UpdateScreenState.ERROR)
            }
        })
    }

    // ── Paso 2: descargar + instalar ─────────────────────────────────────────
    private fun startDownload() {
        val apkUrl = pendingApkUrl ?: return
        render(UpdateScreenState.DOWNLOADING)

        AppUpdater.downloadAndInstall(
            context = this,
            apkUrl = apkUrl,
            onProgress = { percent ->
                progressDownload.progress = percent
                txtPercent.text = "$percent%"
            },
            onCompleted = {
                render(UpdateScreenState.INSTALLING)
            },
            onFailed = { message ->
                txtMessage.text = message
                render(UpdateScreenState.ERROR)
            }
        )
    }

    // ── Render de estados ────────────────────────────────────────────────────
    private fun render(state: UpdateScreenState) {
        // Por defecto todo oculto; cada estado prende lo que necesita.
        progressIndeterminate.visibility = View.GONE
        progressDownload.visibility = View.GONE
        txtPercent.visibility = View.GONE
        btnPrimary.visibility = View.GONE
        btnSecondary.visibility = View.GONE

        when (state) {
            UpdateScreenState.CHECKING -> {
                txtTitle.text = "Buscando actualizaciones…"
                txtMessage.text = "Consultando GitHub, un momento."
                progressIndeterminate.visibility = View.VISIBLE
            }

            UpdateScreenState.UP_TO_DATE -> {
                txtTitle.text = "Ya estás al día"
                txtMessage.text = "Tenés instalada la última versión disponible."
                btnPrimary.text = "Volver"
                btnPrimary.setOnClickListener { finish() }
                btnPrimary.visibility = View.VISIBLE
            }

            UpdateScreenState.AVAILABLE -> {
                val version = pendingRemoteVersion.orEmpty()
                txtTitle.text = "Actualización disponible"
                txtMessage.text = "Hay una nueva versión disponible: $version\n\n¿Querés descargarla e instalarla ahora?"
                btnPrimary.text = "Descargar"
                btnPrimary.setOnClickListener { startDownload() }
                btnSecondary.text = "Más tarde"
                btnSecondary.setOnClickListener { finish() }
                btnPrimary.visibility = View.VISIBLE
                btnSecondary.visibility = View.VISIBLE
            }

            UpdateScreenState.DOWNLOADING -> {
                txtTitle.text = "Descargando…"
                txtMessage.text = "No cierres esta pantalla hasta que termine."
                progressDownload.progress = 0
                progressDownload.visibility = View.VISIBLE
                txtPercent.text = "0%"
                txtPercent.visibility = View.VISIBLE
            }

            UpdateScreenState.INSTALLING -> {
                txtTitle.text = "Descarga completa"
                txtMessage.text = "Se abrió el instalador del sistema. Seguí los pasos en esa pantalla para terminar de instalar la actualización."
                btnPrimary.text = "Cerrar"
                btnPrimary.setOnClickListener { finish() }
                btnPrimary.visibility = View.VISIBLE
            }

            UpdateScreenState.ERROR -> {
                txtTitle.text = "No se pudo completar"
                btnPrimary.text = "Reintentar"
                btnPrimary.setOnClickListener { startCheck() }
                btnSecondary.text = "Volver"
                btnSecondary.setOnClickListener { finish() }
                btnPrimary.visibility = View.VISIBLE
                btnSecondary.visibility = View.VISIBLE
            }
        }
    }
}
