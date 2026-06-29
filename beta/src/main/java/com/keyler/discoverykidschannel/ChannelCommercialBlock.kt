/*
 ESTO TIENE LICENCIA GNU LICENSE APACHE

 NO ELIMINAR NINGUN COMENTARIO AGREGADO
*/
package com.keyler.discoverykidschannel

import android.util.Log

/**
 * ChannelCommercialBlock.kt — Preview 2006.4.1.0.21
 *
 * Funciones de extensión de LiveDiscoveryKids: el bloque comercial que
 * interrumpe un programa en curso. Secuencia: ya_regresa (pre-comercial,
 * determinístico por programa) → comercial (aleatorio) → continuamos
 * (pareado con el ya_regresa elegido) → retoma el programa.
 *
 * resumeCommercialBlock() reconstruye el paso exacto del bloque (commercialStep)
 * usando los mismos recursos ya elegidos si la app pasó a segundo plano a
 * mitad del bloque — ver Release 2006.4.1.1 en el CHANGELOG.
 *
 * Reorganización 4.1.0.21 — código movido tal cual desde LiveDiscoveryKids.kt.
 * Sin cambios de comportamiento.
 */

// ══════════════════════════════════════════════════════════════════════════
// Commercial playback
// Secuencia: ya_regresa1/2 (pre-comercial) → comercial(es) → continuamos(pareado) → programa
// ══════════════════════════════════════════════════════════════════════════

internal fun LiveDiscoveryKids.playCommercial(resumeProgramAtMs: Int) {
    cancelAllTasks()
    setBugAlpha(0f)
    stopBgMusic()
    isInProgramSegment = false
    isInCommercialBlock = true
    commercialResumeMs = resumeProgramAtMs
    currentItemType = "commercial"

    val commercialCandidates = LiveDiscoveryKids.COMMERCIALS.filter { it != lastCommercialRes }
        .ifEmpty { LiveDiscoveryKids.COMMERCIALS }
    val chosenCommercial = commercialCandidates.random()
    lastCommercialRes = chosenCommercial

    // Release 4.0.1 — BUG FIX: ya_regresa determinístico por programa.
    // currentProgramIndex (0-based) indexa directamente ENSEGUIDAS_PRE_COMERCIAL,
    // garantizando que cada programa siempre muestre su propio ya_regresa/continuamos.
    val chosenPreComercial = LiveDiscoveryKids.ENSEGUIDAS_PRE_COMERCIAL[currentProgramIndex % LiveDiscoveryKids.ENSEGUIDAS_PRE_COMERCIAL.size]
    lastEnseguidaPreComercialRes = chosenPreComercial

    val chosenYaVolvemos = LiveDiscoveryKids.ENSEGUIDA_YA_VOLVEMOS_MAP[chosenPreComercial]
        ?: R.raw.continuamos1   // fallback defensivo

    // Release 2006.4.1.1 — se promueven a campos de instancia para que
    // onResume() pueda reconstruir exactamente este bloque comercial
    // (qué clips se eligieron y en qué paso estaba) si la app pasa a
    // segundo plano a mitad del bloque, en vez de volver a sortear y
    // reiniciar todo desde ya_regresa.
    commercialChosenPreComercial = chosenPreComercial
    commercialChosenCommercial   = chosenCommercial
    commercialChosenYaVolvemos   = chosenYaVolvemos
    commercialStep = LiveDiscoveryKids.CommercialStep.PRE_COMERCIAL

    Log.d(LiveDiscoveryKids.TAG, "▶ ENSEGUIDA pre-comercial [res=$chosenPreComercial] → continuamos [res=$chosenYaVolvemos]")

    videoView.animate()
        .alpha(0f)
        .setDuration(LiveDiscoveryKids.TRANSITION_FADE_OUT_MS)
        .withEndAction {
            playCommercialStepPreComercial(chosenPreComercial, chosenCommercial, chosenYaVolvemos, resumeProgramAtMs, startOffsetMs = 0)
        }
        .start()
}

