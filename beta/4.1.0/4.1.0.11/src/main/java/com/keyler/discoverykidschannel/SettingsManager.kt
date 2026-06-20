package com.keyler.discoverykidschannel

import android.content.Context
import android.content.SharedPreferences

/**
 * SettingsManager — Preview 2006.4.1.0.11
 *
 * Wrapper sobre SharedPreferences para las opciones configurables de la app.
 * Único punto de lectura/escritura de configuración; tanto LiveDiscoveryKids
 * como SettingsActivity pasan siempre por aquí (nunca acceden a SharedPreferences
 * directamente) para evitar inconsistencias de keys o valores por defecto duplicados.
 *
 * Modos de configuración:
 *   COMPLETA      → muestra todas las opciones disponibles.
 *   PROFESIONAL   → muestra únicamente las opciones esenciales (subset simplificado).
 * El modo solo afecta qué se MUESTRA en SettingsActivity; los valores guardados
 * son los mismos sin importar el modo activo.
 */
object SettingsManager {

    private const val PREFS_NAME = "dk_settings"

    private const val KEY_BG_MUSIC_ENABLED = "bg_music_enabled"
    private const val KEY_DEBUG_MODE_ENABLED = "debug_mode_enabled"
    private const val KEY_CRT_BRIGHTNESS = "crt_brightness"      // 0.0f..1.0f (multiplicador sobre los alphas base)
    private const val KEY_SETTINGS_MODE = "settings_mode"        // "completa" | "profesional"

    enum class Mode(val key: String) {
        COMPLETA("completa"),
        PROFESIONAL("profesional");

        companion object {
            fun fromKey(key: String?): Mode = values().find { it.key == key } ?: PROFESIONAL
        }
    }

    // ── Valores por defecto ─────────────────────────────────────────────────
    const val DEFAULT_BG_MUSIC_ENABLED = true
    const val DEFAULT_DEBUG_MODE_ENABLED = false
    const val DEFAULT_CRT_BRIGHTNESS = 1.0f
    val DEFAULT_MODE = Mode.PROFESIONAL

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Música de fondo ──────────────────────────────────────────────────────
    fun isBgMusicEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BG_MUSIC_ENABLED, DEFAULT_BG_MUSIC_ENABLED)

    fun setBgMusicEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_BG_MUSIC_ENABLED, enabled).apply()
    }

    // ── Modo debug (FPS / RAM / versión en pantalla) ────────────────────────
    fun isDebugModeEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DEBUG_MODE_ENABLED, DEFAULT_DEBUG_MODE_ENABLED)

    fun setDebugModeEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_DEBUG_MODE_ENABLED, enabled).apply()
    }

    // ── Brillo del overlay CRT (0.0 = apagado, 1.0 = intensidad base de la Era) ──
    fun getCrtBrightness(context: Context): Float =
        prefs(context).getFloat(KEY_CRT_BRIGHTNESS, DEFAULT_CRT_BRIGHTNESS)

    fun setCrtBrightness(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_CRT_BRIGHTNESS, value.coerceIn(0f, 1f)).apply()
    }

    // ── Modo de la pantalla de Configuración ────────────────────────────────
    fun getSettingsMode(context: Context): Mode =
        Mode.fromKey(prefs(context).getString(KEY_SETTINGS_MODE, DEFAULT_MODE.key))

    fun setSettingsMode(context: Context, mode: Mode) {
        prefs(context).edit().putString(KEY_SETTINGS_MODE, mode.key).apply()
    }
}
