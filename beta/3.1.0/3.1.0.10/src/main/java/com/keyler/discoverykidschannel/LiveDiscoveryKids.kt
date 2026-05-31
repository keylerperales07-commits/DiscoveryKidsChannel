/*
 ESTO TIENE LICENCIA GNU LICENSE APACHE
 
 NO ELIMINAR NINGUN COMENTARIO AGREGADO
*/
package com.keyler.discoverykidschannel

import android.app.ActivityManager
import android.app.AlertDialog
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.media.MediaPlayer
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

/**
 * Beta 2002.3.0.0.4
 *
 * Cambios respecto a 3.0.0.3:
 *   - Tallas eliminadas por completo (vídeos ya no existen).
 *   - PlayItem.Talla removido del sealed class y de la playlist.
 *   - playTalla() eliminada.
 *   - TALLAS y TALLA_SCREENBUG_MAP eliminados.
 *   - Screenbug S, M y L eliminados; siempre se usa screenbug.webp.
 *   - ya_volvemos → continuamos1, ya_volvemos2 → continuamos2.
 *   - enseguida3 → ya_regresa1, enseguida4 → ya_regresa2.
 *
 * Beta 2002.3.0.0.2 — StandaloneCommercial, assets Era 2002–2005.
 * Beta 2002.3.0.0.1 — Inicio rama 3.x.x, bumpers Era 2002–2005.
 * Release 2001.2.6.0 — Pausa Universal, bumper5, intervalo 9 min.
 * Release 2001.2.5.x — Bug fixes de segundo plano y breakQueue.
 * Release 2001.2.5.0 — Sistema de Tallas, Enseguidas por horario, Era 2001.
 *
 * Problema de fondo: el sistema anterior (2.5.2) usaba múltiples flags
 * (isInProgramSegment, isInCommercialBlock, pausedByLifecycle) para decidir
 * QUÉ pausar en onPause() y CÓMO reanudar en onResume(). Esto generaba
 * inconsistencias porque la lógica de pausa/reanudación era diferente para
 * cada tipo de ítem, y cualquier estado intermedio (ya_regresa1/2, continuamos)
 * podía no quedar cubierto por ninguno de los flags.
 *
 * Nueva estrategia (2.6.0) — PAUSA UNIVERSAL:
 *   onPause(): siempre pausa el VideoView y guarda su posición actual.
 *              Siempre pausa bgPlayer. No depende de ningún flag de estado.
 *              Cancela los timers de screenbug y comerciales.
 *   onResume(): si había un video en reproducción, siempre hace seekTo +
 *              start(). Siempre reanuda bgPlayer si existía. Siempre reinicia
 *              el position tracker. Para segmentos de programa, reprograma
 *              el scheduleSegmentLogic desde la posición guardada.
 *
 * Esto garantiza que CUALQUIER video (programa, bumper, enseguida, comercial,
 * ya_volvemos, talla) se pausa y reanuda correctamente al salir/volver,
 * sin importar qué flag de estado esté activo.
 *
 * Otros cambios en 2.6.0:
 *   - bumper5.mp4 incorporado a la lista de bumpers (anuncio previo a 3.0.0)
 *   - BREAK_INTERVAL_MS ajustado a 9 minutos para programación más realista
 *   - Esta es una de las últimas versiones de la fase 1.1 (Era 1998-2001)
 *     y de la rama 2.x.x, previa a la Gran Update 3.0.0
 *
 * Beta 2001.2.5.2 — BUG FIX: posición incorrecta al volver de segundo plano durante un comercial.
 * Beta 2001.2.5.1 — BUG FIX: posición incorrecta al reanudar después de un comercial (breakQueue).
 * Beta 2001.2.5.0 — Sistema de Tallas, Enseguidas por horario, assets Era 2001.
 * Release 2000.2.3.1 — Migración de strings a strings.xml.
 *
 * Playlist sequence:
 *   Enseguida(1/2) → StandaloneCommercial → Bumper → Programa → Enseguida(1/2) → StandaloneCommercial → Bumper → Programa → ...
 *
 * Programs (pro1..pro4.mp4) are read from the user's Movies folder.
 * Bumpers (bumper.mp4–bumper5.mp4) son aleatorios, sin repetir el mismo dos veces seguidas.
 * Commercial scheduling: 1 break per every 9 minutes of program content, equally spaced.
 * Missing programs are skipped automatically.
 */
class LiveDiscoveryKids : AppCompatActivity() {

    // ── Views ──────────────────────────────────────────────────────────────────
    private lateinit var videoView: VideoView
    private lateinit var screenBug: ImageView
    private lateinit var versionInfo: TextView
    private lateinit var debugTextView: TextView
    private lateinit var prevButton: ImageButton
    private lateinit var nextButton: ImageButton
    // Overlay CRT: scanlines + phosphor mask + vignette + flicker (Canvas puro)
    private lateinit var crtOverlay: CrtOverlayView

    // ── Background music (solo durante programas) ──────────────────────────────
    // MediaPlayer independiente del VideoView para poder pausar/reanudar
    // sin afectar la reproducción del video principal.
    private var bgPlayer: MediaPlayer? = null

    // ── Estado de segundo plano — PAUSA UNIVERSAL (Release 2.6.0) ──────────────
    // Estrategia: onPause() SIEMPRE pausa el video y guarda la posición.
    // onResume() SIEMPRE reanuda desde esa posición. Sin flags de tipo de ítem.
    // Funciona para programa, bumper, enseguida, comercial y continuamos.
    private var wasPlayingBeforePause = false  // true si el video estaba activo al pausar
    private var lastVideoPositionMs   = 0      // posición guardada universalmente en onPause

    // ── Flags de estado (usados solo por la lógica interna de reproducción) ────
    // NO se usan en onPause/onResume desde 2.6.0. Solo sirven para saber
    // qué reprogramar en scheduleSegmentLogic al reanudar un segmento de programa.
    private var isInProgramSegment    = false
    private var isInCommercialBlock   = false  // conservado para saveChannelState()
    private var commercialResumeMs    = 0      // punto de retoma del programa post-comercial

    // ── Tipo de ítem actual (para persistencia de sesión) ──────────────────────
    // Registra qué estaba reproduciendo la app en el momento de cerrar.
    // Valores: "program", "bumper", "enseguida", "talla", "commercial"
    private var currentItemType: String = "bumper"
    // pausedPositionMs: alias de lastVideoPositionMs mantenido por compatibilidad
    // con las referencias internas de saveChannelState() y scheduleSegmentLogic().
    // Ambas variables apuntan al mismo valor; el tracker las mantiene sincronizadas.
    private var pausedPositionMs: Int
        get() = lastVideoPositionMs
        set(value) { lastVideoPositionMs = value }

