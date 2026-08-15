/*
 ESTO TIENE LICENCIA GNU LICENSE APACHE

 NO ELIMINAR NINGUN COMENTARIO AGREGADO
*/
package com.keyler.discoverykidshd

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

/**
 * ProgramConfigActivity — Release 5.5.0, reescrita en la 5.6.0
 *
 * BUG FIX (malentendido de arquitectura, revertido): la 5.5.0 había
 * convertido esta pantalla en una lista de TODOS los programas (cantidad +
 * video de cada uno) más los ScreenBugs de eventos (global) — equivocado.
 * "Los programas" (cantidad, video de cada uno) viven en el Launcher
 * (DiscoveryKidsLauncherActivity) — siempre vivieron ahí, no correspondía
 * moverlos. Los ScreenBugs de eventos (globales, no de un programa en
 * particular) también se movieron de vuelta al Launcher.
 *
 * Ahora esta Activity es SOLO las OPCIONES de UN programa puntual —
 * recibe [EXTRA_PROGRAM_INDEX] por Intent (obligatorio; si falta, se cierra
 * sola) desde el botón "⚙️ Opciones" de la fila de ESE programa en el
 * Launcher. Cada programa tiene su propia configuración, completamente
 * independiente de la de los demás — todo acá abajo se lee/guarda con el
 * MISMO [programIndex] en SettingsManager, así que cambiar algo para el
 * Programa 1 nunca toca la configuración del Programa 2 ni de ningún otro.
 *
 * Contenido:
 *   - Activar comerciales (Release 5.6.0, NUEVO — Predeterminado: activado).
 *   - continuamos personalizado.
 *   - Intro / Créditos (sin default de fábrica).
 *   - A continuación personalizado (antes "NextProgram personalizado" —
 *     renombrado en la 5.6.0).
 *
 * Preview 2013.6.0.0.2 — ELIMINADO por completo: ya_regresa personalizado
 * (PickTarget.YA_REGRESA y el switch/botón correspondientes). El clip
 * "ya_regresa" (pre-comercial) se eliminó por completo del canal — ver
 * LiveDiscoveryKids.kt.
 *
 * RELEASE 2013.6.0.0 — "Funciones experimentales" se eliminó por completo
 * (ver SettingsManager.kt/DiscoveryKidsLauncherActivity.kt): esta Activity
 * ya no depende de ningún chequeo previo, se accede siempre desde el botón
 * "⚙️ Opciones" de cada fila de programa en Discovery Kids Launcher.
 */
class ProgramConfigActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PROGRAM_INDEX = "program_index"
    }

    private enum class PickTarget { CONTINUAMOS, INTRO, CREDITOS, NEXTPROGRAM, EPISODE }

    private var programIndex = -1
    private var pendingPickTarget: PickTarget? = null
    // Release 5.8.0 — qué episodio (0-based) se está por elegir/reemplazar,
    // solo válido cuando pendingPickTarget == PickTarget.EPISODE.
    private var pendingEpisodeIndex = -1

    /**
     * SAF: selector de archivos del sistema. El tipo MIME varía según qué
     * botón lo disparó — video para continuamos/Intro/Créditos,
     * image para A continuación (imagen o GIF, ver PickTarget.NEXTPROGRAM
     * más abajo, en el setOnClickListener que lanza esto).
     */
    private val pickVideoLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) handlePickedVideo(uri)
        pendingPickTarget = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        programIndex = intent.getIntExtra(EXTRA_PROGRAM_INDEX, -1)
        if (programIndex < 0) {
            // No debería pasar nunca (siempre se llega acá desde la fila de
            // un programa puntual en el Launcher, que siempre manda el
            // índice) — pero ante la duda, no mostrar una pantalla de
            // "opciones de ningún programa" sin sentido.
            Log.e("ProgramConfig", "Falta EXTRA_PROGRAM_INDEX, cerrando")
            finish()
            return
        }

        setContentView(R.layout.activity_program_config)

        supportActionBar?.apply {
            title = "Programa ${programIndex + 1} — Opciones"
        }

        // Release 5.8.0 — BUG FIX ("el ActionBar se come una parte del
        // Layout"), ver mismo comentario en DiscoveryKidsLauncherActivity.onCreate().
        val programConfigRoot = findViewById<View>(R.id.programConfigRoot)
        val rootPaddingLeft = programConfigRoot.paddingLeft
        val rootPaddingRight = programConfigRoot.paddingRight
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(programConfigRoot) { view, insets ->
            val bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            view.setPadding(rootPaddingLeft, bars.top, rootPaddingRight, bars.bottom)
            insets
        }

        bindEpisodes()
        bindCommercialsSwitch()
        bindProgramOptions()
    }

    // ── Episodios (Release 5.8.0, NUEVO) ─────────────────────────────────────
    // Un programa puede tener varios videos (episodios) en vez de uno solo.
    // El episodio 0 usa el MISMO storage de siempre (SettingsManager.
    // getProgramUri/setProgramUri) — un programa clásico de un solo video
    // sigue funcionando exactamente igual, como "1 episodio". Los episodios
    // 1+ son un concepto nuevo, sin equivalente en la Era clásica.

    private fun bindEpisodes() {
        val container = findViewById<LinearLayout>(R.id.containerEpisodes)
        val btnAdd = findViewById<TextView>(R.id.btnAddEpisode)

        renderEpisodeRows(container)

        btnAdd.setOnClickListener {
            val count = SettingsManager.getEpisodeCount(this, programIndex)
            SettingsManager.setEpisodeCount(this, programIndex, count + 1)
            renderEpisodeRows(container)
        }
    }

    private fun renderEpisodeRows(container: LinearLayout) {
        container.removeAllViews()
        val count = SettingsManager.getEpisodeCount(this, programIndex)
        val inflater = layoutInflater
        for (episodeIndex in 0 until count) {
            val row = inflater.inflate(R.layout.item_episode_row, container, false)
            val txtLabel = row.findViewById<TextView>(R.id.txtEpisodeLabel)
            val txtStatus = row.findViewById<TextView>(R.id.txtEpisodeStatus)
            val btnRemove = row.findViewById<TextView>(R.id.btnRemoveEpisode)

            txtLabel.text = "Episodio ${episodeIndex + 1}"
            val uri = SettingsManager.getEpisodeUri(this, programIndex, episodeIndex)
            txtStatus.text = if (uri.isNullOrBlank()) {
                if (episodeIndex == 0) "pro${programIndex + 1}.mp4 (o elegí uno)" else "Sin video elegido"
            } else {
                displayNameFor(Uri.parse(uri))
            }

            row.setOnClickListener {
                pendingPickTarget = PickTarget.EPISODE
                pendingEpisodeIndex = episodeIndex
                pickVideoLauncher.launch(arrayOf("video/*"))
            }

            // El episodio 0 siempre existe — un programa nunca queda con 0
            // episodios. Los demás se pueden quitar.
            if (episodeIndex == 0) {
                btnRemove.visibility = View.GONE
            } else {
                btnRemove.visibility = View.VISIBLE
                btnRemove.setOnClickListener {
                    // Recorre los episodios de ESTE en adelante, corriendo
                    // cada Uri un lugar hacia atrás, para no dejar un
                    // "hueco" en el medio de la lista.
                    val currentCount = SettingsManager.getEpisodeCount(this, programIndex)
                    for (i in episodeIndex until currentCount - 1) {
                        val nextUri = SettingsManager.getEpisodeUri(this, programIndex, i + 1)
                        SettingsManager.setEpisodeUri(this, programIndex, i, nextUri)
                    }
                    SettingsManager.removeEpisodeUri(this, programIndex, currentCount - 1)
                    SettingsManager.setEpisodeCount(this, programIndex, (currentCount - 1).coerceAtLeast(1))
                    renderEpisodeRows(container)
                }
            }

            container.addView(row)
        }
    }

    /**
     * BUG FIX/NUEVO (Release 5.8.0) — "el programa tiene que ser mínimo 1
     * minuto": valida la duración real del video ANTES de aceptarlo como
     * episodio, usando MediaMetadataRetriever. Si dura menos de 1 minuto,
     * se rechaza con un aviso claro — el episodio se queda con lo que tenía
     * antes (o vacío, si era nuevo).
     */
    private fun isVideoAtLeastOneMinute(uri: Uri): Boolean {
        val retriever = android.media.MediaMetadataRetriever()
        return try {
            retriever.setDataSource(this, uri)
            val durationMs = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            durationMs >= 60_000L
        } catch (e: Exception) {
            Log.w("ProgramConfig", "No se pudo leer la duración de $uri, se deja pasar", e)
            true   // si no se puede determinar la duración, no bloqueamos al usuario por las dudas
        } finally {
            try { retriever.release() } catch (e: Exception) { /* no-op */ }
        }
    }

    // ── Activar comerciales (Release 5.6.0, NUEVO) ───────────────────────────

    private fun bindCommercialsSwitch() {
        val switchCommercials = findViewById<SwitchCompat>(R.id.switchCommercialsEnabled)
        switchCommercials.isChecked = SettingsManager.isCommercialsEnabled(this, programIndex)
        switchCommercials.setOnCheckedChangeListener { _, checked ->
            SettingsManager.setCommercialsEnabled(this, programIndex, checked)
        }
    }

    // ── Opciones del programa (continuamos, Intro, Créditos, A continuación) ──
    // Preview 2013.6.0.0.2 — ya_regresa personalizado ELIMINADO por completo.

    private fun bindProgramOptions() {
        val switchContinuamos = findViewById<SwitchCompat>(R.id.switchContinuamosCustom)
        val btnPickContinuamos = findViewById<LinearLayout>(R.id.btnPickContinuamosVideo)

        switchContinuamos.isChecked = SettingsManager.isContinuamosCustom(this, programIndex)
        btnPickContinuamos.visibility = if (switchContinuamos.isChecked) View.VISIBLE else View.GONE
        switchContinuamos.setOnCheckedChangeListener { _, checked ->
            SettingsManager.setContinuamosCustom(this, programIndex, checked)
            btnPickContinuamos.visibility = if (checked) View.VISIBLE else View.GONE
        }
        btnPickContinuamos.setOnClickListener {
            pendingPickTarget = PickTarget.CONTINUAMOS
            pickVideoLauncher.launch(arrayOf("video/*"))
        }

        // ── Intro / Créditos (Release 5.4.0) — sin video predeterminado: el
        // switch activa/desactiva, y el texto de estado siempre está visible
        // mientras esté activado (a diferencia de continuamos, acá
        // SÍ importa que el usuario vea claramente si ya eligió un video o
        // no, porque si no elige uno el clip simplemente no aparece).
        val txtIntroStatus = findViewById<TextView>(R.id.txtIntroVideoStatus)
        val switchIntro = findViewById<SwitchCompat>(R.id.switchIntroEnabled)
        val btnPickIntro = findViewById<LinearLayout>(R.id.btnPickIntroVideo)

        fun refreshIntroStatus() {
            val uri = SettingsManager.getIntroUri(this, programIndex)
            txtIntroStatus.text = if (uri.isNullOrBlank()) "Sin video elegido" else "Video elegido: ${displayNameFor(Uri.parse(uri))}"
        }
        switchIntro.isChecked = SettingsManager.isIntroEnabled(this, programIndex)
        txtIntroStatus.visibility = if (switchIntro.isChecked) View.VISIBLE else View.GONE
        btnPickIntro.visibility = if (switchIntro.isChecked) View.VISIBLE else View.GONE
        refreshIntroStatus()
        switchIntro.setOnCheckedChangeListener { _, checked ->
            SettingsManager.setIntroEnabled(this, programIndex, checked)
            txtIntroStatus.visibility = if (checked) View.VISIBLE else View.GONE
            btnPickIntro.visibility = if (checked) View.VISIBLE else View.GONE
        }
        btnPickIntro.setOnClickListener {
            pendingPickTarget = PickTarget.INTRO
            pickVideoLauncher.launch(arrayOf("video/*"))
        }

        val txtCreditosStatus = findViewById<TextView>(R.id.txtCreditosVideoStatus)
        val switchCreditos = findViewById<SwitchCompat>(R.id.switchCreditosEnabled)
        val btnPickCreditos = findViewById<LinearLayout>(R.id.btnPickCreditosVideo)

        fun refreshCreditosStatus() {
            val uri = SettingsManager.getCreditosUri(this, programIndex)
            txtCreditosStatus.text = if (uri.isNullOrBlank()) "Sin video elegido" else "Video elegido: ${displayNameFor(Uri.parse(uri))}"
        }
        switchCreditos.isChecked = SettingsManager.isCreditosEnabled(this, programIndex)
        txtCreditosStatus.visibility = if (switchCreditos.isChecked) View.VISIBLE else View.GONE
        btnPickCreditos.visibility = if (switchCreditos.isChecked) View.VISIBLE else View.GONE
        refreshCreditosStatus()
        switchCreditos.setOnCheckedChangeListener { _, checked ->
            SettingsManager.setCreditosEnabled(this, programIndex, checked)
            txtCreditosStatus.visibility = if (checked) View.VISIBLE else View.GONE
            btnPickCreditos.visibility = if (checked) View.VISIBLE else View.GONE
        }
        btnPickCreditos.setOnClickListener {
            pendingPickTarget = PickTarget.CREDITOS
            pickVideoLauncher.launch(arrayOf("video/*"))
        }

        // ── A continuación personalizado (Release 5.5.0, renombrado en la
        // 5.6.0 — antes "NextProgram personalizado") — SÍ tiene default de
        // fábrica (nextprogramN.gif), por eso sigue el patrón "personalizado"
        // de continuamos en vez del de Intro/Créditos.
        val switchNextProgram = findViewById<SwitchCompat>(R.id.switchNextProgramCustom)
        val btnPickNextProgram = findViewById<LinearLayout>(R.id.btnPickNextProgramImage)

        switchNextProgram.isChecked = SettingsManager.isNextProgramCustom(this, programIndex)
        btnPickNextProgram.visibility = if (switchNextProgram.isChecked) View.VISIBLE else View.GONE
        switchNextProgram.setOnCheckedChangeListener { _, checked ->
            SettingsManager.setNextProgramCustom(this, programIndex, checked)
            btnPickNextProgram.visibility = if (checked) View.VISIBLE else View.GONE
        }
        btnPickNextProgram.setOnClickListener {
            pendingPickTarget = PickTarget.NEXTPROGRAM
            // image/* — a diferencia de todo lo demás en esta pantalla (que
            // son videos), A continuación personalizado es una imagen o GIF.
            pickVideoLauncher.launch(arrayOf("image/*"))
        }
    }

    /**
     * Se llama cuando el usuario eligió un archivo en el selector del
     * sistema. Persiste el permiso de lectura (para que la Uri no expire al
     * cerrar la app) y lo guarda en SettingsManager según qué botón lo
     * disparó (pendingPickTarget, fijado justo antes de lanzar el selector)
     * — siempre con [programIndex], el programa de ESTA pantalla.
     */
    private fun handlePickedVideo(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
            Log.w("ProgramConfig", "No se pudo persistir el permiso de lectura de $uri", e)
        }

        when (pendingPickTarget) {
            PickTarget.CONTINUAMOS -> SettingsManager.setContinuamosUri(this, programIndex, uri.toString())
            PickTarget.INTRO -> SettingsManager.setIntroUri(this, programIndex, uri.toString())
            PickTarget.CREDITOS -> SettingsManager.setCreditosUri(this, programIndex, uri.toString())
            PickTarget.NEXTPROGRAM -> SettingsManager.setNextProgramUri(this, programIndex, uri.toString())
            PickTarget.EPISODE -> {
                if (!isVideoAtLeastOneMinute(uri)) {
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Video muy corto")
                        .setMessage("Este programa tiene que durar al menos 1 minuto. Elegí otro video.")
                        .setPositiveButton("Entendido", null)
                        .show()
                    pendingPickTarget = null
                    pendingEpisodeIndex = -1
                    return
                }
                SettingsManager.setEpisodeUri(this, programIndex, pendingEpisodeIndex, uri.toString())
                pendingEpisodeIndex = -1
            }
            null -> return
        }

        // Recrear es más simple y menos propenso a errores que actualizar a
        // mano los textos de estado de cada sección.
        recreate()
    }

    /** Nombre legible del archivo elegido (vía SAF), con fallback al último segmento de la Uri. */
    private fun displayNameFor(uri: Uri): String {
        return try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: uri.lastPathSegment ?: "archivo"
        } catch (e: Exception) {
            uri.lastPathSegment ?: "archivo"
        }
    }
}
