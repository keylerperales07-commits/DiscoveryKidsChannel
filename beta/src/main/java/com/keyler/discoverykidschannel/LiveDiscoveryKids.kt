/*
 ESTO TIENE LICENCIA GNU LICENSE APACHE
 
 NO ELIMINAR NINGUN COMENTARIO AGREGADO
*/
package com.keyler.discoverykidschannel

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

/**
 * Preview 2006.4.1.0.21
 *
 * Discovery Kids - TV Simulator • Era Doki 1.0 (2005–2009) • Preview Era 2006
 *
 * Playlist sequence (all transitions: FadeOut 500 ms / FadeIn 1 s):
 *   Enseguida(1–4) → StandaloneCommercial → Bumper → Programa → Enseguida(1–4) → StandaloneCommercial → Bumper → Programa → ...
 *
 * ya_regresa assignment: determinístico por índice de programa (0-based).
 *   programa 0 (pro1) → ya_regresa1/continuamos1
 *   programa 1 (pro2) → ya_regresa2/continuamos2
 *   programa 2 (pro3) → ya_regresa3/continuamos3
 *   programa 3 (pro4) → ya_regresa4/continuamos4
 *
 * Programs (pro1..pro4.mp4) are read from the user's Movies folder.
 * Bumpers (bumper.mp4–bumper5.mp4) son aleatorios, sin repetir el mismo dos veces seguidas.
 * StandaloneCommercial: 4 comerciales (comercial1–4.mp4), aleatorios sin repetir el mismo
 *   dos veces seguidas. comercial1/comercial2 = Era 2006 (Preview 4.1.0.10);
 *   comercial3/comercial4 agregados en esta Preview (4.1.0.11).
 * Commercial scheduling: 1 break per every 3–9 minutes of program content, at random intervals.
 * Missing programs are skipped automatically.
 *
 * Configuración (Preview 4.1.0.12): SettingsActivity, accesible desde el botón ⚙️,
 * permite alternar música de fondo, efecto CRT, Forzar 4:3, y ajustar la duración
 * del Screenbug y el intervalo aleatorio de comerciales. Ver SettingsManager.
 * (El modo debug ya no es configurable: vuelve a ser automático en builds Preview).
 *
 * Release 2006.4.1.1 — BUG FIX: el Screenbug y los clips no-programa (bumper,
 * enseguida, comercial) se reiniciaban al volver de segundo plano o de un
 * cambio de Activity. Ver scheduleSegmentLogic(), resumeUriWithSeek() y
 * resumeCommercialBlock() para el detalle de la corrección.
 *
 * Preview 4.1.0.21 — REORGANIZACIÓN DE CÓDIGO (10 semanas desde el primer
 * release). Esta clase ahora solo contiene: propiedades de instancia, el
 * companion object (constantes y listas de recursos), y los métodos de
 * ciclo de vida de Activity (onCreate/onPause/onResume/onStop/onDestroy,
 * dispatchTouchEvent, onRequestPermissionsResult). El resto del flujo del
 * canal vive en funciones de extensión de esta misma clase, agrupadas por
 * responsabilidad en archivos separados — ver el bloque de comentarios
 * más abajo (antes de ChannelDebugOverlay) para el mapa completo de qué
 * vive en cada archivo. Es un cambio puramente organizativo: cero cambios
 * de comportamiento respecto a la 4.1.0.20.
 */
class LiveDiscoveryKids : AppCompatActivity() {

    // ── Views ──────────────────────────────────────────────────────────────────
    internal lateinit var videoView: VideoView
    internal lateinit var screenBug: ImageView
    internal lateinit var versionInfo: TextView
    internal lateinit var debugTextView: TextView
    internal lateinit var prevButton: ImageButton
    internal lateinit var nextButton: ImageButton
    internal lateinit var settingsButton: ImageButton  // Preview 2006.4.1.0.11
    // Overlay CRT: scanlines + phosphor mask + vignette + flicker (Canvas puro)
    internal lateinit var crtOverlay: CrtOverlayView

