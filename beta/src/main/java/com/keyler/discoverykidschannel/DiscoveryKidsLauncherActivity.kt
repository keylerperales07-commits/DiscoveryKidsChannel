/*
 ESTO TIENE LICENCIA GNU LICENSE APACHE

 NO ELIMINAR NINGUN COMENTARIO AGREGADO
*/
package com.keyler.discoverykidschannel

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import java.io.File

/**
 * DiscoveryKidsLauncherActivity — Release 2009.5.0.0 ("Parque Imaginario")
 *
 * Es la Activity de inicio REAL de la app — ver el intent-filter
 * MAIN/LAUNCHER en AndroidManifest.xml — pero todo lo que implica (esta
 * pantalla, elegir el video de cada programa, cuántos programas armar) vive
 * detrás del interruptor maestro "Habilitar funciones experimentales" de
 * Configuración (SettingsManager.isExperimentalEnabled(), desactivado por
 * defecto):
 *
 *   - Experimental DESACTIVADO (default): onCreate() redirige de inmediato a
 *     LiveDiscoveryKids y hace finish(), sin mostrar nunca esta UI.
 *   - Experimental ACTIVADO: se muestra esta pantalla, con:
 *       • Botón "Iniciar canal" → LiveDiscoveryKids.
 *       • Sección ScreenBugs de eventos (global — Navidad, Año Nuevo,
 *         Pascua, Día de la Tierra).
 *       • Sección Programas: cantidad (1–24) y una fila por programa
 *         (item_program_row.xml) donde el usuario elige el video vía
 *         selector de archivos del sistema (SAF), y un botón "⚙️ Opciones"
 *         que abre ProgramConfigActivity para ESE programa puntual.
 *
 * Release 5.6.0 — BUG FIX (malentendido de arquitectura, revertido): la
 * 5.5.0 había movido ENTERA la sección "Programas" (y de paso, sin que
 * correspondiera, los ScreenBugs de eventos) a ProgramConfigActivity — mal.
 * La separación correcta:
 *   - LAUNCHER (esta Activity): los PROGRAMAS en sí — cantidad, y qué video
 *     es cada uno — y los ScreenBugs de eventos, que son GLOBALES (no
 *     pertenecen a un programa en particular, así que no tiene sentido que
 *     vivan en una pantalla de opciones "de programa").
 *   - ProgramConfigActivity (un botón "⚙️ Opciones" por fila, acá abajo):
 *     solo las OPCIONES de CADA programa — continuamos, Intro,
 *     Créditos, A continuación personalizado, activar comerciales —
 *     completamente independientes entre programas.
 *
 * Todas las Uri elegidas se persisten con
 * ContentResolver.takePersistableUriPermission() para seguir siendo válidas
 * entre reinicios de la app (SAF: sin esto, el permiso de lectura expira al
 * cerrar la app). LiveDiscoveryKids las consulta en resolveProgram().
 */
class DiscoveryKidsLauncherActivity : AppCompatActivity() {

    private lateinit var containerPrograms: LinearLayout
    private lateinit var txtProgramCountValue: TextView

    // Release 5.8.0 — BUG FIX (diseño): "elegir video" acá se eliminó — lo
    // reemplaza "Episodios" dentro de Configuración de Programa
    // (ProgramConfigActivity), que permite elegir VARIOS videos por
    // programa en vez de uno solo. pendingPickIndex/pickVideoLauncher ya
    // no hacen falta acá.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ver doc de la clase: con Experimental desactivado, esta Activity es
        // transparente — pasa directo al canal, sin mostrar nada.
        if (!SettingsManager.isExperimentalEnabled(this)) {
            startActivity(Intent(this, LiveDiscoveryKids::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_launcher)

        // Release 2009.5.1.0: ActionBar ORIGINAL de Android (Theme.Material3.
        // DayNight, ver themes.xml) en vez del MenuBar custom hecho a mano.
        // El botón Configuración pasó a ser un ítem del menú de opciones —
        // ver onCreateOptionsMenu()/onOptionsItemSelected() más abajo.
        //
        // Release 2009.5.2.1 — BUG FIX (diseño): la ActionBar ahora solo
        // tiene el título — el logo se sacó de acá y pasó al cuerpo de la
        // pantalla, entre la ActionBar y el botón "Iniciar canal" (ver
        // activity_launcher.xml, @+id/imgLauncherLogo).
        supportActionBar?.apply {
            title = "Discovery Kids Launcher"
        }

        // Release 5.8.0 — BUG FIX ("el ActionBar se come una parte del
        // Layout"): con targetSdk 36, Android fuerza edge-to-edge — el
        // contenido puede dibujarse detrás de la barra de estado/ActionBar
        // si nadie reserva ese espacio explícitamente (antes lo hacía solo
        // el sistema). Se aplica el inset de barras del sistema como padding
        // superior/inferior del contenido, para que nunca quede tapado.
        val launcherRoot = findViewById<View>(R.id.launcherRoot)
        val rootPaddingLeft = launcherRoot.paddingLeft
        val rootPaddingRight = launcherRoot.paddingRight
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(launcherRoot) { view, insets ->
            val bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            view.setPadding(rootPaddingLeft, bars.top, rootPaddingRight, bars.bottom)
            insets
        }

        findViewById<MaterialButton>(R.id.btnStartChannel).setOnClickListener {
            val problems = validateChannelSetup()
            if (problems.isEmpty()) {
                startActivity(Intent(this, LiveDiscoveryKids::class.java))
            } else {
                showSetupProblemsDialog(problems)
            }
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
            return
        }
        // Por si el usuario volvió de ProgramConfigActivity habiendo elegido
        // un video de continuamos/etc — acá no cambia nada visible
        // (esa parte vive en la otra Activity), pero si volvió habiendo
        // cambiado la cantidad de programas desde otra instancia, conviene
        // refrescar por las dudas.
        refreshProgramCountLabel()
        rebuildProgramList()
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.menu_launcher, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == R.id.menuLauncherSettings) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // Release 5.8.0 — BUG FIX (diseño): los ScreenBugs de eventos
    // (Navidad, Año Nuevo, Pascua, Día de la Tierra) se sacaron de acá —
    // el switch maestro "Activar eventos" + el selector de evento actual
    // ahora viven en Configuración (SettingsActivity).

    // ── Cantidad de programas (1–24) ─────────────────────────────────────────

    private fun refreshProgramCountLabel() {
        val count = SettingsManager.getProgramCount(this)
        txtProgramCountValue.text =
            "$count de ${SettingsManager.MAX_PROGRAM_COUNT} programas (Predeterminado: ${SettingsManager.DEFAULT_PROGRAM_COUNT})"
    }

    private fun showProgramCountDialog() {
        val input = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
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
            val row = inflater.inflate(R.layout.item_program_row, containerPrograms, false)
            bindProgramRow(row, index)
            containerPrograms.addView(row)
        }
    }

