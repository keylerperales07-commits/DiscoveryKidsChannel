/*
 ESTO TIENE LICENCIA GNU LICENSE APACHE
 
 NO ELIMINAR NINGUN COMENTARIO AGREGADO
*/
package com.keyler.discoverykidschannel

import android.app.ActivityManager
import android.content.ContentUris
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

/**
 * Discovery Kids Channel – TV Simulator
 * Release 1998.2.0.1 – basado en el año 1998
 *
 * Playlist sequence:
 *   bumper(aleatorio) → pro1 → enseguida(aleatorio) → bumper(aleatorio) → pro2 → enseguida(aleatorio) → ...  (then loops)
 *
 * Programs (pro1..pro4.mp4) are read from the user's Movies folder.
 * Bumpers (bumper.mp4, bumper2.mp4) son aleatorios, sin repetir el mismo dos veces seguidas.
 * Enseguidas (enseguida1-4.mp4) aparecen después de cada programa y antes del bumper, aleatorios sin repetir.
 * Commercials (comercial1.mp4, comercial2.mp4) are bundled in res/raw.
 * Ya_volvemos (ya_volvemos.mp4, ya_volvemos_animalesasombrosos.mp4) son aleatorios por corte.
 * ScreenBug (screenbug.png) is an overlay image bundled in res/drawable.
 *
 * Background: al salir al segundo plano el programa queda pausado y retoma exactamente donde quedó.
 * FPS: medidor de frames por segundo usando Choreographer (visible en debug overlay).
 *
 * Commercial scheduling:
 *   - 1 break per every 5 minutes of program content, equally spaced.
 *   - Programs shorter than 2 minutes have no commercials.
 *
 * ScreenBug timing (per segment = program portion between two commercial breaks):
 *   - Fades IN  20 s after segment begins (program start or commercial end).
 *   - Fades OUT 20 s before segment ends  (commercial start or program end).
 *   - Hidden entirely during bumpers and commercials.
 *
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

    // ── Background music (solo durante programas) ──────────────────────────────
    // MediaPlayer independiente del VideoView para poder pausar/reanudar
    // sin afectar la reproducción del video principal.
    private var bgPlayer: MediaPlayer? = null

    // ── Estado de segundo plano ────────────────────────────────────────────────
    // Cuando la app va a segundo plano durante un programa, se pausa el video
    // y la música, y se retoma exactamente donde quedó al volver al frente.
    private var pausedByLifecycle = false   // true si fuimos a background durante un programa
    private var pausedPositionMs  = 0       // posición del video al pausar
    // Flag que indica si el video principal de un programa está activo.
    // Se usa para distinguir si pausar al ir a background (no pausar en bumpers/comerciales).
    private var isInProgramSegment = false

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
    // Handler que guarda la posición del video cada 500 ms mientras hay un programa activo.
    // Soluciona el bug donde videoView.isPlaying ya es false en onPause y
    // videoView.currentPosition devuelve 0 o un valor incorrecto.
    private val positionTrackerHandler = Handler(Looper.getMainLooper())
    private val positionTrackerRunnable = object : Runnable {
        override fun run() {
            if (isInProgramSegment && videoView.isPlaying) {
                pausedPositionMs = videoView.currentPosition
            }
            // Se reprograma a sí mismo cada 500 ms; se cancela en stopPositionTracker()
            positionTrackerHandler.postDelayed(this, 500)
        }
    }

    // ── Playlist definition ────────────────────────────────────────────────────
    private sealed class PlayItem {
        object Bumper : PlayItem()
        object Enseguida : PlayItem()
        data class Program(val index: Int) : PlayItem()   // 0-based → pro(n+1).mp4
    }

    private val playlist = listOf(
        PlayItem.Enseguida,
        PlayItem.Bumper,
        PlayItem.Program(0),
        PlayItem.Enseguida,
        PlayItem.Bumper,
        PlayItem.Program(1),
        PlayItem.Enseguida,
        PlayItem.Bumper,
        PlayItem.Program(2),
        PlayItem.Enseguida,
        PlayItem.Bumper,
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
    // Último ya_volvemos reproducido; evita repetir el mismo en cortes consecutivos
    private var lastYaVolvemosRes: Int = -1
    // Último bumper reproducido; evita repetir el mismo dos veces seguidas
    private var lastBumperRes: Int = -1
    // Último enseguida reproducido; evita repetir el mismo dos veces seguidas
    private var lastEnseguidaRes: Int = -1

    // ── Constants ──────────────────────────────────────────────────────────────
    companion object {
        private const val TAG = "DKChannel"

        /** Screenbug appears this many ms after segment start or commercial end. */
        private const val BUG_SHOW_DELAY = 20_000L

        /** Screenbug hides this many ms before segment end or commercial start. */
        private const val BUG_HIDE_EARLY = 20_000L

        /** One commercial break is inserted for every this many ms of program. */
        private const val BREAK_INTERVAL_MS = 8 * 60 * 1_000L   // 8 min

        /** Programs shorter than this have zero commercial breaks. */
        private const val MIN_DURATION_FOR_BREAKS = 3 * 60 * 1_000L  // 3 min

        /** Alpha-animation duration for screenbug fade. */
        private const val FADE_MS = 1_000L

        private const val PERM_REQUEST = 42

        /** Lista de comerciales disponibles; se elige uno al azar en cada corte. */
        private val COMMERCIALS = listOf(R.raw.comercial1, R.raw.comercial2, R.raw.comercial3, R.raw.comercial4)

        /**
         * Lista de bumpers disponibles.
         * Se elige uno al azar antes de cada programa, evitando repetir el mismo dos veces seguidas.
         */
        private val BUMPERS = listOf(R.raw.bumper, R.raw.bumper2, R.raw.bumper3)

        /**
         * Lista de enseguidas disponibles.
         * Se reproducen después de cada programa y antes del bumper.
         * Se elige uno al azar evitando repetir el mismo dos veces seguidas.
         */
        private val ENSEGUIDAS = listOf(
            R.raw.enseguida1,
            R.raw.enseguida2,
            R.raw.enseguida3,
            R.raw.enseguida4
        )

        /**
         * Lista de ya_volvemos disponibles.
         * Se elige UNO al inicio de cada corte y se usa el MISMO para pre y post comercial,
         * evitando que el ya_volvemos cambie en medio de un mismo bloque publicitario.
         */
        private val YA_VOLVEMOS = listOf(R.raw.ya_volvemos, R.raw.ya_volvemos2)
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

        // Iniciar medidor de FPS (Choreographer se dispara en cada vsync del sistema)
        Choreographer.getInstance().postFrameCallback(fpsFrameCallback)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) goFullscreen()
    }

    /**
     * Al ir a segundo plano: si hay un programa activo, pausa el video y la
     * música y cancela los timers pendientes.
     *
     * BUG FIX 1998.2.0.1:
     *   - Se eliminó la condición "videoView.isPlaying" porque Android pausa
     *     el VideoView automáticamente ANTES de llamar onPause, haciendo que
     *     isPlaying siempre sea false y nunca se guardara la posición.
     *   - pausedPositionMs ya viene actualizado cada 500 ms por positionTrackerRunnable,
     *     así que no dependemos de currentPosition en el momento crítico.
     *   - Se detiene el tracker de posición para no seguir escribiendo mientras
     *     estamos en background.
     */
    override fun onPause() {
        super.onPause()
        stopPositionTracker()
        if (isInProgramSegment) {
            // pausedPositionMs ya tiene la última posición guardada por el tracker
            videoView.pause()
            bgPlayer?.pause()   // pausa sin liberar el player
            cancelAllTasks()    // cancela timers de screenbug y comerciales
            pausedByLifecycle = true
            Log.d(TAG, "Background – pausado en ${pausedPositionMs}ms")
        }
    }

    /**
     * Al volver al frente: si estábamos pausados por lifecycle, reanuda el
     * video y la música desde exactamente donde se pausó y reprograma los
     * timers del segmento actual.
     *
     * BUG FIX 1998.2.0.1:
     *   - Se agrega seekTo(pausedPositionMs) ANTES de start() para asegurar
     *     que el video retome la posición correcta. Sin seekTo, el video puede
     *     retomar desde el principio o desde donde el sistema lo dejó.
     *   - Se reinicia el tracker de posición al reanudar.
     */
    override fun onResume() {
        super.onResume()
        if (pausedByLifecycle) {
            pausedByLifecycle = false
            // seekTo garantiza que el video esté en la posición correcta antes de start()
            videoView.seekTo(pausedPositionMs)
            videoView.start()
            bgPlayer?.start()
            startPositionTracker()
            // Reprogramar screenbug y el próximo corte comercial desde la posición guardada
            scheduleSegmentLogic(pausedPositionMs)
            Log.d(TAG, "Foreground – reanudando desde ${pausedPositionMs}ms")
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
        playlistIndex = 0
        advance()
    }

    /** Move to the next playlist item (wraps around). */
    private fun advance() {
        if (playlistIndex >= playlist.size) playlistIndex = 0
        when (val item = playlist[playlistIndex]) {
            is PlayItem.Bumper    -> playBumper()
            is PlayItem.Enseguida -> playEnseguida()
            is PlayItem.Program   -> playProgram(item.index)
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
    // Enseguida playback – aparece después del programa y antes del bumper
    // ══════════════════════════════════════════════════════════════════════════

    private fun playEnseguida() {
        cancelAllTasks()
        setBugAlpha(0f)
        stopBgMusic()   // enseguida → sin música de fondo
        isInProgramSegment = false   // no pausar al ir a background en enseguida

        // Elige un enseguida al azar evitando repetir el anterior
        val candidates = ENSEGUIDAS.filter { it != lastEnseguidaRes }.ifEmpty { ENSEGUIDAS }
        val chosenEnseguida = candidates.random()
        lastEnseguidaRes = chosenEnseguida

        Log.d(TAG, "▶ ENSEGUIDA [res=$chosenEnseguida]")

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

        videoView.setVideoURI(uri)
        videoView.setOnPreparedListener { mp ->
            mp.isLooping = false

            programDuration = mp.duration

            if (isFirstPlay) {
                breakQueue = calcBreaks(programDuration).toMutableList()
            }

            if (startOffsetMs > 0) videoView.seekTo(startOffsetMs)

            scheduleSegmentLogic(startOffsetMs)
            videoView.start()
            isInProgramSegment = true   // programa activo → pausar al ir a background
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
    // ══════════════════════════════════════════════════════════════════════════

    private fun playCommercial(resumeProgramAtMs: Int) {
        cancelAllTasks()
        setBugAlpha(0f)
        stopBgMusic()   // comercial → sin música de fondo
        isInProgramSegment = false   // no pausar al ir a background en comercial
        stopPositionTracker()        // no hace falta tracker fuera del programa

        // Elige un comercial al azar evitando repetir el anterior
        val commercialCandidates = COMMERCIALS.filter { it != lastCommercialRes }
            .ifEmpty { COMMERCIALS }
        val chosenCommercial = commercialCandidates.random()
        lastCommercialRes = chosenCommercial

        // Elige un ya_volvemos al azar evitando repetir el anterior.
        // IMPORTANTE: se elige UNA sola vez aquí para que el pre y el post
        // sean el MISMO ya_volvemos dentro del mismo corte publicitario.
        val yaVolvemosCandidates = YA_VOLVEMOS.filter { it != lastYaVolvemosRes }
            .ifEmpty { YA_VOLVEMOS }
        val chosenYaVolvemos = yaVolvemosCandidates.random()
        lastYaVolvemosRes = chosenYaVolvemos

        Log.d(TAG, "▶ YA VOLVEMOS pre-commercial [res=$chosenYaVolvemos]")

        // Secuencia: ya_volvemos (elegido) → comercial (aleatorio) → mismo ya_volvemos → programa
        playUri(rawUri(chosenYaVolvemos)) {
            Log.d(TAG, "▶ COMMERCIAL [res=$chosenCommercial] (resumes program at ${resumeProgramAtMs}ms)")

            playUri(rawUri(chosenCommercial)) {
                Log.d(TAG, "▶ YA VOLVEMOS post-commercial [res=$chosenYaVolvemos] (mismo que el pre)")

                // Reutiliza el MISMO ya_volvemos del inicio del corte
                playUri(rawUri(chosenYaVolvemos)) {
                    // Después del segundo ya_volvemos → retomar programa
                    // scheduleSegmentLogic se encargará del delay de 20 s del ScreenBug
                    val uri = currentProgramUri ?: run {
                        Log.e(TAG, "No currentProgramUri – advancing")
                        playlistIndex++
                        advance()
                        return@playUri
                    }
                    Log.d(TAG, "Ya volvemos done – resuming program at ${resumeProgramAtMs}ms")
                    beginProgramSegment(uri, startOffsetMs = resumeProgramAtMs, isFirstPlay = false)
                }
            }
        }
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
                setVolume(0.1f, 0.1f)  // volumen al 100% para no tapar el audio del video
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
        Log.d(TAG, "ScreenBug FADE IN")
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