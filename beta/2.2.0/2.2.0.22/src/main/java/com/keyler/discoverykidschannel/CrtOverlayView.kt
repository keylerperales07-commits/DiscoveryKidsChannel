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
 * Beta 1999.2.2.0.23 – Discovery Kids Channel
 *
 * Renderiza cinco capas de efecto CRT usando Canvas puro (sin OpenGL ni shaders):
 *
 *  1. SCANLINES
 *     Líneas horizontales oscuras semi-transparentes cada [scanlineSpacing] px.
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
 *     La degradación empieza desde el 40% del radio para que los bordes sean
 *     claramente más oscuros que el centro.
 *     Intensidad ajustable con [vignetteAlpha].
 *
 *  4. CRT BORDER GRADIENT (nuevo en 1999.2.2.0.23)
 *     Cuatro franjas LinearGradient en los cuatro bordes del rectángulo 4:3.
 *     Cada franja va de negro semi-transparente en el borde hacia transparente
 *     hacia adentro, simulando el oscurecimiento de los bordes del tubo CRT real
 *     (distinto al vignette radial: este es lineal y cubre el borde completo).
 *     Ancho ajustable con [borderWidth]. Alpha con [borderAlpha].
 *
 *  5. FLICKER
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
    var scanlineAlpha: Int = 75   // 1999.2.2.0.21: subido de 55 → 75 para más visibilidad

    /** Separación en píxeles entre scanlines. 3 = muy fino, 5 = visible. */
    var scanlineSpacing: Int = 3

    /** Velocidad de desplazamiento vertical de las scanlines (px por frame). */
    var scanlineScrollSpeed: Float = 0.4f

    /** Alpha (0-255) de la máscara de fósforo RGB. 15 = casi invisible, 35 = notable. */
    var phosphorAlpha: Int = 22   // 1999.2.2.0.21: subido de 18 → 22

    /**
     * Alpha (0-255) del vignette en los bordes.
     * 1999.2.2.0.21: subido de 150 → 200 para oscurecer más los bordes.
     */
    var vignetteAlpha: Int = 200

    /**
     * Intensidad del flicker (0.0 = sin flicker, 0.04 = muy sutil, 0.12 = notable).
     * El flicker oscila el alpha global del view entre (1 - flickerIntensity) y 1.
     */
    var flickerIntensity: Float = 0.045f  // 1999.2.2.0.21: subido de 0.035 → 0.045

    /**
     * Ancho en píxeles de las franjas de borde CRT (1999.2.2.0.23).
     * Cada franja cubre este ancho desde el borde hacia adentro, degradando a transparente.
     * 60dp = valor recomendado para pantallas 1080p. Ajustar según densidad del dispositivo.
     */
    var borderWidth: Float = 60f * resources.displayMetrics.density

    /**
     * Alpha máximo (0-255) del negro en el borde CRT (1999.2.2.0.23).
     * 170 = semi-transparente, notable pero sin ocultar completamente la imagen.
     */
    var borderAlpha: Int = 170

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