    private fun bindProgramRow(row: View, index: Int) {
        val txtTitle = row.findViewById<TextView>(R.id.txtProgramTitle)
        val txtVideoStatus = row.findViewById<TextView>(R.id.txtProgramVideoStatus)
        val btnOptions = row.findViewById<LinearLayout>(R.id.btnProgramOptions)

        txtTitle.text = "Programa ${index + 1}"

        // Release 5.8.0 — BUG FIX (diseño): ya no se elige un video acá
        // directamente (ver Episodios en ProgramConfigActivity) — la fila
        // solo resume cuántos episodios tiene configurados este programa.
        val episodeCount = SettingsManager.getEpisodeCount(this, index)
        txtVideoStatus.text = if (episodeCount <= 1) {
            "1 episodio — Opciones → Episodios"
        } else {
            "$episodeCount episodios — Opciones → Episodios"
        }

        // Opciones de ESTE programa puntual (Episodios, continuamos, Intro,
        // Créditos, A continuación personalizado, activar comerciales) —
        // ver ProgramConfigActivity.
        btnOptions.setOnClickListener {
            val intent = Intent(this, ProgramConfigActivity::class.java)
            intent.putExtra(ProgramConfigActivity.EXTRA_PROGRAM_INDEX, index)
            startActivity(intent)
        }
    }

    /**
     * Release 5.4.0 — chequeo previo a "Iniciar canal": evita que
     * LiveDiscoveryKids arranque un ciclo con clips activados (Intro,
     * Créditos, continuamos personalizado, A continuación
     * personalizado, o Programas) que en realidad no tienen un video
     * asociado — antes esto se saltaba en silencio dentro del canal
     * (advance() sin más), lo cual el usuario podía no notar hasta ver el
     * hueco en la programación.
     *
     * @return lista de problemas legibles para el usuario; vacía si todo OK.
     */
    private fun validateChannelSetup(): List<String> {
        val problems = mutableListOf<String>()
        val count = SettingsManager.getProgramCount(this)

        for (index in 0 until count) {
            val programUri = SettingsManager.getProgramUri(this, index)
            if (programUri.isNullOrBlank() && !classicProgramFileExists(index)) {
                problems += "Programa ${index + 1}: no elegiste un video y no se encontró pro${index + 1}.mp4 en Videos"
            }
            if (SettingsManager.isIntroEnabled(this, index) && SettingsManager.getIntroUri(this, index).isNullOrBlank()) {
                problems += "Programa ${index + 1}: activaste Intro pero no elegiste el video (Opciones del programa)"
            }
            if (SettingsManager.isCreditosEnabled(this, index) && SettingsManager.getCreditosUri(this, index).isNullOrBlank()) {
                problems += "Programa ${index + 1}: activaste Créditos pero no elegiste el video (Opciones del programa)"
            }
            if (SettingsManager.isContinuamosCustom(this, index) && SettingsManager.getContinuamosUri(this, index).isNullOrBlank()) {
                problems += "Programa ${index + 1}: activaste \"continuamos\" personalizado pero no elegiste el video (Opciones del programa)"
            }
            if (SettingsManager.isNextProgramCustom(this, index) && SettingsManager.getNextProgramUri(this, index).isNullOrBlank()) {
                problems += "Programa ${index + 1}: activaste \"A continuación\" personalizado pero no elegiste la imagen/GIF (Opciones del programa)"
            }
        }
        return problems
    }

    /** Mismo chequeo liviano de archivo/MediaStore que resolveProgram() en LiveDiscoveryKids.kt, para pro{N}.mp4. */
    private fun classicProgramFileExists(index: Int): Boolean {
        val fileName = "pro${index + 1}.mp4"
        val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        if (File(moviesDir, fileName).exists()) return true
        return try {
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            else
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            contentResolver.query(
                collection,
                arrayOf(MediaStore.Video.Media._ID),
                "${MediaStore.Video.Media.DISPLAY_NAME} = ?",
                arrayOf(fileName),
                null
            )?.use { it.moveToFirst() } ?: false
        } catch (e: Exception) {
            Log.w("DKLauncher", "No se pudo chequear $fileName vía MediaStore", e)
            true   // ante la duda, no bloquear el arranque por un error de nuestro chequeo
        }
    }

    private fun showSetupProblemsDialog(problems: List<String>) {
        val message = "Antes de iniciar el canal, revisá esto:\n\n• " +
            problems.joinToString("\n• ")
        AlertDialog.Builder(this)
            .setTitle("Faltan videos por elegir")
            .setMessage(message)
            .setPositiveButton("Entendido", null)
            .show()
    }

}
