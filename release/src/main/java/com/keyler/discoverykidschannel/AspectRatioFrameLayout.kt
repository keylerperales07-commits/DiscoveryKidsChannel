package com.keyler.discoverykidschannel

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * BUG FIX (2009.5.1.0) — "Forzar 4:3 fuerza cuando está desactivado o
 * activado": onMeasure() forzaba SIEMPRE el contenedor a proporción 4:3, sin
 * mirar el switch de Configuración. applySettings() intentaba controlarlo
 * cambiando el width del videoView hijo (WRAP_CONTENT/MATCH_PARENT), pero
 * eso no servía de nada porque este contenedor PADRE ya venía forzado a 4:3
 * de antemano — el hijo solo podía llenar ese espacio ya recortado.
 *
 * Ahora el forzado de 4:3 es opcional vía [forceAspectRatio] (ver
 * LiveDiscoveryKids.applySettings(), que lo sincroniza con
 * SettingsManager.isForceAspectRatioEnabled() y llama requestLayout()).
 * Desactivado: el contenedor ocupa el espacio completo (match_parent real),
 * sin recorte — necesario para TextureView en resoluciones altas, donde el
 * recorte a 4:3 también cortaba el video. Activado: mismo comportamiento de
 * siempre (recorte 4:3), para quien lo prefiera.
 */
class AspectRatioFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var forceAspectRatio: Boolean = true

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (!forceAspectRatio) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }

        val height = MeasureSpec.getSize(heightMeasureSpec)
        val width = (height * 4) / 3

        val newWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
        val newHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)

        super.onMeasure(newWidthSpec, newHeightSpec)
    }
}
