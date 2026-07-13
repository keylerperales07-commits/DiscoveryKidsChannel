package com.keyler.discoverykidschannel

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Movie
import android.graphics.PixelFormat
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/**
 * Fix (build): android-gif-drawable hace crashear a D8/R8 (NullPointerException
 * interno) al procesar su jar "runtime" multi-release, tanto en debug
 * (mergeExtDexDebug) como en release (con o sin minifyEnabled). El bug es del
 * toolchain de dexing, no de configuración, así que la única vía confiable es
 * no depender de ese jar en absoluto.
 *
 * GifMovieDrawable reproduce GIFs usando android.graphics.Movie, parte del SDK
 * de Android desde siempre (deprecada desde API 28 pero totalmente funcional
 * en todas las versiones soportadas por este proyecto, minSdk 23). Sin
 * librerías externas → sin riesgo de romper el dexer.
 */
@Suppress("DEPRECATION")
internal class GifMovieDrawable(private val movie: Movie) : Drawable(), Animatable {

    companion object {
        private const val FRAME_DELAY_MS = 40L  // ~25 fps, suficiente para GIFs cortos de UI
    }

    private var running = false
    private var startTimeMs = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val invalidateRunnable = Runnable { tick() }

    override fun draw(canvas: Canvas) {
        if (running) {
            val duration = movie.duration().takeIf { it > 0 } ?: 1
            val relativeTime = ((SystemClock.uptimeMillis() - startTimeMs) % duration).toInt()
            movie.setTime(relativeTime)
        }
        val b = bounds
        if (b.width() <= 0 || b.height() <= 0 || movie.width() <= 0 || movie.height() <= 0) return
        val scaleX = b.width().toFloat() / movie.width()
        val scaleY = b.height().toFloat() / movie.height()
        canvas.save()
        canvas.translate(b.left.toFloat(), b.top.toFloat())
        canvas.scale(scaleX, scaleY)
        movie.draw(canvas, 0f, 0f)
        canvas.restore()
    }

    private fun tick() {
        invalidateSelf()
        if (running) {
            handler.postDelayed(invalidateRunnable, FRAME_DELAY_MS)
        }
    }

    /** Reinicia la reproducción desde el primer frame (llamar antes de mostrar el drawable de nuevo). */
    fun seekToStart() {
        startTimeMs = SystemClock.uptimeMillis()
    }

    override fun start() {
        if (running) return
        running = true
        startTimeMs = SystemClock.uptimeMillis()
        tick()
    }

    override fun stop() {
        running = false
        handler.removeCallbacks(invalidateRunnable)
    }

    override fun isRunning(): Boolean = running

    override fun setAlpha(alpha: Int) { /* no-op: el fade lo maneja el ImageView contenedor */ }
    override fun setColorFilter(colorFilter: ColorFilter?) { /* no-op */ }
    @Deprecated("Deprecated in Java", ReplaceWith(""))
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    override fun getIntrinsicWidth(): Int = movie.width()
    override fun getIntrinsicHeight(): Int = movie.height()
}
