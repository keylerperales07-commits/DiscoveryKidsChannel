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
import com.google.android.material.button.MaterialButton
import java.io.File

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

    // Release 5.5.0: PickTarget, pendingPickIndex/pendingPickTarget, containerPrograms,
    // txtProgramCountValue y pickVideoLauncher se mudaron a
    // ProgramConfigActivity junto con el resto de "Programas" — ver esa
    // clase.

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

        // Release 5.5.0 — BUG FIX ("la ActionBar tapa el layout"): acá había
        // un setTheme(R.style.LauncherTheme) redundante ANTES de
        // setContentView() — el manifiesto (AndroidManifest.xml) ya declara
        // android:theme="@style/LauncherTheme" para esta Activity. Reaplicar
        // el tema en runtime, después de que la ventana ya se creó con el
        // tema del manifiesto, puede alterar cómo AppCompat calcula el
        // inset de contenido bajo la ActionBar (un problema conocido de
        // AppCompat con setTheme() tardío) — es la causa más probable del
        // solapamiento. Se saca por completo: el manifiesto ya alcanza.

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

        findViewById<MaterialButton>(R.id.btnStartChannel).setOnClickListener {
            val problems = validateChannelSetup()
            if (problems.isEmpty()) {
                startActivity(Intent(this, LiveDiscoveryKids::class.java))
            } else {
                showSetupProblemsDialog(problems)
            }
        }

        findViewById<MaterialButton>(R.id.btnProgramConfig).setOnClickListener {
            startActivity(Intent(this, ProgramConfigActivity::class.java))
        }
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

    // Release 5.5.0: cantidad de programas, filas por programa (bindProgramRow),
    // el selector de archivos (pickVideoLauncher/handlePickedVideo) y
    // displayNameFor() se mudaron a ProgramConfigActivity. Acá solo queda la
    // validación previa a "Iniciar canal" (validateChannelSetup) — sigue
    // teniendo sentido acá porque es sobre ESTE botón, no sobre la pantalla
    // de configuración en sí.

    /**
     * Release 5.4.0 — chequeo previo a "Iniciar canal": evita que
     * LiveDiscoveryKids arranque un ciclo con clips activados (Intro,
     * Créditos, ya_regresa/continuamos personalizados, o Programas) que en
     * realidad no tienen un video asociado — antes esto se saltaba en
     * silencio dentro del canal (advance() sin más), lo cual el usuario
     * podía no notar hasta ver el hueco en la programación.
     *
     * No reimplica la resolución completa de resolveProgram() (que vive
     * como función de extensión de LiveDiscoveryKids, otra Activity) —
     * hace el mismo chequeo de archivo/MediaStore en forma liviana, acá
     * mismo, solo para el caso "no eligió Uri propia".
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
                problems += "Programa ${index + 1}: activaste Intro pero no elegiste el video"
            }
            if (SettingsManager.isCreditosEnabled(this, index) && SettingsManager.getCreditosUri(this, index).isNullOrBlank()) {
                problems += "Programa ${index + 1}: activaste Créditos pero no elegiste el video"
            }
            if (SettingsManager.isYaRegresaCustom(this, index) && SettingsManager.getYaRegresaUri(this, index).isNullOrBlank()) {
                problems += "Programa ${index + 1}: activaste \"ya_regresa\" personalizado pero no elegiste el video"
            }
            if (SettingsManager.isContinuamosCustom(this, index) && SettingsManager.getContinuamosUri(this, index).isNullOrBlank()) {
                problems += "Programa ${index + 1}: activaste \"continuamos\" personalizado pero no elegiste el video"
            }
            if (SettingsManager.isNextProgramCustom(this, index) && SettingsManager.getNextProgramUri(this, index).isNullOrBlank()) {
                problems += "Programa ${index + 1}: activaste NextProgram personalizado pero no elegiste la imagen/GIF"
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
        val message = "Antes de iniciar el canal, revisá esto en Configuración de Programa:\n\n• " +
            problems.joinToString("\n• ")
        AlertDialog.Builder(this)
            .setTitle("Faltan videos por elegir")
            .setMessage(message)
            .setPositiveButton("Entendido", null)
            .show()
    }

}
