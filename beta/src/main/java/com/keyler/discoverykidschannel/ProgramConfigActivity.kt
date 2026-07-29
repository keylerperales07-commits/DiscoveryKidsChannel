/*
 ESTO TIENE LICENCIA GNU LICENSE APACHE

 NO ELIMINAR NINGUN COMENTARIO AGREGADO
*/
package com.keyler.discoverykidschannel

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

/**
 * ProgramConfigActivity — Release 5.5.0
 *
 * Extraída de DiscoveryKidsLauncherActivity: antes toda esta pantalla
 * ("Programas": cantidad, video de cada uno, ya_regresa/continuamos/Intro/
 * Créditos/NextProgram personalizados) vivía inline en activity_launcher.xml.
 * Pasa a ser su propia Activity, accesible desde un botón en el Launcher —
 * el resto del comportamiento (SAF, persistencia de permisos de lectura,
 * etc.) es exactamente el mismo, solo movido de archivo.
 *
 * NUEVO en esta Release:
 *   - NextProgram personalizado por programa (imagen/GIF propio en vez del
 *     nextprogramN.gif de fábrica).
 *   - Sección "ScreenBugs de eventos": activar/desactivar Navidad, Año
 *     Nuevo, Pascua y Día de la Tierra (global, no por programa).
 *
 * Igual que el resto de la configuración "experimental", esta Activity
 * asume que ya se llegó acá desde un lugar que confirmó
 * SettingsManager.isExperimentalEnabled() — no repite ese chequeo (a
 * diferencia de DiscoveryKidsLauncherActivity, que sí es la puerta de
 * entrada real de la app).
 */
class ProgramConfigActivity : AppCompatActivity() {

    private enum class PickTarget { PROGRAM, YA_REGRESA, CONTINUAMOS, INTRO, CREDITOS, NEXTPROGRAM }

    private var pendingPickIndex = -1
    private var pendingPickTarget: PickTarget? = null

    private lateinit var containerPrograms: LinearLayout
    private lateinit var txtProgramCountValue: TextView

