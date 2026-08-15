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
import com.keyler.discoverykidschannel.R
/**
 * CrtOverlayView – Efecto CRT sobre VideoView y ScreenBug.
 * Release 2006.4.1.0.12-preview – Discovery Kids Channel · Era Doki 1.0 (2005–2009)
 *
 * Renderiza cinco capas de efecto CRT usando Canvas puro (sin OpenGL ni shaders):
 *
 *  1. SCANLINES REALISTAS
 *     Cada línea de barrido tiene 1px oscura + 1px de "glow" semi-transparente
 *     entre ellas para simular el bloom de fósforo real de un CRT.
 *     2006.4.1.0.10-preview: separación subida a 3px y alphas reducidos — los
 *     televisores CRT de mediados/fines de los 2000 (Trinitron y sucesores)
 *     tenían barrido más fino y menos visible que los tubos de los 90.
 *     Se desplazan hacia abajo sincronizadas con el vsync.
 *
 *  2. PHOSPHOR MASK (RGB pixel grid)
 *     Franjas verticales de 1px R-G-B alternadas imitando la shadow mask del tubo.
 *     2006.4.1.0.10-preview: alpha reducido para una máscara más sutil.
 *
 *  3. VIGNETTE
 *     Gradiente radial desde el centro hacia los bordes con oscurecimiento
 *     progresivo desde el 40% del radio.
 *     2006.4.1.0.10-preview: oscurecimiento general reducido — tubos más
 *     planos y con mejor uniformidad de brillo en los bordes.
 *
 *  4. CRT BORDER GRADIENT – DELGADO (release 1999.2.2.0.01)
 *     Ahora ubicado DENTRO del AspectRatioFrameLayout: el degradado negro
 *     aparece exactamente en el borde del rectángulo 4:3, no en el letterbox.
 *     2006.4.1.0.10-preview: grosor reducido aún más (~12dp) y alpha más bajo,
 *     simulando el marco casi imperceptible de los TV de pantalla plana CRT.
 *
 *  5. FLICKER
 *     Oscilación sinusoidal combinando dos frecuencias para efecto orgánico.
 *     2006.4.1.0.10-preview: intensidad reducida — fuentes de alimentación más
 *     estables en los televisores de esta época, menor parpadeo perceptible.
 *
 * EFECTO CONFIGURABLE (2006.4.1.0.12-preview)
 *     effectEnabled (Boolean) sustituye al brightnessMultiplier (Float 0.0–1.0)
 *     de la Preview 4.1.0.11: en Configuración el control de "Brillo del CRT"
 *     pasó de un slider a un simple activar/desactivar. Internamente se sigue
 *     usando un factor (1f u 0f) para reutilizar toda la lógica de escalado de
 *     alphas ya existente, sin modificar los valores base de la Era 2006.
 */
class CrtOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ── Parámetros ajustables ──────────────────────────────────────────────────

    /** Alpha (0-255) de las líneas de barrido oscuras. */
    var scanlineAlpha: Int = 65  // 2006.4.1.0.10-preview: reducido 100→65, barrido más fino (Era Doki / TVs de 2006)

    /**
     * Alpha (0-255) del glow entre scanlines (bloom de fósforo).
     * 1999.2.2.0.01: nuevo parámetro. Simula la luz que emite el fósforo entre líneas.
     * 18 = muy sutil, 35 = bloom visible.
     */
    var scanlineGlowAlpha: Int = 16  // 2006.4.1.0.10-preview: reducido 25→16, bloom más sutil

    /** Separación total en píxeles de cada ciclo de scanline (oscura + glow). */
    var scanlineSpacing: Int = 3   // 2006.4.1.0.10-preview: subido 2→3, barrido menos denso

    /** Velocidad de desplazamiento vertical de las scanlines (px por frame). */
    var scanlineScrollSpeed: Float = 0.5f

    /** Alpha (0-255) de la máscara de fósforo RGB. */
    var phosphorAlpha: Int = 18   // 2006.4.1.0.10-preview: reducido 30→18, máscara RGB más discreta

    /** Alpha (0-255) del vignette en los bordes. */
    var vignetteAlpha: Int = 150  // 2006.4.1.0.10-preview: reducido 210→150, menor oscurecimiento de bordes

    /**
     * Intensidad del flicker (0.0 = sin flicker, 0.12 = notable).
     */
    var flickerIntensity: Float = 0.035f  // 2006.4.1.0.10-preview: reducido 0.065→0.035, parpadeo casi imperceptible

    /**
     * Ancho en píxeles de las franjas de borde CRT.
     * 2006.4.1.0.10-preview: reducido a ~12dp, marco casi imperceptible.
     * El degradado ahora vive dentro del 4:3 gracias al cambio en activity_main.xml.
     */
    var borderWidth: Float = 12f * resources.displayMetrics.density

    /** Alpha máximo (0-255) del negro en el borde CRT. */
    var borderAlpha: Int = 150  // 2006.4.1.0.10-preview: reducido 210→150, borde más tenue

    /**
     * Activa/desactiva el efecto CRT completo. Preview 2006.4.1.0.12: reemplaza
     * a brightnessMultiplier (4.1.0.11). Controlado desde SettingsActivity
     * ("Efecto CRT") vía SettingsManager y aplicado en LiveDiscoveryKids.onCreate().
     * Cuando es false, todas las capas (scanlines, phosphor mask, vignette,
     * bordes y flicker) se renderizan a alpha 0 — la vista no se oculta con
     * visibility = GONE para no alterar el árbol de medidas del AspectRatioFrameLayout.
     */
    var effectEnabled: Boolean = true

    /** Factor interno (1f / 0f) derivado de [effectEnabled]. Reusa toda la lógica de escalado ya existente. */
    private val intensityFactor: Float get() = if (effectEnabled) 1f else 0f

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
    private var cachedIntensity = -1f
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

        val factor = intensityFactor

        // ── Flicker global ─────────────────────────────────────────────────────
        // Calcula alpha global con oscilación sinusoidal de ~60 Hz visual.
        val elapsedSec = (System.currentTimeMillis() - startTimeMs) / 1000f
        // Combina dos frecuencias para un flicker más orgánico (menos regular)
        val flickerVal = sin(elapsedSec * 62f) * 0.6f + sin(elapsedSec * 119f) * 0.4f
        val globalAlpha = (1f - flickerIntensity * factor * flickerVal).coerceIn(0f, 1f)
        alpha = globalAlpha

        // ── Scanlines realistas con glow de fósforo (1999.2.2.0.01) ───────────
        // Cada ciclo = 1px glow (fósforo iluminado) + 1px oscuro (banda entre electrones).
        // Esto imita el patrón visual real de un CRT: las bandas negras son las
        // separaciones entre líneas de barrido, y el glow es la línea encendida.
        scanlinePaint.alpha = (scanlineAlpha * factor).toInt()
        scanlineGlowPaint.alpha = (scanlineGlowAlpha * factor).toInt()
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
        val scaledPhosphorAlpha = (phosphorAlpha * factor).toInt()
        phosphorPaintR.alpha = scaledPhosphorAlpha
        phosphorPaintG.alpha = scaledPhosphorAlpha
        phosphorPaintB.alpha = scaledPhosphorAlpha
        var x = 0f
        while (x < w) {
            canvas.drawRect(x,       0f, x + 1f, h, phosphorPaintR)
            canvas.drawRect(x + 1f, 0f, x + 2f, h, phosphorPaintG)
            canvas.drawRect(x + 2f, 0f, x + 3f, h, phosphorPaintB)
            x += 3f
        }

        // ── Vignette + Border shaders (se reconstruyen si cambia el tamaño o el factor) ──
        if (w.toInt() != cachedWidth || h.toInt() != cachedHeight || factor != cachedIntensity) {
            cachedWidth  = w.toInt()
            cachedHeight = h.toInt()
            cachedIntensity = factor
            val scaledVignetteAlpha = (vignetteAlpha * factor).toInt()
            val cx = w / 2f
            val cy = h / 2f
            // Radio del gradiente: hipotenusa para cubrir las esquinas
            val radius = Math.sqrt((cx * cx + cy * cy).toDouble()).toFloat() * 1.05f
            vignetteShader = RadialGradient(
                cx, cy, radius,
                intArrayOf(
                    Color.TRANSPARENT,                                  // centro: sin oscurecimiento
                    Color.TRANSPARENT,                                  // 40%: aún claro (antes era 55%)
                    Color.argb(scaledVignetteAlpha / 4, 0, 0, 0),      // 60%: empieza a oscurecer suavemente
                    Color.argb(scaledVignetteAlpha / 2, 0, 0, 0),      // 78%: oscurecimiento medio
                    Color.argb(scaledVignetteAlpha, 0, 0, 0)            // bordes: oscuridad total
                ),
                floatArrayOf(0f, 0.40f, 0.60f, 0.78f, 1f),
                Shader.TileMode.CLAMP
            )

            // ── CRT Border shaders – franjas lineales en los 4 bordes (1999.2.2.0.23) ──
            // Cada shader va de negro (borderAlpha) en el borde hacia transparente adentro.
            // La franja cubre [borderWidth] px desde el borde hacia el centro.
            val blackEdge  = Color.argb((borderAlpha * factor).toInt(), 0, 0, 0)
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
