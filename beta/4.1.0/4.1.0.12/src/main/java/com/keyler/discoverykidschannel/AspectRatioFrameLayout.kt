/*
 ESTO TIENE LICENCIA GNU LICENSE APACHE

 NO ELIMINAR NINGUN COMENTARIO AGREGADO
*/
package com.keyler.discoverykidschannel

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * AspectRatioFrameLayout — Preview 2006.4.1.0.12
 *
 * forceAspectRatio = true  (default, comportamiento histórico):
 *     onMeasure() sobreescribe las specs recibidas para forzar SIEMPRE una
 *     proporción 4:3 (width = height * 4 / 3), sin importar lo que diga el
 *     XML (que declara match_parent/match_parent solo como valor de partida).
 *
 * forceAspectRatio = false (Preview 4.1.0.12):
 *     onMeasure() NO sobreescribe nada — deja pasar las specs originales tal
 *     cual las definió el XML (android:layout_height="match_parent" /
 *     android:layout_width="match_parent"), o sea super.onMeasure() con los
 *     specs recibidos sin modificar. El contenedor ocupa entonces el tamaño
 *     real de pantalla, sin imponer 4:3.
 *
 * Controlado desde SettingsActivity ("Forzar 4:3") vía SettingsManager,
 * aplicado en LiveDiscoveryKids.applySettings().
 */
class AspectRatioFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    /**
     * true  → fuerza 4:3 en onMeasure() (comportamiento histórico).
     * false → no fuerza nada; usa las specs originales del XML
     *         (match_parent / match_parent → tamaño real de pantalla).
     * requestLayout() para que el cambio se refleje de inmediato si se
     * modifica mientras la vista ya está medida/dibujada en pantalla.
     */
    var forceAspectRatio: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            requestLayout()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (!forceAspectRatio) {
            // Forzar 4:3 OFF: no se tocan los specs, se usan tal cual los manda
            // el padre/XML (match_parent / match_parent → tamaño real de pantalla).
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
