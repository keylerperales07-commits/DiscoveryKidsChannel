package com.keyler.discoverykidschannel

import android.content.Context
import android.util.AttributeSet
import android.widget.VideoView

class AspectRatioVideoView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : VideoView(context, attrs, defStyleAttr) {

   override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 1. Obtenemos el alto máximo de la pantalla (el límite en horizontal)
        val height = MeasureSpec.getSize(heightMeasureSpec)
    
        // 2. Calculamos el ancho para que sea 4:3 
        // Si la relación es 4 (ancho) a 3 (alto), entonces: Ancho = Alto * 4 / 3
        val width = (height * 4) / 3
    
        // 3. Le ordenamos a Android usar estas medidas exactas
        setMeasuredDimension(width, height)
    }

}
