/*
 ESTO TIENE LICENCIA GNU LICENSE APACHE

 NO ELIMINAR NINGUN COMENTARIO AGREGADO
*/
package com.keyler.discoverykidschannel

import android.net.Uri
import android.util.Log

/**
 * ChannelProgramPlayback.kt — Preview 2006.4.1.0.21
 *
 * Funciones de extensión de LiveDiscoveryKids: reproducción del programa
 * (pro1–pro4.mp4), el scheduling de Screenbug y cortes comerciales dentro
 * de un segmento de programa, y el cálculo de las posiciones de corte.
 *
 * Reorganización 4.1.0.21 — código movido tal cual desde LiveDiscoveryKids.kt.
 * Sin cambios de comportamiento.
 */

// ══════════════════════════════════════════════════════════════════════════
// Program playback
// ══════════════════════════════════════════════════════════════════════════

internal fun LiveDiscoveryKids.playProgram(idx: Int, restartFromBeginning: Boolean = true) {
    currentProgramIndex = idx
    // Release 4.3.1 — a partir de acá currentProgramIndex ya refleja un programa
    // real que empezó a salir al aire; ver comentario en goToAdjacentProgram().
    hasPlayedAnyProgram = true
    val uri = resolveProgram(idx)
    if (uri == null) {
        Log.w(LiveDiscoveryKids.TAG, "pro${idx + 1}.mp4 not found – skipping")
        playlistIndex++
        advance()
        return
    }

    Log.d(LiveDiscoveryKids.TAG, "▶ PROGRAM pro${idx + 1}")
    currentProgramUri = uri
    breakQueue.clear()
    // Release 2006.4.1.1: limpia cualquier estado de clip no-programa heredado
    // del ítem anterior (bumper/enseguida/comercial), ya que isInProgramSegment
    // tendrá prioridad de cualquier forma, pero evita estado fantasma confuso en logs.
    currentClipUri = null
    currentClipOnComplete = null

    val startPos = if (restartFromBeginning) 0 else videoView.currentPosition
    beginProgramSegment(uri, startOffsetMs = startPos, isFirstPlay = restartFromBeginning)
}

/**
* Plays the program starting at [startOffsetMs].
* [isFirstPlay] = true  → recalculate breaks from scratch.
* [isFirstPlay] = false → breaks already trimmed; resume only.
*/
internal fun LiveDiscoveryKids.beginProgramSegment(
    uri: Uri,
    startOffsetMs: Int,
    isFirstPlay: Boolean,
    isNewSegment: Boolean = true
) {
    cancelAllTasks()
    setBugAlpha(0f)
    isInCommercialBlock = false   // garantiza reset si se llega aquí desde cualquier ruta

    // Beta 3.4.0.42 — BUG FIX: cancelar cualquier animación pendiente del videoView
    // y forzar alpha=0f ANTES de setVideoURI() para garantizar estado limpio
    // independientemente de animaciones previas interrumpidas (ej: fadeOut a medias
    // al salir al segundo plano).
    videoView.animate().cancel()
    videoView.alpha = 0f

    videoView.setVideoURI(uri)
    videoView.setOnPreparedListener { mp ->
        mp.isLooping = false

        programDuration = mp.duration

        if (isFirstPlay) {
            breakQueue = calcBreaks(programDuration).toMutableList()
        }

        if (startOffsetMs > 0) videoView.seekTo(startOffsetMs)

        scheduleSegmentLogic(startOffsetMs, isNewSegment = isNewSegment)
        videoView.alpha = 0f
        videoView.start()
        videoView.animate()
            .alpha(1f)
            .setDuration(LiveDiscoveryKids.TRANSITION_FADE_IN_MS)
            .start()
        isInProgramSegment = true
        currentItemType = "program"
        startPositionTracker()
        startBgMusic()
    }
    videoView.setOnCompletionListener {
        Log.d(LiveDiscoveryKids.TAG, "Program ended")
        cancelAllTasks()
        setBugAlpha(0f)
        isInProgramSegment = false
        stopPositionTracker()
        pausedPositionMs = 0
        stopBgMusic()
        playlistIndex++
        advance()
    }
}

/**
 * Schedules screenbug show/hide and the next commercial break
 * for the current segment starting at [segmentStartMs] in program time.
 *
 * Beta 3.4.0.42 — BUG FIX: ajusta los delays del screenbug descontando el
 * tiempo ya transcurrido en el segmento antes de pausar.
 * - elapsed = segmentStartMs - currentSegmentStartMs (ms ya vividos en este segmento)
 * - Si elapsed >= bugShowDelayMs → screenbug debe estar visible ya; aparece
 *   inmediatamente con setBugAlpha(1f) y se programa solo el fadeOut.
 * - Si elapsed < bugShowDelayMs → se programa fadeIn con delay reducido.
 * Esto evita que al volver de segundo plano el screenbug reinicie su cuenta
 * de [bugShowDelayMs] desde cero aunque ya debía estar visible.
 *
 * Preview 2006.4.1.0.12: bugShowDelayMs ahora es configurable desde
 * SettingsActivity (antes BUG_SHOW_DELAY fijo en 20 s).
 */
