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
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

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
 *   - NUEVO: sección "Experimental" con el switch maestro "Habilitar
 *     funciones experimentales" (desactivado por defecto). Al cambiarlo se
 *     guarda inmediatamente en SettingsManager y se muestra un AlertDialog
 *     ofreciendo reiniciar la app ahora o más tarde — ver
 *     showExperimentalRestartDialog(). Activarlo habilita el Discovery Kids
 *     Launcher como pantalla de inicio real (en vez de pasar directo al
 *     canal) y toda su configuración avanzada de programas.
 *   - NUEVO: sección "Compatibilidad de video" con el switch "Usar
 *     TextureView" (desactivado por defecto, NO es experimental). Activa el
 *     motor de video basado en TextureView (ver DkVideoView.kt) para que
 *     videos de 720p o superior no tapen el ScreenBug. Requiere reabrir el
 *     canal para tomar efecto.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var switchBgMusic: SwitchCompat
    private lateinit var switchCrtEffect: SwitchCompat
    private lateinit var switchForceAspectRatio: SwitchCompat
    private lateinit var switchPreviewUpdates: SwitchCompat
    private lateinit var switchExperimental: SwitchCompat       // Release 2009.5.0.0
    private lateinit var switchTextureView: SwitchCompat        // Release 2009.5.0.0

    private lateinit var itemScreenbugDelay: LinearLayout
    private lateinit var txtScreenbugDelayValue: TextView

    private lateinit var itemCommercialInterval: LinearLayout
    private lateinit var txtCommercialIntervalValue: TextView

    private lateinit var itemCheckUpdate: LinearLayout
    private lateinit var txtCheckUpdateValue: TextView

    // Release 2009.5.2.0: "Recortar 4:3" (ex "Usar TextureView") se
    // deshabilita cuando "Forzar 4:3" está activado — ver updateCropSwitchEnabledState().
    private lateinit var itemTextureView: LinearLayout
    private lateinit var txtTextureViewLabel: TextView
    private lateinit var txtTextureViewDesc: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.SettingsTheme)
        setContentView(R.layout.activity_settings)

        bindViews()
        loadCurrentValues()
        setupListeners()
        
        settingsVersionInfo()
    }

    private fun bindViews() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

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

        switchExperimental = findViewById(R.id.switchExperimental)
        switchTextureView = findViewById(R.id.switchTextureView)
        itemTextureView = findViewById(R.id.itemTextureView)
        txtTextureViewLabel = findViewById(R.id.txtTextureViewLabel)
        txtTextureViewDesc = findViewById(R.id.txtTextureViewDesc)
    }

    /** Carga los valores guardados en SettingsManager y los refleja en cada control. */
    private fun loadCurrentValues() {
        switchBgMusic.isChecked = SettingsManager.isBgMusicEnabled(this)
        switchCrtEffect.isChecked = SettingsManager.isCrtEffectEnabled(this)
        switchForceAspectRatio.isChecked = SettingsManager.isForceAspectRatioEnabled(this)
        switchPreviewUpdates.isChecked = SettingsManager.isPreviewUpdatesEnabled(this)
        switchExperimental.isChecked = SettingsManager.isExperimentalEnabled(this)
        switchTextureView.isChecked = SettingsManager.isTextureViewEnabled(this)

        refreshScreenbugDelayLabel()
        refreshCommercialIntervalLabel()
        refreshCheckUpdateLabel()
        updateCropSwitchEnabledState()
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
            // Release 2009.5.2.0: "Recortar 4:3" no tiene sentido con "Forzar
            // 4:3" activado (ya se recorta a 4:3 de todas formas) — se
            // deshabilita mientras tanto.
            updateCropSwitchEnabledState()
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

        // ── Experimental (Release 2009.5.0.0) ───────────────────────────────
        findViewById<LinearLayout>(R.id.itemExperimental).setOnClickListener {
            switchExperimental.isChecked = !switchExperimental.isChecked
        }
        switchExperimental.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.setExperimentalEnabled(this, isChecked)
            showRestartDialog()
        }

        // ── Compatibilidad de video / TextureView (Release 2009.5.0.0) ──────
        findViewById<LinearLayout>(R.id.itemTextureView).setOnClickListener {
            switchTextureView.isChecked = !switchTextureView.isChecked
        }
        switchTextureView.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.setTextureViewEnabled(this, isChecked)
            showRestartDialog()
        }
    }

    /**
     * Release 2009.5.2.0 — "Recortar 4:3" (ex "Usar TextureView") queda
     * deshabilitado (grisado, no clickeable) mientras "Forzar 4:3" esté
     * activado: en ese caso el video ya se recorta a 4:3 de todas formas
     * (ver AspectRatioFrameLayout/DkVideoView), así que esta opción no
     * cambia nada. Se habilita de nuevo apenas se desactiva "Forzar 4:3".
     */
    private fun updateCropSwitchEnabledState() {
        val enabled = !switchForceAspectRatio.isChecked
        itemTextureView.isEnabled = enabled
        itemTextureView.isClickable = enabled
        itemTextureView.isFocusable = enabled
        switchTextureView.isEnabled = enabled
        val alpha = if (enabled) 1f else 0.4f
        txtTextureViewLabel.alpha = alpha
        txtTextureViewDesc.alpha = alpha
        switchTextureView.alpha = alpha
    }

    /**
     * Release 2009.5.0.0 — diálogo genérico de "reiniciar ahora o más
     * tarde", usado tanto por el switch de Experimental como por el de
     * TextureView (ninguno de los dos puede tomar efecto con las Activities
     * ya creadas: el Launcher decide si redirige al canal en su propio
     * onCreate(), y el tipo de superficie de video se fija una sola vez al
     * crear el DkVideoView). "Reiniciar ahora" relanza la app desde
     * DiscoveryKidsLauncherActivity y mata el proceso actual con
     * Runtime.exit(); "Más tarde" simplemente cierra el diálogo — el cambio
     * ya quedó guardado y se aplicará la próxima vez que se abra la app.
     */
    private fun showRestartDialog() {
        AlertDialog.Builder(this)
            .setTitle("Reiniciar la app")
            .setMessage("Este cambio necesita que reinicies la app para aplicarse. ¿Reiniciar ahora?")
            .setPositiveButton("Reiniciar ahora") { _, _ ->
                val intent = android.content.Intent(this, DiscoveryKidsLauncherActivity::class.java)
                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                Runtime.getRuntime().exit(0)
            }
            .setNegativeButton("Más tarde", null)
            .setCancelable(true)
            .show()
    }

    // ── Diálogo: Duración del Screenbug ─────────────────────────────────────
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
        val inflater = LayoutInflater.from(this)
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
        val label = packageManager.getApplicationLabel(packageInfo.applicationInfo).toString()
        val versionInfoText = "$label • $versionName"

        versionInfo.text = versionInfoText
    }
}
