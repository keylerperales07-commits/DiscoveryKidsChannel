/*
 ESTO TIENE LICENCIA GNU LICENSE APACHE

 NO ELIMINAR NINGUN COMENTARIO AGREGADO
*/
package com.keyler.discoverykidschannel

import android.app.AlertDialog
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageInfo
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.keyler.discoverykidschannel.R

/**
 * SettingsActivity — Preview 2006.4.1.0.21
 *
 * Rediseño completo a lista simple (estilo Android Settings / AndroidIDE),
 * reemplazando el menú OSD de TV CRT de la Preview 4.1.0.11. Se abre desde
 * el botón ⚙️ de LiveDiscoveryKids.
 *
 * Cambios respecto a la 4.1.0.11:
 *   - Se eliminó el selector de modo Completa/Profesional: ahora es una
 *     sola lista, sin secciones que ocultar.
 *   - Se eliminó la opción de modo debug: el overlay de FPS/RAM/versión
 *     ahora se muestra automáticamente en builds Preview, sin control de usuario.
 *   - El brillo del CRT (slider 0–100%) pasó a ser un simple activar/desactivar.
 *   - Se agregaron: duración del Screenbug, intervalo de comerciales (min/max)
 *     y Forzar 4:3 — los dos primeros abren un diálogo simple al tocarlos.
 *
 * Todas las opciones persisten inmediatamente vía SettingsManager al cambiar
 * (no hay botón "Guardar"); LiveDiscoveryKids las vuelve a leer en su próximo
 * onCreate()/onResume() para aplicarlas.
 *
 * Preview 4.2.0.20 — NUEVO: sección "Actualizaciones" con el item "Buscar
 * actualizaciones", que delega en AppUpdater (consulta GitHub Releases,
 * descarga el .apk del último release si es más nuevo, y abre el instalador
 * del sistema). Ver AppUpdater.kt para el detalle del flujo completo.
 *
 * Preview 4.1.0.21 — NUEVO: switch "Habilitar versiones Preview" dentro de
 * "Actualizaciones" (desactivado por defecto). Cuando está activado,
 * AppUpdater también puede detectar e instalar releases marcados como
 * Preview en GitHub, no solo los estables.
 *
 * Release 2007.4.3.0 — CAMBIO: "Buscar actualizaciones" ya no consulta a
 * AppUpdater ni muestra AlertDialogs desde esta Activity. Ahora solo abre
 * `UpdateActivity` (startActivity simple, sin pasar datos) — es esa pantalla
 * la que hace la consulta, pide confirmación, descarga con barra de progreso
 * en vivo, e instala. Se eliminaron checkForUpdate(), isCheckingUpdate y el
 * CheckCallback que vivían acá.
 *
 * Release 4.6.0 — NUEVO: sección "Programación" con el item "Elegir
 * programas", que abre el nuevo DiscoveryKidsLauncherActivity (mismo
 * patrón que "Buscar actualizaciones" → UpdateActivity: startActivity
 * simple, toda la lógica vive en la otra Activity).
 *
 * Release 2009.5.0.0 — "Parque Imaginario":
 *   - ELIMINADO: sección "Programación" / item "Elegir programas". Discovery
 *     Kids Launcher pasó a ser la Activity de inicio real de la app (ver
 *     AndroidManifest.xml y DiscoveryKidsLauncherActivity.onCreate()), así
 *     que ya no hace falta un atajo desde acá — se accede abriendo la app.
 *
 * RELEASE 2013.6.0.0 — ELIMINADO por completo: la sección "Experimental" y
 *   su switch maestro "Habilitar funciones experimentales" (con
 *   showExperimentalRestartDialog()) que se había agregado en la Release
 *   2009.5.0.0 — ver nota de esa Release, arriba. Discovery Kids Launcher ya
 *   no depende de ningún interruptor: siempre es la pantalla de inicio real
 *   de la app.
 *
 * Release 2009.5.2.1 — ELIMINADO por completo: el motor de video basado en
 *   TextureView (y el switch "Recortar 4:3", ex "Usar TextureView", que lo
 *   activaba) y el AlertDialog que avisaba sobre programas de 720p+. Ver
 *   DkVideoView.kt y LiveDiscoveryKids.kt.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var switchBgMusic: SwitchCompat
    private lateinit var switchCrtEffect: SwitchCompat
    private lateinit var switchForceAspectRatio: SwitchCompat
    private lateinit var switchPreviewUpdates: SwitchCompat

    private lateinit var itemScreenbugDelay: LinearLayout
    private lateinit var txtScreenbugDelayValue: TextView

    private lateinit var itemCommercialInterval: LinearLayout
    private lateinit var txtCommercialIntervalValue: TextView

    private lateinit var itemCheckUpdate: LinearLayout
    private lateinit var txtCheckUpdateValue: TextView

    // Release 5.8.0 — Eventos: switch maestro + selector de evento actual.
    private lateinit var switchEventsEnabled: SwitchCompat
    private lateinit var itemSelectedEvent: LinearLayout
    private lateinit var txtSelectedEventLabel: TextView
    private lateinit var txtSelectedEventValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.SettingsTheme)
        setContentView(R.layout.activity_settings)

        // Release 2009.5.2.1 — BUG FIX: "elimina el menú falso, usá el
        // ActionBar del SDK de Android". Antes había un header hecho a mano
        // en el propio layout (ImageButton "Atrás" + TextView "Configuración"
        // simulando una barra). Ahora es la ActionBar real, con navegación
        // "Up" nativa — ver onSupportNavigateUp() más abajo.
        supportActionBar?.apply {
            title = "Configuración"
            setDisplayHomeAsUpEnabled(true)
        }

        // Release 5.8.0 — BUG FIX ("el ActionBar se come una parte del
        // Layout"), ver mismo comentario en DiscoveryKidsLauncherActivity.onCreate().
        val settingsRoot = findViewById<View>(R.id.settingsRoot)
        val rootPaddingLeft = settingsRoot.paddingLeft
        val rootPaddingRight = settingsRoot.paddingRight
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(settingsRoot) { view, insets ->
            val bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            view.setPadding(rootPaddingLeft, bars.top, rootPaddingRight, bars.bottom)
            insets
        }

        bindViews()
        loadCurrentValues()
        setupListeners()
        
        settingsVersionInfo()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun bindViews() {
        switchBgMusic = findViewById(R.id.switchBgMusic)
        switchCrtEffect = findViewById(R.id.switchCrtEffect)
        switchForceAspectRatio = findViewById(R.id.switchForceAspectRatio)
        switchPreviewUpdates = findViewById(R.id.switchPreviewUpdates)

        itemScreenbugDelay = findViewById(R.id.itemScreenbugDelay)
        txtScreenbugDelayValue = findViewById(R.id.txtScreenbugDelayValue)

        itemCommercialInterval = findViewById(R.id.itemCommercialInterval)
        txtCommercialIntervalValue = findViewById(R.id.txtCommercialIntervalValue)

        itemCheckUpdate = findViewById(R.id.itemCheckUpdate)
        txtCheckUpdateValue = findViewById(R.id.txtCheckUpdateValue)

        switchEventsEnabled = findViewById(R.id.switchEventsEnabled)
        itemSelectedEvent = findViewById(R.id.itemSelectedEvent)
        txtSelectedEventLabel = findViewById(R.id.txtSelectedEventLabel)
        txtSelectedEventValue = findViewById(R.id.txtSelectedEventValue)
    }

    /** Carga los valores guardados en SettingsManager y los refleja en cada control. */
    private fun loadCurrentValues() {
        switchBgMusic.isChecked = SettingsManager.isBgMusicEnabled(this)
        switchCrtEffect.isChecked = SettingsManager.isCrtEffectEnabled(this)
        switchForceAspectRatio.isChecked = SettingsManager.isForceAspectRatioEnabled(this)
        switchPreviewUpdates.isChecked = SettingsManager.isPreviewUpdatesEnabled(this)
        switchEventsEnabled.isChecked = SettingsManager.isEventsEnabled(this)

        refreshScreenbugDelayLabel()
        refreshCommercialIntervalLabel()
        refreshCheckUpdateLabel()
        refreshSelectedEventLabel()
        updateEventSelectorEnabledState()
    }

    private fun setupListeners() {
        // Switches: toda la fila es clickeable y alterna el switch (mismo patrón
        // que Android Settings nativo), además del propio switch por si lo tocan directo.
        findViewById<LinearLayout>(R.id.itemBgMusic).setOnClickListener {
            switchBgMusic.isChecked = !switchBgMusic.isChecked
        }
        switchBgMusic.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.setBgMusicEnabled(this, isChecked)
        }

        findViewById<LinearLayout>(R.id.itemCrtEffect).setOnClickListener {
            switchCrtEffect.isChecked = !switchCrtEffect.isChecked
        }
        switchCrtEffect.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.setCrtEffectEnabled(this, isChecked)
        }

        findViewById<LinearLayout>(R.id.itemForceAspectRatio).setOnClickListener {
            switchForceAspectRatio.isChecked = !switchForceAspectRatio.isChecked
        }
        switchForceAspectRatio.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.setForceAspectRatioEnabled(this, isChecked)
        }

        findViewById<LinearLayout>(R.id.itemPreviewUpdates).setOnClickListener {
            switchPreviewUpdates.isChecked = !switchPreviewUpdates.isChecked
        }
        switchPreviewUpdates.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.setPreviewUpdatesEnabled(this, isChecked)
        }

        itemScreenbugDelay.setOnClickListener { showScreenbugDelayDialog() }
        itemCommercialInterval.setOnClickListener { showCommercialIntervalDialog() }

        itemCheckUpdate.setOnClickListener { checkForUpdate() }

        // ── Eventos (Release 5.8.0) ──────────────────────────────────────────
        findViewById<LinearLayout>(R.id.itemEventsEnabled).setOnClickListener {
            switchEventsEnabled.isChecked = !switchEventsEnabled.isChecked
        }
        switchEventsEnabled.setOnCheckedChangeListener { _, checked ->
            SettingsManager.setEventsEnabled(this, checked)
            updateEventSelectorEnabledState()
        }
        itemSelectedEvent.setOnClickListener {
            if (SettingsManager.isEventsEnabled(this)) showSelectedEventDialog()
        }
    }

    // ── Diálogo: Duración del Screenbug ─────────────────────────────────────
    // ── Diálogo: Evento actual (Release 5.8.0) ──────────────────────────────
    private val eventLabels = listOf(
        SettingsManager.EVENT_NORMAL to "Normal — la app decide sola según la fecha",
        SettingsManager.EVENT_NAVIDAD to "Navidad",
        SettingsManager.EVENT_DIA_TIERRA to "Día de la Tierra",
        SettingsManager.EVENT_ANIO_NUEVO to "Año Nuevo",
        SettingsManager.EVENT_PASCUA to "Huevo de Pascua"
    )

    private fun showSelectedEventDialog() {
        val current = SettingsManager.getSelectedEvent(this)
        val labels = eventLabels.map { it.second }.toTypedArray()
        val currentIndex = eventLabels.indexOfFirst { it.first == current }.coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Evento actual")
            .setSingleChoiceItems(labels, currentIndex) { dialog, which ->
                SettingsManager.setSelectedEvent(this, eventLabels[which].first)
                refreshSelectedEventLabel()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun refreshSelectedEventLabel() {
        val current = SettingsManager.getSelectedEvent(this)
        val label = eventLabels.firstOrNull { it.first == current }?.second ?: eventLabels[0].second
        txtSelectedEventValue.text = if (current == SettingsManager.EVENT_NORMAL) {
            "$label (Predeterminado)"
        } else {
            label
        }
    }

    /**
     * "Si el usuario desactivó esta opción el menú [de selección de evento]
     * está deshabilitado" — grisa el item y le saca el click mientras
     * "Activar eventos" esté desactivado.
     */
    private fun updateEventSelectorEnabledState() {
        val enabled = switchEventsEnabled.isChecked
        itemSelectedEvent.isEnabled = enabled
        itemSelectedEvent.isClickable = enabled
        itemSelectedEvent.isFocusable = enabled
        val alpha = if (enabled) 1f else 0.4f
        txtSelectedEventLabel.alpha = alpha
        txtSelectedEventValue.alpha = alpha
    }

    private fun showScreenbugDelayDialog() {
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(SettingsManager.getScreenbugDelaySec(this@SettingsActivity).toString())
            setSelection(text.length)
        }
        val container = wrapInputWithPadding(input)

        AlertDialog.Builder(this)
            .setTitle("Duración del Screenbug")
            .setMessage("Segundos antes de que aparezca el Screenbug al iniciar un segmento (Predeterminado: ${SettingsManager.DEFAULT_SCREENBUG_DELAY_SEC} s)")
            .setView(container)
            .setPositiveButton("Guardar") { _, _ ->
                val seconds = input.text.toString().toIntOrNull() ?: SettingsManager.DEFAULT_SCREENBUG_DELAY_SEC
                SettingsManager.setScreenbugDelaySec(this, seconds)
                refreshScreenbugDelayLabel()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun refreshScreenbugDelayLabel() {
        val sec = SettingsManager.getScreenbugDelaySec(this)
        txtScreenbugDelayValue.text =
            "Aparece a los $sec s de iniciado el segmento (Predeterminado: ${SettingsManager.DEFAULT_SCREENBUG_DELAY_SEC} s)"
    }

    // ── Diálogo: Intervalo de comerciales (Min/Max) ─────────────────────────
    private fun showCommercialIntervalDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, 0)
        }

        val labelMin = TextView(this).apply {
            text = "Mínimo (minutos)"
            setTextColor(getColor(R.color.dk_text_secondary))
        }
        val inputMin = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(SettingsManager.getCommercialMinMinutes(this@SettingsActivity).toString())
        }
        val labelMax = TextView(this).apply {
            text = "Máximo (minutos)"
            setTextColor(getColor(R.color.dk_text_secondary))
            val topPad = (12 * resources.displayMetrics.density).toInt()
            setPadding(0, topPad, 0, 0)
        }
        val inputMax = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(SettingsManager.getCommercialMaxMinutes(this@SettingsActivity).toString())
        }

        container.addView(labelMin)
        container.addView(inputMin)
        container.addView(labelMax)
        container.addView(inputMax)

        AlertDialog.Builder(this)
            .setTitle("Intervalo entre comerciales")
            .setMessage("Cada corte ocurre en un intervalo aleatorio entre estos dos valores (Predeterminado: ${SettingsManager.DEFAULT_COMMERCIAL_MIN_MINUTES}–${SettingsManager.DEFAULT_COMMERCIAL_MAX_MINUTES} min)")
            .setView(container)
            .setPositiveButton("Guardar") { _, _ ->
                val min = inputMin.text.toString().toIntOrNull() ?: SettingsManager.DEFAULT_COMMERCIAL_MIN_MINUTES
                val max = inputMax.text.toString().toIntOrNull() ?: SettingsManager.DEFAULT_COMMERCIAL_MAX_MINUTES
                SettingsManager.setCommercialInterval(this, min, max)
                refreshCommercialIntervalLabel()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun refreshCommercialIntervalLabel() {
        val min = SettingsManager.getCommercialMinMinutes(this)
        val max = SettingsManager.getCommercialMaxMinutes(this)
        txtCommercialIntervalValue.text =
            "Cada $min–$max minutos, al azar (Predeterminado: ${SettingsManager.DEFAULT_COMMERCIAL_MIN_MINUTES}–${SettingsManager.DEFAULT_COMMERCIAL_MAX_MINUTES} min)"
    }

    // ── Actualizaciones (Release 2007.4.3.0) ────────────────────────────────
    //
    // "Buscar actualizaciones" ya no hace la consulta ni muestra diálogos
    // desde acá: simplemente abre UpdateActivity, que se encarga de todo el
    // flujo (consulta a GitHub, confirmación, descarga con progreso e
    // instalación). Ver UpdateActivity.kt para el detalle completo.
    private fun checkForUpdate() {
        startActivity(android.content.Intent(this, UpdateActivity::class.java))
    }

    private fun refreshCheckUpdateLabel() {
        txtCheckUpdateValue.text = "Comprobá si hay una nueva versión disponible en GitHub"
    }

    /** Envuelve un EditText con padding horizontal estándar para diálogos. */
    private fun wrapInputWithPadding(input: EditText): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, 0, pad, 0)
            addView(input)
        }
    }
    
    private fun settingsVersionInfo() {
        val versionInfo = findViewById<TextView>(R.id.txtSettingsVersion)

        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val versionName = packageInfo.versionName
        val label = packageInfo.applicationInfo?.loadLabel(packageManager)?.toString() ?: ""
        val versionInfoText = "$label • $versionName"

        versionInfo.text = versionInfoText
    }
}