    /**
     * SAF: selector de archivos del sistema. El tipo MIME varía según qué
     * botón lo disparó — video para Programa/ya_regresa/continuamos/Intro/
     * Créditos, image para NextProgram (imagen o GIF, ver
     * PickTarget.NEXTPROGRAM más abajo, en el setOnClickListener que lanza esto).
     */
    private val pickVideoLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) handlePickedVideo(uri)
        pendingPickIndex = -1
        pendingPickTarget = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_program_config)

        supportActionBar?.apply {
            title = "Configuración de Programa"
        }

        containerPrograms = findViewById(R.id.containerPrograms)
        txtProgramCountValue = findViewById(R.id.txtProgramCountValue)
        findViewById<LinearLayout>(R.id.itemProgramCount).setOnClickListener { showProgramCountDialog() }

        bindEventSwitches()
        refreshProgramCountLabel()
        rebuildProgramList()
    }

    // ── ScreenBugs de eventos (Release 5.5.0) — global, no por programa ──────

    private fun bindEventSwitches() {
        val switchNavidad = findViewById<SwitchCompat>(R.id.switchEventNavidad)
        switchNavidad.isChecked = SettingsManager.isNavidadScreenBugEnabled(this)
        switchNavidad.setOnCheckedChangeListener { _, checked ->
            SettingsManager.setNavidadScreenBugEnabled(this, checked)
        }

        val switchAnioNuevo = findViewById<SwitchCompat>(R.id.switchEventAnioNuevo)
        switchAnioNuevo.isChecked = SettingsManager.isAnoNuevoScreenBugEnabled(this)
        switchAnioNuevo.setOnCheckedChangeListener { _, checked ->
            SettingsManager.setAnoNuevoScreenBugEnabled(this, checked)
        }

        val switchPascua = findViewById<SwitchCompat>(R.id.switchEventPascua)
        switchPascua.isChecked = SettingsManager.isPascuaScreenBugEnabled(this)
        switchPascua.setOnCheckedChangeListener { _, checked ->
            SettingsManager.setPascuaScreenBugEnabled(this, checked)
        }

        val switchTierra = findViewById<SwitchCompat>(R.id.switchEventTierra)
        switchTierra.isChecked = SettingsManager.isDiaTierraScreenBugEnabled(this)
        switchTierra.setOnCheckedChangeListener { _, checked ->
            SettingsManager.setDiaTierraScreenBugEnabled(this, checked)
        }
    }

    // ── Cantidad de programas (1–24) ─────────────────────────────────────────

    private fun refreshProgramCountLabel() {
        val count = SettingsManager.getProgramCount(this)
        txtProgramCountValue.text =
            "$count de ${SettingsManager.MAX_PROGRAM_COUNT} programas (Predeterminado: ${SettingsManager.DEFAULT_PROGRAM_COUNT})"
    }

    private fun showProgramCountDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(SettingsManager.getProgramCount(this@ProgramConfigActivity).toString())
            setSelection(text.length)
        }
        val pad = (16 * resources.displayMetrics.density).toInt()
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, 0, pad, 0)
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle("Cantidad de programas")
            .setMessage("¿Cuántos programas (videos) querés que arme la programación? (${SettingsManager.MIN_PROGRAM_COUNT}–${SettingsManager.MAX_PROGRAM_COUNT}, Predeterminado: ${SettingsManager.DEFAULT_PROGRAM_COUNT})")
            .setView(container)
            .setPositiveButton("Guardar") { _, _ ->
                val count = input.text.toString().toIntOrNull() ?: SettingsManager.DEFAULT_PROGRAM_COUNT
                SettingsManager.setProgramCount(this, count)
                refreshProgramCountLabel()
                rebuildProgramList()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ── Filas de programas ───────────────────────────────────────────────────

    /**
     * Reconstruye desde cero la lista de filas (Programa 1..N). Se llama al
     * entrar, al cambiar la cantidad de programas, y después de elegir
     * cualquier video — es más simple y menos propenso a errores que tratar
     * de actualizar una sola fila a mano.
     */
    private fun rebuildProgramList() {
        containerPrograms.removeAllViews()
        val count = SettingsManager.getProgramCount(this)
        val inflater = LayoutInflater.from(this)
        for (index in 0 until count) {
            val row = inflater.inflate(R.layout.item_program_config, containerPrograms, false)
            bindProgramRow(row, index)
            containerPrograms.addView(row)
        }
    }

    private fun bindProgramRow(row: View, index: Int) {
        val txtTitle = row.findViewById<TextView>(R.id.txtProgramTitle)
        val txtVideoStatus = row.findViewById<TextView>(R.id.txtProgramVideoStatus)
        val btnPickVideo = row.findViewById<LinearLayout>(R.id.btnPickProgramVideo)
        val switchYaRegresa = row.findViewById<SwitchCompat>(R.id.switchYaRegresaCustom)
        val btnPickYaRegresa = row.findViewById<LinearLayout>(R.id.btnPickYaRegresaVideo)
        val switchContinuamos = row.findViewById<SwitchCompat>(R.id.switchContinuamosCustom)
        val btnPickContinuamos = row.findViewById<LinearLayout>(R.id.btnPickContinuamosVideo)

        txtTitle.text = "Programa ${index + 1}"

        val savedUri = SettingsManager.getProgramUri(this, index)
        txtVideoStatus.text = if (savedUri.isNullOrBlank()) {
            "Sin video elegido — usa pro${index + 1}.mp4 en Videos"
        } else {
            "Video elegido: ${displayNameFor(Uri.parse(savedUri))}"
        }

        btnPickVideo.setOnClickListener {
            pendingPickIndex = index
            pendingPickTarget = PickTarget.PROGRAM
            pickVideoLauncher.launch(arrayOf("video/*"))
        }

        switchYaRegresa.isChecked = SettingsManager.isYaRegresaCustom(this, index)
        btnPickYaRegresa.visibility = if (switchYaRegresa.isChecked) View.VISIBLE else View.GONE
        switchYaRegresa.setOnCheckedChangeListener { _, checked ->
            SettingsManager.setYaRegresaCustom(this, index, checked)
            btnPickYaRegresa.visibility = if (checked) View.VISIBLE else View.GONE
        }
        btnPickYaRegresa.setOnClickListener {
            pendingPickIndex = index
            pendingPickTarget = PickTarget.YA_REGRESA
            pickVideoLauncher.launch(arrayOf("video/*"))
        }

        switchContinuamos.isChecked = SettingsManager.isContinuamosCustom(this, index)
        btnPickContinuamos.visibility = if (switchContinuamos.isChecked) View.VISIBLE else View.GONE
        switchContinuamos.setOnCheckedChangeListener { _, checked ->
            SettingsManager.setContinuamosCustom(this, index, checked)
            btnPickContinuamos.visibility = if (checked) View.VISIBLE else View.GONE
        }
        btnPickContinuamos.setOnClickListener {
            pendingPickIndex = index
            pendingPickTarget = PickTarget.CONTINUAMOS
            pickVideoLauncher.launch(arrayOf("video/*"))
        }

        // ── Intro / Créditos (Release 5.4.0) — sin video predeterminado: el
        // switch activa/desactiva, y el texto de estado siempre está visible
        // mientras esté activado (a diferencia de ya_regresa/continuamos, acá
        // SÍ importa que el usuario vea claramente si ya eligió un video o
        // no, porque si no elige uno el clip simplemente no aparece).
        val txtIntroStatus = row.findViewById<TextView>(R.id.txtIntroVideoStatus)
        val switchIntro = row.findViewById<SwitchCompat>(R.id.switchIntroEnabled)
        val btnPickIntro = row.findViewById<LinearLayout>(R.id.btnPickIntroVideo)

        fun refreshIntroStatus() {
            val uri = SettingsManager.getIntroUri(this, index)
            txtIntroStatus.text = if (uri.isNullOrBlank()) "Sin video elegido" else "Video elegido: ${displayNameFor(Uri.parse(uri))}"
        }
        switchIntro.isChecked = SettingsManager.isIntroEnabled(this, index)
        txtIntroStatus.visibility = if (switchIntro.isChecked) View.VISIBLE else View.GONE
        btnPickIntro.visibility = if (switchIntro.isChecked) View.VISIBLE else View.GONE
        refreshIntroStatus()
        switchIntro.setOnCheckedChangeListener { _, checked ->
            SettingsManager.setIntroEnabled(this, index, checked)
            txtIntroStatus.visibility = if (checked) View.VISIBLE else View.GONE
            btnPickIntro.visibility = if (checked) View.VISIBLE else View.GONE
        }
        btnPickIntro.setOnClickListener {
            pendingPickIndex = index
            pendingPickTarget = PickTarget.INTRO
            pickVideoLauncher.launch(arrayOf("video/*"))
        }

        val txtCreditosStatus = row.findViewById<TextView>(R.id.txtCreditosVideoStatus)
        val switchCreditos = row.findViewById<SwitchCompat>(R.id.switchCreditosEnabled)
        val btnPickCreditos = row.findViewById<LinearLayout>(R.id.btnPickCreditosVideo)

        fun refreshCreditosStatus() {
            val uri = SettingsManager.getCreditosUri(this, index)
            txtCreditosStatus.text = if (uri.isNullOrBlank()) "Sin video elegido" else "Video elegido: ${displayNameFor(Uri.parse(uri))}"
        }
        switchCreditos.isChecked = SettingsManager.isCreditosEnabled(this, index)
        txtCreditosStatus.visibility = if (switchCreditos.isChecked) View.VISIBLE else View.GONE
        btnPickCreditos.visibility = if (switchCreditos.isChecked) View.VISIBLE else View.GONE
        refreshCreditosStatus()
        switchCreditos.setOnCheckedChangeListener { _, checked ->
            SettingsManager.setCreditosEnabled(this, index, checked)
            txtCreditosStatus.visibility = if (checked) View.VISIBLE else View.GONE
            btnPickCreditos.visibility = if (checked) View.VISIBLE else View.GONE
        }
        btnPickCreditos.setOnClickListener {
            pendingPickIndex = index
            pendingPickTarget = PickTarget.CREDITOS
            pickVideoLauncher.launch(arrayOf("video/*"))
        }

        // ── NextProgram personalizado (Release 5.5.0) — SÍ tiene default de
        // fábrica (nextprogramN.gif), por eso sigue el patrón "personalizado"
        // de ya_regresa/continuamos en vez del de Intro/Créditos.
        val switchNextProgram = row.findViewById<SwitchCompat>(R.id.switchNextProgramCustom)
        val btnPickNextProgram = row.findViewById<LinearLayout>(R.id.btnPickNextProgramImage)

        switchNextProgram.isChecked = SettingsManager.isNextProgramCustom(this, index)
        btnPickNextProgram.visibility = if (switchNextProgram.isChecked) View.VISIBLE else View.GONE
        switchNextProgram.setOnCheckedChangeListener { _, checked ->
            SettingsManager.setNextProgramCustom(this, index, checked)
            btnPickNextProgram.visibility = if (checked) View.VISIBLE else View.GONE
        }
        btnPickNextProgram.setOnClickListener {
            pendingPickIndex = index
            pendingPickTarget = PickTarget.NEXTPROGRAM
            // image/* — a diferencia de todo lo demás en esta pantalla (que
            // son videos), NextProgram personalizado es una imagen o GIF.
            pickVideoLauncher.launch(arrayOf("image/*"))
        }
    }

    /**
     * Se llama cuando el usuario eligió un archivo en el selector del
     * sistema. Persiste el permiso de lectura (para que la Uri no expire al
     * cerrar la app) y lo guarda en SettingsManager según qué botón lo
     * disparó (pendingPickTarget/pendingPickIndex, fijados justo antes de
     * lanzar el selector).
     */
    private fun handlePickedVideo(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
            Log.w("ProgramConfig", "No se pudo persistir el permiso de lectura de $uri", e)
        }

        val index = pendingPickIndex
        if (index < 0) return

        when (pendingPickTarget) {
            PickTarget.PROGRAM -> SettingsManager.setProgramUri(this, index, uri.toString())
            PickTarget.YA_REGRESA -> SettingsManager.setYaRegresaUri(this, index, uri.toString())
            PickTarget.CONTINUAMOS -> SettingsManager.setContinuamosUri(this, index, uri.toString())
            PickTarget.INTRO -> SettingsManager.setIntroUri(this, index, uri.toString())
            PickTarget.CREDITOS -> SettingsManager.setCreditosUri(this, index, uri.toString())
            PickTarget.NEXTPROGRAM -> SettingsManager.setNextProgramUri(this, index, uri.toString())
            null -> return
        }

        rebuildProgramList()
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