    // ── FPS (frames por segundo) ───────────────────────────────────────────────
    // Medido con Choreographer.FrameCallback que se dispara en cada vsync.
    // currentFps se actualiza cada segundo y se muestra en el debug overlay.
    private var fpsFrameCount   = 0
    private var fpsLastTimeNs   = 0L
    private var currentFps      = 0
    // object : en lugar de lambda → dentro del bloque "this" refiere al FrameCallback,
    // no a la Activity. Con lambda "this" es la Activity y el compilador rechaza el tipo.
    private val fpsFrameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (fpsLastTimeNs == 0L) fpsLastTimeNs = frameTimeNanos
            fpsFrameCount++
            val elapsed = frameTimeNanos - fpsLastTimeNs
            if (elapsed >= 1_000_000_000L) {   // acumuló 1 segundo
                currentFps    = (fpsFrameCount * 1_000_000_000L / elapsed).toInt()
                fpsFrameCount = 0
                fpsLastTimeNs = frameTimeNanos
            }
            // Re-registrar para el siguiente frame; "this" es el FrameCallback
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    // ── Scheduling ─────────────────────────────────────────────────────────────
    private val handler = Handler(Looper.getMainLooper())
    private val pendingTasks = mutableListOf<Runnable>()
    private val debugHandler = Handler(Looper.getMainLooper())
    // Handler para ocultar los botones nav automáticamente tras 3 s sin tocar
    private val navHideHandler = Handler(Looper.getMainLooper())
    // Handler que guarda la posición del video cada 16 ms (≈60 fps).
    // Release 2.6.0: actualiza lastVideoPositionMs universalmente para
    // cualquier video en reproducción — programa, comercial, bumper o enseguida.
    // Esto hace que onPause() siempre tenga la posición correcta sin importar
    // qué tipo de ítem se estaba reproduciendo.
    private val positionTrackerHandler = Handler(Looper.getMainLooper())
    private val positionTrackerRunnable = object : Runnable {
        override fun run() {
            // Actualización universal: si el video está reproduciendo, guardar posición.
            // No se distingue entre programa, comercial, bumper, etc.
            // pausedPositionMs es una computed property que apunta a lastVideoPositionMs,
            // por lo que actualizar lastVideoPositionMs actualiza ambas referencias.
            if (videoView.isPlaying) {
                lastVideoPositionMs = videoView.currentPosition
            }
            positionTrackerHandler.postDelayed(this, 16)
        }
    }

    // ── Playlist definition ────────────────────────────────────────────────────
    private sealed class PlayItem {
        object Bumper : PlayItem()
        object Enseguida : PlayItem()
        // Beta 3.0.0.2: comercial standalone en la programación lineal
        // (entre Enseguida y Talla), independiente del bloque publicitario de programas.
        object StandaloneCommercial : PlayItem()
        data class Program(val index: Int) : PlayItem()   // 0-based → pro(n+1).mp4
    }

    private val playlist = listOf(
        PlayItem.Enseguida,
        PlayItem.Bumper,
        PlayItem.StandaloneCommercial,   // Beta 3.0.0.2: comercial entre enseguida y bumper
        PlayItem.Program(0),
        PlayItem.Enseguida,
        PlayItem.Bumper,
        PlayItem.StandaloneCommercial,
        PlayItem.Program(1),
        PlayItem.Enseguida,
        PlayItem.Bumper,
        PlayItem.StandaloneCommercial,
        PlayItem.Program(2),
        PlayItem.Enseguida,
        PlayItem.Bumper,
        PlayItem.StandaloneCommercial,
        PlayItem.Program(3)
    )

    private var playlistIndex = 0
    private var currentProgramIndex = 0

    // ── Program state (persisted across commercial breaks) ─────────────────────
    private var currentProgramUri: Uri? = null
    private var programDuration  = 0          // total ms
    private var breakQueue       = mutableListOf<Int>()   // upcoming break positions in ms
    // Último comercial reproducido; se usa para evitar repetir el mismo dos veces seguidas
    private var lastCommercialRes: Int = -1
    // Último bumper reproducido; evita repetir el mismo dos veces seguidas
    private var lastBumperRes: Int = -1
    // Último enseguida post-programa reproducido (enseguida1/2/5); ya no se usa para evitar repetición
    // aleatoria (la selección es ahora horaria), pero se conserva para posibles usos futuros
    private var lastEnseguidaPostProgramaRes: Int = -1
    // Último enseguida pre-comercial reproducido (ya_regresa1/2); evita repetir el mismo
    private var lastEnseguidaPreComercialRes: Int = -1
    // ScreenBug que se mostrará durante el programa siguiente según la talla elegida.
    // Se actualiza en playTalla() y se restaura desde SharedPreferences al reanudar sesión.
    private var currentScreenBugRes: Int = R.drawable.screenbug

