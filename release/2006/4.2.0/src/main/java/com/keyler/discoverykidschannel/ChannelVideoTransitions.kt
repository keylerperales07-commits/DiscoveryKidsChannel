/*
 ESTO TIENE LICENCIA GNU LICENSE APACHE

 NO ELIMINAR NINGUN COMENTARIO AGREGADO
*/
package com.keyler.discoverykidschannel

import android.net.Uri

/**
 * ChannelVideoTransitions.kt — Preview 2006.4.1.0.21
 *
 * Funciones de extensión de LiveDiscoveryKids: los helpers de bajo nivel
 * que reproducen un clip en el VideoView con FadeOut/FadeIn, usados por
 * TODOS los cambios de video del canal (bumper, enseguida, comerciales,
 * continuamos, retoma de programa tras un corte).
 *
 *   - playUri(): reproducción simple sin transición (usada internamente
 *     por rutas que manejan su propio FadeIn, ej. beginProgramSegment).
 *   - playUriWithTransition(): FadeOut/FadeIn estándar para arrancar un
 *     clip nuevo desde el principio.
 *   - resumeUriWithSeek(): igual que playUriWithTransition() pero retoma
 *     un clip en una posición ya avanzada (seekTo dentro de onPrepared),
 *     usado al volver de segundo plano (Release 2006.4.1.1).
 *
 * Reorganización 4.1.0.21 — código movido tal cual desde LiveDiscoveryKids.kt.
 * Sin cambios de comportamiento.
 */

// ══════════════════════════════════════════════════════════════════════════
// Video playback helper
// ══════════════════════════════════════════════════════════════════════════

/** Plays [uri] and calls [onComplete] when the video finishes. */
internal fun LiveDiscoveryKids.playUri(uri: Uri, onComplete: () -> Unit) {
    videoView.setOnPreparedListener { mp ->
        mp.isLooping = false
        videoView.start()
    }
    videoView.setOnCompletionListener { onComplete() }
    videoView.setVideoURI(uri)
    videoView.requestFocus()
}

/**
 * Beta 2003.3.2.0.21 — BUG FIX: FadeOut 2 s ANTES del fin del video.
 * Beta 2003.3.2.0.22 — Parámetro [fadeOutMs] para FadeOut diferenciado por tipo de clip.
 *
 * Versión anterior (3.2.0.20): el FadeOut se disparaba DESPUÉS de que el video
 * ya había terminado (en onCompletion), causando un corte abrupto visible.
 *
 * Nueva estrategia (3.2.0.21+):
 *   1. Al prepararse el video (onPrepared) se calcula el retardo del FadeOut:
 *      max(0, duration - fadeOutMs). Esto dispara la animación [fadeOutMs] ms
 *      antes del fin real del clip.
 *   2. El FadeOut cancela el onCompletionListener activo y al terminar la
 *      animación ejecuta [onComplete] directamente, iniciando el siguiente clip.
 *   3. onCompletionListener actúa solo como FALLBACK para clips más cortos
 *      que [fadeOutMs] (el handler ya habrá ejecutado onComplete
 *      antes o al mismo tiempo, la segunda llamada se ignora con la guardia
 *      [transitionCompleted]).
 *
 * Release 3.3.0: todos los FadeOut unificados a 500 ms (TRANSITION_FADE_OUT_MS).
 * El parámetro [fadeOutMs] se conserva para posibles ajustes futuros; todos los
 * callers actuales usan el valor por defecto.
 * El FadeIn del clip entrante siempre usa TRANSITION_FADE_IN_MS (1 s).
 *
 * Se usa en TODOS los cambios de video del canal:
 *   enseguida, bumper, StandaloneCommercial, ya_regresa, comercial,
 *   continuamos y retoma de programa tras un corte comercial.
 *
 * playUri() se conserva sin modificar para las rutas internas que no
 * necesitan transición (ej.: beginProgramSegment maneja su propio FadeIn).
 */
