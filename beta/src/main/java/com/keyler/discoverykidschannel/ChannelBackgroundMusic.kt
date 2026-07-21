/*
 ESTO TIENE LICENCIA GNU LICENSE APACHE

 NO ELIMINAR NINGUN COMENTARIO AGREGADO
*/
package com.keyler.discoverykidschannel

import android.media.MediaPlayer
import android.util.Log

/**
 * ChannelBackgroundMusic.kt — Preview 2006.4.1.0.21
 *
 * Funciones de extensión de LiveDiscoveryKids: música de fondo, activa
 * únicamente durante la reproducción de programas. Usa un MediaPlayer
 * independiente del VideoView para poder pausar/reanudar la música sin
 * afectar el video principal.
 *
 * Reorganización 4.1.0.21 — código movido tal cual desde LiveDiscoveryKids.kt.
 * Sin cambios de comportamiento.
 */

// ══════════════════════════════════════════════════════════════════════════
// Background music – solo durante programas
// ══════════════════════════════════════════════════════════════════════════

/**
 * Inicia la música de fondo en loop SIN silencio entre repeticiones.
 * Se usa setOnCompletionListener + seekTo(0) + start() en lugar de isLooping=true,
 * ya que isLooping deja un gap audible en muchos dispositivos Android.
 * Si ya hay un MediaPlayer reproduciéndose, no hace nada.
 * Si existe pero estaba detenido, lo reanuda desde donde quedó.
 */
internal fun LiveDiscoveryKids.startBgMusic() {
    if (!SettingsManager.isBgMusicEnabled(this)) {  // Preview 2006.4.1.0.11
        Log.d(LiveDiscoveryKids.TAG, "BG Music SKIPPED – deshabilitada en Configuración")
        return
    }
    if (bgPlayer == null) {
        bgPlayer = MediaPlayer.create(this, R.raw.bg_music)?.apply {
            isLooping = false
            setVolume(0.08f, 0.08f)
            setOnCompletionListener { mp ->
                mp.seekTo(0)
                mp.start()
                Log.d(LiveDiscoveryKids.TAG, "BG Music LOOP (gapless restart)")
            }
            start()
            Log.d(LiveDiscoveryKids.TAG, "BG Music STARTED")
        }
    } else if (bgPlayer?.isPlaying == false) {
        bgPlayer?.start()
        Log.d(LiveDiscoveryKids.TAG, "BG Music RESUMED")
    }
}

/**
 * Detiene y libera el MediaPlayer de música de fondo.
 * Llamar en bumpers, comerciales y al destruir la Activity.
 */
internal fun LiveDiscoveryKids.stopBgMusic() {
    bgPlayer?.let {
        if (it.isPlaying) it.stop()
        it.release()
        Log.d(LiveDiscoveryKids.TAG, "BG Music STOPPED")
    }
    bgPlayer = null
}