    // ── Configuración (Preview 2006.4.1.0.12) ───────────────────────────────────
    // Antes eran `const val` en companion object; ahora son configurables desde
    // SettingsActivity, así que pasan a ser propiedades de instancia. Se inicializan
    // con el valor por defecto y se sobreescriben en applySettings() (onCreate/onResume).
    internal var bugShowDelayMs: Long = SettingsManager.DEFAULT_SCREENBUG_DELAY_SEC * 1_000L
    internal var breakIntervalMinMs: Long = SettingsManager.DEFAULT_COMMERCIAL_MIN_MINUTES * 60 * 1_000L
    internal var breakIntervalMaxMs: Long = SettingsManager.DEFAULT_COMMERCIAL_MAX_MINUTES * 60 * 1_000L

    // ── Background music (solo durante programas) ──────────────────────────────
    // MediaPlayer independiente del VideoView para poder pausar/reanudar
    // sin afectar la reproducción del video principal.
    internal var bgPlayer: MediaPlayer? = null

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

    internal var pausedPositionMs      = 0       // posición del programa guardada por el tracker
    internal var pausedByLifecycle     = false   // true si onPause() pausó la app
    internal var currentSegmentStartMs = 0       // posición del programa donde arrancó el segmento activo (para calcular elapsed en screenbug)

    // ── Estado de reanudación para clips NO-programa (Release 2006.4.1.1) ──────
    //
    // ANTES: bumper / enseguida / standaloneCommercial / pasos del bloque comercial
    // se reiniciaban siempre desde el principio al volver de segundo plano o de
    // un cambio de Activity, porque onResume() llamaba advance() a ciegas para
    // cualquier ítem que no fuera "program" o un bloque comercial en curso.
    //
    // AHORA: igual que con el programa, se trackea la posición del clip actual
    // (currentClipPositionMs, vía el position tracker que ahora corre siempre)
    // y se guarda su Uri (currentClipUri) más una "receta" de cómo continuar el
    // flujo cuando termine (currentClipOnComplete). onResume() usa esto para
    // retomar el MISMO clip en la MISMA posición con seekTo(), en vez de volver
    // a llamar advance().
    internal var currentClipUri: Uri?            = null
    internal var currentClipPositionMs           = 0
    internal var currentClipOnComplete: (() -> Unit)? = null

    // Paso actual dentro del bloque comercial (playCommercial), para poder
    // reconstruir exactamente dónde estábamos al reanudar: el bloque encadena
    // 3 clips (ya_regresa → comercial → continuamos) y cada uno necesita saber
    // qué recurso eligió el paso anterior para no volver a sortear al azar.
    internal enum class CommercialStep { PRE_COMERCIAL, COMERCIAL, POST_COMERCIAL }
    internal var commercialStep: CommercialStep        = CommercialStep.PRE_COMERCIAL
    internal var commercialChosenPreComercial: Int      = -1
    internal var commercialChosenCommercial: Int        = -1
    internal var commercialChosenYaVolvemos: Int         = -1

    // ── Flags de estado ────────────────────────────────────────────────────────
    internal var isInProgramSegment    = false
    internal var isInCommercialBlock   = false
    internal var commercialResumeMs    = 0

    // ── Tipo de ítem actual ────────────────────────────────────────────────────
    // Valores: "program", "bumper", "enseguida", "talla", "commercial"
    internal var currentItemType: String = "bumper"

