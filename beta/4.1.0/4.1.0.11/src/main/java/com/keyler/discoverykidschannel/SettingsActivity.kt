/*
 ESTO TIENE LICENCIA GNU LICENSE APACHE

 NO ELIMINAR NINGUN COMENTARIO AGREGADO
*/
package com.keyler.discoverykidschannel

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

/**
 * SettingsActivity — Preview 2006.4.1.0.11
 *
 * Pantalla de configuración de la app, con diseño tipo menú OSD de TV CRT
 * (ver activity_settings.xml). Se abre desde el botón ⚙️ de LiveDiscoveryKids.
 *
 * Dos modos de visualización (no afectan los valores guardados, solo qué
 * secciones se muestran):
 *
 *   COMPLETA    → todas las opciones visibles (sección "Avanzado" incluida).
 *   PROFESIONAL → solo la sección "General" (música de fondo). Es el modo
 *                 por defecto: una vista simplificada para no abrumar con
 *                 controles técnicos (brillo CRT, debug) a quien solo quiere
 *                 prender/apagar la música.
 *
 * Todas las opciones persisten inmediatamente vía SettingsManager al cambiar
 * (no hay botón "Guardar"); LiveDiscoveryKids las lee de nuevo en su próximo
 * onCreate()/onResume() para aplicarlas.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var btnModeCompleta: LinearLayout
    private lateinit var btnModeProfesional: LinearLayout
    private lateinit var txtModeCompletaLabel: TextView
    private lateinit var txtModeProfesionalLabel: TextView

    private lateinit var sectionAvanzado: LinearLayout

    private lateinit var switchBgMusic: SwitchCompat
    private lateinit var switchDebugMode: SwitchCompat
    private lateinit var seekCrtBrightness: SeekBar
    private lateinit var txtCrtBrightnessValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.SettingsTheme)
        setContentView(R.layout.activity_settings)

        bindViews()
        loadCurrentValues()
        setupListeners()
    }

    private fun bindViews() {
        findViewById<android.widget.ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        btnModeCompleta = findViewById(R.id.btnModeCompleta)
        btnModeProfesional = findViewById(R.id.btnModeProfesional)
        txtModeCompletaLabel = findViewById(R.id.txtModeCompletaLabel)
        txtModeProfesionalLabel = findViewById(R.id.txtModeProfesionalLabel)

        sectionAvanzado = findViewById(R.id.sectionAvanzado)

        switchBgMusic = findViewById(R.id.switchBgMusic)
        switchDebugMode = findViewById(R.id.switchDebugMode)
        seekCrtBrightness = findViewById(R.id.seekCrtBrightness)
        txtCrtBrightnessValue = findViewById(R.id.txtCrtBrightnessValue)
    }

    /** Carga los valores guardados en SettingsManager y refleja el modo activo. */
    private fun loadCurrentValues() {
        switchBgMusic.isChecked = SettingsManager.isBgMusicEnabled(this)
        switchDebugMode.isChecked = SettingsManager.isDebugModeEnabled(this)

        val brightnessPercent = (SettingsManager.getCrtBrightness(this) * 100).toInt()
        seekCrtBrightness.progress = brightnessPercent
        txtCrtBrightnessValue.text = "$brightnessPercent%"

        applyModeUi(SettingsManager.getSettingsMode(this))
    }

    private fun setupListeners() {
        btnModeCompleta.setOnClickListener {
            SettingsManager.setSettingsMode(this, SettingsManager.Mode.COMPLETA)
            applyModeUi(SettingsManager.Mode.COMPLETA)
        }
        btnModeProfesional.setOnClickListener {
            SettingsManager.setSettingsMode(this, SettingsManager.Mode.PROFESIONAL)
            applyModeUi(SettingsManager.Mode.PROFESIONAL)
        }

        switchBgMusic.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.setBgMusicEnabled(this, isChecked)
        }

        switchDebugMode.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.setDebugModeEnabled(this, isChecked)
        }

        seekCrtBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                txtCrtBrightnessValue.text = "$progress%"
                if (fromUser) {
                    SettingsManager.setCrtBrightness(this@SettingsActivity, progress / 100f)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    /**
     * Alterna la visibilidad de la sección "Avanzado" y resalta el botón
     * de modo activo. PROFESIONAL oculta brillo CRT y modo debug; COMPLETA
     * los muestra. La música de fondo es siempre visible en ambos modos.
     */
    private fun applyModeUi(mode: SettingsManager.Mode) {
        val isCompleta = mode == SettingsManager.Mode.COMPLETA
        sectionAvanzado.visibility = if (isCompleta) View.VISIBLE else View.GONE

        btnModeCompleta.setBackgroundResource(
            if (isCompleta) R.drawable.bg_mode_selected else R.drawable.bg_mode_unselected
        )
        btnModeProfesional.setBackgroundResource(
            if (isCompleta) R.drawable.bg_mode_unselected else R.drawable.bg_mode_selected
        )

        val activeColor = getColor(R.color.dk_phosphor_green)
        val inactiveColor = getColor(R.color.dk_text_secondary)
        txtModeCompletaLabel.setTextColor(if (isCompleta) activeColor else inactiveColor)
        txtModeProfesionalLabel.setTextColor(if (isCompleta) inactiveColor else activeColor)
    }
}
