/*
 ESTO TIENE LICENCIA GNU LICENSE APACHE

 NO ELIMINAR NINGUN COMENTARIO AGREGADO
*/
package com.keyler.discoverykidschannel

import android.util.Log

/**
 * ChannelPlaylist.kt — Preview 2006.4.1.0.21
 *
 * Funciones de extensión de LiveDiscoveryKids: el driver principal de la
 * playlist (advance()), los tres tipos de clip "de relleno" entre programas
 * (Bumper, Enseguida post-programa, StandaloneCommercial), y la navegación
 * Prev/Next que salta directo entre programas.
 *
 * La reproducción del programa en sí (playProgram, beginProgramSegment,
 * scheduleSegmentLogic, calcBreaks) vive en ChannelProgramPlayback.kt; el
 * bloque comercial que interrumpe un programa en curso (playCommercial y
 * sus pasos) vive en ChannelCommercialBlock.kt.
 *
 * Reorganización 4.1.0.21 — código movido tal cual desde LiveDiscoveryKids.kt.
 * Sin cambios de comportamiento.
 */

// ══════════════════════════════════════════════════════════════════════════
// Playlist driver
// ══════════════════════════════════════════════════════════════════════════
// startChannel() → ver ChannelSessionState.kt

/** Move to the next playlist item (wraps around). */
internal fun LiveDiscoveryKids.advance() {
    if (playlistIndex >= playlist.size) playlistIndex = 0
    when (val item = playlist[playlistIndex]) {
        is LiveDiscoveryKids.PlayItem.Bumper               -> playBumper()
        is LiveDiscoveryKids.PlayItem.Enseguida            -> playEnseguida()
        is LiveDiscoveryKids.PlayItem.StandaloneCommercial -> playStandaloneCommercial()
        is LiveDiscoveryKids.PlayItem.Program              -> playProgram(item.index)
    }
}

// ══════════════════════════════════════════════════════════════════════════
// Bumper playback
// ══════════════════════════════════════════════════════════════════════════

internal fun LiveDiscoveryKids.playBumper() {
    cancelAllTasks()
    setBugAlpha(0f)
    stopBgMusic()
    isInProgramSegment = false
    currentItemType = "bumper"

    val candidates = LiveDiscoveryKids.BUMPERS.filter { it != lastBumperRes }.ifEmpty { LiveDiscoveryKids.BUMPERS }
    val chosenBumper = candidates.random()
    lastBumperRes = chosenBumper

    Log.d(LiveDiscoveryKids.TAG, "▶ BUMPER [res=$chosenBumper]")

    playUriWithTransition(rawUri(chosenBumper)) {
        playlistIndex++
        advance()
    }
}

// ══════════════════════════════════════════════════════════════════════════
// Standalone Commercial – comercial en la programación lineal
// Aparece entre Enseguida y Talla como parte del flujo de canal.
// Es independiente del bloque publicitario (playCommercial) que interrumpe
// programas: no tiene ya_volvemos ni lógica de breakQueue.
// Beta 3.0.0.2
// ══════════════════════════════════════════════════════════════════════════

internal fun LiveDiscoveryKids.playStandaloneCommercial() {
    cancelAllTasks()
    setBugAlpha(0f)
    stopBgMusic()
    isInProgramSegment  = false
    isInCommercialBlock = false
    currentItemType     = "standaloneCommercial"

    val candidates = LiveDiscoveryKids.COMMERCIALS.filter { it != lastCommercialRes }.ifEmpty { LiveDiscoveryKids.COMMERCIALS }
    val chosenCommercial = candidates.random()
    lastCommercialRes  = chosenCommercial

    Log.d(LiveDiscoveryKids.TAG, "▶ STANDALONE COMMERCIAL [res=$chosenCommercial]")

    playUriWithTransition(rawUri(chosenCommercial)) {
        playlistIndex++
        advance()
    }
}

// ══════════════════════════════════════════════════════════════════════════
// Enseguida playback – post-programa: aparece entre el fin del programa
// y el comercial standalone.
// Beta 3.0.0.3: selección aleatoria con anti-repetición entre
// [enseguida1, enseguida2]. Se eliminó la selección por horario y enseguida5.
// ══════════════════════════════════════════════════════════════════════════

