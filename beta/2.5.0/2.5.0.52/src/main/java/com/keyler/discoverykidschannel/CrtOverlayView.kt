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
 * Release 1999.2.2.0.01 – Discovery Kids Channel
 *
 * Renderiza cinco capas de efecto CRT usando Canvas puro (sin OpenGL ni shaders):
 *
 *  1. SCANLINES REALISTAS
 *     Cada línea de barrido tiene 1px oscura + 1px de "glow" semi-transparente
 *     entre ellas para simular el bloom de fósforo real de un CRT.
 *     Separación de 2px (densa) para imitar un monitor CRT de alta resolución.
 *     Se desplazan hacia abajo sincronizadas con el vsync.
 *
 *  2. PHOSPHOR MASK (RGB pixel grid)
 *     Franjas verticales de 1px R-G-B alternadas imitando la shadow mask del tubo.
 *
 *  3. VIGNETTE
 *     Gradiente radial desde el centro hacia los bordes con oscurecimiento
 *     progresivo desde el 40% del radio.
 *
 *  4. CRT BORDER GRADIENT – DELGADO (release 1999.2.2.0.01)
 *     Ahora ubicado DENTRO del AspectRatioFrameLayout: el degradado negro
 *     aparece exactamente en el borde del rectángulo 4:3, no en el letterbox.
 *     Grosor reducido a ~18dp para un degradado fino y elegante.
 *
 *  5. FLICKER
 *     Oscilación sinusoidal combinando dos frecuencias para efecto orgánico.
 */
class CrtOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ── Parámetros ajustables ──────────────────────────────────────────────────

    /** Alpha (0-255) de las líneas de barrido oscuras. */
    var scanlineAlpha: Int = 100  // 1999.2.2.0.01: subido 75→100 para más impacto CRT

    /**
     * Alpha (0-255) del glow entre scanlines (bloom de fósforo).
     * 1999.2.2.0.01: nuevo parámetro. Simula la luz que emite el fósforo entre líneas.
     * 18 = muy sutil, 35 = bloom visible.
     */
    var scanlineGlowAlpha: Int = 25

    /** Separación total en píxeles de cada ciclo de scanline (oscura + glow). */
    var scanlineSpacing: Int = 2   // 1999.2.2.0.01: reducido 3→2, más denso y realista

    /** Velocidad de desplazamiento vertical de las scanlines (px por frame). */
    var scanlineScrollSpeed: Float = 0.5f  // 1999.2.2.0.01: 0.4→0.5

    /** Alpha (0-255) de la máscara de fósforo RGB. */
    var phosphorAlpha: Int = 30   // 1999.2.2.0.01: subido 22→30

    /** Alpha (0-255) del vignette en los bordes. */
    var vignetteAlpha: Int = 210  // 1999.2.2.0.01: subido 200→210

    /**
     * Intensidad del flicker (0.0 = sin flicker, 0.12 = notable).
     */
    var flickerIntensity: Float = 0.065f  // 1999.2.2.0.01: subido 0.045→0.065

    /**
     * Ancho en píxeles de las franjas de borde CRT.
     * 1999.2.2.0.01: reducido a ~18dp para borde fino y elegante.
     * El degradado ahora vive dentro del 4:3 gracias al cambio en activity_main.xml.
     */
    var borderWidth: Float = 18f * resources.displayMetrics.density

    /** Alpha máximo (0-255) del negro en el borde CRT. */
    var borderAlpha: Int = 210  // 1999.2.2.0.01: ligeramente más oscuro en borde fino

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
    // Paint para el glow de fósforo entre scanlines (1999.2.2.0.01)
    private val scanlineGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        // Glow cálido levemente amarillento, como el fósforo P22 de los CRT a color
        color = Color.argb(scanlineGlowAlpha, 255, 245, 200)
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
    // Paint para las franjas de borde CRT (1999.2.2.0.23)
    // Se reutiliza para los 4 bordes cambiando el shader cada vez.
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // ── Shaders cacheados (se reconstruyen si cambia el tamaño) ───────────────

    private var cachedWidth  = -1
    private var cachedHeight = -1
    private var vignetteShader: RadialGradient? = null
    // Shaders lineales para los 4 bordes CRT (1999.2.2.0.23)
    private var borderTopShader:    LinearGradient? = null
    private var borderBottomShader: LinearGradient? = null
    private var borderLeftShader:   LinearGradient? = null
    private var borderRightShader:  LinearGradient? = null

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

        // ── Scanlines realistas con glow de fósforo (1999.2.2.0.01) ───────────
        // Cada ciclo = 1px glow (fósforo iluminado) + 1px oscuro (banda entre electrones).
        // Esto imita el patrón visual real de un CRT: las bandas negras son las
        // separaciones entre líneas de barrido, y el glow es la línea encendida.
        scanlinePaint.alpha = scanlineAlpha
        scanlineGlowPaint.alpha = scanlineGlowAlpha
        scanlineOffset = (scanlineOffset + scanlineScrollSpeed) % scanlineSpacing
        var y = -scanlineOffset
        while (y < h) {
            // Línea de glow (fósforo encendido)
            canvas.drawRect(0f, y, w, y + 1f, scanlineGlowPaint)
            // Banda oscura (separación entre líneas de barrido)
            canvas.drawRect(0f, y + 1f, w, y + scanlineSpacing.toFloat(), scanlinePaint)
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

        // ── Vignette + Border shaders (se reconstruyen si cambia el tamaño) ──────
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
                    Color.TRANSPARENT,                                  // centro: sin oscurecimiento
                    Color.TRANSPARENT,                                  // 40%: aún claro (antes era 55%)
                    Color.argb(vignetteAlpha / 4, 0, 0, 0),           // 60%: empieza a oscurecer suavemente
                    Color.argb(vignetteAlpha / 2, 0, 0, 0),           // 78%: oscurecimiento medio
                    Color.argb(vignetteAlpha, 0, 0, 0)                 // bordes: oscuridad total
                ),
                floatArrayOf(0f, 0.40f, 0.60f, 0.78f, 1f),
                Shader.TileMode.CLAMP
            )

            // ── CRT Border shaders – franjas lineales en los 4 bordes (1999.2.2.0.23) ──
            // Cada shader va de negro (borderAlpha) en el borde hacia transparente adentro.
            // La franja cubre [borderWidth] px desde el borde hacia el centro.
            val blackEdge  = Color.argb(borderAlpha, 0, 0, 0)
            val clearInner = Color.TRANSPARENT

            // Borde superior: negro arriba (y=0) → transparente abajo (y=borderWidth)
            borderTopShader = LinearGradient(
                0f, 0f, 0f, borderWidth,
                blackEdge, clearInner, Shader.TileMode.CLAMP
            )
            // Borde inferior: transparente arriba (y=h-borderWidth) → negro abajo (y=h)
            borderBottomShader = LinearGradient(
                0f, h - borderWidth, 0f, h,
                clearInner, blackEdge, Shader.TileMode.CLAMP
            )
            // Borde izquierdo: negro izq (x=0) → transparente der (x=borderWidth)
            borderLeftShader = LinearGradient(
                0f, 0f, borderWidth, 0f,
                blackEdge, clearInner, Shader.TileMode.CLAMP
            )
            // Borde derecho: transparente izq (x=w-borderWidth) → negro der (x=w)
            borderRightShader = LinearGradient(
                w - borderWidth, 0f, w, 0f,
                clearInner, blackEdge, Shader.TileMode.CLAMP
            )
        }

        // ── Dibujar vignette ───────────────────────────────────────────────────
        vignettePaint.shader = vignetteShader
        canvas.drawRect(0f, 0f, w, h, vignettePaint)

        // ── Dibujar franjas de borde CRT (1999.2.2.0.23) ─────────────────────
        // Se dibujan DESPUÉS del vignette para que sean la capa más oscura en bordes.
        // Las esquinas se solapan correctamente porque cada franja es un rect completo.
        borderPaint.shader = borderTopShader
        canvas.drawRect(0f, 0f, w, borderWidth, borderPaint)

        borderPaint.shader = borderBottomShader
        canvas.drawRect(0f, h - borderWidth, w, h, borderPaint)

        borderPaint.shader = borderLeftShader
        canvas.drawRect(0f, 0f, borderWidth, h, borderPaint)

        borderPaint.shader = borderRightShader
        canvas.drawRect(w - borderWidth, 0f, w, h, borderPaint)

        // ── Solicitar siguiente frame sincronizado con vsync ───────────────────
        postInvalidateOnAnimation()
    }
}