/*
 ESTO TIENE LICENCIA GNU LICENSE APACHE

 NO ELIMINAR NINGUN COMENTARIO AGREGADO
*/
package com.keyler.discoverykidschannel

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import android.widget.VideoView

/**
 * DkVideoView — Release 2009.5.0.0
 *
 * Antes de esta Release, LiveDiscoveryKids usaba directamente un
 * `android.widget.VideoView` (declarado en activity_main.xml). El problema
 * conocido (ver README, sección "Notas Importantes"): a partir de 720p,
 * VideoView activa aceleración de hardware para el SurfaceView interno, lo
 * que hace que el ScreenBug (un ImageView superpuesto) quede oculto DETRÁS
 * del video en vez de encima — el SurfaceView de VideoView no respeta el
 * z-order normal de la jerarquía de vistas.
 *
 * DkVideoView es una capa de abstracción: un FrameLayout abstracto que
 * expone exactamente la misma API que ya usaba el resto del código contra
 * `videoView` (setVideoURI, start, pause, stopPlayback, seekTo, isPlaying,
 * currentPosition, setOnPreparedListener, setOnCompletionListener), para
 * que NINGÚN call-site existente en LiveDiscoveryKids.kt tuviera que
 * cambiar. Dos implementaciones:
 *
 *   - LegacyVideoView: envuelve un VideoView clásico. Comportamiento
 *     idéntico al de antes de esta Release. Sigue siendo el default.
 *
 *   - TextureVideoView: MediaPlayer + TextureView manejados a mano
 *     (TextureView.SurfaceTextureListener). TextureView sí es una vista
 *     "normal" (no tiene su propia ventana de superficie como SurfaceView),
 *     así que el ScreenBug (dibujado después en la jerarquía) queda
 *     correctamente por ENCIMA del video en cualquier resolución.
 *
 * Cuál se usa se decide una sola vez, en create() (llamado desde
 * LiveDiscoveryKids.onCreate()), según SettingsManager.isTextureViewEnabled().
 * Cambiar la opción en Configuración no reconfigura la vista ya creada —
 * hay que reabrir el canal (ver SettingsActivity, sección "Compatibilidad
 * de video").
 *
 * Ver también: LiveDiscoveryKids.checkVideoResolutionAndWarn(), que muestra
 * un AlertDialog recomendando activar esta opción si detecta un programa de
 * 720p o superior estando todavía con LegacyVideoView.
 */
abstract class DkVideoView(context: Context) : FrameLayout(context) {

    abstract fun setVideoURI(uri: Uri)
    abstract fun start()
    abstract fun pause()
    abstract fun stopPlayback()
    abstract fun seekTo(ms: Int)
    abstract val isPlaying: Boolean
    abstract val currentPosition: Int
    abstract fun setOnPreparedListener(listener: MediaPlayer.OnPreparedListener?)
    abstract fun setOnCompletionListener(listener: MediaPlayer.OnCompletionListener?)

    companion object {
        /** Fábrica: instancia la implementación correcta según Configuración. */
        fun create(context: Context): DkVideoView =
            if (SettingsManager.isTextureViewEnabled(context)) {
                Log.d("DkVideoView", "Motor de video: TextureView (compatibilidad 720p+)")
                TextureVideoView(context)
            } else {
                Log.d("DkVideoView", "Motor de video: VideoView clásico")
                LegacyVideoView(context)
            }
    }
}

/**
 * Implementación clásica (default). Un VideoView interno ocupa todo el
 * DkVideoView; el layoutParams "real" (el que ajusta Forzar 4:3 y el resto
 * de la lógica de aspecto) sigue viviendo en el propio DkVideoView, que es
 * lo que queda expuesto como `videoView` en LiveDiscoveryKids.
 */
class LegacyVideoView(context: Context) : DkVideoView(context) {

    private val inner = VideoView(context)