internal fun LiveDiscoveryKids.playEnseguida() {
    cancelAllTasks()
    setBugAlpha(0f)
    stopBgMusic()
    isInProgramSegment = false
    currentItemType = "enseguida"

    val candidates = LiveDiscoveryKids.ENSEGUIDAS_POST_PROGRAMA
        .filter { it != lastEnseguidaPostProgramaRes }
        .ifEmpty { LiveDiscoveryKids.ENSEGUIDAS_POST_PROGRAMA }
    val chosenEnseguida = candidates.random()
    lastEnseguidaPostProgramaRes = chosenEnseguida

    Log.d(LiveDiscoveryKids.TAG, "▶ ENSEGUIDA post-programa [res=$chosenEnseguida]")

    playUriWithTransition(rawUri(chosenEnseguida)) {
        playlistIndex++
        advance()
    }
}

// ══════════════════════════════════════════════════════════════════════════
// Navegación Prev / Next
//
// Release 3.4.1 — Prev / Next saltan directo al programa destino.
//
// Problema del enfoque anterior (iniciar desde Enseguida):
//   playEnseguida() → playUriWithTransition() registra el timer del FadeOut en pendingTasks.
//   Cuando la enseguida termina, su onComplete llama playBumper() → cancelAllTasks(),
//   que borra el timer del FadeOut del bumper antes de que corra. Además,
//   encadenar playUriWithTransition() dentro del onComplete de otro cancela la
//   animación del segundo via ViewPropertyAnimator (instancia única del videoView),
//   por lo que el withEndAction del FadeOut inicial nunca se ejecuta y el bumper
//   nunca arranca.
//
// Solución: Prev / Next se comportan como un cambio de canal — van directo al
// programa sin pasar por Enseguida → StandaloneCommercial → Bumper. Ese bloque
// ya ocurre naturalmente cuando el programa termine por su propio onCompletionListener.
// playlistIndex se fija en el PlayItem.Program para que advance() continúe
// correctamente desde la Enseguida del siguiente ciclo al terminar el programa.
// ══════════════════════════════════════════════════════════════════════════

internal fun LiveDiscoveryKids.goToAdjacentProgram(direction: Int) {
    val target = findAvailableProgramIndex(currentProgramIndex, direction) ?: return

    if (target == currentProgramIndex) return

    Log.d(LiveDiscoveryKids.TAG, "▶ Navegando directo al programa ${target + 1} (direction=$direction)")
    cancelAllTasks()
    setBugAlpha(0f)
    stopPositionTracker()
    stopBgMusic()
    isInProgramSegment = false
    isInCommercialBlock = false
    // Release 2006.4.1.1: limpia el estado de reanudación de clips no-programa;
    // Prev/Next descarta cualquier bumper/enseguida/comercial en curso, así que
    // no debe quedar un currentClipUri obsoleto que onResume() intente reanudar.
    currentClipUri = null
    currentClipOnComplete = null
    videoView.stopPlayback()

    // Busca el índice del PlayItem.Program destino en el playlist y fija playlistIndex ahí.
    // El programa terminará normalmente y su onCompletionListener hará playlistIndex++ + advance(),
    // arrancando la Enseguida del siguiente bloque sin ningún conflicto de ViewPropertyAnimator.
    val programIdx = playlist.indexOfFirst { it is LiveDiscoveryKids.PlayItem.Program && it.index == target }
        .takeIf { it >= 0 } ?: 0

    playlistIndex = programIdx
    currentProgramIndex = target

    playProgram(target, restartFromBeginning = true)
}

internal fun LiveDiscoveryKids.findAvailableProgramIndex(startIndex: Int, direction: Int): Int? {
    if (direction == 0) return null

    val totalPrograms = 4
    var candidate = startIndex

    repeat(totalPrograms) {
        candidate = (candidate + direction + totalPrograms) % totalPrograms
        if (resolveProgram(candidate) != null) return candidate
    }
    return null
}
