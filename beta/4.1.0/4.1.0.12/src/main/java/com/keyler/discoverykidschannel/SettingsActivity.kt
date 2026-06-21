/*
 ESTO TIENE LICENCIA GNU LICENSE APACHE

 NO ELIMINAR NINGUN COMENTARIO AGREGADO
*/
package com.keyler.discoverykidschannel

import android.app.AlertDialog
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
 * SettingsActivity — Preview 2006.4.1.0.12
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
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var switchBgMusic: SwitchCompat
    private lateinit var switchCrtEffect: SwitchCompat
    private lateinit var switchForceAspectRatio: SwitchCompat

    private lateinit var itemScreenbugDelay: LinearLayout
    private lateinit var txtScreenbugDelayValue: TextView

    private lateinit var itemCommercialInterval: LinearLayout
    private lateinit var txtCommercialIntervalValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.SettingsTheme)
        setContentView(R.layout.activity_settings)

        bindViews()
        loadCurrentValues()
        setupListeners()
    }

    private fun bindViews() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        switchBgMusic = findViewById(R.id.switchBgMusic)
        switchCrtEffect = findViewById(R.id.switchCrtEffect)
        switchForceAspectRatio = findViewById(R.id.switchForceAspectRatio)

        itemScreenbugDelay = findViewById(R.id.itemScreenbugDelay)
        txtScreenbugDelayValue = findViewById(R.id.txtScreenbugDelayValue)

        itemCommercialInterval = findViewById(R.id.itemCommercialInterval)
        txtCommercialIntervalValue = findViewById(R.id.txtCommercialIntervalValue)
    }

    /** Carga los valores guardados en SettingsManager y los refleja en cada control. */
    private fun loadCurrentValues() {
        switchBgMusic.isChecked = SettingsManager.isBgMusicEnabled(this)
        switchCrtEffect.isChecked = SettingsManager.isCrtEffectEnabled(this)
        switchForceAspectRatio.isChecked = SettingsManager.isForceAspectRatioEnabled(this)

        refreshScreenbugDelayLabel()
        refreshCommercialIntervalLabel()
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

        itemScreenbugDelay.setOnClickListener { showScreenbugDelayDialog() }
        itemCommercialInterval.setOnClickListener { showCommercialIntervalDialog() }
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

    /** Envuelve un EditText con padding horizontal estándar para diálogos. */
    private fun wrapInputWithPadding(input: EditText): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, 0, pad, 0)
            addView(input)
        }
    }
}
