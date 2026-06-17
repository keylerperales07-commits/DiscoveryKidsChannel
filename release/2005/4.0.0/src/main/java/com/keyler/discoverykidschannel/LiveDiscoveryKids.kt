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
 * Beta 2005.4.0.0.4
 *
 * Discovery Kids - TV Simulator • Era 2005
 *
 * Playlist sequence (all transitions: FadeOut 500 ms / FadeIn 1 s):
 *   Enseguida(1–4) → StandaloneCommercial → Bumper → Programa → Enseguida(1–4) → StandaloneCommercial → Bumper → Programa → ...
 *
 * ya_regresa assignment: un "shuffled pool" de 4 slots se rellena antes de cada ciclo de 4 programas.
 *   Cada programa consume yaRegresaPool[yaRegresaPoolIndex++]. Al agotar los 4 slots el pool
 *   se regenera con un nuevo shuffle, garantizando que en ningún ciclo se repita el mismo ya_regresa.
 *
 * Programs (pro1..pro4.mp4) are read from the user's Movies folder.
 * Bumpers (bumper.mp4–bumper5.mp4) son aleatorios, sin repetir el mismo dos veces seguidas.
 * Commercial scheduling: 1 break per every 3–9 minutes of program content, at random intervals.
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

    // ── Estado de pausa (Beta 3.4.0.40) ────────────────────────────────────────
    //
    // ESTRATEGIA DIFERENCIADA POR TIPO DE ÍTEM:
    //
    //   PROGRAMA → pausa real con seekTo en onPrepared al volver.
    //     pausedPositionMs guarda la posición cada 16 ms via tracker.
    //     onResume() llama beginProgramSegment(uri, pausedPositionMs, isFirstPlay=false)
    //     que internamente hace setVideoURI + seekTo dentro de onPrepared,
    //     garantizando que el seek ocurra cuando el MediaPlayer está listo.
    //
    //   TODO LO DEMÁS (bumper, enseguida, comercial, ya_regresa, continuamos) →
    //     al volver se reinicia el ítem desde el principio llamando advance().
    //     Son clips cortos (< 30 s); no vale la pena seek y además
    //     cancelAllTasks() en onPause() destruye los listeners de playUriWithTransition,
    //     haciendo imposible reanudar a mitad sin reconfigurarlos.
    //
    // POR QUÉ NO SE USA seekTo() DIRECTO EN onResume():
    //   Android puede haber liberado el surface del VideoView mientras estaba
    //   en segundo plano. En ese estado seekTo() se ignora silenciosamente.
    //   La única forma segura es setVideoURI() + seekTo() dentro de onPrepared().

    private var pausedPositionMs      = 0       // posición del programa guardada por el tracker
    private var pausedByLifecycle     = false   // true si onPause() pausó la app
    private var currentSegmentStartMs = 0       // posición del programa donde arrancó el segmento activo (para calcular elapsed en screenbug)

    // ── Flags de estado ────────────────────────────────────────────────────────
    private var isInProgramSegment    = false
    private var isInCommercialBlock   = false
    private var commercialResumeMs    = 0

    // ── Tipo de ítem actual ────────────────────────────────────────────────────
    // Valores: "program", "bumper", "enseguida", "talla", "commercial"
    private var currentItemType: String = "bumper"

    // ── FPS (frames por segundo) ───────────────────────────────────────────────
    // Medido con Choreographer.FrameCallback que se dispara en cada vsync.
    // currentFps se actualiza cada segundo y se muestra en el debug overlay.
    private var fpsFrameCount   = 0
    private var fpsLastTimeNs   = 0L
    private var currentFps      = 0
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
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    // ── Scheduling ─────────────────────────────────────────────────────────────
    private val handler = Handler(Looper.getMainLooper())
    private val pendingTasks = mutableListOf<Runnable>()
    private val debugHandler = Handler(Looper.getMainLooper())
    private val navHideHandler = Handler(Looper.getMainLooper())
    private val positionTrackerHandler = Handler(Looper.getMainLooper())
    private val positionTrackerRunnable = object : Runnable {
        override fun run() {
            if (videoView.isPlaying) {
                pausedPositionMs = videoView.currentPosition
            }
            positionTrackerHandler.postDelayed(this, 16)
        }
    }

    // ── Playlist definition ────────────────────────────────────────────────────
    private sealed class PlayItem {
        object Bumper : PlayItem()
        object Enseguida : PlayItem()
        object StandaloneCommercial : PlayItem()
        data class Program(val index: Int) : PlayItem()   // 0-based → pro(n+1).mp4
    }

    private val playlist = listOf(
        PlayItem.Enseguida,
        PlayItem.Bumper,
        PlayItem.StandaloneCommercial,
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
    private var lastCommercialRes: Int = -1
    private var lastBumperRes: Int = -1
    private var lastEnseguidaPostProgramaRes: Int = -1
    // ya_regresa shuffled pool: ciclo de 4 sin repetición; se regenera al agotarse.
    private var yaRegresaPool: MutableList<Int> = mutableListOf()
    private var yaRegresaPoolIndex: Int = 4   // inicia en 4 → fuerza generación en el primer uso
    private var lastEnseguidaPreComercialRes: Int = -1
    private var currentScreenBugRes: Int = R.drawable.screenbug

    // ── Constants ──────────────────────────────────────────────────────────────
    companion object {
        private const val TAG = "DKids"

        /** Screenbug appears this many ms after segment start or commercial end. */
        private const val BUG_SHOW_DELAY = 20_000L

        /** Screenbug hides this many ms before segment end or commercial start. */
        private const val BUG_HIDE_EARLY = 20_000L

        /** One commercial break is inserted at a random interval between these two values. */
        private const val BREAK_INTERVAL_MIN_MS = 3 * 60 * 1_000L   // 3 min
        private const val BREAK_INTERVAL_MAX_MS = 9 * 60 * 1_000L   // 9 min

        /** Programs shorter than this have zero commercial breaks. */
        private const val MIN_DURATION_FOR_BREAKS = 3 * 60 * 1_000L  // 3 min

        /** Alpha-animation duration for screenbug fade. */
        private const val FADE_MS = 500L

        /** FadeOut duration for video transitions (ms). Applied before every video change. Release 3.3.0: unified to 500 ms for all clip types. */
        private const val TRANSITION_FADE_OUT_MS = 500L

        /** FadeIn duration for video transitions (ms). Applied when the new video starts. */
        private const val TRANSITION_FADE_IN_MS = 500L

        private const val PERM_REQUEST = 42

        // ── SharedPreferences – persistencia de sesión al cerrar la app ─────────
        private const val PREFS_NAME         = "dk_channel_state"
        private const val PREF_HAS_STATE     = "has_saved_state"
        private const val PREF_PLAYLIST_IDX  = "playlist_index"
        private const val PREF_POSITION_MS   = "position_ms"
        private const val PREF_PROGRAM_IDX   = "program_index"
        private const val PREF_ITEM_TYPE     = "item_type"       // "program"|"bumper"|"enseguida"|"talla"|"commercial"
        private const val PREF_COMMERCIAL_MS = "commercial_resume_ms"
        private const val PREF_SCREENBUG_RES = "screenbug_res"
        private const val PREF_BREAK_QUEUE   = "break_queue"

        /** Lista de comerciales disponibles; se elige uno al azar en cada corte. */
        private val COMMERCIALS = listOf(R.raw.comercial1, R.raw.comercial2, R.raw.comercial3, R.raw.comercial4)

        /**
         * Lista de bumpers disponibles.
         * Se elige uno al azar antes de cada programa, evitando repetir el mismo dos veces seguidas.
         * Beta 2005.4.0.0.4: bumper6 reemplazado por nuevo bumper de aviso de la Era Doki
         * (Actualización La Era Doki / nuevo Discovery Kids).
         */
        private val BUMPERS = listOf(
            R.raw.bumper,
            R.raw.bumper2,
            R.raw.bumper3,
            R.raw.bumper4,
            R.raw.bumper5,
            R.raw.bumper6,
            R.raw.bumper7,
            R.raw.bumper8,
            R.raw.bumper9,
            R.raw.bumper10,
            R.raw.bumper11,
            R.raw.bumper12,
            R.raw.bumper13
        )

        /**
         * Enseguidas post-programa (van entre el fin del programa y el comercial standalone).
         * Beta 3.0.0.3: selección aleatoria con anti-repetición.
         * Se eliminó enseguida5 y la selección por horario.
         * enseguida1 y enseguida2 actualizados a la Era 2002.
         */
        private val ENSEGUIDAS_POST_PROGRAMA = listOf(
            R.raw.enseguida1,
            R.raw.enseguida2
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
            R.raw.ya_regresa3,
            R.raw.ya_regresa4
        )

        /**
         * Mapeo: enseguida pre-comercial → continuamos que se debe usar en ese corte.
         * ya_regresa1 → continuamos1 | ya_regresa2 → continuamos2
         */
        private val ENSEGUIDA_YA_VOLVEMOS_MAP = mapOf(
            R.raw.ya_regresa1 to R.raw.continuamos1,
            R.raw.ya_regresa2 to R.raw.continuamos2,
            R.raw.ya_regresa3 to R.raw.continuamos3,
            R.raw.ya_regresa4 to R.raw.continuamos4
        )
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ══════════════════════════════════════════════════════════════════════════

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        crtOverlay = findViewById(R.id.crtOverlay)

        // Botones ocultos al inicio; aparecen al tocar la pantalla
        prevButton.visibility = View.GONE
        nextButton.visibility = View.GONE

        prevButton.setOnClickListener {
            resetNavHideTimer()
            goToAdjacentProgram(-1)
        }
        nextButton.setOnClickListener {
            resetNavHideTimer()
            goToAdjacentProgram(+1)
        }

        requestStoragePermission()
        
        setupDebugInfo()
        displayInfo()

        Choreographer.getInstance().postFrameCallback(fpsFrameCallback)

        startPositionTracker()

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
     * Beta 3.4.0.40 — PAUSA DIFERENCIADA.
     *
     * Pausa el video y la música inmediatamente al salir de la app.
     * pausedByLifecycle = true señala a onResume() que debe reanudar.
     * cancelAllTasks() cancela timers de screenbug y comerciales; se reconfiguran
     * en beginProgramSegment() al volver si era un programa.
     */
    override fun onPause() {
        super.onPause()
        pausedByLifecycle = true
        stopPositionTracker()
        videoView.pause()
        bgPlayer?.pause()
        cancelAllTasks()
        Log.d(TAG, "onPause – tipo=$currentItemType pos=${pausedPositionMs}ms")
    }

    /**
     * Beta 3.4.0.40 — REANUDACIÓN DIFERENCIADA.
     *
     * PROGRAMA: llama beginProgramSegment() con la posición guardada.
     *   beginProgramSegment hace setVideoURI + seekTo DENTRO de onPrepared,
     *   que es el único momento en que Android garantiza que el seek funciona
     *   (el surface pudo haberse liberado en segundo plano).
     *
     * TODO LO DEMÁS: llama advance() para reiniciar el ítem desde el principio.
     *   playUriWithTransition() reconfigura sus propios listeners desde cero,
     *   por lo que no hay riesgo de listeners huérfanos.
     *
     * COMERCIAL EN CURSO: si nos fuimos en mitad de un bloque comercial,
     *   retomamos el programa en commercialResumeMs (saltamos el comercial,
     *   igual que hace la restauración de sesión).
     */
    override fun onResume() {
        super.onResume()
        if (!pausedByLifecycle) return
        pausedByLifecycle = false

        when {
            isInProgramSegment -> {
                val uri = currentProgramUri ?: run {
                    Log.e(TAG, "onResume: isInProgramSegment pero no hay URI – advance()")
                    advance(); return
                }
                Log.d(TAG, "onResume – reanudando programa desde ${pausedPositionMs}ms")
                beginProgramSegment(uri, startOffsetMs = pausedPositionMs, isFirstPlay = false)
            }
            isInCommercialBlock -> {
                val uri = currentProgramUri ?: run {
                    Log.e(TAG, "onResume: isInCommercialBlock pero no hay URI – advance()")
                    advance(); return
                }
                Log.d(TAG, "onResume – bloque comercial interrumpido, retomando programa en ${commercialResumeMs}ms")
                isInCommercialBlock = false
                beginProgramSegment(uri, startOffsetMs = commercialResumeMs, isFirstPlay = false)
            }
            else -> {
                Log.d(TAG, "onResume – tipo=$currentItemType (no-programa) → reiniciando ítem")
                advance()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelAllTasks()
        navHideHandler.removeCallbacksAndMessages(null)
        positionTrackerHandler.removeCallbacksAndMessages(null)
        videoView.stopPlayback()
        stopBgMusic()
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
        stopBgMusic()
        isInProgramSegment = false
        currentItemType = "bumper"

        val candidates = BUMPERS.filter { it != lastBumperRes }.ifEmpty { BUMPERS }
        val chosenBumper = candidates.random()
        lastBumperRes = chosenBumper

        Log.d(TAG, "▶ BUMPER [res=$chosenBumper]")

        playUriWithTransition(rawUri(chosenBumper)) {
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

        val candidates = COMMERCIALS.filter { it != lastCommercialRes }.ifEmpty { COMMERCIALS }
        val chosenCommercial = candidates.random()
        lastCommercialRes  = chosenCommercial

        Log.d(TAG, "▶ STANDALONE COMMERCIAL [res=$chosenCommercial]")

        playUriWithTransition(rawUri(chosenCommercial)) {
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

        val candidates = ENSEGUIDAS_POST_PROGRAMA
            .filter { it != lastEnseguidaPostProgramaRes }
            .ifEmpty { ENSEGUIDAS_POST_PROGRAMA }
        val chosenEnseguida = candidates.random()
        lastEnseguidaPostProgramaRes = chosenEnseguida

        Log.d(TAG, "▶ ENSEGUIDA post-programa [res=$chosenEnseguida]")

        playUriWithTransition(rawUri(chosenEnseguida)) {
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

        // Beta 3.4.0.42 — BUG FIX: cancelar cualquier animación pendiente del videoView
        // y forzar alpha=0f ANTES de setVideoURI() para garantizar estado limpio
        // independientemente de animaciones previas interrumpidas (ej: fadeOut a medias
        // al salir al segundo plano).
        videoView.animate().cancel()
        videoView.alpha = 0f

        videoView.setVideoURI(uri)
        videoView.setOnPreparedListener { mp ->
            mp.isLooping = false

            programDuration = mp.duration

            if (isFirstPlay) {
                breakQueue = calcBreaks(programDuration).toMutableList()
            }

            if (startOffsetMs > 0) videoView.seekTo(startOffsetMs)

            scheduleSegmentLogic(startOffsetMs)
            videoView.alpha = 0f
            videoView.start()
            videoView.animate()
                .alpha(1f)
                .setDuration(TRANSITION_FADE_IN_MS)
                .start()
            isInProgramSegment = true
            currentItemType = "program"
            startPositionTracker()
            startBgMusic()
        }
        videoView.setOnCompletionListener {
            Log.d(TAG, "Program ended")
            cancelAllTasks()
            setBugAlpha(0f)
            isInProgramSegment = false
            stopPositionTracker()
            pausedPositionMs = 0
            stopBgMusic()
            playlistIndex++
            advance()
        }
    }

    /**
     * Schedules screenbug show/hide and the next commercial break
     * for the current segment starting at [segmentStartMs] in program time.
     *
     * Beta 3.4.0.42 — BUG FIX: ajusta los delays del screenbug descontando el
     * tiempo ya transcurrido en el segmento antes de pausar.
     * - elapsed = segmentStartMs - currentSegmentStartMs (ms ya vividos en este segmento)
     * - Si elapsed >= BUG_SHOW_DELAY → screenbug debe estar visible ya; aparece
     *   inmediatamente con setBugAlpha(1f) y se programa solo el fadeOut.
     * - Si elapsed < BUG_SHOW_DELAY → se programa fadeIn con delay reducido.
     * Esto evita que al volver de segundo plano el screenbug reinicie su cuenta
     * de 20 s desde cero aunque ya debía estar visible.
     */
    private fun scheduleSegmentLogic(segmentStartMs: Int) {
        // Guarda dónde arranca este segmento para calcular elapsed en llamadas futuras
        currentSegmentStartMs = segmentStartMs

        // Determine end of this segment (next break or program end)
        val segmentEndMs = if (breakQueue.isNotEmpty()) breakQueue[0] else programDuration
        val segmentDuration = (segmentEndMs - segmentStartMs).toLong().coerceAtLeast(0)

        Log.d(TAG, "Segment: ${segmentStartMs}ms → ${segmentEndMs}ms (${segmentDuration}ms)")

        // Calcula cuántos ms del segmento ya transcurrieron antes de este (re)arranque
        // En la primera llamada de un segmento nuevo elapsed = 0.
        // Al reanudar desde segundo plano elapsed = pausedPositionMs - currentSegmentStartMs.
        val elapsed = (segmentStartMs - currentSegmentStartMs).toLong().coerceAtLeast(0L)

        val bugShowDelay = (BUG_SHOW_DELAY - elapsed).coerceAtLeast(0L)

        if (elapsed >= BUG_SHOW_DELAY) {
            // El screenbug ya debía estar visible — aparece inmediatamente sin animación
            Log.d(TAG, "ScreenBug: elapsed=${elapsed}ms >= BUG_SHOW_DELAY → aparece inmediatamente")
            setBugAlpha(1f)
        } else if (segmentDuration > bugShowDelay) {
            post(bugShowDelay) { fadeInBug() }
        }

        val hideAt = segmentDuration - BUG_HIDE_EARLY
        if (hideAt > bugShowDelay) {
            post(hideAt) { fadeOutBug() }
        }

        if (breakQueue.isNotEmpty()) {
            val breakProgramPos = breakQueue[0]
            post(segmentDuration) {
                val resumePos = breakProgramPos
                breakQueue.removeAt(0)
                playCommercial(resumePos)
            }
        }
    }

    /**
     * Release 3.4.1 — Prev / Next saltan directo al programa destino.
     *
     * Problema del enfoque anterior (iniciar desde Enseguida):
     *   playEnseguida() → playUriWithTransition() registra el timer del FadeOut en pendingTasks.
     *   Cuando la enseguida termina, su onComplete llama playBumper() → cancelAllTasks(),
     *   que borra el timer del FadeOut del bumper antes de que corra. Además,
     *   encadenar playUriWithTransition() dentro del onComplete de otro cancela la
     *   animación del segundo via ViewPropertyAnimator (instancia única del videoView),
     *   por lo que el withEndAction del FadeOut inicial nunca se ejecuta y el bumper
     *   nunca arranca.
     *
     * Solución: Prev / Next se comportan como un cambio de canal — van directo al
     * programa sin pasar por Enseguida → StandaloneCommercial → Bumper. Ese bloque
     * ya ocurre naturalmente cuando el programa termine por su propio onCompletionListener.
     * playlistIndex se fija en el PlayItem.Program para que advance() continúe
     * correctamente desde la Enseguida del siguiente ciclo al terminar el programa.
     */
    private fun goToAdjacentProgram(direction: Int) {
        val target = findAvailableProgramIndex(currentProgramIndex, direction) ?: return

        if (target == currentProgramIndex) return

        Log.d(TAG, "▶ Navegando directo al programa ${target + 1} (direction=$direction)")
        cancelAllTasks()
        setBugAlpha(0f)
        stopPositionTracker()
        stopBgMusic()
        isInProgramSegment = false
        videoView.stopPlayback()

        // Busca el índice del PlayItem.Program destino en el playlist y fija playlistIndex ahí.
        // El programa terminará normalmente y su onCompletionListener hará playlistIndex++ + advance(),
        // arrancando la Enseguida del siguiente bloque sin ningún conflicto de ViewPropertyAnimator.
        val programIdx = playlist.indexOfFirst { it is PlayItem.Program && it.index == target }
            .takeIf { it >= 0 } ?: 0

        playlistIndex = programIdx
        currentProgramIndex = target

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
        stopBgMusic()
        isInProgramSegment = false
        isInCommercialBlock = true
        commercialResumeMs = resumeProgramAtMs
        startPositionTracker()
        currentItemType = "commercial"

        val commercialCandidates = COMMERCIALS.filter { it != lastCommercialRes }
            .ifEmpty { COMMERCIALS }
        val chosenCommercial = commercialCandidates.random()
        lastCommercialRes = chosenCommercial

        if (yaRegresaPoolIndex >= yaRegresaPool.size) {
            yaRegresaPool = ENSEGUIDAS_PRE_COMERCIAL.shuffled().toMutableList()
            yaRegresaPoolIndex = 0
            Log.d(TAG, "ya_regresa pool regenerado: $yaRegresaPool")
        }
        val chosenPreComercial = yaRegresaPool[yaRegresaPoolIndex++]
        lastEnseguidaPreComercialRes = chosenPreComercial

        val chosenYaVolvemos = ENSEGUIDA_YA_VOLVEMOS_MAP[chosenPreComercial]
            ?: R.raw.continuamos1   // fallback defensivo

        Log.d(TAG, "▶ ENSEGUIDA pre-comercial [res=$chosenPreComercial] → continuamos [res=$chosenYaVolvemos]")

        videoView.animate()
            .alpha(0f)
            .setDuration(TRANSITION_FADE_OUT_MS)
            .withEndAction {
                // Paso 1: ya_regresa (pre-comercial) — FadeIn 1 s
                videoView.alpha = 0f
                videoView.setOnPreparedListener { mp ->
                    mp.isLooping = false
                    videoView.start()
                    videoView.animate().alpha(1f).setDuration(TRANSITION_FADE_IN_MS).start()
                }
                videoView.setOnCompletionListener {
                    Log.d(TAG, "▶ COMMERCIAL [res=$chosenCommercial] (resumes program at ${resumeProgramAtMs}ms)")

                    // Paso 2: comercial — FadeOut 500 ms (TRANSITION_FADE_OUT_MS)
                    playUriWithTransition(rawUri(chosenCommercial)) {
                        Log.d(TAG, "▶ YA VOLVEMOS post-comercial [res=$chosenYaVolvemos]")

                        // Paso 3: continuamos (FadeOut 500 ms / FadeIn 1 s)
                        playUriWithTransition(rawUri(chosenYaVolvemos)) {
                            val uri = currentProgramUri ?: run {
                                Log.e(TAG, "No currentProgramUri – advancing")
                                playlistIndex++
                                advance()
                                return@playUriWithTransition
                            }
                            Log.d(TAG, "Ya volvemos done – resuming program at ${resumeProgramAtMs}ms")
                            isInCommercialBlock = false
                            beginProgramSegment(uri, startOffsetMs = resumeProgramAtMs, isFirstPlay = false)
                        }
                    }
                }
                videoView.setVideoURI(rawUri(chosenPreComercial))
                videoView.requestFocus()
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

    /**
     * Beta 2003.3.2.0.21 — BUG FIX: FadeOut 2 s ANTES del fin del video.
     * Beta 2003.3.2.0.22 — Parámetro [fadeOutMs] para FadeOut diferenciado por tipo de clip.
     *
     * Versión anterior (3.2.0.20): el FadeOut se disparaba DESPUÉS de que el video
     * ya había terminado (en onCompletion), causando un corte abrupto visible.
     *
     * Nueva estrategia (3.2.0.21+):
     *   1. Al prepararse el video (onPrepared) se calcula el retardo del FadeOut:
     *      max(0, duration - fadeOutMs). Esto dispara la animación [fadeOutMs] ms
     *      antes del fin real del clip.
     *   2. El FadeOut cancela el onCompletionListener activo y al terminar la
     *      animación ejecuta [onComplete] directamente, iniciando el siguiente clip.
     *   3. onCompletionListener actúa solo como FALLBACK para clips más cortos
     *      que [fadeOutMs] (el handler ya habrá ejecutado onComplete
     *      antes o al mismo tiempo, la segunda llamada se ignora con la guardia
     *      [transitionCompleted]).
     *
     * Release 3.3.0: todos los FadeOut unificados a 500 ms (TRANSITION_FADE_OUT_MS).
     * El parámetro [fadeOutMs] se conserva para posibles ajustes futuros; todos los
     * callers actuales usan el valor por defecto.
     * El FadeIn del clip entrante siempre usa TRANSITION_FADE_IN_MS (1 s).
     *
     * Se usa en TODOS los cambios de video del canal:
     *   enseguida, bumper, StandaloneCommercial, ya_regresa, comercial,
     *   continuamos y retoma de programa tras un corte comercial.
     *
     * playUri() se conserva sin modificar para las rutas internas que no
     * necesitan transición (ej.: beginProgramSegment maneja su propio FadeIn).
     */
    private fun playUriWithTransition(
        uri: Uri,
        fadeOutMs: Long = TRANSITION_FADE_OUT_MS,
        onComplete: () -> Unit
    ) {
        // FadeOut de cierre del clip anterior — 500 ms (TRANSITION_FADE_OUT_MS) uniforme para todos los clips
        videoView.animate()
            .alpha(0f)
            .setDuration(fadeOutMs)
            .withEndAction {
                var transitionCompleted = false   // guardia anti-doble disparo

                videoView.setOnPreparedListener { mp ->
                    mp.isLooping = false
                    videoView.alpha = 0f
                    videoView.start()
                    videoView.animate()
                        .alpha(1f)
                        .setDuration(TRANSITION_FADE_IN_MS)
                        .start()

                    val duration = mp.duration
                    val fadeOutDelay = (duration - fadeOutMs).coerceAtLeast(0L)
                    post(fadeOutDelay) {
                        if (!transitionCompleted) {
                            transitionCompleted = true
                            videoView.setOnCompletionListener(null)
                            videoView.animate()
                                .alpha(0f)
                                .setDuration(fadeOutMs)
                                .withEndAction { onComplete() }
                                .start()
                        }
                    }
                }

                videoView.setOnCompletionListener {
                    if (!transitionCompleted) {
                        transitionCompleted = true
                        onComplete()
                    }
                }

                videoView.setVideoURI(uri)
                videoView.requestFocus()
            }
            .start()
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Commercial break calculation
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Returns a list of program positions (in ms) where commercial breaks occur.
     *
     * Beta 2005.4.0.0.4 — Distribución de intervalo aleatorio:
     *   Los breaks se colocan en posiciones acumuladas usando un intervalo aleatorio
     *   entre [BREAK_INTERVAL_MIN_MS] (3 min) y [BREAK_INTERVAL_MAX_MS] (9 min).
     *   Cada corte elige su propio intervalo independientemente, generando una
     *   programación publicitaria variable más parecida a la TV real.
     *
     * Distribución anterior (≤3.4.1): intervalo fijo de 9 minutos exactos —
     *   los breaks siempre ocurrían a los 9 min, 18 min, 27 min, etc.
     *
     * Programs shorter than [MIN_DURATION_FOR_BREAKS] get no breaks.
     */
    private fun calcBreaks(durationMs: Int): List<Int> {
        if (durationMs < MIN_DURATION_FOR_BREAKS) return emptyList()
        val breaks = mutableListOf<Int>()
        var breakPos = (BREAK_INTERVAL_MIN_MS + (Math.random() * (BREAK_INTERVAL_MAX_MS - BREAK_INTERVAL_MIN_MS)).toLong())
        while (breakPos < durationMs) {
            breaks.add(breakPos.toInt())
            breakPos += (BREAK_INTERVAL_MIN_MS + (Math.random() * (BREAK_INTERVAL_MAX_MS - BREAK_INTERVAL_MIN_MS)).toLong())
        }
        return breaks
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
            bgPlayer = MediaPlayer.create(this, R.raw.bg_music)?.apply {
                isLooping = false
                setVolume(0.08f, 0.08f)
                setOnCompletionListener { mp ->
                    mp.seekTo(0)
                    mp.start()
                    Log.d(TAG, "BG Music LOOP (gapless restart)")
                }
                start()
                Log.d(TAG, "BG Music STARTED")
            }
        } else if (bgPlayer?.isPlaying == false) {
            bgPlayer?.start()
            Log.d(TAG, "BG Music RESUMED")
        }
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
        val posToSave = when {
            isInCommercialBlock -> commercialResumeMs
            else                -> pausedPositionMs
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
        val breakQueueStr = prefs.getString(PREF_BREAK_QUEUE, "") ?: ""

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
                pausedPositionMs = 0
                resumeSavedState(itemType, plIdx, progIdx, posMs, commMs, screenbugRes, breakQueueStr, prefs)
            }
            .setNegativeButton(getString(R.string.dialog_resume_negative)) { _, _ ->
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
                    breakQueue = restoredBreaks
                    Log.d(TAG, "Restaurando programa en ${posMs}ms, breaks pendientes: $breakQueue")
                    beginProgramSegment(uri, startOffsetMs = posMs, isFirstPlay = false)
                } else {
                    Log.w(TAG, "Restauración: pro${progIdx+1}.mp4 no encontrado, avanzando")
                    playlistIndex = 0
                    advance()
                }
            }
            "commercial" -> {
                val uri = resolveProgram(progIdx)
                if (uri != null) {
                    currentProgramUri = uri
                    breakQueue = restoredBreaks
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
                saveChannelState()
                finish()
            }
            .setNegativeButton(getString(R.string.dialog_exit_no_save)) { _, _ ->
                finish()
            }
            .setNeutralButton(getString(R.string.dialog_exit_cancel)) { _, _ ->
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
    
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val versionName = packageInfo.versionName
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            packageInfo.versionCode.toLong()
        }

        val androidVersion = Build.VERSION.RELEASE
        val sdkInt = Build.VERSION.SDK_INT
        val model = Build.MODEL
        val manufacturer = Build.MANUFACTURER
    
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

                val debugText = "Preview $versionName, versionCode: $versionCode, Android $androidVersion $apiName\n" +
                "SDK: $sdkInt, $manufacturer $model, RAM Total: ${totalRamMB}MB, RAM Disponible: ${availableRamMB}MB, FPS: $currentFps"

                debugTextView.text = debugText

                debugHandler.postDelayed(this, 1000)
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