    init {
        addView(inner, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    override fun setVideoURI(uri: Uri) = inner.setVideoURI(uri)
    override fun start() = inner.start()
    override fun pause() = inner.pause()
    override fun stopPlayback() = inner.stopPlayback()
    override fun seekTo(ms: Int) = inner.seekTo(ms)
    override val isPlaying: Boolean get() = inner.isPlaying
    override val currentPosition: Int get() = inner.currentPosition
    override fun setOnPreparedListener(listener: MediaPlayer.OnPreparedListener?) = inner.setOnPreparedListener(listener)
    override fun setOnCompletionListener(listener: MediaPlayer.OnCompletionListener?) = inner.setOnCompletionListener(listener)
}

/**
 * Implementación nueva (Release 2009.5.0.0), activable desde Configuración.
 * MediaPlayer manual + TextureView. Reimplementa a mano lo que VideoView
 * hace internamente: onMeasure() calcula el ancho a partir del alto y la
 * relación de aspecto real del video (mismo efecto visual que el
 * layout_width="wrap_content" que usa LegacyVideoView), porque TextureView
 * por sí sola simplemente estira su contenido al tamaño que le den.
 */
class TextureVideoView(context: Context) : DkVideoView(context), TextureView.SurfaceTextureListener {

    private val textureView = TextureView(context)
    private var mediaPlayer: MediaPlayer? = null
    private var surface: Surface? = null
    private var pendingUri: Uri? = null
    private var prepared = false
    private var videoAspect = 0f   // width / height del video actual; 0 = desconocido todavía

    private var preparedListener: MediaPlayer.OnPreparedListener? = null
    private var completionListener: MediaPlayer.OnCompletionListener? = null

    init {
        textureView.surfaceTextureListener = this
        addView(textureView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (videoAspect > 0f) {
            val heightSize = View.MeasureSpec.getSize(heightMeasureSpec)
            val computedWidth = (heightSize * videoAspect).toInt().coerceAtLeast(1)
            super.onMeasure(View.MeasureSpec.makeMeasureSpec(computedWidth, View.MeasureSpec.EXACTLY), heightMeasureSpec)
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    override fun setVideoURI(uri: Uri) {
        pendingUri = uri
        prepared = false
        releasePlayer()
        if (surface != null) openMediaPlayer(uri)
    }

    private fun openMediaPlayer(uri: Uri) {
        try {
            val mp = MediaPlayer()
            mp.setDataSource(context, uri)
            mp.setSurface(surface)
            mp.setOnVideoSizeChangedListener { _, w, h ->
                if (w > 0 && h > 0) {
                    videoAspect = w.toFloat() / h.toFloat()
                    requestLayout()
                }
            }
            mp.setOnPreparedListener { player ->
                prepared = true
                preparedListener?.onPrepared(player)
            }
            mp.setOnCompletionListener { player -> completionListener?.onCompletion(player) }
            mp.setOnErrorListener { _, what, extra ->
                Log.e("DkVideoView", "MediaPlayer error what=$what extra=$extra uri=$uri")
                true   // consumido: evita el crash por defecto del sistema
            }
            mp.isLooping = false
            mp.prepareAsync()
            mediaPlayer = mp
        } catch (e: Exception) {
            Log.e("DkVideoView", "No se pudo abrir $uri", e)
        }
    }

    private fun releasePlayer() {
        mediaPlayer?.let {
            try { it.reset(); it.release() } catch (e: Exception) { /* no-op */ }
        }
        mediaPlayer = null
    }

    override fun start() { try { mediaPlayer?.start() } catch (e: Exception) { /* no-op */ } }
    override fun pause() { try { mediaPlayer?.let { if (it.isPlaying) it.pause() } } catch (e: Exception) { /* no-op */ } }
    override fun stopPlayback() { releasePlayer(); prepared = false }

    override fun seekTo(ms: Int) {
        try { if (prepared) mediaPlayer?.seekTo(ms) } catch (e: Exception) { /* no-op */ }
    }

    override val isPlaying: Boolean
        get() = try { mediaPlayer?.isPlaying == true } catch (e: Exception) { false }

    override val currentPosition: Int
        get() = try { mediaPlayer?.currentPosition ?: 0 } catch (e: Exception) { 0 }

    override fun setOnPreparedListener(listener: MediaPlayer.OnPreparedListener?) { preparedListener = listener }
    override fun setOnCompletionListener(listener: MediaPlayer.OnCompletionListener?) { completionListener = listener }

    override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
        surface = Surface(st)
        pendingUri?.let { openMediaPlayer(it) }
    }

    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) { /* no-op */ }

    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
        releasePlayer()
        surface?.release()
        surface = null
        return true
    }

    override fun onSurfaceTextureUpdated(st: SurfaceTexture) { /* no-op */ }
}
