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
 * DkVideoView — Release 2009.5.0.0 (motores de video), 2009.5.2.0 (fit real de aspecto)
 *
 * Antes de la 2009.5.0.0, LiveDiscoveryKids usaba directamente un
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
 * ── BUG FIX (Release 2009.5.2.0 — investigación a fondo) ──────────────────
 * Con "Forzar 4:3" desactivado, el video aparecía estirado a 16:9 (la forma
 * de la pantalla completa), distorsionado, sin respetar su proporción real.
 * Causa raíz: `LiveDiscoveryKids.applySettings()` forzaba el `layoutParams`
 * de este view a `MATCH_PARENT` SIEMPRE, pisando el `WRAP_CONTENT` +
 * `gravity=CENTER` original con el que se agrega en `onCreate()` — que era
 * justo lo que permitía calcular un tamaño propio según la proporción real
 * del video. Además, `LegacyVideoView` nunca tuvo lógica propia de ajuste
 * de aspecto (dependía enteramente, y de forma poco confiable, del
 * comportamiento interno de `VideoView` con layoutParams WRAP_CONTENT).
 *
 * Ahora el ajuste de aspecto vive acá, en la clase base, compartido por
 * los dos motores: [videoAspect] (ancho/alto real del video, conocido recién
 * cuando el reproductor lo informa) y [forceAspectRatio] (sincronizado con
 * SettingsManager.isForceAspectRatioEnabled() desde
 * LiveDiscoveryKids.applySettings(), igual que en AspectRatioFrameLayout).
 *
 *   - forceAspectRatio = true: sin ajuste propio acá — se llena el espacio
 *     que le den (el 4:3 exacto ya lo fuerza el contenedor externo
 *     AspectRatioFrameLayout — mismo comportamiento de siempre).
 *   - forceAspectRatio = false: fit real preservando la proporción real del
 *     video dentro del espacio disponible (pillarbox/letterbox según
 *     corresponda) — nunca estirado/distorsionado, sea cual sea la forma
 *     del contenedor (que con Forzar 4:3 desactivado ya no es 4:3, sino la
 *     pantalla completa).
 */
abstract class DkVideoView(context: Context) : FrameLayout(context) {

    /** Ancho/alto real del video cargado actualmente. 0 = todavía no se conoce. */
    protected var videoAspect: Float = 0f

    /** Ver documentación de la clase. Sincronizado desde applySettings(). */
    var forceAspectRatio: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                requestLayout()
            }
        }

    /** Llamar quien conozca las dimensiones reales del video (una vez preparado el reproductor). */
    protected fun setKnownVideoAspect(aspect: Float) {
        if (aspect > 0f && kotlin.math.abs(aspect - videoAspect) > 0.001f) {
            videoAspect = aspect
            requestLayout()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (forceAspectRatio || videoAspect <= 0f) {
            // Forzado: llenar lo que dé el contenedor (ya viene en 4:3 desde
            // AspectRatioFrameLayout). Sin forzar pero aspecto aún desconocido:
            // usar el espacio disponible tal cual hasta que se sepa el real
            // (se reajusta solo, vía requestLayout(), en cuanto se conoce).
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
 * DkVideoView; el ajuste de aspecto real (forzado 4:3 vs. fit preservando
 * proporción) ahora lo calcula la clase base DkVideoView.onMeasure() — ver
 * comentario de la clase — así que acá el VideoView interno simplemente
 * llena el espacio que DkVideoView ya calculó correctamente.
 */
class LegacyVideoView(context: Context) : DkVideoView(context) {

    private val inner = VideoView(context)
    private var userPreparedListener: MediaPlayer.OnPreparedListener? = null

    init {
        addView(inner, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        // BUG FIX (2009.5.2.0): antes esta clase no conocía la proporción real
        // del video en absoluto. Ahora se captura acá (mp.videoWidth/videoHeight,
        // disponibles apenas el MediaPlayer interno de VideoView prepara) y se
        // pasa a la clase base para el fit — sin esto, el fit con
        // forceAspectRatio=false no tiene forma de saber qué proporción usar.
        inner.setOnPreparedListener { mp ->
            if (mp.videoWidth > 0 && mp.videoHeight > 0) {
                setKnownVideoAspect(mp.videoWidth.toFloat() / mp.videoHeight.toFloat())
            }
            userPreparedListener?.onPrepared(mp)
        }
    }

    override fun setVideoURI(uri: Uri) = inner.setVideoURI(uri)
    override fun start() = inner.start()
    override fun pause() = inner.pause()
    override fun stopPlayback() = inner.stopPlayback()
    override fun seekTo(ms: Int) = inner.seekTo(ms)
    override val isPlaying: Boolean get() = inner.isPlaying
    override val currentPosition: Int get() = inner.currentPosition
    override fun setOnPreparedListener(listener: MediaPlayer.OnPreparedListener?) {
        userPreparedListener = listener
    }
    override fun setOnCompletionListener(listener: MediaPlayer.OnCompletionListener?) = inner.setOnCompletionListener(listener)
}

/**
 * Implementación nueva (Release 2009.5.0.0), activable desde Configuración.
 * MediaPlayer manual + TextureView. El ajuste de aspecto (antes calculado acá
 * mismo, en un onMeasure() propio) ahora vive en la clase base DkVideoView
 * (compartido con LegacyVideoView) — ver comentario de la clase.
 */
class TextureVideoView(context: Context) : DkVideoView(context), TextureView.SurfaceTextureListener {

    private val textureView = TextureView(context)
    private var mediaPlayer: MediaPlayer? = null
    private var surface: Surface? = null
    private var pendingUri: Uri? = null
    private var prepared = false

    private var preparedListener: MediaPlayer.OnPreparedListener? = null
    private var completionListener: MediaPlayer.OnCompletionListener? = null

    init {
        textureView.surfaceTextureListener = this
        addView(textureView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
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
                    setKnownVideoAspect(w.toFloat() / h.toFloat())
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