internal fun LiveDiscoveryKids.scheduleSegmentLogic(segmentStartMs: Int, isNewSegment: Boolean) {
    // Release 2006.4.1.1 — BUG FIX: el cálculo de `elapsed` comparaba
    // segmentStartMs contra currentSegmentStartMs DESPUÉS de haberlo
    // sobreescrito con el mismo segmentStartMs, dando siempre elapsed = 0.
    // Esto hacía que el screenbug reiniciara su cuenta cada vez que la app
    // volvía de segundo plano o de un cambio de Activity (ej: SettingsActivity),
    // en vez de "recordar" cuánto tiempo ya había transcurrido en el segmento.
    //
    // Ahora currentSegmentStartMs solo se actualiza cuando arranca un segmento
    // REALMENTE nuevo (isNewSegment = true, ej: tras un corte comercial o al
    // iniciar el programa). Al reanudar el mismo segmento (isNewSegment = false)
    // se conserva el valor anterior, permitiendo calcular cuánto tiempo pasó.
    val previousSegmentStartMs = currentSegmentStartMs
    if (isNewSegment) {
        currentSegmentStartMs = segmentStartMs
    }

    // Determine end of this segment (next break or program end)
    val segmentEndMs = if (breakQueue.isNotEmpty()) breakQueue[0] else programDuration
    val segmentDuration = (segmentEndMs - segmentStartMs).toLong().coerceAtLeast(0)

    Log.d(LiveDiscoveryKids.TAG, "Segment: ${segmentStartMs}ms → ${segmentEndMs}ms (${segmentDuration}ms)")

    // Calcula cuántos ms del segmento ya transcurrieron antes de este (re)arranque.
    // En la primera llamada de un segmento nuevo elapsed = 0 (previousSegmentStartMs
    // todavía no se actualizó arriba, así que coincide con segmentStartMs).
    // Al reanudar desde segundo plano elapsed = segmentStartMs - currentSegmentStartMs
    // (el valor de arranque ORIGINAL del segmento, que no se tocó).
    val baseSegmentStartMs = if (isNewSegment) segmentStartMs else previousSegmentStartMs
    val elapsed = (segmentStartMs - baseSegmentStartMs).toLong().coerceAtLeast(0L)

    val bugShowDelay = (bugShowDelayMs - elapsed).coerceAtLeast(0L)

    if (elapsed >= bugShowDelayMs) {
        // El screenbug ya debía estar visible — aparece inmediatamente sin animación
        Log.d(LiveDiscoveryKids.TAG, "ScreenBug: elapsed=${elapsed}ms >= bugShowDelayMs(${bugShowDelayMs}) → aparece inmediatamente")
        setBugAlpha(1f)
    } else if (segmentDuration > bugShowDelay) {
        post(bugShowDelay) { fadeInBug() }
    }

    val hideAt = segmentDuration - LiveDiscoveryKids.BUG_HIDE_EARLY
    if (hideAt > bugShowDelay) {
        post(hideAt) { fadeOutBug() }
    }

    if (breakQueue.isNotEmpty()) {
        val breakProgramPos = breakQueue[0]
        post(segmentDuration) {
            val resumePos = breakProgramPos
            breakQueue.removeAt(0)
            playCommercial(resumePos)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════
// Commercial break calculation
// ══════════════════════════════════════════════════════════════════════════

/**
 * Returns a list of program positions (in ms) where commercial breaks occur.
 *
 * Beta 2005.4.0.0.4 — Distribución de intervalo aleatorio:
 *   Los breaks se colocan en posiciones acumuladas usando un intervalo aleatorio
 *   entre [breakIntervalMinMs] (3 min por defecto) y [breakIntervalMaxMs] (9 min por defecto).
 *   Cada corte elige su propio intervalo independientemente, generando una
 *   programación publicitaria variable más parecida a la TV real.
 *
 * Preview 2006.4.1.0.12 — Intervalo configurable desde SettingsActivity
 *   (antes BREAK_INTERVAL_MIN_MS/MAX_MS fijos en 3–9 min).
 *
 * Release 4.0.1 — Zona de protección al final del programa:
 *   No se programa ningún corte dentro de los últimos [BREAK_CUTOFF_MS] (3 min)
 *   del programa. El while ahora compara contra (durationMs - BREAK_CUTOFF_MS)
 *   en lugar de durationMs, garantizando que el final del programa nunca sea
 *   interrumpido por un comercial.
 *
 * Distribución anterior (≤3.4.1): intervalo fijo de 9 minutos exactos —
 *   los breaks siempre ocurrían a los 9 min, 18 min, 27 min, etc.
 *
 * Programs shorter than [MIN_DURATION_FOR_BREAKS] get no breaks.
 */
internal fun LiveDiscoveryKids.calcBreaks(durationMs: Int): List<Int> {
    if (durationMs < LiveDiscoveryKids.MIN_DURATION_FOR_BREAKS) return emptyList()
    val cutoff = durationMs - LiveDiscoveryKids.BREAK_CUTOFF_MS
    if (cutoff <= 0) return emptyList()
    val breaks = mutableListOf<Int>()
    var breakPos = (breakIntervalMinMs + (Math.random() * (breakIntervalMaxMs - breakIntervalMinMs)).toLong())
    while (breakPos < cutoff) {
        breaks.add(breakPos.toInt())
        breakPos += (breakIntervalMinMs + (Math.random() * (breakIntervalMaxMs - breakIntervalMinMs)).toLong())
    }
    return breaks
}
