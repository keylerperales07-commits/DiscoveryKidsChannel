/*
 ESTO TIENE LICENCIA GNU LICENSE APACHE

 NO ELIMINAR NINGUN COMENTARIO AGREGADO
*/
package com.keyler.discoverykidschannel

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.view.View
import android.widget.FrameLayout
import android.widget.VideoView

/**
 * DkVideoView — Release 2009.5.0.0 (introdujo un motor de video alternativo
 * basado en TextureView), Release 2009.5.2.1 (TextureView eliminado por
 * completo — ver más abajo).
 *
 * BUG FIX (2009.5.2.1) — "ELIMINAR POR COMPLETO TEXTURE VIEW Y SU LÓGICA
 * INCLUYENDO EL ALERTDIALOG": la Release 2009.5.0.0 había introducido un
 * segundo motor de video (TextureVideoView, MediaPlayer + TextureView
 * manejados a mano) como alternativa al VideoView clásico, para que el
 * ScreenBug no quedara oculto detrás del video en resoluciones altas
 * (720p+). Ese motor alternativo se elimina por completo en esta Release
 * — junto con el switch "Recortar 4:3" (ex "Usar TextureView") en
 * Configuración, y el AlertDialog que avisaba sobre programas de 720p+
 * (ver LiveDiscoveryKids.kt / SettingsActivity.kt / SettingsManager.kt).
 * `DkVideoView` vuelve a ser una sola clase concreta (ya no hace falta la
 * separación abstracta con dos implementaciones).
 *
 * Motivo por el que existía esta clase envoltorio (en vez de usar
 * `android.widget.VideoView` directo, como antes de la 2009.5.0.0): permite
 * mantener, en un solo lugar, el ajuste de aspecto real del video (ver
 * onMeasure() más abajo) sin que el resto de LiveDiscoveryKids.kt tenga que
 * cambiar (misma API: setVideoURI, start, pause, stopPlayback, seekTo,
 * isPlaying, currentPosition, setOnPreparedListener, setOnCompletionListener).
 *
 * ── Ajuste de aspecto real (Release 2009.5.2.0, corregido en 2009.5.2.1) ──
 * El contenedor externo (AspectRatioFrameLayout, ver ese archivo) SIEMPRE
 * fuerza una caja de proporción 4:3 — sin excepción. Lo que decide el
 * switch "Forzar 4:3" de Configuración es lo que pasa DENTRO de esa caja:
 *
 *   - forceAspectRatio = true: el video se estira para llenar la caja de
 *     4:3 exactamente (puede distorsionar contenido que no sea nativamente
 *     4:3 — es el comportamiento "forzado" que da nombre al switch).
 *   - forceAspectRatio = false: el video se ajusta preservando su
 *     proporción real (ancho/alto reportados por el propio reproductor)
 *     DENTRO de esa misma caja de 4:3, sin estirarse — un video 16:9, por
 *     ejemplo, encaja con franjas arriba/abajo en vez de deformarse.
 */
class DkVideoView(context: Context) : FrameLayout(context) {

    private val inner = VideoView(context)
    private var userPreparedListener: MediaPlayer.OnPreparedListener? = null

    /** Ancho/alto real del video cargado actualmente. 0 = todavía no se conoce. */
    private var videoAspect: Float = 0f

    /** Ver documentación de la clase. Sincronizado desde LiveDiscoveryKids.applySettings(). */
    var forceAspectRatio: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                requestLayout()
            }
        }

    init {
        addView(inner, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        // mp.videoWidth/videoHeight están disponibles apenas el MediaPlayer
        // interno de VideoView termina de preparar — sin esto, el fit con
        // forceAspectRatio=false no tiene forma de saber qué proporción usar.
        inner.setOnPreparedListener { mp ->
            if (mp.videoWidth > 0 && mp.videoHeight > 0) {
                val aspect = mp.videoWidth.toFloat() / mp.videoHeight.toFloat()
                if (kotlin.math.abs(aspect - videoAspect) > 0.001f) {
                    videoAspect = aspect
                    requestLayout()
                }
            }
            userPreparedListener?.onPrepared(mp)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (forceAspectRatio || videoAspect <= 0f) {
            // Forzado: llenar lo que dé el contenedor (la caja de 4:3 que
            // siempre fuerza AspectRatioFrameLayout). Sin forzar pero
            // aspecto aún desconocido: usar el espacio disponible tal cual
            // hasta que se sepa el real (se reajusta solo, vía
            // requestLayout(), en cuanto se conoce — ver arriba).
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }

        val availW = View.MeasureSpec.getSize(widthMeasureSpec)
        val availH = View.MeasureSpec.getSize(heightMeasureSpec)
        var fitW = availW
        var fitH = (fitW / videoAspect).toInt()
        if (fitH > availH) {
            fitH = availH
            fitW = (fitH * videoAspect).toInt()
        }
        super.onMeasure(
            View.MeasureSpec.makeMeasureSpec(fitW.coerceAtLeast(1), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(fitH.coerceAtLeast(1), View.MeasureSpec.EXACTLY)
        )
    }

    fun setVideoURI(uri: Uri) = inner.setVideoURI(uri)
    fun start() = inner.start()
    fun pause() = inner.pause()
    fun stopPlayback() = inner.stopPlayback()
    fun seekTo(ms: Int) = inner.seekTo(ms)
    val isPlaying: Boolean get() = inner.isPlaying
    val currentPosition: Int get() = inner.currentPosition
    fun setOnPreparedListener(listener: MediaPlayer.OnPreparedListener?) {
        userPreparedListener = listener
    }
    fun setOnCompletionListener(listener: MediaPlayer.OnCompletionListener?) = inner.setOnCompletionListener(listener)
}
