/*
 ESTO TIENE LICENCIA GNU LICENSE APACHE

 NO ELIMINAR NINGUN COMENTARIO AGREGADO
*/
package com.keyler.discoverykidschannel

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin

/**
 * CrtOverlayView – Efecto CRT sobre VideoView y ScreenBug.
 * Beta 2.2.0.20 – Discovery Kids Channel
 *
 * Renderiza cuatro capas de efecto CRT usando Canvas puro (sin OpenGL ni shaders):
 *
 *  1. SCANLINES
 *     Líneas horizontales oscuras semi-transparentes cada [SCANLINE_SPACING] px.
 *     Desplazan lentamente hacia abajo para simular el barrido del cañón de electrones.
 *     Intensidad ajustable con [scanlineAlpha].
 *
 *  2. PHOSPHOR MASK (RGB pixel grid)
 *     Franjas verticales muy sutiles alternando R-G-B para imitar la máscara de
 *     sombra (shadow mask) de los tubos de colores reales.
 *     Intensidad ajustable con [phosphorAlpha].
 *
 *  3. VIGNETTE
 *     Gradiente radial desde el centro (transparente) hacia los bordes (negro).
 *     Simula la menor luminosidad en los bordes de un tubo CRT real.
 *     Intensidad ajustable con [vignetteAlpha].
 *
 *  4. FLICKER
 *     Oscilación sinusoidal muy sutil del alpha global del overlay completo.
 *     Simula la variación de brillo de 60 Hz de un monitor analógico.
 *     Intensidad ajustable con [flickerIntensity] (0..1).
 *
 * Uso en XML:
 *   <com.keyler.discoverykidschannel.CrtOverlayView
 *       android:id="@+id/crtOverlay"
 *       android:layout_width="match_parent"
 *       android:layout_height="match_parent" />
 *
 * Todo se renderiza en onDraw() y se anima con postInvalidateOnAnimation()
 * sincronizado con el vsync del sistema (≈60 fps), sin Threads adicionales.
 */
class CrtOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ── Parámetros ajustables ──────────────────────────────────────────────────

    /** Alpha (0-255) de las líneas de barrido. 40 = sutil, 80 = fuerte. */
    var scanlineAlpha: Int = 55

    /** Separación en píxeles entre scanlines. 3 = muy fino, 5 = visible. */
    var scanlineSpacing: Int = 3

    /** Velocidad de desplazamiento vertical de las scanlines (px por frame). */
    var scanlineScrollSpeed: Float = 0.4f

    /** Alpha (0-255) de la máscara de fósforo RGB. 15 = casi invisible, 35 = notable. */
    var phosphorAlpha: Int = 18

    /** Alpha (0-255) del vignette en los bordes. 160 = moderado. */
    var vignetteAlpha: Int = 150

    /**
     * Intensidad del flicker (0.0 = sin flicker, 0.04 = muy sutil, 0.12 = notable).
     * El flicker oscila el alpha global del view entre (1 - flickerIntensity) y 1.
     */
    var flickerIntensity: Float = 0.035f

    // ── Estado de animación ────────────────────────────────────────────────────

    /** Offset de desplazamiento acumulado de las scanlines (módulo scanlineSpacing). */
    private var scanlineOffset: Float = 0f

    /** Tiempo de inicio para el cálculo del flicker sinusoidal. */
    private var startTimeMs: Long = System.currentTimeMillis()

    /** Paints reutilizados para evitar allocations en onDraw. */
    private val scanlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.BLACK
    }
    private val phosphorPaintR = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(phosphorAlpha, 255, 0, 0)
    }
    private val phosphorPaintG = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(phosphorAlpha, 0, 255, 0)
    }
    private val phosphorPaintB = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(phosphorAlpha, 0, 0, 255)
    }
    private val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // ── Shaders cacheados (se reconstruyen si cambia el tamaño) ───────────────

    private var cachedWidth  = -1
    private var cachedHeight = -1
    private var vignetteShader: RadialGradient? = null

    // ── onDraw ─────────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) {
            postInvalidateOnAnimation()
            return
        }

        // ── Flicker global ─────────────────────────────────────────────────────
        // Calcula alpha global con oscilación sinusoidal de ~60 Hz visual.
        val elapsedSec = (System.currentTimeMillis() - startTimeMs) / 1000f
        // Combina dos frecuencias para un flicker más orgánico (menos regular)
        val flickerVal = sin(elapsedSec * 62f) * 0.6f + sin(elapsedSec * 119f) * 0.4f
        val globalAlpha = (1f - flickerIntensity * flickerVal).coerceIn(0f, 1f)
        alpha = globalAlpha

        // ── Scanlines ──────────────────────────────────────────────────────────
        scanlinePaint.alpha = scanlineAlpha
        scanlineOffset = (scanlineOffset + scanlineScrollSpeed) % scanlineSpacing
        var y = -scanlineOffset
        while (y < h) {
            // Dibuja una línea de 1px de alto cada scanlineSpacing px
            canvas.drawRect(0f, y, w, y + 1f, scanlinePaint)
            y += scanlineSpacing
        }

        // ── Phosphor mask (RGB vertical stripes) ──────────────────────────────
        // Franjas de 1px R, 1px G, 1px B repetidas horizontalmente
        phosphorPaintR.alpha = phosphorAlpha
        phosphorPaintG.alpha = phosphorAlpha
        phosphorPaintB.alpha = phosphorAlpha
        var x = 0f
        while (x < w) {
            canvas.drawRect(x,       0f, x + 1f, h, phosphorPaintR)
            canvas.drawRect(x + 1f, 0f, x + 2f, h, phosphorPaintG)
            canvas.drawRect(x + 2f, 0f, x + 3f, h, phosphorPaintB)
            x += 3f
        }

        // ── Vignette ───────────────────────────────────────────────────────────
        // Reconstruir shader solo si el tamaño cambió
        if (w.toInt() != cachedWidth || h.toInt() != cachedHeight) {
            cachedWidth  = w.toInt()
            cachedHeight = h.toInt()
            val cx = w / 2f
            val cy = h / 2f
            // Radio del gradiente: hipotenusa para cubrir las esquinas
            val radius = Math.sqrt((cx * cx + cy * cy).toDouble()).toFloat() * 1.05f
            vignetteShader = RadialGradient(
                cx, cy, radius,
                intArrayOf(
                    Color.TRANSPARENT,                          // centro: sin oscurecimiento
                    Color.TRANSPARENT,                          // 55% del radio: aún claro
                    Color.argb(vignetteAlpha / 3, 0, 0, 0),   // 75%: empieza a oscurecer
                    Color.argb(vignetteAlpha, 0, 0, 0)         // bordes: oscuridad total
                ),
                floatArrayOf(0f, 0.55f, 0.75f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        vignettePaint.shader = vignetteShader
        canvas.drawRect(0f, 0f, w, h, vignettePaint)

        // ── Solicitar siguiente frame sincronizado con vsync ───────────────────
        postInvalidateOnAnimation()
    }
}
