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
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

/**
 * DiscoveryKidsLauncherActivity — Release 2009.5.0.0 ("Parque Imaginario")
 *
 * Reescritura completa. Antes (Release 4.6.0) era una pantalla secundaria,
 * accesible solo desde Configuración → "Elegir programas", con una lista
 * fija de 4 switches (activar/desactivar pro1–pro4.mp4).
 *
 * Ahora es la Activity de inicio REAL de la app — ver el intent-filter
 * MAIN/LAUNCHER en AndroidManifest.xml, que se movió acá desde
 * LiveDiscoveryKids — pero todo lo que implica (esta pantalla, elegir el
 * video de cada programa, personalizar ya_regresa/continuamos, elegir
 * cuántos programas armar la programación) vive detrás del interruptor
 * maestro "Habilitar funciones experimentales" de Configuración
 * (SettingsManager.isExperimentalEnabled(), desactivado por defecto):
 *
 *   - Experimental DESACTIVADO (default): onCreate() redirige de inmediato a
 *     LiveDiscoveryKids y hace finish(), sin mostrar nunca esta UI —
 *     comportamiento 100% idéntico al de abrir la app antes de esta Release.
 *   - Experimental ACTIVADO: se muestra el diseño nuevo (MenuBar con
 *     degradado azul→cian y logo, fuente dk_font) con:
 *       • Botón "Iniciar canal" → LiveDiscoveryKids.
 *       • Botón (ícono) "Configuración" → SettingsActivity.
 *       • Sección Programas: cantidad (1–24, SettingsManager.getProgramCount())
 *         y una fila por programa (item_program_config.xml, inflada en
 *         código porque puede haber hasta 24) donde el usuario:
 *           - elige el video del programa vía selector de archivos del
 *             sistema (SAF, ACTION_OPEN_DOCUMENT) — ya no hace falta
 *             renombrarlo a pro{N}.mp4 ni copiarlo a la carpeta Movies.
 *           - puede activar "ya_regresa personalizado" / "continuamos
 *             personalizado" y elegir un video propio para cada uno, en vez
 *             del que trae la app por defecto.
 *
 * Todas las Uri elegidas se persisten con
 * ContentResolver.takePersistableUriPermission() para seguir siendo válidas
 * entre reinicios de la app (SAF: sin esto, el permiso de lectura expira al
 * cerrar la app). LiveDiscoveryKids las consulta en resolveProgram(),
 * resolveYaRegresaUri() y resolveContinuamosUri().
 */
class DiscoveryKidsLauncherActivity : AppCompatActivity() {

    private enum class PickTarget { PROGRAM, YA_REGRESA, CONTINUAMOS }

    private var pendingPickIndex = -1
    private var pendingPickTarget: PickTarget? = null

    private lateinit var containerPrograms: LinearLayout
    private lateinit var txtProgramCountValue: TextView

    /** SAF: selector de archivos del sistema para elegir un video (programa, ya_regresa o continuamos). */
    private val pickVideoLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) handlePickedVideo(uri)
        pendingPickIndex = -1
        pendingPickTarget = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ver doc de la clase: con Experimental desactivado, esta Activity es
        // transparente — pasa directo al canal, sin mostrar nada.
        if (!SettingsManager.isExperimentalEnabled(this)) {
            startActivity(Intent(this, LiveDiscoveryKids::class.java))
            finish()
            return
        }

        setTheme(R.style.LauncherTheme)
        setContentView(R.layout.activity_launcher)

        findViewById<ImageButton>(R.id.btnLauncherSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.btnStartChannel).setOnClickListener {
            startActivity(Intent(this, LiveDiscoveryKids::class.java))
        }

        containerPrograms = findViewById(R.id.containerPrograms)
        txtProgramCountValue = findViewById(R.id.txtProgramCountValue)
        findViewById<LinearLayout>(R.id.itemProgramCount).setOnClickListener { showProgramCountDialog() }

        refreshProgramCountLabel()
        rebuildProgramList()
    }

    override fun onResume() {
        super.onResume()
        // Por si el usuario desactivó Experimental desde Configuración y
        // volvió acá con el botón Atrás del sistema: no debería quedar
        // mostrando una pantalla que ya no corresponde.
        if (!SettingsManager.isExperimentalEnabled(this)) {
            startActivity(Intent(this, LiveDiscoveryKids::class.java))
            finish()
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
            setText(SettingsManager.getProgramCount(this@DiscoveryKidsLauncherActivity).toString())
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
            Log.w("DKLauncher", "No se pudo persistir el permiso de lectura de $uri", e)
        }

        val index = pendingPickIndex
        if (index < 0) return

        when (pendingPickTarget) {
            PickTarget.PROGRAM -> SettingsManager.setProgramUri(this, index, uri.toString())
            PickTarget.YA_REGRESA -> SettingsManager.setYaRegresaUri(this, index, uri.toString())
            PickTarget.CONTINUAMOS -> SettingsManager.setContinuamosUri(this, index, uri.toString())
            null -> return
        }

        rebuildProgramList()
    }

    /** Nombre legible del archivo elegido (vía SAF), con fallback al último segmento de la Uri. */
    private fun displayNameFor(uri: Uri): String {
        return try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: uri.lastPathSegment ?: "video"
        } catch (e: Exception) {
            uri.lastPathSegment ?: "video"
        }
    }
}
