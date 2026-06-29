/*
 ESTO TIENE LICENCIA GNU LICENSE APACHE

 NO ELIMINAR NINGUN COMENTARIO AGREGADO
*/
package com.keyler.discoverykidschannel

import android.util.Log

/**
 * ChannelPositionTracker.kt — Preview 2006.4.1.0.21
 *
 * Funciones de extensión de LiveDiscoveryKids: position tracker (guarda la
 * posición del video cada 16 ms) y los helpers de scheduling de tareas
 * diferidas (post/cancelAllTasks) usados en todo el resto del flujo del canal.
 *
 * Reorganización 4.1.0.21 — código movido tal cual desde LiveDiscoveryKids.kt
 * (10 semanas desde el primer release, ver CHANGELOG). Sin cambios de
 * comportamiento: mismo estado de instancia, mismas funciones, solo
 * agrupadas por responsabilidad en su propio archivo.
 */

// ══════════════════════════════════════════════════════════════════════════
// Position tracker – guarda la posición del video cada 500 ms
// Resuelve el bug donde videoView.currentPosition devuelve 0 en onPause
// porque Android ya pausó el VideoView antes de llamar al callback.
// ══════════════════════════════════════════════════════════════════════════

/** Inicia el guardado continuo de posición. Llamar al arrancar un segmento de programa. */
internal fun LiveDiscoveryKids.startPositionTracker() {
    positionTrackerHandler.removeCallbacksAndMessages(null)
    positionTrackerHandler.post(positionTrackerRunnable)
    Log.d(LiveDiscoveryKids.TAG, "PositionTracker STARTED")
}

/** Detiene el guardado continuo de posición. Llamar en bumpers, comerciales y onPause. */
internal fun LiveDiscoveryKids.stopPositionTracker() {
    positionTrackerHandler.removeCallbacksAndMessages(null)
    val loggedPos = if (isInProgramSegment) pausedPositionMs else currentClipPositionMs
    Log.d(LiveDiscoveryKids.TAG, "PositionTracker STOPPED at ${loggedPos}ms (tipo=$currentItemType)")
}

// ══════════════════════════════════════════════════════════════════════════
// Task scheduling helpers
// ══════════════════════════════════════════════════════════════════════════

internal fun LiveDiscoveryKids.post(delayMs: Long, action: () -> Unit) {
    val r = Runnable(action)
    pendingTasks += r
    handler.postDelayed(r, delayMs)
}

internal fun LiveDiscoveryKids.cancelAllTasks() {
    pendingTasks.forEach { handler.removeCallbacks(it) }
    pendingTasks.clear()
}
