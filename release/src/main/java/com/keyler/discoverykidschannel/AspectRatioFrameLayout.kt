package com.keyler.discoverykidschannel

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.keyler.discoverykidschannel.R

/**
 * BUG FIX (2009.5.2.1 — investigación a fondo, corrige un error de diseño de
 * la 2009.5.1.0/2009.5.2.0):
 *
 * El CONTENEDOR (este view — envuelve el video, el ScreenBug y el overlay
 * CRT, ver activity_main.xml) SIEMPRE tiene que estar en proporción 4:3,
 * SIN EXCEPCIÓN — esa es la "forma de pantalla" que toda la app espera
 * (ScreenBug y CRT posicionados para 4:3, igual que un televisor real de
 * la época). El switch "Forzar 4:3" de Configuración NO controla la forma
 * de este contenedor — controla si el VIDEO (adentro, ver DkVideoView.kt)
 * se estira para llenar exactamente esta caja de 4:3 (activado — puede
 * distorsionar contenido que no sea nativamente 4:3) o si se ajusta
 * preservando su proporción real, sin estirarse, adentro de esta misma
 * caja de 4:3 (desactivado — un video 16:9, por ejemplo, encaja con
 * franjas arriba/abajo, sin deformarse).
 *
 * Releases anteriores (2009.5.1.0 y 2009.5.2.0) tenían esto invertido: este
 * contenedor dejaba de ser 4:3 (pasaba a ocupar toda la pantalla, forma
 * 16:9 típica) cuando "Forzar 4:3" estaba desactivado — por eso el video
 * terminaba viéndose en 16:9 en vez de encajar dentro de un marco 4:3.
 * Corregido: este view ya no tiene ningún toggle para ESE caso — siempre
 * fuerza 4:3 cuando se usa a pantalla completa.
 *
 * BUG FIX (5.6.0 — investigación a fondo, "cambiar la posición del
 * VideoView en el NextProgram"): cuando LiveDiscoveryKids.showVideoInBox()
 * achica y reposiciona ESTE MISMO view (videoContainer) para que el video
 * quede dentro del recuadro del marco NextProgram, el onMeasure() de acá
 * abajo IGNORABA por completo el ancho que showVideoInBox() intentaba
 * asignarle — siempre recalculaba width = height×4/3, sin importar el
 * layoutParams recibido. El recuadro real del marco NextProgram NO es 4:3
 * (mide ≈1.4:1, medido con precisión de píxel sobre una captura de
 * referencia), así que el video quedaba ~22px más angosto de lo que
 * debía, corrido de posición respecto al borde amarillo real del recuadro.
 *
 * [forceAspectRatio] permite desactivar el forzado de acá para ESE caso
 * puntual: con él en false, este view respeta el ancho/alto explícitos que
 * le pasen (ver showVideoInBox()/restoreVideoFullScreen() en
 * LiveDiscoveryKids.kt) en vez de recalcular su propio 4:3.
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