/** Paso 1 del bloque comercial: ya_regresa (pre-comercial). FadeIn 1 s, sin FadeOut de entrada (ya lo hizo el caller). */
internal fun LiveDiscoveryKids.playCommercialStepPreComercial(
    chosenPreComercial: Int,
    chosenCommercial: Int,
    chosenYaVolvemos: Int,
    resumeProgramAtMs: Int,
    startOffsetMs: Int
) {
    commercialStep = LiveDiscoveryKids.CommercialStep.PRE_COMERCIAL
    currentClipUri = rawUri(chosenPreComercial)
    currentClipPositionMs = startOffsetMs
    currentClipOnComplete = null   // este paso no usa playUriWithTransition; se maneja manualmente
    startPositionTracker()

    videoView.alpha = 0f
    videoView.setOnPreparedListener { mp ->
        mp.isLooping = false
        if (startOffsetMs > 0) videoView.seekTo(startOffsetMs)
        videoView.start()
        videoView.animate().alpha(1f).setDuration(LiveDiscoveryKids.TRANSITION_FADE_IN_MS).start()
    }
    videoView.setOnCompletionListener {
        Log.d(LiveDiscoveryKids.TAG, "▶ COMMERCIAL [res=$chosenCommercial] (resumes program at ${resumeProgramAtMs}ms)")
        commercialStep = LiveDiscoveryKids.CommercialStep.COMERCIAL

        // Paso 2: comercial — FadeOut 500 ms (TRANSITION_FADE_OUT_MS)
        playUriWithTransition(rawUri(chosenCommercial)) {
            Log.d(LiveDiscoveryKids.TAG, "▶ YA VOLVEMOS post-comercial [res=$chosenYaVolvemos]")
            commercialStep = LiveDiscoveryKids.CommercialStep.POST_COMERCIAL

            // Paso 3: continuamos (FadeOut 500 ms / FadeIn 1 s)
            playUriWithTransition(rawUri(chosenYaVolvemos)) {
                val uri = currentProgramUri ?: run {
                    Log.e(LiveDiscoveryKids.TAG, "No currentProgramUri – advancing")
                    playlistIndex++
                    advance()
                    return@playUriWithTransition
                }
                Log.d(LiveDiscoveryKids.TAG, "Ya volvemos done – resuming program at ${resumeProgramAtMs}ms")
                isInCommercialBlock = false
                beginProgramSegment(uri, startOffsetMs = resumeProgramAtMs, isFirstPlay = false)
            }
        }
    }
    videoView.setVideoURI(rawUri(chosenPreComercial))
    videoView.requestFocus()
}

/**
 * Release 2006.4.1.1 — Reanuda el bloque comercial exactamente en el paso
 * y la posición donde estaba al pasar a segundo plano, usando los mismos
 * recursos ya elegidos (commercialChosenPreComercial/Commercial/YaVolvemos)
 * en vez de volver a sortear. Se llama desde onResume().
 */
internal fun LiveDiscoveryKids.resumeCommercialBlock(startOffsetMs: Int) {
    when (commercialStep) {
        LiveDiscoveryKids.CommercialStep.PRE_COMERCIAL -> {
            Log.d(LiveDiscoveryKids.TAG, "onResume – reanudando ya_regresa (pre-comercial) en ${startOffsetMs}ms")
            playCommercialStepPreComercial(
                commercialChosenPreComercial,
                commercialChosenCommercial,
                commercialChosenYaVolvemos,
                commercialResumeMs,
                startOffsetMs = startOffsetMs
            )
        }
        LiveDiscoveryKids.CommercialStep.COMERCIAL -> {
            Log.d(LiveDiscoveryKids.TAG, "onResume – reanudando comercial en ${startOffsetMs}ms")
            resumeUriWithSeek(rawUri(commercialChosenCommercial), startOffsetMs) {
                Log.d(LiveDiscoveryKids.TAG, "▶ YA VOLVEMOS post-comercial [res=$commercialChosenYaVolvemos]")
                commercialStep = LiveDiscoveryKids.CommercialStep.POST_COMERCIAL
                playUriWithTransition(rawUri(commercialChosenYaVolvemos)) {
                    val uri = currentProgramUri ?: run {
                        Log.e(LiveDiscoveryKids.TAG, "No currentProgramUri – advancing")
                        playlistIndex++
                        advance()
                        return@playUriWithTransition
                    }
                    isInCommercialBlock = false
                    beginProgramSegment(uri, startOffsetMs = commercialResumeMs, isFirstPlay = false)
                }
            }
        }
        LiveDiscoveryKids.CommercialStep.POST_COMERCIAL -> {
            Log.d(LiveDiscoveryKids.TAG, "onResume – reanudando continuamos en ${startOffsetMs}ms")
            resumeUriWithSeek(rawUri(commercialChosenYaVolvemos), startOffsetMs) {
                val uri = currentProgramUri ?: run {
                    Log.e(LiveDiscoveryKids.TAG, "No currentProgramUri – advancing")
                    playlistIndex++
                    advance()
                    return@resumeUriWithSeek
                }
                isInCommercialBlock = false
                beginProgramSegment(uri, startOffsetMs = commercialResumeMs, isFirstPlay = false)
            }
        }
    }
}
