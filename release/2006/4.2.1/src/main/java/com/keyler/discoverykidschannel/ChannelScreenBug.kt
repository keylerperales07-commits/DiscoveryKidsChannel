/*
 ESTO TIENE LICENCIA GNU LICENSE APACHE

 NO ELIMINAR NINGUN COMENTARIO AGREGADO
*/
package com.keyler.discoverykidschannel

import android.util.Log

/**
 * ChannelScreenBug.kt — Preview 2006.4.1.0.21
 *
 * Funciones de extensión de LiveDiscoveryKids: animación de aparición/
 * desaparición del Screenbug (logo superpuesto). El cálculo de CUÁNDO
 * mostrarlo/ocultarlo vive en ChannelProgramPlayback.kt (scheduleSegmentLogic);
 * aquí solo está el cómo (fade in/out con Canvas+ViewPropertyAnimator).
 *
 * Reorganización 4.1.0.21 — código movido tal cual desde LiveDiscoveryKids.kt.
 * Sin cambios de comportamiento.
 */

// ══════════════════════════════════════════════════════════════════════════
// ScreenBug animation
// ══════════════════════════════════════════════════════════════════════════

internal fun LiveDiscoveryKids.fadeInBug() {
    Log.d(LiveDiscoveryKids.TAG, "ScreenBug FADE IN [res=$currentScreenBugRes]")
    screenBug.setImageResource(currentScreenBugRes)
    screenBug.animate()
        .alpha(1f)
        .setDuration(LiveDiscoveryKids.FADE_MS)
        .start()
}

internal fun LiveDiscoveryKids.fadeOutBug() {
    Log.d(LiveDiscoveryKids.TAG, "ScreenBug FADE OUT")
    screenBug.animate()
        .alpha(0f)
        .setDuration(LiveDiscoveryKids.FADE_MS)
        .start()
}

/** Instantly sets alpha without animation (used during transitions). */
internal fun LiveDiscoveryKids.setBugAlpha(alpha: Float) {
    screenBug.animate().cancel()
    screenBug.alpha = alpha
}
