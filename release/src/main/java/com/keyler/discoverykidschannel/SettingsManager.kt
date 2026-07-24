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
 *
 * Release 4.6.0 — NUEVO: estado activado/desactivado por programa
 * (KEY_PROGRAM_ENABLED_PREFIX + índice), usado por el nuevo
 * DiscoveryKidsLauncherActivity para que el usuario elija qué programas
 * (pro1–pro4.mp4) quiere que salgan al aire. Todos activados por defecto
 * (comportamiento idéntico al de antes de esta Release). LiveDiscoveryKids
 * consulta isProgramEnabled() en playProgram() y findAvailableProgramIndex()
 * para saltear los programas desactivados, igual que ya salteaba los que
 * faltaban en la carpeta Movies.
 *
 * Release 2009.5.0.0 — "Parque Imaginario" (Fase 4, inicio rama 5.x). NUEVO:
 *   - KEY_EXPERIMENTAL_ENABLED: interruptor maestro de la sección "Experimental"
 *     de Configuración (desactivado por defecto). Habilita el nuevo Discovery
 *     Kids Launcher como pantalla de inicio real (en vez de pasar directo a
 *     LiveDiscoveryKids) y toda la configuración avanzada de programas
 *     (cantidad de programas, video elegido por el usuario, ya_regresa /
 *     continuamos personalizados por programa). Ver DiscoveryKidsLauncherActivity.
 *   - KEY_PROGRAM_COUNT: cantidad de programas que arma la programación
 *     (1–24). Solo tiene efecto con Experimental activado; con Experimental
 *     desactivado el canal sigue el comportamiento clásico de 4 programas
 *     fijos (pro1–pro4.mp4 en la carpeta Movies).
 *   - KEY_PROGRAM_URI_PREFIX: Uri (content://, persistida via SAF) del video
 *     que el usuario eligió para el programa N. Si no hay Uri guardada,
 *     resolveProgram() cae al comportamiento clásico (buscar pro{N}.mp4).
 *   - KEY_YAREGRESA_CUSTOM_PREFIX / KEY_YAREGRESA_URI_PREFIX y
 *     KEY_CONTINUAMOS_CUSTOM_PREFIX / KEY_CONTINUAMOS_URI_PREFIX: por
 *     programa, si el usuario activó "Personalizado" para el ya_regresa o el
 *     continuamos de ESE programa (en vez del predeterminado que trae la
 *     app) y qué Uri eligió. Ver resolveYaRegresaUri()/resolveContinuamosUri()
 *     en LiveDiscoveryKids.kt.
 *
 * Release 2009.5.2.1 — ELIMINADO: KEY_TEXTURE_VIEW_ENABLED y el motor de
 *   video basado en TextureView que activaba (ver DkVideoView.kt).
 *
 * Release 5.4.0 — NUEVO: KEY_INTRO_ENABLED_PREFIX/KEY_INTRO_URI_PREFIX y
 *   KEY_CREDITOS_ENABLED_PREFIX/KEY_CREDITOS_URI_PREFIX — Intro y Créditos
 *   por programa, configurables desde Discovery Kids Launcher →
 *   Configuración de Programa. A diferencia de ya_regresa/continuamos NO
 *   traen un video predeterminado: por eso son "activado" + "Uri" en vez de
 *   "personalizado" (si no hay Uri elegida, no se agregan al playlist — ver
 *   LiveDiscoveryKids.hasValidIntro()/hasValidCreditos()).
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
    private const val KEY_PROGRAM_ENABLED_PREFIX = "program_enabled_"   // Release 4.6.0

    // ── Release 2009.5.0.0 — Experimental / Discovery Kids Launcher ─────────
    private const val KEY_EXPERIMENTAL_ENABLED = "experimental_enabled"
    private const val KEY_PROGRAM_COUNT = "program_count"
    private const val KEY_PROGRAM_URI_PREFIX = "program_uri_"
    private const val KEY_YAREGRESA_CUSTOM_PREFIX = "yaregresa_custom_"
    private const val KEY_YAREGRESA_URI_PREFIX = "yaregresa_uri_"
    private const val KEY_CONTINUAMOS_CUSTOM_PREFIX = "continuamos_custom_"
    private const val KEY_CONTINUAMOS_URI_PREFIX = "continuamos_uri_"
    // ── Release 5.4.0 — Intro / Créditos por programa ────────────────────────
    // A diferencia de ya_regresa/continuamos, Intro y Créditos NO tienen un
    // video predeterminado incluido en la app: por eso son dos keys por
    // separado (activado + Uri) en vez de "personalizado" — si no hay Uri
    // elegida, no hay nada que reproducir (ver LiveDiscoveryKids.hasValidIntro()/hasValidCreditos()).
    private const val KEY_INTRO_ENABLED_PREFIX = "intro_enabled_"
    private const val KEY_INTRO_URI_PREFIX = "intro_uri_"
    private const val KEY_CREDITOS_ENABLED_PREFIX = "creditos_enabled_"
    private const val KEY_CREDITOS_URI_PREFIX = "creditos_uri_"

    // ── Valores por defecto ─────────────────────────────────────────────────
    const val DEFAULT_BG_MUSIC_ENABLED = true
    const val DEFAULT_CRT_EFFECT_ENABLED = true
    const val DEFAULT_SCREENBUG_DELAY_SEC = 20
    const val DEFAULT_COMMERCIAL_MIN_MINUTES = 3
    const val DEFAULT_COMMERCIAL_MAX_MINUTES = 9
    const val DEFAULT_FORCE_ASPECT_RATIO = false
    const val DEFAULT_PREVIEW_UPDATES_ENABLED = false
    const val DEFAULT_PROGRAM_ENABLED = true   // Release 4.6.0 — todos activados por defecto
    const val DEFAULT_EXPERIMENTAL_ENABLED = false   // Release 2009.5.0.0
    const val DEFAULT_PROGRAM_COUNT = 4
    const val MIN_PROGRAM_COUNT = 1
    const val MAX_PROGRAM_COUNT = 24
    const val DEFAULT_YAREGRESA_CUSTOM = false
    const val DEFAULT_CONTINUAMOS_CUSTOM = false
    const val DEFAULT_INTRO_ENABLED = false      // Release 5.4.0
    const val DEFAULT_CREDITOS_ENABLED = false   // Release 5.4.0

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

    // ── Programas activados/desactivados (Release 4.6.0 — DiscoveryKidsLauncherActivity) ──
    // [index] es 0-based (0 → pro1.mp4, 1 → pro2.mp4, etc.), igual que en todo
    // el resto del código (currentProgramIndex, PlayItem.Program, resolveProgram).
    fun isProgramEnabled(context: Context, index: Int): Boolean =
        prefs(context).getBoolean(KEY_PROGRAM_ENABLED_PREFIX + index, DEFAULT_PROGRAM_ENABLED)

    fun setProgramEnabled(context: Context, index: Int, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PROGRAM_ENABLED_PREFIX + index, enabled).apply()
    }

    // ── Experimental (Release 2009.5.0.0) ───────────────────────────────────
    // Interruptor maestro: habilita el Discovery Kids Launcher como pantalla
    // de inicio real y la configuración avanzada de programas. Al cambiarlo,
    // SettingsActivity muestra un diálogo para reiniciar la app ahora o más
    // tarde — ver showExperimentalRestartDialog() en SettingsActivity.
    fun isExperimentalEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_EXPERIMENTAL_ENABLED, DEFAULT_EXPERIMENTAL_ENABLED)

    fun setExperimentalEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_EXPERIMENTAL_ENABLED, enabled).apply()
    }

    // ── Cantidad de programas (1–24) — solo aplica con Experimental activado ──
    fun getProgramCount(context: Context): Int =
        prefs(context).getInt(KEY_PROGRAM_COUNT, DEFAULT_PROGRAM_COUNT).coerceIn(MIN_PROGRAM_COUNT, MAX_PROGRAM_COUNT)

    fun setProgramCount(context: Context, count: Int) {
        prefs(context).edit().putInt(KEY_PROGRAM_COUNT, count.coerceIn(MIN_PROGRAM_COUNT, MAX_PROGRAM_COUNT)).apply()
    }

    // ── Video elegido por el usuario para el programa [index] (SAF, content://) ──
    // Uri en formato String (persistida vía ContentResolver.takePersistableUriPermission
    // al elegirla, ver DiscoveryKidsLauncherActivity.pickProgramVideo()).
    // null/vacío → resolveProgram() cae al comportamiento clásico (pro{N}.mp4).
    fun getProgramUri(context: Context, index: Int): String? =
        prefs(context).getString(KEY_PROGRAM_URI_PREFIX + index, null)

    fun setProgramUri(context: Context, index: Int, uri: String?) {
        prefs(context).edit().putString(KEY_PROGRAM_URI_PREFIX + index, uri).apply()
    }

    // ── ya_regresa personalizado por programa ───────────────────────────────
    fun isYaRegresaCustom(context: Context, index: Int): Boolean =
        prefs(context).getBoolean(KEY_YAREGRESA_CUSTOM_PREFIX + index, DEFAULT_YAREGRESA_CUSTOM)

    fun setYaRegresaCustom(context: Context, index: Int, custom: Boolean) {
        prefs(context).edit().putBoolean(KEY_YAREGRESA_CUSTOM_PREFIX + index, custom).apply()
    }

    fun getYaRegresaUri(context: Context, index: Int): String? =
        prefs(context).getString(KEY_YAREGRESA_URI_PREFIX + index, null)

    fun setYaRegresaUri(context: Context, index: Int, uri: String?) {
        prefs(context).edit().putString(KEY_YAREGRESA_URI_PREFIX + index, uri).apply()
    }

    // ── continuamos personalizado por programa ──────────────────────────────
    fun isContinuamosCustom(context: Context, index: Int): Boolean =
        prefs(context).getBoolean(KEY_CONTINUAMOS_CUSTOM_PREFIX + index, DEFAULT_CONTINUAMOS_CUSTOM)

    fun setContinuamosCustom(context: Context, index: Int, custom: Boolean) {
        prefs(context).edit().putBoolean(KEY_CONTINUAMOS_CUSTOM_PREFIX + index, custom).apply()
    }

    fun getContinuamosUri(context: Context, index: Int): String? =
        prefs(context).getString(KEY_CONTINUAMOS_URI_PREFIX + index, null)

    fun setContinuamosUri(context: Context, index: Int, uri: String?) {
        prefs(context).edit().putString(KEY_CONTINUAMOS_URI_PREFIX + index, uri).apply()
    }

    // ── Intro por programa (Release 5.4.0) ──────────────────────────────────
    // Sin video predeterminado: isIntroEnabled=true con getIntroUri=null
    // significa "activado pero sin elegir todavía" — LiveDiscoveryKids no lo
    // incluye en el playlist hasta que haya Uri (ver hasValidIntro()), y
    // DiscoveryKidsLauncherActivity.validateChannelSetup() lo marca como
    // pendiente para avisarle al usuario antes de arrancar el canal.
    fun isIntroEnabled(context: Context, index: Int): Boolean =
        prefs(context).getBoolean(KEY_INTRO_ENABLED_PREFIX + index, DEFAULT_INTRO_ENABLED)

    fun setIntroEnabled(context: Context, index: Int, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_INTRO_ENABLED_PREFIX + index, enabled).apply()
    }

    fun getIntroUri(context: Context, index: Int): String? =
        prefs(context).getString(KEY_INTRO_URI_PREFIX + index, null)

    fun setIntroUri(context: Context, index: Int, uri: String?) {
        prefs(context).edit().putString(KEY_INTRO_URI_PREFIX + index, uri).apply()
    }

    // ── Créditos por programa (Release 5.4.0) ───────────────────────────────
    fun isCreditosEnabled(context: Context, index: Int): Boolean =
        prefs(context).getBoolean(KEY_CREDITOS_ENABLED_PREFIX + index, DEFAULT_CREDITOS_ENABLED)

    fun setCreditosEnabled(context: Context, index: Int, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_CREDITOS_ENABLED_PREFIX + index, enabled).apply()
    }

    fun getCreditosUri(context: Context, index: Int): String? =
        prefs(context).getString(KEY_CREDITOS_URI_PREFIX + index, null)

    fun setCreditosUri(context: Context, index: Int, uri: String?) {
        prefs(context).edit().putString(KEY_CREDITOS_URI_PREFIX + index, uri).apply()
    }
}