    // ── Constants ──────────────────────────────────────────────────────────────
    companion object {
        private const val TAG = "DKids"

        /** Screenbug appears this many ms after segment start or commercial end. */
        private const val BUG_SHOW_DELAY = 20_000L

        /** Screenbug hides this many ms before segment end or commercial start. */
        private const val BUG_HIDE_EARLY = 20_000L

        /** One commercial break is inserted for every this many ms of program. */
        private const val BREAK_INTERVAL_MS = 9 * 60 * 1_000L   // 9 min

        /** Programs shorter than this have zero commercial breaks. */
        private const val MIN_DURATION_FOR_BREAKS = 3 * 60 * 1_000L  // 3 min

        /** Alpha-animation duration for screenbug fade. */
        private const val FADE_MS = 1_000L

        private const val PERM_REQUEST = 42

        // ── SharedPreferences – persistencia de sesión al cerrar la app ─────────
        private const val PREFS_NAME         = "dk_channel_state"
        private const val PREF_HAS_STATE     = "has_saved_state"
        private const val PREF_PLAYLIST_IDX  = "playlist_index"
        private const val PREF_POSITION_MS   = "position_ms"
        private const val PREF_PROGRAM_IDX   = "program_index"
        private const val PREF_ITEM_TYPE     = "item_type"       // "program"|"bumper"|"enseguida"|"talla"|"commercial"
        private const val PREF_COMMERCIAL_MS = "commercial_resume_ms"
        // Persiste el screenbug elegido por la talla para restaurarlo al reanudar sesión
        private const val PREF_SCREENBUG_RES = "screenbug_res"
        // BUG FIX 2001.2.5.1: persiste los breaks pendientes del programa para que al
        // reanudar después de un comercial no se recalculen desde cero y se dispare
        // un break ya consumido. Se guarda como String con formato "ms1,ms2,ms3".
        private const val PREF_BREAK_QUEUE   = "break_queue"

        /** Lista de comerciales disponibles; se elige uno al azar en cada corte. */
        private val COMMERCIALS = listOf(R.raw.comercial1, R.raw.comercial2, R.raw.comercial3, R.raw.comercial4)

        /**
         * Lista de bumpers disponibles.
         * Se elige uno al azar antes de cada programa, evitando repetir el mismo dos veces seguidas.
         */
        private val BUMPERS = listOf(R.raw.bumper, R.raw.bumper2, R.raw.bumper3, R.raw.bumper4, R.raw.bumper5)

        /**
         * Enseguidas post-programa (van entre el fin del programa y el comercial standalone).
         * Beta 3.0.0.3: selección aleatoria con anti-repetición.
         * Se eliminó enseguida5 y la selección por horario.
         * enseguida1 y enseguida2 actualizados a la Era 2002.
         */
        private val ENSEGUIDAS_POST_PROGRAMA = listOf(
            R.raw.enseguida1,
            R.raw.enseguida2,
            R.raw.enseguida3
        )

        /**
         * Enseguidas pre-comercial (van justo ANTES del bloque publicitario,
         * reemplazando al antiguo pre-continuamos).
         * ya_regresa1 → se usa continuamos1 como post-comercial.
         * ya_regresa2 → se usa continuamos2 como post-comercial.
         * Se elige una al azar evitando repetir la misma dos veces seguidas.
         */
        private val ENSEGUIDAS_PRE_COMERCIAL = listOf(
            R.raw.ya_regresa1,
            R.raw.ya_regresa2,
            R.raw.ya_regresa3
        )

        /**
         * Mapeo: enseguida pre-comercial → continuamos que se debe usar en ese corte.
         * ya_regresa1 → continuamos1 | ya_regresa2 → continuamos2
         */
        private val ENSEGUIDA_YA_VOLVEMOS_MAP = mapOf(
            R.raw.ya_regresa1 to R.raw.continuamos1,
            R.raw.ya_regresa2 to R.raw.continuamos2,
            R.raw.ya_regresa3 to R.raw.continuamos3
        )
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ══════════════════════════════════════════════════════════════════════════

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on + hide system bars via window flags BEFORE setContentView.
        // DO NOT call goFullscreen() here – the DecorView is not yet attached to
        // the WindowManager on Android 11+ and causes a NullPointerException.
        // goFullscreen() is called safely from onWindowFocusChanged instead.
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setContentView(R.layout.activity_main)

        videoView = findViewById(R.id.videoView)
        screenBug = findViewById(R.id.screenBug)
        screenBug.alpha = 0f
        prevButton = findViewById(R.id.btnPrevious)
        nextButton = findViewById(R.id.btnNext)
        // Inicializar overlay CRT; empieza a dibujarse automáticamente via postInvalidateOnAnimation
        crtOverlay = findViewById(R.id.crtOverlay)
        // Release 1999.2.2.0.01: border delgado (~18dp) definido como default en CrtOverlayView.
        // El CrtOverlayView ahora vive DENTRO del AspectRatioFrameLayout (ver activity_main.xml)
        // para que el degradado quede exactamente en el borde del 4:3.
        // Los valores default de CrtOverlayView ya reflejan la nueva configuración.
        // Si se desea ajustar desde código:
        // crtOverlay.borderWidth = 18f * resources.displayMetrics.density
        // crtOverlay.borderAlpha = 210

        // Botones ocultos al inicio; aparecen al tocar la pantalla
        prevButton.visibility = View.GONE
        nextButton.visibility = View.GONE

        prevButton.setOnClickListener {
            resetNavHideTimer()   // reinicia los 3 s al pulsar
            goToAdjacentProgram(-1)
        }
        nextButton.setOnClickListener {
            resetNavHideTimer()   // reinicia los 3 s al pulsar
            goToAdjacentProgram(+1)
        }

        requestStoragePermission()
        
        setupDebugInfo()
        displayInfo()

        // Iniciar medidor de FPS
        Choreographer.getInstance().postFrameCallback(fpsFrameCallback)

        // Release 2.6.0: iniciar el position tracker GLOBALMENTE desde el inicio.
        // El tracker actualiza lastVideoPositionMs cada 16 ms para cualquier video
        // en reproducción, sin importar el tipo. onPause() siempre tendrá la posición
        // correcta sin depender de flags de estado.
        startPositionTracker()

        // Interceptar el botón atrás para confirmar salida y guardar estado.
        // Se usa OnBackPressedDispatcher (API moderna) en lugar del deprecated onBackPressed().
        // El estado se guarda SOLO aquí para que el AlertDialog de reanudación no aparezca
        // al volver de un cambio de app temporal (back-grounding sin cerrar).
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmationDialog()
            }
        })
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) goFullscreen()
    }

    /**
     * Release 2.6.0 — PAUSA UNIVERSAL.
     *
     * Estrategia anterior (2.5.x): usaba flags (isInProgramSegment, isInCommercialBlock)
     * para decidir qué pausar. Cualquier estado no cubierto por un flag generaba un bug.
     *
     * Nueva estrategia: onPause() SIEMPRE pausa el video y guarda la posición,
     * sin importar qué tipo de ítem se estaba reproduciendo (programa, bumper,
     * enseguida, comercial o continuamos.
     *
     * Por qué NO leer videoView.currentPosition aquí:
     *   Android pausa el VideoView ANTES de llamar onPause(), por lo que
     *   currentPosition devuelve 0 o un valor incorrecto en ese momento.
     *   lastVideoPositionMs viene actualizado cada 16 ms por el tracker,
     *   garantizando un valor correcto sin importar el timing del SO.
     */
    override fun onPause() {
        super.onPause()
        // El tracker ya actualizó lastVideoPositionMs continuamente hasta este momento.
        // Solo hay que detenerlo y marcar que había reproducción activa.
        wasPlayingBeforePause = videoView.isPlaying || lastVideoPositionMs > 0
        stopPositionTracker()
        videoView.pause()
        bgPlayer?.pause()
        // Cancelar timers de screenbug y comerciales para evitar que se disparen
        // en background. Se reconfiguran en onResume para segmentos de programa.
        cancelAllTasks()
        Log.d(TAG, "onPause – posición guardada: ${lastVideoPositionMs}ms (wasPlaying=$wasPlayingBeforePause)")
    }

    /**
     * Release 2.6.0 — REANUDACIÓN UNIVERSAL.
     *
     * Si había video activo al pausar: seekTo(lastVideoPositionMs) + start().
     * Para segmentos de programa se reprograman además los timers de screenbug
     * y el próximo corte comercial desde la posición guardada.
     * Para cualquier otro ítem (bumper, enseguida, comercial, etc.) el onCompletion
     * ya registrado por playUri() continúa la secuencia normalmente al terminar.
     */
    override fun onResume() {
        super.onResume()
        if (!wasPlayingBeforePause) return
        wasPlayingBeforePause = false

        videoView.seekTo(lastVideoPositionMs)
        videoView.start()
        bgPlayer?.start()
        startPositionTracker()

        // Si estábamos en un segmento de programa, reprogramar los timers
        // de screenbug y el próximo corte comercial desde la posición guardada.
        if (isInProgramSegment) {
            scheduleSegmentLogic(lastVideoPositionMs)
            Log.d(TAG, "onResume – programa reanudado desde ${lastVideoPositionMs}ms")
        } else {
            Log.d(TAG, "onResume – video reanudado desde ${lastVideoPositionMs}ms (no-programa)")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelAllTasks()
        navHideHandler.removeCallbacksAndMessages(null)
        positionTrackerHandler.removeCallbacksAndMessages(null)
        videoView.stopPlayback()
        // Liberar MediaPlayer de música de fondo para evitar leaks
        stopBgMusic()
        // Detener el medidor de FPS
        Choreographer.getInstance().removeFrameCallback(fpsFrameCallback)
    }

    /**
     * onStop: ya NO guarda el estado aquí.
     * El guardado ocurre únicamente cuando el usuario confirma que quiere salir
     * a través del diálogo de confirmación (showExitConfirmationDialog).
     * Esto evita que el AlertDialog de reanudación aparezca al volver de
     * un cambio temporal de app.
     */
    override fun onStop() {
        super.onStop()
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Toque de pantalla → mostrar / ocultar botones de navegación
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * dispatchTouchEvent se ejecuta ANTES de que cualquier vista (incluido
     * VideoView) consuma el evento, por lo que captura todo toque en pantalla.
     * Solo reaccionamos a ACTION_DOWN para no disparar múltiples veces
     * por gesto.
     */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) showNavButtons()
        return super.dispatchTouchEvent(ev)
    }

    /** Hace visibles los botones y programa su ocultado a los 3 segundos. */
    private fun showNavButtons() {
        prevButton.visibility = View.VISIBLE
        nextButton.visibility = View.VISIBLE
        resetNavHideTimer()
    }

    /** Cancela el temporizador anterior y lo reinicia desde cero (3 s). */
    private fun resetNavHideTimer() {
        navHideHandler.removeCallbacksAndMessages(null)
        navHideHandler.postDelayed({
            prevButton.visibility = View.GONE
            nextButton.visibility = View.GONE
        }, 3_000L)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Permissions
    // ══════════════════════════════════════════════════════════════════════════

    private fun requestStoragePermission() {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            android.Manifest.permission.READ_MEDIA_VIDEO
        else
            android.Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) {
            startChannel()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(perm), PERM_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Start regardless – programs will be skipped if not found
        startChannel()
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Playlist driver
    // ══════════════════════════════════════════════════════════════════════════

    private fun startChannel() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(PREF_HAS_STATE, false)) {
            // Hay sesión guardada → preguntar al usuario
            showResumeDialog(prefs)
        } else {
            // Sin sesión → arrancar desde el principio
            playlistIndex = 0
            advance()
        }
    }

    /** Move to the next playlist item (wraps around). */
    private fun advance() {
        if (playlistIndex >= playlist.size) playlistIndex = 0
        when (val item = playlist[playlistIndex]) {
            is PlayItem.Bumper               -> playBumper()
            is PlayItem.Enseguida            -> playEnseguida()
            is PlayItem.StandaloneCommercial -> playStandaloneCommercial()
            is PlayItem.Program              -> playProgram(item.index)
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Bumper playback
    // ══════════════════════════════════════════════════════════════════════════

    private fun playBumper() {
        cancelAllTasks()
        setBugAlpha(0f)
        stopBgMusic()   // bumper → sin música de fondo
        isInProgramSegment = false   // no pausar al ir a background en bumper
        currentItemType = "bumper"

        // Elige un bumper al azar evitando repetir el anterior
        val candidates = BUMPERS.filter { it != lastBumperRes }.ifEmpty { BUMPERS }
        val chosenBumper = candidates.random()
        lastBumperRes = chosenBumper

        Log.d(TAG, "▶ BUMPER [res=$chosenBumper]")

        playUri(rawUri(chosenBumper)) {
            playlistIndex++
            advance()
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Standalone Commercial – comercial en la programación lineal
    // Aparece entre Enseguida y Talla como parte del flujo de canal.
    // Es independiente del bloque publicitario (playCommercial) que interrumpe
    // programas: no tiene ya_volvemos ni lógica de breakQueue.
    // Beta 3.0.0.2
    // ══════════════════════════════════════════════════════════════════════════

    private fun playStandaloneCommercial() {
        cancelAllTasks()
        setBugAlpha(0f)
        stopBgMusic()
        isInProgramSegment  = false
        isInCommercialBlock = false
        currentItemType     = "standaloneCommercial"

        // Reutiliza la lista COMMERCIALS y el anti-repetición existentes
        val candidates     = COMMERCIALS.filter { it != lastCommercialRes }.ifEmpty { COMMERCIALS }
        val chosenCommercial = candidates.random()
        lastCommercialRes  = chosenCommercial

        Log.d(TAG, "▶ STANDALONE COMMERCIAL [res=$chosenCommercial]")

        playUri(rawUri(chosenCommercial)) {
            playlistIndex++
            advance()
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Enseguida playback – post-programa: aparece entre el fin del programa y el bumper.
    // ══════════════════════════════════════════════════════════════════════════
    // Enseguida playback – post-programa: aparece entre el fin del programa
    // y el comercial standalone.
    // Beta 3.0.0.3: selección aleatoria con anti-repetición entre
    // [enseguida1, enseguida2]. Se eliminó la selección por horario y enseguida5.
    // ══════════════════════════════════════════════════════════════════════════

    private fun playEnseguida() {
        cancelAllTasks()
        setBugAlpha(0f)
        stopBgMusic()
        isInProgramSegment = false
        currentItemType = "enseguida"

        // Selección aleatoria con anti-repetición (igual que bumpers y comerciales)
        val candidates = ENSEGUIDAS_POST_PROGRAMA
            .filter { it != lastEnseguidaPostProgramaRes }
            .ifEmpty { ENSEGUIDAS_POST_PROGRAMA }
        val chosenEnseguida = candidates.random()
        lastEnseguidaPostProgramaRes = chosenEnseguida

        Log.d(TAG, "▶ ENSEGUIDA post-programa [res=$chosenEnseguida]")

        playUri(rawUri(chosenEnseguida)) {
            playlistIndex++
            advance()
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Program playback
    // ══════════════════════════════════════════════════════════════════════════

    private fun playProgram(idx: Int, restartFromBeginning: Boolean = true) {
        currentProgramIndex = idx
        val uri = resolveProgram(idx)
        if (uri == null) {
            Log.w(TAG, "pro${idx + 1}.mp4 not found – skipping")
            playlistIndex++
            advance()
            return
        }

        Log.d(TAG, "▶ PROGRAM pro${idx + 1}")
        currentProgramUri = uri
        breakQueue.clear()

        val startPos = if (restartFromBeginning) 0 else videoView.currentPosition
        beginProgramSegment(uri, startOffsetMs = startPos, isFirstPlay = restartFromBeginning)
    }

    /**
    * Plays the program starting at [startOffsetMs].
    * [isFirstPlay] = true  → recalculate breaks from scratch.
    * [isFirstPlay] = false → breaks already trimmed; resume only.
    */
    private fun beginProgramSegment(uri: Uri, startOffsetMs: Int, isFirstPlay: Boolean) {
        cancelAllTasks()
        setBugAlpha(0f)
        isInCommercialBlock = false   // garantiza reset si se llega aquí desde cualquier ruta

        videoView.setVideoURI(uri)
        videoView.setOnPreparedListener { mp ->
            mp.isLooping = false

            programDuration = mp.duration

            if (isFirstPlay) {
                breakQueue = calcBreaks(programDuration).toMutableList()
            }

            if (startOffsetMs > 0) videoView.seekTo(startOffsetMs)

            scheduleSegmentLogic(startOffsetMs)
            // Nueva funcionalidad 2.4.1 (portado a beta): FadeIn del VideoView al iniciar el
            // programa, tanto en el arranque inicial como al regresar del bloque comercial.
            videoView.alpha = 0f
            videoView.start()
            videoView.animate()
                .alpha(1f)
                .setDuration(500L)
                .start()
            isInProgramSegment = true   // programa activo → pausar al ir a background
            currentItemType = "program"
            startPositionTracker()      // comienza a guardar posición cada 500 ms
            startBgMusic()   // inicia/reanuda música de fondo al comenzar segmento
        }
        videoView.setOnCompletionListener {
            Log.d(TAG, "Program ended")
            cancelAllTasks()
            setBugAlpha(0f)
            isInProgramSegment = false   // programa terminó
            stopPositionTracker()        // ya no hace falta guardar posición
            pausedPositionMs = 0         // reset para el próximo programa
            stopBgMusic()    // programa terminó → detener música de fondo
            playlistIndex++
            advance()
        }
    }

    /**
     * Schedules screenbug show/hide and the next commercial break
     * for the current segment starting at [segmentStartMs] in program time.
     */
    private fun scheduleSegmentLogic(segmentStartMs: Int) {
        // Determine end of this segment (next break or program end)
        val segmentEndMs = if (breakQueue.isNotEmpty()) breakQueue[0] else programDuration
        val segmentDuration = (segmentEndMs - segmentStartMs).toLong().coerceAtLeast(0)

        Log.d(TAG, "Segment: ${segmentStartMs}ms → ${segmentEndMs}ms (${segmentDuration}ms)")

        // ── ScreenBug: show 20 s into segment ────────────────────────────────
        if (segmentDuration > BUG_SHOW_DELAY) {
            post(BUG_SHOW_DELAY) { fadeInBug() }
        }

        // ── ScreenBug: hide 20 s before segment ends ──────────────────────────
        val hideAt = segmentDuration - BUG_HIDE_EARLY
        if (hideAt > BUG_SHOW_DELAY) {   // ensure hide happens strictly after show
            post(hideAt) { fadeOutBug() }
        }

        // ── Schedule commercial if break queue is non-empty ───────────────────
        if (breakQueue.isNotEmpty()) {
            val breakProgramPos = breakQueue[0]
            post(segmentDuration) {
                // Capture actual player position for accurate resume
                val resumePos = breakProgramPos   // use precomputed position for accuracy
                breakQueue.removeAt(0)
                playCommercial(resumePos)
            }
        }
    }

    private fun goToAdjacentProgram(direction: Int) {
        val target = findAvailableProgramIndex(currentProgramIndex, direction) ?: return

        if (target == currentProgramIndex) return

        Log.d(TAG, "▶ Navegando al programa ${target + 1} (direction=$direction)")
        cancelAllTasks()
        setBugAlpha(0f)
        videoView.stopPlayback()

        playlistIndex = playlist.indexOfFirst { it is PlayItem.Program && it.index == target }
            .takeIf { it >= 0 } ?: 0

        playProgram(target, restartFromBeginning = true)
    }

    private fun findAvailableProgramIndex(startIndex: Int, direction: Int): Int? {
        if (direction == 0) return null

        val totalPrograms = 4
        var candidate = startIndex

        repeat(totalPrograms) {
            candidate = (candidate + direction + totalPrograms) % totalPrograms
            if (resolveProgram(candidate) != null) return candidate
        }
        return null
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Commercial playback
    // Secuencia: ya_regresa1/2 (pre-comercial) → comercial(es) → continuamos(pareado) → programa
    // ══════════════════════════════════════════════════════════════════════════

    private fun playCommercial(resumeProgramAtMs: Int) {
        cancelAllTasks()
        setBugAlpha(0f)
        stopBgMusic()   // comercial → sin música de fondo
        isInProgramSegment = false   // no pausar al ir a background en comercial
        isInCommercialBlock = true    // BUG FIX 2001.2.5.0.53: marcar bloque comercial activo
        commercialResumeMs = resumeProgramAtMs   // guardar para persistencia de sesión
        // BUG FIX 2001.2.5.2: reiniciar el tracker (en lugar de detenerlo) para que
        // positionTrackerRunnable actualice commercialPausedMs cada 16 ms durante toda
        // la secuencia comercial. Sin esto, onPause() leía videoView.currentPosition
        // cuando Android ya había pausado el VideoView, obteniendo 0 o un valor incorrecto
        // — el mismo bug clásico resuelto con el tracker para programas en BUG FIX 1998.2.0.1.
        startPositionTracker()

        // BUG FIX 1999.2.1.0.11:
        // currentItemType se actualiza AQUÍ (antes de reproducir) para que onStop
        // guarde "commercial" correctamente si la app se cierra durante la secuencia.
        // El tipo "commercial" cubre todo el bloque: ya_regresa1/2 + comercial + continuamos.
        currentItemType = "commercial"

        // Elige un comercial al azar evitando repetir el anterior
        val commercialCandidates = COMMERCIALS.filter { it != lastCommercialRes }
            .ifEmpty { COMMERCIALS }
        val chosenCommercial = commercialCandidates.random()
        lastCommercialRes = chosenCommercial

        // Elige ya_regresa1 o ya_regresa2 como pre-comercial evitando repetir la anterior.
        // La elección determina qué continuamos se usará al final del corte.
        val preComCandidates = ENSEGUIDAS_PRE_COMERCIAL
            .filter { it != lastEnseguidaPreComercialRes }
            .ifEmpty { ENSEGUIDAS_PRE_COMERCIAL }
        val chosenPreComercial = preComCandidates.random()
        lastEnseguidaPreComercialRes = chosenPreComercial

        // Determina el continuamos a usar según la enseguida elegida (ver mapa en companion)
        val chosenYaVolvemos = ENSEGUIDA_YA_VOLVEMOS_MAP[chosenPreComercial]
            ?: R.raw.continuamos1   // fallback defensivo

        Log.d(TAG, "▶ ENSEGUIDA pre-comercial [res=$chosenPreComercial] → continuamos [res=$chosenYaVolvemos]")

        // BUG FIX / Nueva funcionalidad 2.4.1 (portado a beta):
        // FadeOut del VideoView antes de iniciar el bloque comercial para una
        // transición suave al corte publicitario. La secuencia arranca una vez
        // que la animación de fade ha completado (withEndAction).
        //
        // BUG FIX 2001.2.5.0.52:
        // Al agregar el FadeOut en 2.4.1 nunca se restableció el alpha del VideoView
        // al comenzar el bloque comercial, dejándolo en 0f durante todo el corte.
        // Se agrega videoView.alpha = 1f (sin animación) al inicio de withEndAction.
        videoView.animate()
            .alpha(0f)
            .setDuration(500L)
            .withEndAction {
                // Restablecer alpha sin animación para que el bloque comercial sea visible
                // desde el primer frame (BUG FIX 2001.2.5.0.52)
                videoView.alpha = 1f

                // Paso 1: ya_regresa1 o ya_regresa2 (pre-comercial)
                playUri(rawUri(chosenPreComercial)) {
                    Log.d(TAG, "▶ COMMERCIAL [res=$chosenCommercial] (resumes program at ${resumeProgramAtMs}ms)")

                    // Paso 2: comercial elegido
                    playUri(rawUri(chosenCommercial)) {
                        Log.d(TAG, "▶ YA VOLVEMOS post-comercial [res=$chosenYaVolvemos]")

                        // Paso 3: continuamos pareado con la enseguida elegida
                        playUri(rawUri(chosenYaVolvemos)) {
                            // Paso 4: retomar programa
                            val uri = currentProgramUri ?: run {
                                Log.e(TAG, "No currentProgramUri – advancing")
                                playlistIndex++
                                advance()
                                return@playUri
                            }
                            Log.d(TAG, "Ya volvemos done – resuming program at ${resumeProgramAtMs}ms")
                            isInCommercialBlock = false   // BUG FIX 2001.2.5.0.53: bloque comercial terminado
                            beginProgramSegment(uri, startOffsetMs = resumeProgramAtMs, isFirstPlay = false)
                        }
                    }
                }
            }
            .start()
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Video playback helper
    // ══════════════════════════════════════════════════════════════════════════

    /** Plays [uri] and calls [onComplete] when the video finishes. */
    private fun playUri(uri: Uri, onComplete: () -> Unit) {
        videoView.setOnPreparedListener { mp ->
            mp.isLooping = false
            videoView.start()
        }
        videoView.setOnCompletionListener { onComplete() }
        videoView.setVideoURI(uri)
        videoView.requestFocus()
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Commercial break calculation
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Returns a list of program positions (in ms) where commercial breaks occur.
     * Breaks are distributed evenly: 1 break per [BREAK_INTERVAL_MS] of content.
     * Programs shorter than [MIN_DURATION_FOR_BREAKS] get no breaks.
     */
    private fun calcBreaks(durationMs: Int): List<Int> {
        if (durationMs < MIN_DURATION_FOR_BREAKS) return emptyList()
        val numBreaks = (durationMs / BREAK_INTERVAL_MS).toInt()
        if (numBreaks == 0) return emptyList()
        val interval = durationMs / (numBreaks + 1)
        return (1..numBreaks).map { i -> interval * i }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // URI resolution – programs from Movies folder or MediaStore
    // ══════════════════════════════════════════════════════════════════════════

    private fun resolveProgram(index: Int): Uri? {
        val fileName = "pro${index + 1}.mp4"

        // 1. Direct path in Movies directory (works on Android ≤ 9 or with MANAGE_EXTERNAL)
        val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val file = File(moviesDir, fileName)
        if (file.exists()) {
            Log.d(TAG, "Found via file path: ${file.absolutePath}")
            return Uri.fromFile(file)
        }

        // 2. MediaStore query (Android 10+)
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        return try {
            contentResolver.query(
                collection,
                arrayOf(MediaStore.Video.Media._ID),
                "${MediaStore.Video.Media.DISPLAY_NAME} = ?",
                arrayOf(fileName),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
                    val uri = ContentUris.withAppendedId(collection, id)
                    Log.d(TAG, "Found via MediaStore: $uri")
                    uri
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaStore query failed for $fileName", e)
            null
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Background music – solo durante programas
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Inicia la música de fondo en loop SIN silencio entre repeticiones.
     * Se usa setOnCompletionListener + seekTo(0) + start() en lugar de isLooping=true,
     * ya que isLooping deja un gap audible en muchos dispositivos Android.
     * Si ya hay un MediaPlayer reproduciéndose, no hace nada.
     * Si existe pero estaba detenido, lo reanuda desde donde quedó.
     */
    private fun startBgMusic() {
        if (bgPlayer == null) {
            // Primera vez: crear y configurar el MediaPlayer con loop gapless
            bgPlayer = MediaPlayer.create(this, R.raw.bg_music)?.apply {
                isLooping = false   // NO usar isLooping; usamos el listener para evitar el gap
                setVolume(0.08f, 0.08f)  // volumen al 2% para no tapar el audio del video
                setOnCompletionListener { mp ->
                    // Al terminar, volver al inicio y reproducir de nuevo sin pausa
                    mp.seekTo(0)
                    mp.start()
                    Log.d(TAG, "BG Music LOOP (gapless restart)")
                }
                start()
                Log.d(TAG, "BG Music STARTED")
            }
        } else if (bgPlayer?.isPlaying == false) {
            // Ya existe pero estaba pausado (ej: volvió de comercial) → reanudar
            bgPlayer?.start()
            Log.d(TAG, "BG Music RESUMED")
        }
        // Si ya está reproduciéndose, no hacer nada
    }

    /**
     * Detiene y libera el MediaPlayer de música de fondo.
     * Llamar en bumpers, comerciales y al destruir la Activity.
     */
    private fun stopBgMusic() {
        bgPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
            Log.d(TAG, "BG Music STOPPED")
        }
        bgPlayer = null
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Persistencia de sesión – guardar y restaurar estado al cerrar la app
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Guarda el estado actual del canal en SharedPreferences.
     * Se llama desde onStop (cuando la app deja de ser visible).
     * Persiste: playlistIndex, posición del video, programa actual,
     * tipo de ítem (bumper/enseguida/program/commercial) y posición
     * de reanudación del programa si estamos en un comercial.
     */
    private fun saveChannelState() {
        // Release 2.6.0: lastVideoPositionMs es la posición universal actualizada cada 16 ms.
        // Para bloque comercial usamos commercialResumeMs (posición de retoma del programa).
        val posToSave = when {
            isInCommercialBlock -> commercialResumeMs
            else                -> lastVideoPositionMs
        }
        val breakQueueStr = breakQueue.joinToString(",")

        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putBoolean(PREF_HAS_STATE,     true)
            putInt    (PREF_PLAYLIST_IDX,  playlistIndex)
            putInt    (PREF_POSITION_MS,   posToSave)
            putInt    (PREF_PROGRAM_IDX,   currentProgramIndex)
            putString (PREF_ITEM_TYPE,     currentItemType)
            putInt    (PREF_COMMERCIAL_MS, commercialResumeMs)
            putInt    (PREF_SCREENBUG_RES, currentScreenBugRes)
            putString (PREF_BREAK_QUEUE,   breakQueueStr)
            apply()
        }
        Log.d(TAG, "Estado guardado: type=$currentItemType pos=${posToSave}ms breaks=$breakQueueStr")
    }

    /**
     * Muestra un AlertDialog preguntando si el usuario quiere continuar
     * donde estaba o empezar desde el principio.
     *
     * El mensaje describe qué estaba reproduciendo para que el usuario
     * pueda decidir con contexto.
     */
    private fun showResumeDialog(prefs: android.content.SharedPreferences) {
        val itemType     = prefs.getString(PREF_ITEM_TYPE, "bumper") ?: "bumper"
        val posMs        = prefs.getInt(PREF_POSITION_MS, 0)
        val progIdx      = prefs.getInt(PREF_PROGRAM_IDX, 0)
        val plIdx        = prefs.getInt(PREF_PLAYLIST_IDX, 0)
        val commMs       = prefs.getInt(PREF_COMMERCIAL_MS, 0)
        val screenbugRes = prefs.getInt(PREF_SCREENBUG_RES, R.drawable.screenbug)
        // BUG FIX 2001.2.5.1: leer los breaks pendientes serializados
        val breakQueueStr = prefs.getString(PREF_BREAK_QUEUE, "") ?: ""

        // Descripción legible de dónde se quedó
        val whereStr = when (itemType) {
            "program"    -> getString(R.string.resume_where_program, progIdx + 1, posMs / 60_000, (posMs % 60_000) / 1_000)
            "commercial" -> getString(R.string.resume_where_commercial, progIdx + 1)
            "bumper"     -> getString(R.string.resume_where_bumper)
            "enseguida"  -> getString(R.string.resume_where_enseguida)

            else         -> getString(R.string.resume_where_unknown)
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_resume_title))
            .setMessage(getString(R.string.dialog_resume_message, whereStr))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.dialog_resume_positive)) { _, _ ->
                // BUG FIX 1999.2.2.0.21: resetear pausedPositionMs ANTES de restaurar.
                pausedPositionMs = 0
                resumeSavedState(itemType, plIdx, progIdx, posMs, commMs, screenbugRes, breakQueueStr, prefs)
            }
            .setNegativeButton(getString(R.string.dialog_resume_negative)) { _, _ ->
                // BUG FIX 1999.2.2.0.21: mismo reset para "empezar de nuevo"
                pausedPositionMs = 0
                clearSavedState()
                playlistIndex = 0
                advance()
            }
            .show()
    }

    /**
     * Restaura el estado guardado según el tipo de ítem que se estaba reproduciendo.
     *
     * - program:    retoma el programa en la posición guardada.
     * - commercial: retoma el programa en la posición de reanudación post-comercial
     *               (se saltea el comercial, es imposible restaurar la mitad de un comercial).
     * - bumper:     reinicia el bumper desde el principio (son cortos, no vale seekar).
     * - enseguida:  reinicia el enseguida desde el principio (igual razonamiento).
     */
    private fun resumeSavedState(
        itemType: String,
        plIdx: Int,
        progIdx: Int,
        posMs: Int,
        commMs: Int,
        screenbugRes: Int,
        breakQueueStr: String,
        prefs: android.content.SharedPreferences
    ) {
        clearSavedState()
        playlistIndex       = plIdx
        currentProgramIndex = progIdx
        currentScreenBugRes = screenbugRes

        // BUG FIX 2001.2.5.1: deserializar el breakQueue guardado.
        // Esto es crítico para el caso "commercial": sin esta restauración,
        // beginProgramSegment con isFirstPlay=false arranca con breakQueue vacío
        // y no hay breaks programados. Con isFirstPlay=true recalcula desde cero
        // e inserta breaks ya consumidos. Restaurar breakQueue es la única solución correcta.
        val restoredBreaks = if (breakQueueStr.isNotBlank()) {
            breakQueueStr.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .toMutableList()
        } else {
            mutableListOf()
        }

        when (itemType) {
            "program" -> {
                val uri = resolveProgram(progIdx)
                if (uri != null) {
                    currentProgramUri = uri
                    breakQueue = restoredBreaks   // restaurar breaks pendientes
                    Log.d(TAG, "Restaurando programa en ${posMs}ms, breaks pendientes: $breakQueue")
                    beginProgramSegment(uri, startOffsetMs = posMs, isFirstPlay = false)
                } else {
                    Log.w(TAG, "Restauración: pro${progIdx+1}.mp4 no encontrado, avanzando")
                    playlistIndex = 0
                    advance()
                }
            }
            "commercial" -> {
                // BUG FIX 2001.2.5.1: al restaurar desde un comercial, se retoma el
                // programa en commMs (posición post-comercial) con los breaks pendientes
                // correctamente restaurados (isFirstPlay=false + breakQueue restaurado).
                // Antes: isFirstPlay=false con breakQueue vacío → sin breaks, o isFirstPlay=true
                // → recalculaba todos los breaks e insertaba uno ya consumido.
                val uri = resolveProgram(progIdx)
                if (uri != null) {
                    currentProgramUri = uri
                    breakQueue = restoredBreaks   // restaurar breaks DESPUÉS del comercial
                    Log.d(TAG, "Restaurando post-comercial en ${commMs}ms, breaks pendientes: $breakQueue")
                    beginProgramSegment(uri, startOffsetMs = commMs, isFirstPlay = false)
                } else {
                    playlistIndex = 0
                    advance()
                }
            }
            "bumper", "enseguida", "talla" -> {
                advance()
            }
            else -> {
                playlistIndex = 0
                advance()
            }
        }
    }

    /** Borra el estado guardado en SharedPreferences. */
    private fun clearSavedState() {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .remove(PREF_HAS_STATE)
            .apply()
        Log.d(TAG, "Estado guardado borrado")
    }

    /**
     * Muestra un diálogo de confirmación al intentar salir de la app.
     * Si el usuario confirma: guarda el estado actual y cierra la Activity.
     * Si cancela: la app sigue corriendo normalmente.
     *
     * El estado se guarda AQUÍ (no en onStop) para que el AlertDialog de
     * reanudación solo aparezca cuando el usuario explícitamente quiso salir,
     * no al cambiar de app temporalmente.
     *
     * Beta 2000.2.4.0.40:
     *   - Al mostrarse el diálogo se pausa el video y la música de fondo para
     *     que el usuario no pierda contenido mientras decide.
     *   - Si el usuario pulsa Cancelar, el video y la música se reanudan
     *     exactamente desde donde fueron pausados.
     */
    private fun showExitConfirmationDialog() {
        // Pausar contenido mientras el diálogo está visible
        if (isInProgramSegment) {
            stopPositionTracker()
            videoView.pause()
            bgPlayer?.pause()
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_exit_title))
            .setMessage(getString(R.string.dialog_exit_message))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.dialog_exit_save)) { _, _ ->
                // Guardar estado SOLO cuando el usuario confirma salir
                saveChannelState()
                finish()
            }
            .setNegativeButton(getString(R.string.dialog_exit_no_save)) { _, _ ->
                finish()
            }
            .setNeutralButton(getString(R.string.dialog_exit_cancel)) { _, _ ->
                // Reanudar contenido si el usuario cancela la salida
                if (isInProgramSegment) {
                    videoView.seekTo(pausedPositionMs)
                    videoView.start()
                    bgPlayer?.start()
                    startPositionTracker()
                    scheduleSegmentLogic(pausedPositionMs)
                    Log.d(TAG, "Exit cancelled – resuming from ${pausedPositionMs}ms")
                }
            }
            .show()
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Position tracker – guarda la posición del video cada 500 ms
    // Resuelve el bug donde videoView.currentPosition devuelve 0 en onPause
    // porque Android ya pausó el VideoView antes de llamar al callback.
    // ══════════════════════════════════════════════════════════════════════════

    /** Inicia el guardado continuo de posición. Llamar al arrancar un segmento de programa. */
    private fun startPositionTracker() {
        positionTrackerHandler.removeCallbacksAndMessages(null)
        positionTrackerHandler.post(positionTrackerRunnable)
        Log.d(TAG, "PositionTracker STARTED")
    }

    /** Detiene el guardado continuo de posición. Llamar en bumpers, comerciales y onPause. */
    private fun stopPositionTracker() {
        positionTrackerHandler.removeCallbacksAndMessages(null)
        Log.d(TAG, "PositionTracker STOPPED at ${pausedPositionMs}ms")
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ScreenBug animation
    // ══════════════════════════════════════════════════════════════════════════

    private fun fadeInBug() {
        Log.d(TAG, "ScreenBug FADE IN [res=$currentScreenBugRes]")
        // Aplica el drawable correspondiente a la talla reproducida antes del programa
        screenBug.setImageResource(currentScreenBugRes)
        screenBug.animate()
            .alpha(1f)
            .setDuration(FADE_MS)
            .start()
    }

    private fun fadeOutBug() {
        Log.d(TAG, "ScreenBug FADE OUT")
        screenBug.animate()
            .alpha(0f)
            .setDuration(FADE_MS)
            .start()
    }

    /** Instantly sets alpha without animation (used during transitions). */
    private fun setBugAlpha(alpha: Float) {
        screenBug.animate().cancel()
        screenBug.alpha = alpha
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Task scheduling helpers
    // ══════════════════════════════════════════════════════════════════════════

    private fun post(delayMs: Long, action: () -> Unit) {
        val r = Runnable(action)
        pendingTasks += r
        handler.postDelayed(r, delayMs)
    }

    private fun cancelAllTasks() {
        pendingTasks.forEach { handler.removeCallbacks(it) }
        pendingTasks.clear()
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Utility
    // ══════════════════════════════════════════════════════════════════════════

    private fun rawUri(resId: Int) = Uri.parse("android.resource://$packageName/$resId")

    @Suppress("DEPRECATION")
    private fun goFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // On Android 11+ use WindowInsetsController.
            // window.insetsController can still be null if the view isn't attached yet,
            // so we guard with ?.let. onWindowFocusChanged guarantees it's ready.
            val controller = window.decorView.windowInsetsController
                ?: window.insetsController
                ?: return
            controller.hide(
                android.view.WindowInsets.Type.statusBars() or
                android.view.WindowInsets.Type.navigationBars()
            )
            controller.systemBarsBehavior =
                android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }
    }

    //Modo Debug solo en beta
    private fun setupDebugInfo() {
        debugTextView = findViewById(R.id.debugInfo)
    
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
        val manufacturer = Build.MANUFACTURER
    
        // 3. Obtener el nombre de la API (Codename)
        // Nota: A partir de Android 10 ya no tienen nombres de dulces oficiales 
        // en el sistema, pero podemos mapear los más comunes.
        val apiName = when (sdkInt) {
            36 -> "BakLava"
            35 -> "Vanilla Ice Cream"
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

        startRamMonitor(versionName, versionCode, androidVersion, apiName, sdkInt, manufacturer, model)
    }
    
    private fun startRamMonitor(
        versionName: String?,
        versionCode: Long,
        androidVersion: String,
        apiName: String,
        sdkInt: Int,
        manufacturer: String,
        model: String
    ) {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
    
        val updateTask = object : Runnable {
            override fun run() {
                val memInfo = android.app.ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(memInfo)

                val totalRam = memInfo.totalMem
                val availableRam = memInfo.availMem

                val totalRamMB = totalRam / (1024 * 1024)
                val availableRamMB = availableRam / (1024 * 1024)

                val debugText = "beta $versionName, versionCode: $versionCode, Android $androidVersion $apiName\n" +
                "SDK: $sdkInt, $manufacturer $model, RAM Total: ${totalRamMB}MB, RAM Disponible: ${availableRamMB}MB, FPS: $currentFps"

                debugTextView.text = debugText

                debugHandler.postDelayed(this, 1000) // cada 1 segundo
            }
        }

        debugHandler.post(updateTask)
    }
    
    private fun displayInfo() {
        versionInfo = findViewById(R.id.versionInfo)
        
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val versionName = packageInfo.versionName
        val versionInfoText = "$versionName"
        
        versionInfo.text = versionInfoText
    }
}