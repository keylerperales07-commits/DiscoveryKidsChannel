/*
 ESTO TIENE LICENCIA GNU LICENSE APACHE

 NO ELIMINAR NINGUN COMENTARIO AGREGADO
*/
package com.keyler.discoverykidschannel

import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import java.io.File

/**
 * DiscoveryKidsLauncherActivity — Release 4.6.0
 *
 * "Discovery Kids Launcher": pantalla donde el usuario elige qué programas
 * (pro1–pro4.mp4) quiere que salgan al aire en la programación lineal.
 * Mismo diseño de lista simple que SettingsActivity/activity_settings.xml
 * (header con botón Atrás + título, rótulo de sección gris, ítems con
 * switch a la derecha) — se abre desde Configuración → "Elegir programas".
 *
 * Cada ítem muestra, además del switch, si el archivo pro{N}.mp4 se
 * encontró realmente en la carpeta Movies (o vía MediaStore) — la misma
 * resolución que usa ChannelMediaResolver.resolveProgram() en
 * LiveDiscoveryKids, duplicada acá en checkProgramFileExists() porque esta
 * Activity no es una instancia de LiveDiscoveryKids y no puede llamar a
 * esa función de extensión directamente.
 *
 * El estado de cada switch se persiste inmediatamente en SettingsManager
 * (SettingsManager.setProgramEnabled), igual que el resto de las opciones
 * de la app — no hay botón "Guardar". LiveDiscoveryKids consulta
 * SettingsManager.isProgramEnabled() en playProgram() y
 * findAvailableProgramIndex() para saltear los programas desactivados,
 * tanto en la programación lineal como en la navegación Prev/Next.
 *
 * Desactivar TODOS los programas no rompe nada: el canal simplemente sigue
 * repitiendo Enseguida → Bumper → Comercial en loop sin nunca encontrar un
 * programa disponible, el mismo comportamiento de fallback que ya existía
 * si a alguien le faltaban los 4 archivos .mp4 en Movies.
 */
class DiscoveryKidsLauncherActivity : AppCompatActivity() {

    private val totalPrograms = 4

    private lateinit var programItems: List<LinearLayout>
    private lateinit var programSwitches: List<SwitchCompat>
    private lateinit var programStatusLabels: List<TextView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.SettingsTheme)
        setContentView(R.layout.activity_launcher)

        findViewById<ImageButton>(R.id.btnLauncherBack).setOnClickListener { finish() }

        programItems = listOf(
            findViewById(R.id.itemProgram1),
            findViewById(R.id.itemProgram2),
            findViewById(R.id.itemProgram3),
            findViewById(R.id.itemProgram4)
        )
        programSwitches = listOf(
            findViewById(R.id.switchProgram1),
            findViewById(R.id.switchProgram2),
            findViewById(R.id.switchProgram3),
            findViewById(R.id.switchProgram4)
        )
        programStatusLabels = listOf(
            findViewById(R.id.txtProgram1Status),
            findViewById(R.id.txtProgram2Status),
            findViewById(R.id.txtProgram3Status),
            findViewById(R.id.txtProgram4Status)
        )

        setupProgramToggles()
    }

    /** Carga el estado guardado de cada programa y busca si su archivo existe, todo en un solo loop. */
    private fun setupProgramToggles() {
        for (index in 0 until totalPrograms) {
            val enabled = SettingsManager.isProgramEnabled(this, index)
            val found = checkProgramFileExists(index)

            programSwitches[index].isChecked = enabled
            refreshStatusLabel(index, found)

            // Mismo patrón que SettingsActivity: toda la fila es clickeable y
            // alterna el switch, además del propio switch por si lo tocan directo.
            programItems[index].setOnClickListener {
                programSwitches[index].isChecked = !programSwitches[index].isChecked
            }
            programSwitches[index].setOnCheckedChangeListener { _, isChecked ->
                SettingsManager.setProgramEnabled(this, index, isChecked)
                refreshStatusLabel(index, found)
            }
        }
    }

    /**
     * Actualiza el subtítulo del ítem combinando dos cosas independientes:
     * si el usuario lo activó (switch) y si el archivo realmente existe.
     * Un programa activado pero no encontrado no se va a reproducir igual
     * (ver playProgram() en LiveDiscoveryKids) — se lo avisamos acá para
     * que no sea sorpresa.
     */
    private fun refreshStatusLabel(index: Int, found: Boolean) {
        val fileName = "pro${index + 1}.mp4"
        programStatusLabels[index].text = when {
            !found -> "$fileName no encontrado en Videos — no va a salir al aire aunque esté activado"
            programSwitches[index].isChecked -> "$fileName encontrado — va a salir al aire"
            else -> "$fileName encontrado — desactivado, no va a salir al aire"
        }
    }

    /**
     * Duplicado deliberado de la lógica de ChannelMediaResolver.resolveProgram():
     * busca pro{N}.mp4 primero por path directo en Movies, y si no lo
     * encuentra cae a una consulta MediaStore (necesario en Android 10+ con
     * scoped storage). Se duplica acá (en vez de reusar la función de
     * extensión) porque esta Activity no es una instancia de LiveDiscoveryKids.
     */
    private fun checkProgramFileExists(index: Int): Boolean {
        val fileName = "pro${index + 1}.mp4"

        val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val file = File(moviesDir, fileName)
        if (file.exists()) return true

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        return try {
            contentResolver.query(
                collection,
                arrayOf(MediaStore.Video.Media._ID),
                "${MediaStore.Video.Media.DISPLAY_NAME} = ?",
                arrayOf(fileName),
                null
            )?.use { cursor -> cursor.moveToFirst() } ?: false
        } catch (e: Exception) {
            false
        }
    }
}