internal fun LiveDiscoveryKids.playUriWithTransition(
    uri: Uri,
    fadeOutMs: Long = LiveDiscoveryKids.TRANSITION_FADE_OUT_MS,
    onComplete: () -> Unit
) {
    // Release 2006.4.1.1: guarda qué clip está sonando y cómo continuar el
    // flujo cuando termine, para que onResume() pueda reanudarlo en vez de
    // reiniciarlo si la app pasa a segundo plano mientras se reproduce.
    currentClipUri = uri
    currentClipPositionMs = 0
    currentClipOnComplete = onComplete
    startPositionTracker()

    // FadeOut de cierre del clip anterior — 500 ms (TRANSITION_FADE_OUT_MS) uniforme para todos los clips
    videoView.animate()
        .alpha(0f)
        .setDuration(fadeOutMs)
        .withEndAction {
            var transitionCompleted = false   // guardia anti-doble disparo

            videoView.setOnPreparedListener { mp ->
                mp.isLooping = false
                videoView.alpha = 0f
                videoView.start()
                videoView.animate()
                    .alpha(1f)
                    .setDuration(LiveDiscoveryKids.TRANSITION_FADE_IN_MS)
                    .start()

                val duration = mp.duration
                val fadeOutDelay = (duration - fadeOutMs).coerceAtLeast(0L)
                post(fadeOutDelay) {
                    if (!transitionCompleted) {
                        transitionCompleted = true
                        videoView.setOnCompletionListener(null)
                        videoView.animate()
                            .alpha(0f)
                            .setDuration(fadeOutMs)
                            .withEndAction {
                                currentClipUri = null
                                currentClipOnComplete = null
                                onComplete()
                            }
                            .start()
                    }
                }
            }

            videoView.setOnCompletionListener {
                if (!transitionCompleted) {
                    transitionCompleted = true
                    currentClipUri = null
                    currentClipOnComplete = null
                    onComplete()
                }
            }

            videoView.setVideoURI(uri)
            videoView.requestFocus()
        }
        .start()
}

/**
 * Release 2006.4.1.1 — Reanuda un clip no-programa (bumper, enseguida,
 * comercial) exactamente en [startOffsetMs] en vez de reiniciarlo desde
 * el principio. Se usa desde onResume() cuando la app vuelve de segundo
 * plano o de un cambio de Activity mientras uno de estos clips sonaba.
 *
 * A diferencia de playUriWithTransition():
 *   - No hace FadeOut del clip "anterior" (ya no hay nada en pantalla,
 *     el VideoView quedó en alpha=0 desde onPause/setBugAlpha).
 *   - Hace seekTo(startOffsetMs) dentro de onPrepared, igual que
 *     beginProgramSegment(), porque Android puede haber liberado el
 *     surface del VideoView mientras la app estaba en segundo plano y
 *     seekTo() directo fuera de onPrepared se ignora silenciosamente.
 *   - El FadeOut de salida hacia el siguiente clip se reprograma con el
 *     tiempo que realmente queda (duration - elapsed), no con la duración
 *     completa, para no cortar el clip antes de tiempo.
 */
internal fun LiveDiscoveryKids.resumeUriWithSeek(
    uri: Uri,
    startOffsetMs: Int,
    fadeOutMs: Long = LiveDiscoveryKids.TRANSITION_FADE_OUT_MS,
    onComplete: () -> Unit
) {
    currentClipUri = uri
    currentClipPositionMs = startOffsetMs
    currentClipOnComplete = onComplete
    startPositionTracker()

    var transitionCompleted = false

    videoView.animate().cancel()
    videoView.alpha = 0f

    videoView.setOnPreparedListener { mp ->
        mp.isLooping = false
        if (startOffsetMs > 0) videoView.seekTo(startOffsetMs)
        videoView.start()
        videoView.animate()
            .alpha(1f)
            .setDuration(LiveDiscoveryKids.TRANSITION_FADE_IN_MS)
            .start()

        val duration = mp.duration
        val remaining = (duration - startOffsetMs).toLong().coerceAtLeast(0L)
        val fadeOutDelay = (remaining - fadeOutMs).coerceAtLeast(0L)
        post(fadeOutDelay) {
            if (!transitionCompleted) {
                transitionCompleted = true
                videoView.setOnCompletionListener(null)
                videoView.animate()
                    .alpha(0f)
                    .setDuration(fadeOutMs)
                    .withEndAction {
                        currentClipUri = null
                        currentClipOnComplete = null
                        onComplete()
                    }
                    .start()
            }
        }
    }

    videoView.setOnCompletionListener {
        if (!transitionCompleted) {
            transitionCompleted = true
            currentClipUri = null
            currentClipOnComplete = null
            onComplete()
        }
    }

    videoView.setVideoURI(uri)
    videoView.requestFocus()
}