    // ── FPS (frames por segundo) ───────────────────────────────────────────────
    // Medido con Choreographer.FrameCallback que se dispara en cada vsync.
    // currentFps se actualiza cada segundo y se muestra en el debug overlay.
    internal var fpsFrameCount   = 0
    internal var fpsLastTimeNs   = 0L
    internal var currentFps      = 0
    internal val fpsFrameCallback = object : Choreographer.FrameCallback {
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
    internal val handler = Handler(Looper.getMainLooper())
    internal val pendingTasks = mutableListOf<Runnable>()
    internal val debugHandler = Handler(Looper.getMainLooper())
    internal val navHideHandler = Handler(Looper.getMainLooper())
    internal val positionTrackerHandler = Handler(Looper.getMainLooper())
    internal val positionTrackerRunnable = object : Runnable {
        override fun run() {
            if (videoView.isPlaying) {
                if (isInProgramSegment) {
                    pausedPositionMs = videoView.currentPosition
                } else {
                    // Release 2006.4.1.1: trackea también la posición de clips
                    // no-programa (bumper, enseguida, comercial) para poder
                    // reanudarlos en vez de reiniciarlos al volver de segundo plano.
                    currentClipPositionMs = videoView.currentPosition
                }
            }
            positionTrackerHandler.postDelayed(this, 16)
        }
    }

    // ── Playlist definition ────────────────────────────────────────────────────
    internal sealed class PlayItem {
        object Bumper : PlayItem()
        object Enseguida : PlayItem()
        object StandaloneCommercial : PlayItem()
        data class Program(val index: Int) : PlayItem()   // 0-based → pro(n+1).mp4
    }

    internal val playlist = listOf(
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

    internal var playlistIndex = 0
    internal var currentProgramIndex = 0

    // Release 4.3.1 — BUG FIX: Prev/Next saltaba al programa equivocado si se
    // tocaba ANTES de que cualquier programa hubiera arrancado en la sesión
    // (ej: durante la Enseguida/Bumper/Comercial inicial, antes de Program(0)).
    // currentProgramIndex nace en 0 por defecto, así que findAvailableProgramIndex()
    // lo trataba como si el programa 0 ya hubiera salido al aire, y "Next" saltaba
    // directo al programa 1 (saltándose el 0) y "Prev" caía en el 3 en vez del 0.
    // Este flag distingue "todavía no arrancó ningún programa" de "currentProgramIndex
    // realmente refleja el último programa que salió al aire" — ver goToAdjacentProgram().
    internal var hasPlayedAnyProgram = false

    // ── Program state (persisted across commercial breaks) ─────────────────────
    internal var currentProgramUri: Uri? = null
    internal var programDuration  = 0          // total ms
    internal var breakQueue       = mutableListOf<Int>()   // upcoming break positions in ms
    internal var lastCommercialRes: Int = -1
    internal var lastBumperRes: Int = -1
    internal var lastEnseguidaPostProgramaRes: Int = -1
    // ya_regresa determinístico: cada programa tiene asignado su propio ya_regresa fijo.
    // programa 0 (pro1) → ya_regresa1 | programa 1 (pro2) → ya_regresa2 | etc.
    // Se indexa por currentProgramIndex en playCommercial().
    internal var lastEnseguidaPreComercialRes: Int = -1
    internal var currentScreenBugRes: Int = R.drawable.screenbug

    // ── Constants ──────────────────────────────────────────────────────────────
    companion object {
        internal const val TAG = "DKids"

        /** Screenbug hides this many ms before segment end or commercial start. */
        internal const val BUG_HIDE_EARLY = 20_000L

        /** No commercial break is scheduled within this many ms of the program end. */
        internal const val BREAK_CUTOFF_MS = 3 * 60 * 1_000L         // 3 min

        /** Programs shorter than this have zero commercial breaks. */
        internal const val MIN_DURATION_FOR_BREAKS = 3 * 60 * 1_000L  // 3 min

        /** Alpha-animation duration for screenbug fade. */
        internal const val FADE_MS = 500L

        /** FadeOut duration for video transitions (ms). Applied before every video change. Release 3.3.0: unified to 500 ms for all clip types. */
        internal const val TRANSITION_FADE_OUT_MS = 500L

        /** FadeIn duration for video transitions (ms). Applied when the new video starts. */
        internal const val TRANSITION_FADE_IN_MS = 500L

        internal const val PERM_REQUEST = 42

        // ── SharedPreferences – persistencia de sesión al cerrar la app ─────────
        internal const val PREFS_NAME         = "dk_channel_state"
        internal const val PREF_HAS_STATE     = "has_saved_state"
        internal const val PREF_PLAYLIST_IDX  = "playlist_index"
        internal const val PREF_POSITION_MS   = "position_ms"
        internal const val PREF_PROGRAM_IDX   = "program_index"
        internal const val PREF_ITEM_TYPE     = "item_type"       // "program"|"bumper"|"enseguida"|"talla"|"commercial"
        internal const val PREF_COMMERCIAL_MS = "commercial_resume_ms"
        internal const val PREF_SCREENBUG_RES = "screenbug_res"
        internal const val PREF_BREAK_QUEUE   = "break_queue"
        internal const val PREF_HAS_PLAYED_PROGRAM = "has_played_program"   // Release 4.3.1

        /** Lista de comerciales disponibles; se elige uno al azar en cada corte. */
        internal val COMMERCIALS = listOf(R.raw.comercial1, R.raw.comercial2, R.raw.comercial3, R.raw.comercial4)

        /**
         * Lista de bumpers disponibles.
         * Se elige uno al azar antes de cada programa, evitando repetir el mismo dos veces seguidas.
         * Beta 2005.4.0.0.4: bumper6 reemplazado por nuevo bumper de aviso de la Era Doki
         * (Actualización La Era Doki / nuevo Discovery Kids).
         */
        internal val BUMPERS = listOf(
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
        internal val ENSEGUIDAS_POST_PROGRAMA = listOf(
            R.raw.enseguida1,
            R.raw.enseguida2
        )

        /**
         * Enseguidas pre-comercial (van justo ANTES del bloque publicitario).
         * Asignación determinística por índice de programa (0-based):
         *   programa 0 (pro1) → ya_regresa1 | programa 1 (pro2) → ya_regresa2
         *   programa 2 (pro3) → ya_regresa3 | programa 3 (pro4) → ya_regresa4
         * Se indexa directamente por currentProgramIndex en playCommercial().
         * ya_regresaN → se usa continuamosN como post-comercial (mapeo ENSEGUIDA_YA_VOLVEMOS_MAP).
         */
        internal val ENSEGUIDAS_PRE_COMERCIAL = listOf(
            R.raw.ya_regresa1,
            R.raw.ya_regresa2,
            R.raw.ya_regresa3,
            R.raw.ya_regresa4
        )

        /**
         * Mapeo: enseguida pre-comercial → continuamos que se debe usar en ese corte.
         * ya_regresa1 → continuamos1 | ya_regresa2 → continuamos2
         */
        internal val ENSEGUIDA_YA_VOLVEMOS_MAP = mapOf(
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
        settingsButton = findViewById(R.id.btnSettings)  // Preview 2006.4.1.0.11
        crtOverlay = findViewById(R.id.crtOverlay)

        // Botones ocultos al inicio; aparecen al tocar la pantalla
        prevButton.visibility = View.GONE
        nextButton.visibility = View.GONE
        settingsButton.visibility = View.GONE

        prevButton.setOnClickListener {
            resetNavHideTimer()
            goToAdjacentProgram(-1)
        }
        nextButton.setOnClickListener {
            resetNavHideTimer()
            goToAdjacentProgram(+1)
        }
        settingsButton.setOnClickListener {
            resetNavHideTimer()
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        requestStoragePermission()
        
        applySettings()  // Preview 2006.4.1.0.12: lee SettingsManager antes de mostrar nada
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
        val loggedPos = if (isInProgramSegment) pausedPositionMs else currentClipPositionMs
        Log.d(TAG, "onPause – tipo=$currentItemType pos=${loggedPos}ms")
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

        // Preview 2006.4.1.0.12: refresca configuración por si el usuario volvió
        // de SettingsActivity con cambios (CRT, intervalo comerciales, screenbug
        // delay, Forzar 4:3). Se hace siempre, sin importar pausedByLifecycle,
        // porque abrir SettingsActivity también pasa por onPause()/onResume()
        // aunque no sea un backgrounding real del sistema.
        applySettings()

        if (!pausedByLifecycle) return
        pausedByLifecycle = false

        when {
            isInProgramSegment -> {
                val uri = currentProgramUri ?: run {
                    Log.e(TAG, "onResume: isInProgramSegment pero no hay URI – advance()")
                    advance(); return
                }
                Log.d(TAG, "onResume – reanudando programa desde ${pausedPositionMs}ms")
                beginProgramSegment(uri, startOffsetMs = pausedPositionMs, isFirstPlay = false, isNewSegment = false)
            }
            isInCommercialBlock -> {
                // Release 2006.4.1.1 — BUG FIX: antes se saltaba todo el bloque
                // comercial en curso (ya_regresa / comercial / continuamos) y se
                // iba directo al programa. Ahora se reanuda el sub-clip exacto
                // donde estaba (commercialStep) en su posición real (currentClipPositionMs),
                // igual que ya se hacía con el programa.
                if (currentProgramUri == null) {
                    Log.e(TAG, "onResume: isInCommercialBlock pero no hay currentProgramUri – advance()")
                    advance(); return
                }
                Log.d(TAG, "onResume – reanudando bloque comercial (paso=$commercialStep) en ${currentClipPositionMs}ms")
                resumeCommercialBlock(currentClipPositionMs)
            }
            currentClipUri != null -> {
                // Release 2006.4.1.1 — BUG FIX: bumper / enseguida / standaloneCommercial
                // se reiniciaban siempre desde el principio al volver de segundo plano.
                // Ahora se reanuda el mismo clip (currentClipUri) en la posición donde
                // quedó (currentClipPositionMs) en vez de llamar advance().
                val uri = currentClipUri!!
                val onComplete = currentClipOnComplete
                if (onComplete == null) {
                    Log.e(TAG, "onResume: currentClipUri sin onComplete registrado – advance()")
                    advance(); return
                }
                Log.d(TAG, "onResume – reanudando clip tipo=$currentItemType en ${currentClipPositionMs}ms")
                resumeUriWithSeek(uri, currentClipPositionMs, onComplete = onComplete)
            }
            else -> {
                Log.d(TAG, "onResume – sin estado de clip guardado (tipo=$currentItemType) → reiniciando ítem")
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
    // Toque de pantalla → mostrar / ocultar botones de navegación,
    // permisos de almacenamiento, y fullscreen: ver ChannelUiHelpers.kt
    // (Reorganización 4.1.0.21). dispatchTouchEvent y
    // onRequestPermissionsResult quedan aquí porque son overrides de Activity.
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

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Start regardless – programs will be skipped if not found
        startChannel()
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Reorganización 4.1.0.21 — el resto del flujo del canal (playlist driver,
    // reproducción de programas, bloque comercial, transiciones de video,
    // cálculo de cortes, resolución de URIs, música de fondo, persistencia
    // de sesión, position tracker y animación del Screenbug) vive ahora en
    // archivos de extensión separados por responsabilidad, todos operando
    // sobre esta misma clase y su mismo estado de instancia:
    //
    //   ChannelPlaylist.kt          → advance, playBumper, playEnseguida,
    //                                  playStandaloneCommercial,
    //                                  goToAdjacentProgram, findAvailableProgramIndex
    //   ChannelProgramPlayback.kt   → playProgram, beginProgramSegment,
    //                                  scheduleSegmentLogic, calcBreaks
    //   ChannelCommercialBlock.kt   → playCommercial, playCommercialStepPreComercial,
    //                                  resumeCommercialBlock
    //   ChannelVideoTransitions.kt  → playUri, playUriWithTransition, resumeUriWithSeek
    //   ChannelMediaResolver.kt     → resolveProgram, rawUri
    //   ChannelBackgroundMusic.kt   → startBgMusic, stopBgMusic
    //   ChannelSessionState.kt      → startChannel, saveChannelState,
    //                                  showResumeDialog, resumeSavedState,
    //                                  clearSavedState, showExitConfirmationDialog
    //   ChannelPositionTracker.kt   → startPositionTracker, stopPositionTracker,
    //                                  post, cancelAllTasks
    //   ChannelScreenBug.kt         → fadeInBug, fadeOutBug, setBugAlpha
    //
    // Sin cambios de comportamiento — ver CHANGELOG.md para el detalle completo.
    // ══════════════════════════════════════════════════════════════════════════

    // ══════════════════════════════════════════════════════════════════════════
    // Configuración (applySettings) y overlay de debug (setupDebugInfo,
    // startRamMonitor, displayInfo): ver ChannelDebugOverlay.kt
    // (Reorganización 4.1.0.21)
    // ══════════════════════════════════════════════════════════════════════════
}