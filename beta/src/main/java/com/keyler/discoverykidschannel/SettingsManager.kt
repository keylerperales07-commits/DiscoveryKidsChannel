/*
 ESTO TIENE LICENCIA GNU LICENSE APACHE

 NO ELIMINAR NINGUN COMENTARIO AGREGADO
*/
package com.keyler.discoverykidschannel

import android.content.Context
import android.content.SharedPreferences

/**
 * SettingsManager — Preview 2006.4.1.0.12
 *
 * Wrapper sobre SharedPreferences para las opciones configurables de la app.
 * Único punto de lectura/escritura de configuración; tanto LiveDiscoveryKids
 * como SettingsActivity pasan siempre por aquí (nunca acceden a SharedPreferences
 * directamente) para evitar inconsistencias de keys o valores por defecto duplicados.
 *
 * Cambios en esta Preview (4.1.0.12):
 *   - ELIMINADO: modo debug configurable. El overlay de FPS/RAM/versión ahora
 *     se muestra automáticamente en builds Preview (sin opción de usuario).
 *   - ELIMINADO: modo de pantalla Completa/Profesional. La pantalla de
 *     Configuración ahora es una sola lista simple, sin secciones que
 *     ocultar (ver activity_settings.xml, estilo lista de Android Settings).
 *   - CAMBIADO: brillo del CRT (0–100% slider) → activar/desactivar (on/off).
 *   - NUEVO: duración antes de aparecer el Screenbug (antes fija en 20 s).
 *   - NUEVO: rango Min/Max del intervalo aleatorio de comerciales (antes fijo 3–9 min).
 *   - NUEVO: Forzar 4:3 (on/off). Ver KEY_FORCE_ASPECT_RATIO.
 */
object SettingsManager {

    private const val PREFS_NAME = "dk_settings"

    private const val KEY_BG_MUSIC_ENABLED = "bg_music_enabled"
    private const val KEY_CRT_EFFECT_ENABLED = "crt_effect_enabled"
    private const val KEY_SCREENBUG_DELAY_SEC = "screenbug_delay_sec"
    private const val KEY_COMMERCIAL_MIN_MIN = "commercial_interval_min_minutes"
    private const val KEY_COMMERCIAL_MAX_MIN = "commercial_interval_max_minutes"
    private const val KEY_FORCE_ASPECT_RATIO = "force_aspect_ratio_4_3"
    private const val KEY_PREVIEW_UPDATES_ENABLED = "preview_updates_enabled"

    // ── Valores por defecto ─────────────────────────────────────────────────
    const val DEFAULT_BG_MUSIC_ENABLED = true
    const val DEFAULT_CRT_EFFECT_ENABLED = true
    const val DEFAULT_SCREENBUG_DELAY_SEC = 20
    const val DEFAULT_COMMERCIAL_MIN_MINUTES = 3
    const val DEFAULT_COMMERCIAL_MAX_MINUTES = 9
    const val DEFAULT_FORCE_ASPECT_RATIO = false
    const val DEFAULT_PREVIEW_UPDATES_ENABLED = false

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Música de fondo ──────────────────────────────────────────────────────
    fun isBgMusicEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BG_MUSIC_ENABLED, DEFAULT_BG_MUSIC_ENABLED)

    fun setBgMusicEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_BG_MUSIC_ENABLED, enabled).apply()
    }

    // ── Efecto CRT (activar/desactivar — Preview 4.1.0.12, antes era brillo 0-100%) ──
    fun isCrtEffectEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CRT_EFFECT_ENABLED, DEFAULT_CRT_EFFECT_ENABLED)

    fun setCrtEffectEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_CRT_EFFECT_ENABLED, enabled).apply()
    }

    // ── Duración antes de aparecer el Screenbug, en segundos ────────────────
    fun getScreenbugDelaySec(context: Context): Int =
        prefs(context).getInt(KEY_SCREENBUG_DELAY_SEC, DEFAULT_SCREENBUG_DELAY_SEC)

    fun setScreenbugDelaySec(context: Context, seconds: Int) {
        prefs(context).edit().putInt(KEY_SCREENBUG_DELAY_SEC, seconds.coerceIn(0, 300)).apply()
    }

    // ── Intervalo aleatorio de comerciales (minutos) ────────────────────────
    fun getCommercialMinMinutes(context: Context): Int =
        prefs(context).getInt(KEY_COMMERCIAL_MIN_MIN, DEFAULT_COMMERCIAL_MIN_MINUTES)

    fun getCommercialMaxMinutes(context: Context): Int =
        prefs(context).getInt(KEY_COMMERCIAL_MAX_MIN, DEFAULT_COMMERCIAL_MAX_MINUTES)

    /**
     * Guarda el rango Min/Max. Si min > max, se intercambian para evitar
     * un rango inválido en calcBreaks() (que asume min <= max).
     */
    fun setCommercialInterval(context: Context, minMinutes: Int, maxMinutes: Int) {
        val safeMin = minMinutes.coerceIn(1, 60)
        val safeMax = maxMinutes.coerceIn(1, 60)
        val finalMin = minOf(safeMin, safeMax)
        val finalMax = maxOf(safeMin, safeMax)
        prefs(context).edit()
            .putInt(KEY_COMMERCIAL_MIN_MIN, finalMin)
            .putInt(KEY_COMMERCIAL_MAX_MIN, finalMax)
            .apply()
    }

    // ── Forzar 4:3 ───────────────────────────────────────────────────────────
    // OFF (false, default) → VideoView con match_parent (alto) / match_parent (ancho):
    //                         ocupa todo el marco 4:3, respetando su proporción real.
    // ON  (true)            → VideoView con match_parent (alto) / wrap_content (ancho):
    //                         se estira para llenar el marco 4:3 (comportamiento histórico).
    fun isForceAspectRatioEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_FORCE_ASPECT_RATIO, DEFAULT_FORCE_ASPECT_RATIO)

    fun setForceAspectRatioEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_FORCE_ASPECT_RATIO, enabled).apply()
    }

    // ── Habilitar versiones Preview en Actualizaciones (Preview 4.1.0.21) ──
    // OFF (false, default) → AppUpdater solo considera releases marcados como
    //                         "Latest" estable en GitHub (release no-prerelease).
    // ON  (true)            → AppUpdater también puede instalar releases
    //                         marcados como Preview/prerelease.
    fun isPreviewUpdatesEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PREVIEW_UPDATES_ENABLED, DEFAULT_PREVIEW_UPDATES_ENABLED)

    fun setPreviewUpdatesEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PREVIEW_UPDATES_ENABLED, enabled).apply()
    }
}
