package com.keyler.discoverykidschannel

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var videoView: AspectRatioVideoView
    private lateinit var screenBug: ImageView

    // Tipos de estado de reproducción
    enum class PlaybackState { BUMPER, PROGRAM, COMMERCIAL }

    // Programación exigida
    private val playlist = listOf(
        "bumper", "pro1.mp4", "bumper", "pro2.mp4",
        "bumper", "pro3.mp4", "bumper", "pro4.mp4", "bumper"
    )
    
    private var currentIndex = 0
    private var currentState = PlaybackState.BUMPER
    
    // Variables para control de interrupciones (Comerciales)
    private var commercialBreakPoints = mutableListOf<Int>()
    private var currentProgramUri: Uri? = null
    private var savedProgramPosition = 0
    private var isResumingProgram = false
    private var lastResumePosition = 0

    // Control del ScreenBug
    private var isScreenBugVisible = false
    private val updateHandler = Handler(Looper.getMainLooper())

    // Monitor constante (se ejecuta cada medio segundo)
    private val playbackMonitor = object : Runnable {
        override fun run() {
            if (videoView.isPlaying && currentState == PlaybackState.PROGRAM) {
                val currentPos = videoView.currentPosition
                val totalDuration = videoView.duration
    
                // PROTECCIÓN: Si el video aún no carga su duración real, saltamos este ciclo
                if (totalDuration > 0) {
                    val nextCommercial = commercialBreakPoints.firstOrNull()
                    
                    // Lógica de saltar a comercial
                    if (nextCommercial != null && currentPos >= nextCommercial) {
                        playCommercial()
                        return 
                    }
    
                    // Cálculo del tiempo hasta el próximo evento (comercial o fin)
                    val timeToNextInterrupt = if (nextCommercial != null) {
                        nextCommercial - currentPos
                    } else {
                        totalDuration - currentPos
                    }
                
                    val timeSinceResume = currentPos - lastResumePosition

                    // DECISIÓN DEL LOGO
                    // Ocultar si faltan 20 segs para el comercial/final
                    if (timeToNextInterrupt <= 20000 && isScreenBugVisible) {
                        hideScreenBug()
                    } 
                    // Mostrar si ya pasaron 20 segs desde que inició/volvió el programa
                    // Y si todavía faltan más de 20 segs para el siguiente corte
                    else if (timeSinceResume >= 20000 && timeToNextInterrupt > 20000 && !isScreenBugVisible) {
                        showScreenBug()
                    }
                }
            }
            updateHandler.postDelayed(this, 500)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        videoView = findViewById(R.id.videoView)
        screenBug = findViewById(R.id.screenBug)

        checkPermissionsAndStart()
        setupDebugInfo()

        videoView.setOnPreparedListener { mp ->
            if (currentState == PlaybackState.PROGRAM) {
                hideScreenBugFast()
                if (isResumingProgram) {
                    videoView.seekTo(savedProgramPosition)
                    lastResumePosition = savedProgramPosition
                    isResumingProgram = false
                } else {
                    // Programa nuevo: calculamos comerciales basados en su duración
                    calculateCommercialBreaks(mp.duration)
                    lastResumePosition = 0
                }
            }
            videoView.start()
        }

        videoView.setOnCompletionListener {
            when (currentState) {
                PlaybackState.BUMPER, PlaybackState.PROGRAM -> {
                    // Pasar al siguiente de la programación
                    currentIndex++
                    playNextItem()
                }
                PlaybackState.COMMERCIAL -> {
                    // Regresar al programa
                    if (commercialBreakPoints.isNotEmpty()) {
                        commercialBreakPoints.removeAt(0) // Quitar el comercial ya emitido
                    }
                    resumeProgram()
                }
            }
        }
    }

    private fun checkPermissionsAndStart() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 101)
        } else {
            startProgramming()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startProgramming()
        } else {
            Toast.makeText(this, "Permiso necesario para leer pro.mp4", Toast.LENGTH_LONG).show()
        }
    }

    private fun startProgramming() {
        currentIndex = 0
        updateHandler.post(playbackMonitor)
        playNextItem()
    }

    private fun playNextItem() {
        if (currentIndex >= playlist.size) {
            currentIndex = 0 // Reinicia la programación si termina
        }

        val itemName = playlist[currentIndex]

        if (itemName == "bumper") {
            currentState = PlaybackState.BUMPER
            hideScreenBugFast()
            val uri = Uri.parse("android.resource://$packageName/${R.raw.bumper}")
            videoView.setVideoURI(uri)
        } else {
            // Intentar cargar el programa de la carpeta Movies
            val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            val file = File(moviesDir, itemName)

            if (file.exists()) {
                currentState = PlaybackState.PROGRAM
                currentProgramUri = Uri.fromFile(file)
                videoView.setVideoURI(currentProgramUri)
            } else {
                // Si no existe, se salta al siguiente ítem inmediatamente
                currentIndex++
                playNextItem()
            }
        }
    }

    private fun playCommercial() {
        // 1. ¡ESTA ES LA LÍNEA QUE FALTABA! 
        // Guardamos el segundo exacto donde se quedó el niño viendo el programa.
        savedProgramPosition = videoView.currentPosition 

        currentState = PlaybackState.COMMERCIAL
    
        // Ocultar ScreenBug inmediatamente
        hideScreenBugFast() 
    
        val uri = Uri.parse("android.resource://$packageName/${R.raw.comercial1}")
        videoView.setVideoURI(uri)
    }


    private fun resumeProgram() {
        currentState = PlaybackState.PROGRAM
        isResumingProgram = true
        lastResumePosition = savedProgramPosition
        hideScreenBugFast()
        videoView.setVideoURI(currentProgramUri)
    }

    // Calcula cuántos comerciales poner según la duración para que sean equivalentes
    private fun calculateCommercialBreaks(duration: Int) {
        commercialBreakPoints.clear()
        // Ejemplo: Insertar un comercial cada 3 minutos (180,000 ms)
        val interval = 180000 
        var point = interval

        // No insertar comerciales si falta menos de 1 minuto para que acabe el programa
        while (point < duration - 60000) {
            commercialBreakPoints.add(point)
            point += interval
        }
    }
    
    private fun setupDebugInfo() {
        val debugTextView: android.widget.TextView = findViewById(R.id.debugInfo)
    
        // 1. Obtener datos de la App (Versión)
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val versionName = packageInfo.versionName
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            packageInfo.versionCode.toLong()
        }

        // 2. Obtener datos del Sistema (Android y Modelo)
        val androidVersion = Build.VERSION.RELEASE
        val sdkInt = Build.VERSION.SDK_INT
        val model = Build.MODEL
    
        // 3. Obtener el nombre de la API (Codename)
        // Nota: A partir de Android 10 ya no tienen nombres de dulces oficiales 
        // en el sistema, pero podemos mapear los más comunes.
        val apiName = when (sdkInt) {
            36 -> "BakLava"
            35 -> "Vanilla IceCream"
            34 -> "Upside Down Cake"
            33 -> "Tiramisu"
            32, 31 -> "S"
            30 -> "R"
            29 -> "Q"
            28 -> "Pie"
            27, 26 -> "Oreo"
            25, 24 -> "Nougat"
            23 -> "Marshmallow"
            22, 21 -> "Lollipop"
            20, 19 -> "Kitkat"
            else -> "$sdkInt"
        }

        // Formatear el texto según tu pedido:
        // alpha [version_name], versionCode: [version_code], Android [Number version] [Api Name], SDK: [Api Number], Model: [Model of Phone]
        val debugText = "alpha $versionName, versionCode: $versionCode, Android $androidVersion $apiName\nSDK: $sdkInt, Model: $model"
    
        debugTextView.text = debugText
    }


        // Animaciones del ScreenBug
    private fun showScreenBug() {
        if (isScreenBugVisible) return // Evita reiniciar la animación si ya está prendido
    
        isScreenBugVisible = true
        
        // ¡LA CLAVE! Forzamos la capa visual a estar por encima del video
        screenBug.bringToFront()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            screenBug.translationZ = 10f // Elevación 3D para que el video no lo tape
        }

        screenBug.visibility = View.VISIBLE
        screenBug.alpha = 0f // Empieza invisible para que se note el fade
        screenBug.animate()
            .alpha(1f)
            .setDuration(1000)
            .start()
    }

    private fun hideScreenBugFast() {
        screenBug.animate().cancel() // IMPORTANTE: Siempre cancelar la animación PRIMERO
        isScreenBugVisible = false
        screenBug.alpha = 0f
        screenBug.visibility = View.GONE
    }


    private fun hideScreenBug() {
        isScreenBugVisible = false
        screenBug.animate().alpha(0f).setDuration(1000).withEndAction {
            if (!isScreenBugVisible) screenBug.visibility = View.GONE
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        updateHandler.removeCallbacks(playbackMonitor)
    }
}
