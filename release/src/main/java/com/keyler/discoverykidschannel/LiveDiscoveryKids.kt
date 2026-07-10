/*
 ESTO TIENE LICENCIA GNU LICENSE APACHE
 
 NO ELIMINAR NINGUN COMENTARIO AGREGADO
*/
package com.keyler.discoverykidschannel

import android.app.ActivityManager
import android.app.AlertDialog
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
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
 *
 * Release 4.6.0 — REUNIFICACIÓN. La reorganización de la 4.1.0.21 repartió
 * el flujo del canal en 11 archivos de extensión (ChannelPlaylist.kt,
 * ChannelProgramPlayback.kt, ChannelCommercialBlock.kt, ChannelVideoTransitions.kt,
 * ChannelMediaResolver.kt, ChannelBackgroundMusic.kt, ChannelSessionState.kt,
 * ChannelPositionTracker.kt, ChannelScreenBug.kt, ChannelUiHelpers.kt,
 * ChannelDebugOverlay.kt). Esta Release los vuelve a unificar todos en este
 * mismo archivo: cada bloque de abajo conserva el comentario de encabezado
 * original de su archivo de origen (a modo de separador de sección), y el
 * código se copió tal cual, sin cambios de comportamiento. Los 11 archivos
 * de extensión se eliminan del proyecto.
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
    //
    // Release 4.6.0 — REUNIFICACIÓN: los archivos de arriba dejan de existir
    // como archivos separados. Todas esas funciones de extensión (más
    // ChannelUiHelpers.kt y ChannelDebugOverlay.kt, ver más abajo) están ahora
    // en este mismo archivo, debajo del cierre de esta clase, cada una bajo el
    // comentario de encabezado original de su archivo de origen.
    // ══════════════════════════════════════════════════════════════════════════

    // ══════════════════════════════════════════════════════════════════════════
    // Configuración (applySettings) y overlay de debug (setupDebugInfo,
    // startRamMonitor, displayInfo): ver ChannelDebugOverlay.kt
    // (Reorganización 4.1.0.21)
    //
    // Release 4.6.0 — REUNIFICACIÓN: ChannelDebugOverlay.kt también se
    // reunificó en este archivo, ver la sección correspondiente más abajo.
    // ══════════════════════════════════════════════════════════════════════════
}

// ════════════════════════════════════════════════════════════════════════════
// Release 4.6.0 — REUNIFICACIÓN DE EXTENSIONES
//
// Desde acá para abajo: todo el código que vivía en los 11 archivos de
// extensión separados (ChannelPlaylist.kt, ChannelProgramPlayback.kt,
// ChannelCommercialBlock.kt, ChannelVideoTransitions.kt, ChannelMediaResolver.kt,
// ChannelBackgroundMusic.kt, ChannelSessionState.kt, ChannelPositionTracker.kt,
// ChannelScreenBug.kt, ChannelUiHelpers.kt, ChannelDebugOverlay.kt) desde la
// Reorganización 4.1.0.21. Se copia tal cual, agrupado por archivo de origen
// (cada uno conserva su comentario de encabezado original), sin cambios de
// comportamiento. Los 11 archivos se eliminan del proyecto.
// ════════════════════════════════════════════════════════════════════════════


// ── Antes en ChannelPlaylist.kt ────────────────────────────────────────

/**
 * ChannelPlaylist.kt — Preview 2006.4.1.0.21
 *
 * Funciones de extensión de LiveDiscoveryKids: el driver principal de la
 * playlist (advance()), los tres tipos de clip "de relleno" entre programas
 * (Bumper, Enseguida post-programa, StandaloneCommercial), y la navegación
 * Prev/Next que salta directo entre programas.
 *
 * La reproducción del programa en sí (playProgram, beginProgramSegment,
 * scheduleSegmentLogic, calcBreaks) vive en ChannelProgramPlayback.kt; el
 * bloque comercial que interrumpe un programa en curso (playCommercial y
 * sus pasos) vive en ChannelCommercialBlock.kt.
 *
 * Reorganización 4.1.0.21 — código movido tal cual desde LiveDiscoveryKids.kt.
 * Sin cambios de comportamiento.
 */

// ══════════════════════════════════════════════════════════════════════════
// Playlist driver
// ══════════════════════════════════════════════════════════════════════════
// startChannel() → ver ChannelSessionState.kt

/** Move to the next playlist item (wraps around). */
internal fun LiveDiscoveryKids.advance() {
    if (playlistIndex >= playlist.size) playlistIndex = 0
    when (val item = playlist[playlistIndex]) {
        is LiveDiscoveryKids.PlayItem.Bumper               -> playBumper()
        is LiveDiscoveryKids.PlayItem.Enseguida            -> playEnseguida()
        is LiveDiscoveryKids.PlayItem.StandaloneCommercial -> playStandaloneCommercial()
        is LiveDiscoveryKids.PlayItem.Program              -> playProgram(item.index)
    }
}

// ══════════════════════════════════════════════════════════════════════════
// Bumper playback
// ══════════════════════════════════════════════════════════════════════════

internal fun LiveDiscoveryKids.playBumper() {
    cancelAllTasks()
    setBugAlpha(0f)
    stopBgMusic()
    isInProgramSegment = false
    currentItemType = "bumper"

    val candidates = LiveDiscoveryKids.BUMPERS.filter { it != lastBumperRes }.ifEmpty { LiveDiscoveryKids.BUMPERS }
    val chosenBumper = candidates.random()
    lastBumperRes = chosenBumper

    Log.d(LiveDiscoveryKids.TAG, "▶ BUMPER [res=$chosenBumper]")

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

internal fun LiveDiscoveryKids.playStandaloneCommercial() {
    cancelAllTasks()
    setBugAlpha(0f)
    stopBgMusic()
    isInProgramSegment  = false
    isInCommercialBlock = false
    currentItemType     = "standaloneCommercial"

    val candidates = LiveDiscoveryKids.COMMERCIALS.filter { it != lastCommercialRes }.ifEmpty { LiveDiscoveryKids.COMMERCIALS }
    val chosenCommercial = candidates.random()
    lastCommercialRes  = chosenCommercial

    Log.d(LiveDiscoveryKids.TAG, "▶ STANDALONE COMMERCIAL [res=$chosenCommercial]")

    playUriWithTransition(rawUri(chosenCommercial)) {
        playlistIndex++
        advance()
    }
}

// ══════════════════════════════════════════════════════════════════════════
// Enseguida playback – post-programa: aparece entre el fin del programa
// y el comercial standalone.
// Beta 3.0.0.3: selección aleatoria con anti-repetición entre
// [enseguida1, enseguida2]. Se eliminó la selección por horario y enseguida5.
// ══════════════════════════════════════════════════════════════════════════

internal fun LiveDiscoveryKids.playEnseguida() {
    cancelAllTasks()
    setBugAlpha(0f)
    stopBgMusic()
    isInProgramSegment = false
    currentItemType = "enseguida"

    val candidates = LiveDiscoveryKids.ENSEGUIDAS_POST_PROGRAMA
        .filter { it != lastEnseguidaPostProgramaRes }
        .ifEmpty { LiveDiscoveryKids.ENSEGUIDAS_POST_PROGRAMA }
    val chosenEnseguida = candidates.random()
    lastEnseguidaPostProgramaRes = chosenEnseguida

    Log.d(LiveDiscoveryKids.TAG, "▶ ENSEGUIDA post-programa [res=$chosenEnseguida]")

    playUriWithTransition(rawUri(chosenEnseguida)) {
        playlistIndex++
        advance()
    }
}

// ══════════════════════════════════════════════════════════════════════════
// Navegación Prev / Next
//
// Release 3.4.1 — Prev / Next saltan directo al programa destino.
//
// Problema del enfoque anterior (iniciar desde Enseguida):
//   playEnseguida() → playUriWithTransition() registra el timer del FadeOut en pendingTasks.
//   Cuando la enseguida termina, su onComplete llama playBumper() → cancelAllTasks(),
//   que borra el timer del FadeOut del bumper antes de que corra. Además,
//   encadenar playUriWithTransition() dentro del onComplete de otro cancela la
//   animación del segundo via ViewPropertyAnimator (instancia única del videoView),
//   por lo que el withEndAction del FadeOut inicial nunca se ejecuta y el bumper
//   nunca arranca.
//
// Solución: Prev / Next se comportan como un cambio de canal — van directo al
// programa sin pasar por Enseguida → StandaloneCommercial → Bumper. Ese bloque
// ya ocurre naturalmente cuando el programa termine por su propio onCompletionListener.
// playlistIndex se fija en el PlayItem.Program para que advance() continúe
// correctamente desde la Enseguida del siguiente ciclo al terminar el programa.
// ══════════════════════════════════════════════════════════════════════════

internal fun LiveDiscoveryKids.goToAdjacentProgram(direction: Int) {
    // Release 4.3.1 — BUG FIX: si todavía no arrancó ningún programa en la
    // sesión (ej: Prev/Next tocado durante la Enseguida/Bumper/Comercial
    // inicial, antes de Program(0)), currentProgramIndex sigue en su valor
    // por defecto (0) sin que el programa 0 haya salido realmente al aire.
    // Antes esto hacía que findAvailableProgramIndex() lo tratara como
    // "programa 0 ya visto" y Next saltara directo al 1 (saltándose el 0) y
    // Prev cayera en el 3 en vez de ir al 0.
    //
    // El punto de partida "virtual" para el wraparound de
    // findAvailableProgramIndex() depende de la dirección: para Next hay que
    // partir de -1 (así el primer candidato que evalúa es el 0); para Prev
    // hay que partir de 0 (así el primer candidato que evalúa, retrocediendo,
    // es el 3 — el último programa). No es el mismo valor para ambos casos.
    val startIndex = when {
        hasPlayedAnyProgram -> currentProgramIndex
        direction > 0       -> -1
        else                -> 0
    }
    val target = findAvailableProgramIndex(startIndex, direction) ?: return

    if (hasPlayedAnyProgram && target == currentProgramIndex) return

    Log.d(LiveDiscoveryKids.TAG, "▶ Navegando directo al programa ${target + 1} (direction=$direction)")
    cancelAllTasks()
    setBugAlpha(0f)
    stopPositionTracker()
    stopBgMusic()
    isInProgramSegment = false
    isInCommercialBlock = false
    // Release 2006.4.1.1: limpia el estado de reanudación de clips no-programa;
    // Prev/Next descarta cualquier bumper/enseguida/comercial en curso, así que
    // no debe quedar un currentClipUri obsoleto que onResume() intente reanudar.
    currentClipUri = null
    currentClipOnComplete = null
    videoView.stopPlayback()

    // Busca el índice del PlayItem.Program destino en el playlist y fija playlistIndex ahí.
    // El programa terminará normalmente y su onCompletionListener hará playlistIndex++ + advance(),
    // arrancando la Enseguida del siguiente bloque sin ningún conflicto de ViewPropertyAnimator.
    val programIdx = playlist.indexOfFirst { it is LiveDiscoveryKids.PlayItem.Program && it.index == target }
        .takeIf { it >= 0 } ?: 0

    playlistIndex = programIdx
    currentProgramIndex = target

    playProgram(target, restartFromBeginning = true)
}

/**
 * Release 4.6.0 — además de comprobar que el archivo exista (resolveProgram),
 * ahora también salta los programas que el usuario desactivó desde el nuevo
 * Discovery Kids Launcher (SettingsManager.isProgramEnabled). Prev/Next
 * nunca aterriza en un programa desactivado, igual que ya evitaba caer en
 * uno cuyo .mp4 no estuviera en la carpeta Movies.
 */
internal fun LiveDiscoveryKids.findAvailableProgramIndex(startIndex: Int, direction: Int): Int? {
    if (direction == 0) return null

    val totalPrograms = 4
    var candidate = startIndex

    repeat(totalPrograms) {
        candidate = (candidate + direction + totalPrograms) % totalPrograms
        if (SettingsManager.isProgramEnabled(this, candidate) && resolveProgram(candidate) != null) return candidate
    }
    return null
}



// ── Antes en ChannelProgramPlayback.kt ────────────────────────────────────────

/**
 * ChannelProgramPlayback.kt — Preview 2006.4.1.0.21
 *
 * Funciones de extensión de LiveDiscoveryKids: reproducción del programa
 * (pro1–pro4.mp4), el scheduling de Screenbug y cortes comerciales dentro
 * de un segmento de programa, y el cálculo de las posiciones de corte.
 *
 * Reorganización 4.1.0.21 — código movido tal cual desde LiveDiscoveryKids.kt.
 * Sin cambios de comportamiento.
 */

// ══════════════════════════════════════════════════════════════════════════
// Program playback
// ══════════════════════════════════════════════════════════════════════════

internal fun LiveDiscoveryKids.playProgram(idx: Int, restartFromBeginning: Boolean = true) {
    currentProgramIndex = idx
    // Release 4.3.1 — a partir de acá currentProgramIndex ya refleja un programa
    // real que empezó a salir al aire; ver comentario en goToAdjacentProgram().
    hasPlayedAnyProgram = true

    // Release 4.6.0 — BUG FIX / NUEVO: si el usuario desactivó este programa
    // desde el Discovery Kids Launcher (SettingsManager.isProgramEnabled),
    // se saltea exactamente igual que un programa cuyo .mp4 no está en la
    // carpeta Movies (mismo camino: playlistIndex++ + advance()).
    if (!SettingsManager.isProgramEnabled(this, idx)) {
        Log.d(LiveDiscoveryKids.TAG, "pro${idx + 1}.mp4 desactivado en el Launcher – skipping")
        playlistIndex++
        advance()
        return
    }

    val uri = resolveProgram(idx)
    if (uri == null) {
        Log.w(LiveDiscoveryKids.TAG, "pro${idx + 1}.mp4 not found – skipping")
        playlistIndex++
        advance()
        return
    }

    Log.d(LiveDiscoveryKids.TAG, "▶ PROGRAM pro${idx + 1}")
    currentProgramUri = uri
    breakQueue.clear()
    // Release 2006.4.1.1: limpia cualquier estado de clip no-programa heredado
    // del ítem anterior (bumper/enseguida/comercial), ya que isInProgramSegment
    // tendrá prioridad de cualquier forma, pero evita estado fantasma confuso en logs.
    currentClipUri = null
    currentClipOnComplete = null

    val startPos = if (restartFromBeginning) 0 else videoView.currentPosition
    beginProgramSegment(uri, startOffsetMs = startPos, isFirstPlay = restartFromBeginning)
}

/**
* Plays the program starting at [startOffsetMs].
* [isFirstPlay] = true  → recalculate breaks from scratch.
* [isFirstPlay] = false → breaks already trimmed; resume only.
*/
internal fun LiveDiscoveryKids.beginProgramSegment(
    uri: Uri,
    startOffsetMs: Int,
    isFirstPlay: Boolean,
    isNewSegment: Boolean = true
) {
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

        scheduleSegmentLogic(startOffsetMs, isNewSegment = isNewSegment)
        videoView.alpha = 0f
        videoView.start()
        videoView.animate()
            .alpha(1f)
            .setDuration(LiveDiscoveryKids.TRANSITION_FADE_IN_MS)
            .start()
        isInProgramSegment = true
        currentItemType = "program"
        startPositionTracker()
        startBgMusic()
    }
    videoView.setOnCompletionListener {
        Log.d(LiveDiscoveryKids.TAG, "Program ended")
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
 * - Si elapsed >= bugShowDelayMs → screenbug debe estar visible ya; aparece
 *   inmediatamente con setBugAlpha(1f) y se programa solo el fadeOut.
 * - Si elapsed < bugShowDelayMs → se programa fadeIn con delay reducido.
 * Esto evita que al volver de segundo plano el screenbug reinicie su cuenta
 * de [bugShowDelayMs] desde cero aunque ya debía estar visible.
 *
 * Preview 2006.4.1.0.12: bugShowDelayMs ahora es configurable desde
 * SettingsActivity (antes BUG_SHOW_DELAY fijo en 20 s).
 */
internal fun LiveDiscoveryKids.scheduleSegmentLogic(segmentStartMs: Int, isNewSegment: Boolean) {
    // Release 2006.4.1.1 — BUG FIX: el cálculo de `elapsed` comparaba
    // segmentStartMs contra currentSegmentStartMs DESPUÉS de haberlo
    // sobreescrito con el mismo segmentStartMs, dando siempre elapsed = 0.
    // Esto hacía que el screenbug reiniciara su cuenta cada vez que la app
    // volvía de segundo plano o de un cambio de Activity (ej: SettingsActivity),
    // en vez de "recordar" cuánto tiempo ya había transcurrido en el segmento.
    //
    // Ahora currentSegmentStartMs solo se actualiza cuando arranca un segmento
    // REALMENTE nuevo (isNewSegment = true, ej: tras un corte comercial o al
    // iniciar el programa). Al reanudar el mismo segmento (isNewSegment = false)
    // se conserva el valor anterior, permitiendo calcular cuánto tiempo pasó.
    val previousSegmentStartMs = currentSegmentStartMs
    if (isNewSegment) {
        currentSegmentStartMs = segmentStartMs
    }

    // Determine end of this segment (next break or program end)
    val segmentEndMs = if (breakQueue.isNotEmpty()) breakQueue[0] else programDuration
    val segmentDuration = (segmentEndMs - segmentStartMs).toLong().coerceAtLeast(0)

    Log.d(LiveDiscoveryKids.TAG, "Segment: ${segmentStartMs}ms → ${segmentEndMs}ms (${segmentDuration}ms)")

    // Calcula cuántos ms del segmento ya transcurrieron antes de este (re)arranque.
    // En la primera llamada de un segmento nuevo elapsed = 0 (previousSegmentStartMs
    // todavía no se actualizó arriba, así que coincide con segmentStartMs).
    // Al reanudar desde segundo plano elapsed = segmentStartMs - currentSegmentStartMs
    // (el valor de arranque ORIGINAL del segmento, que no se tocó).
    val baseSegmentStartMs = if (isNewSegment) segmentStartMs else previousSegmentStartMs
    val elapsed = (segmentStartMs - baseSegmentStartMs).toLong().coerceAtLeast(0L)

    val bugShowDelay = (bugShowDelayMs - elapsed).coerceAtLeast(0L)

    if (elapsed >= bugShowDelayMs) {
        // El screenbug ya debía estar visible — aparece inmediatamente sin animación
        Log.d(LiveDiscoveryKids.TAG, "ScreenBug: elapsed=${elapsed}ms >= bugShowDelayMs(${bugShowDelayMs}) → aparece inmediatamente")
        setBugAlpha(1f)
    } else if (segmentDuration > bugShowDelay) {
        post(bugShowDelay) { fadeInBug() }
    }

    val hideAt = segmentDuration - LiveDiscoveryKids.BUG_HIDE_EARLY
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

// ══════════════════════════════════════════════════════════════════════════
// Commercial break calculation
// ══════════════════════════════════════════════════════════════════════════

/**
 * Returns a list of program positions (in ms) where commercial breaks occur.
 *
 * Beta 2005.4.0.0.4 — Distribución de intervalo aleatorio:
 *   Los breaks se colocan en posiciones acumuladas usando un intervalo aleatorio
 *   entre [breakIntervalMinMs] (3 min por defecto) y [breakIntervalMaxMs] (9 min por defecto).
 *   Cada corte elige su propio intervalo independientemente, generando una
 *   programación publicitaria variable más parecida a la TV real.
 *
 * Preview 2006.4.1.0.12 — Intervalo configurable desde SettingsActivity
 *   (antes BREAK_INTERVAL_MIN_MS/MAX_MS fijos en 3–9 min).
 *
 * Release 4.0.1 — Zona de protección al final del programa:
 *   No se programa ningún corte dentro de los últimos [BREAK_CUTOFF_MS] (3 min)
 *   del programa. El while ahora compara contra (durationMs - BREAK_CUTOFF_MS)
 *   en lugar de durationMs, garantizando que el final del programa nunca sea
 *   interrumpido por un comercial.
 *
 * Distribución anterior (≤3.4.1): intervalo fijo de 9 minutos exactos —
 *   los breaks siempre ocurrían a los 9 min, 18 min, 27 min, etc.
 *
 * Programs shorter than [MIN_DURATION_FOR_BREAKS] get no breaks.
 */
internal fun LiveDiscoveryKids.calcBreaks(durationMs: Int): List<Int> {
    if (durationMs < LiveDiscoveryKids.MIN_DURATION_FOR_BREAKS) return emptyList()
    val cutoff = durationMs - LiveDiscoveryKids.BREAK_CUTOFF_MS
    if (cutoff <= 0) return emptyList()
    val breaks = mutableListOf<Int>()
    var breakPos = (breakIntervalMinMs + (Math.random() * (breakIntervalMaxMs - breakIntervalMinMs)).toLong())
    while (breakPos < cutoff) {
        breaks.add(breakPos.toInt())
        breakPos += (breakIntervalMinMs + (Math.random() * (breakIntervalMaxMs - breakIntervalMinMs)).toLong())
    }
    return breaks
}



// ── Antes en ChannelCommercialBlock.kt ────────────────────────────────────────

/**
 * ChannelCommercialBlock.kt — Preview 2006.4.1.0.21
 *
 * Funciones de extensión de LiveDiscoveryKids: el bloque comercial que
 * interrumpe un programa en curso. Secuencia: ya_regresa (pre-comercial,
 * determinístico por programa) → comercial (aleatorio) → continuamos
 * (pareado con el ya_regresa elegido) → retoma el programa.
 *
 * resumeCommercialBlock() reconstruye el paso exacto del bloque (commercialStep)
 * usando los mismos recursos ya elegidos si la app pasó a segundo plano a
 * mitad del bloque — ver Release 2006.4.1.1 en el CHANGELOG.
 *
 * Reorganización 4.1.0.21 — código movido tal cual desde LiveDiscoveryKids.kt.
 * Sin cambios de comportamiento.
 */

// ══════════════════════════════════════════════════════════════════════════
// Commercial playback
// Secuencia: ya_regresa1/2 (pre-comercial) → comercial(es) → continuamos(pareado) → programa
// ══════════════════════════════════════════════════════════════════════════

internal fun LiveDiscoveryKids.playCommercial(resumeProgramAtMs: Int) {
    cancelAllTasks()
    setBugAlpha(0f)
    stopBgMusic()
    isInProgramSegment = false
    isInCommercialBlock = true
    commercialResumeMs = resumeProgramAtMs
    currentItemType = "commercial"

    val commercialCandidates = LiveDiscoveryKids.COMMERCIALS.filter { it != lastCommercialRes }
        .ifEmpty { LiveDiscoveryKids.COMMERCIALS }
    val chosenCommercial = commercialCandidates.random()
    lastCommercialRes = chosenCommercial

    // Release 4.0.1 — BUG FIX: ya_regresa determinístico por programa.
    // currentProgramIndex (0-based) indexa directamente ENSEGUIDAS_PRE_COMERCIAL,
    // garantizando que cada programa siempre muestre su propio ya_regresa/continuamos.
    val chosenPreComercial = LiveDiscoveryKids.ENSEGUIDAS_PRE_COMERCIAL[currentProgramIndex % LiveDiscoveryKids.ENSEGUIDAS_PRE_COMERCIAL.size]
    lastEnseguidaPreComercialRes = chosenPreComercial

    val chosenYaVolvemos = LiveDiscoveryKids.ENSEGUIDA_YA_VOLVEMOS_MAP[chosenPreComercial]
        ?: R.raw.continuamos1   // fallback defensivo

    // Release 2006.4.1.1 — se promueven a campos de instancia para que
    // onResume() pueda reconstruir exactamente este bloque comercial
    // (qué clips se eligieron y en qué paso estaba) si la app pasa a
    // segundo plano a mitad del bloque, en vez de volver a sortear y
    // reiniciar todo desde ya_regresa.
    commercialChosenPreComercial = chosenPreComercial
    commercialChosenCommercial   = chosenCommercial
    commercialChosenYaVolvemos   = chosenYaVolvemos
    commercialStep = LiveDiscoveryKids.CommercialStep.PRE_COMERCIAL

    Log.d(LiveDiscoveryKids.TAG, "▶ ENSEGUIDA pre-comercial [res=$chosenPreComercial] → continuamos [res=$chosenYaVolvemos]")

    videoView.animate()
        .alpha(0f)
        .setDuration(LiveDiscoveryKids.TRANSITION_FADE_OUT_MS)
        .withEndAction {
            playCommercialStepPreComercial(chosenPreComercial, chosenCommercial, chosenYaVolvemos, resumeProgramAtMs, startOffsetMs = 0)
        }
        .start()
}

/** Paso 1 del bloque comercial: ya_regresa (pre-comercial). FadeIn 1 s, sin FadeOut de entrada (ya lo hizo el caller). */
internal fun LiveDiscoveryKids.playCommercialStepPreComercial(
    chosenPreComercial: Int,
    chosenCommercial: Int,
    chosenYaVolvemos: Int,
    resumeProgramAtMs: Int,
    startOffsetMs: Int
) {
    commercialStep = LiveDiscoveryKids.CommercialStep.PRE_COMERCIAL
    currentClipUri = rawUri(chosenPreComercial)
    currentClipPositionMs = startOffsetMs
    currentClipOnComplete = null   // este paso no usa playUriWithTransition; se maneja manualmente
    startPositionTracker()

    videoView.alpha = 0f
    videoView.setOnPreparedListener { mp ->
        mp.isLooping = false
        if (startOffsetMs > 0) videoView.seekTo(startOffsetMs)
        videoView.start()
        videoView.animate().alpha(1f).setDuration(LiveDiscoveryKids.TRANSITION_FADE_IN_MS).start()
    }
    videoView.setOnCompletionListener {
        Log.d(LiveDiscoveryKids.TAG, "▶ COMMERCIAL [res=$chosenCommercial] (resumes program at ${resumeProgramAtMs}ms)")
        commercialStep = LiveDiscoveryKids.CommercialStep.COMERCIAL

        // Paso 2: comercial — FadeOut 500 ms (TRANSITION_FADE_OUT_MS)
        playUriWithTransition(rawUri(chosenCommercial)) {
            Log.d(LiveDiscoveryKids.TAG, "▶ YA VOLVEMOS post-comercial [res=$chosenYaVolvemos]")
            commercialStep = LiveDiscoveryKids.CommercialStep.POST_COMERCIAL

            // Paso 3: continuamos (FadeOut 500 ms / FadeIn 1 s)
            playUriWithTransition(rawUri(chosenYaVolvemos)) {
                val uri = currentProgramUri ?: run {
                    Log.e(LiveDiscoveryKids.TAG, "No currentProgramUri – advancing")
                    playlistIndex++
                    advance()
                    return@playUriWithTransition
                }
                Log.d(LiveDiscoveryKids.TAG, "Ya volvemos done – resuming program at ${resumeProgramAtMs}ms")
                isInCommercialBlock = false
                beginProgramSegment(uri, startOffsetMs = resumeProgramAtMs, isFirstPlay = false)
            }
        }
    }
    videoView.setVideoURI(rawUri(chosenPreComercial))
    videoView.requestFocus()
}

/**
 * Release 2006.4.1.1 — Reanuda el bloque comercial exactamente en el paso
 * y la posición donde estaba al pasar a segundo plano, usando los mismos
 * recursos ya elegidos (commercialChosenPreComercial/Commercial/YaVolvemos)
 * en vez de volver a sortear. Se llama desde onResume().
 */
internal fun LiveDiscoveryKids.resumeCommercialBlock(startOffsetMs: Int) {
    when (commercialStep) {
        LiveDiscoveryKids.CommercialStep.PRE_COMERCIAL -> {
            Log.d(LiveDiscoveryKids.TAG, "onResume – reanudando ya_regresa (pre-comercial) en ${startOffsetMs}ms")
            playCommercialStepPreComercial(
                commercialChosenPreComercial,
                commercialChosenCommercial,
                commercialChosenYaVolvemos,
                commercialResumeMs,
                startOffsetMs = startOffsetMs
            )
        }
        LiveDiscoveryKids.CommercialStep.COMERCIAL -> {
            Log.d(LiveDiscoveryKids.TAG, "onResume – reanudando comercial en ${startOffsetMs}ms")
            resumeUriWithSeek(rawUri(commercialChosenCommercial), startOffsetMs) {
                Log.d(LiveDiscoveryKids.TAG, "▶ YA VOLVEMOS post-comercial [res=$commercialChosenYaVolvemos]")
                commercialStep = LiveDiscoveryKids.CommercialStep.POST_COMERCIAL
                playUriWithTransition(rawUri(commercialChosenYaVolvemos)) {
                    val uri = currentProgramUri ?: run {
                        Log.e(LiveDiscoveryKids.TAG, "No currentProgramUri – advancing")
                        playlistIndex++
                        advance()
                        return@playUriWithTransition
                    }
                    isInCommercialBlock = false
                    beginProgramSegment(uri, startOffsetMs = commercialResumeMs, isFirstPlay = false)
                }
            }
        }
        LiveDiscoveryKids.CommercialStep.POST_COMERCIAL -> {
            Log.d(LiveDiscoveryKids.TAG, "onResume – reanudando continuamos en ${startOffsetMs}ms")
            resumeUriWithSeek(rawUri(commercialChosenYaVolvemos), startOffsetMs) {
                val uri = currentProgramUri ?: run {
                    Log.e(LiveDiscoveryKids.TAG, "No currentProgramUri – advancing")
                    playlistIndex++
                    advance()
                    return@resumeUriWithSeek
                }
                isInCommercialBlock = false
                beginProgramSegment(uri, startOffsetMs = commercialResumeMs, isFirstPlay = false)
            }
        }
    }
}



// ── Antes en ChannelVideoTransitions.kt ────────────────────────────────────────

/**
 * ChannelVideoTransitions.kt — Preview 2006.4.1.0.21
 *
 * Funciones de extensión de LiveDiscoveryKids: los helpers de bajo nivel
 * que reproducen un clip en el VideoView con FadeOut/FadeIn, usados por
 * TODOS los cambios de video del canal (bumper, enseguida, comerciales,
 * continuamos, retoma de programa tras un corte).
 *
 *   - playUri(): reproducción simple sin transición (usada internamente
 *     por rutas que manejan su propio FadeIn, ej. beginProgramSegment).
 *   - playUriWithTransition(): FadeOut/FadeIn estándar para arrancar un
 *     clip nuevo desde el principio.
 *   - resumeUriWithSeek(): igual que playUriWithTransition() pero retoma
 *     un clip en una posición ya avanzada (seekTo dentro de onPrepared),
 *     usado al volver de segundo plano (Release 2006.4.1.1).
 *
 * Reorganización 4.1.0.21 — código movido tal cual desde LiveDiscoveryKids.kt.
 * Sin cambios de comportamiento.
 */

// ══════════════════════════════════════════════════════════════════════════
// Video playback helper
// ══════════════════════════════════════════════════════════════════════════

/** Plays [uri] and calls [onComplete] when the video finishes. */
internal fun LiveDiscoveryKids.playUri(uri: Uri, onComplete: () -> Unit) {
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
internal fun LiveDiscoveryKids.playUriWithTransition(
    uri: Uri,
    fadeOutMs: Long = LiveDiscoveryKids.TRANSITION_FADE_OUT_MS,
    onComplete: () -> Unit
) {
    // Release 2006.4.1.1: guarda qué clip está sonando y cómo continuar el
    // flujo cuando termine, para que onResume() pueda reanudarlo en vez de
    // reiniciarlo si la app pasa a segundo plano mientras se reproduce.
    currentClipUri = uri
    currentClipPositionMs = 0
    currentClipOnComplete = onComplete
    startPositionTracker()

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
                    .setDuration(LiveDiscoveryKids.TRANSITION_FADE_IN_MS)
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
                            .withEndAction {
                                currentClipUri = null
                                currentClipOnComplete = null
                                onComplete()
                            }
                            .start()
                    }
                }
            }

            videoView.setOnCompletionListener {
                if (!transitionCompleted) {
                    transitionCompleted = true
                    currentClipUri = null
                    currentClipOnComplete = null
                    onComplete()
                }
            }

            videoView.setVideoURI(uri)
            videoView.requestFocus()
        }
        .start()
}

/**
 * Release 2006.4.1.1 — Reanuda un clip no-programa (bumper, enseguida,
 * comercial) exactamente en [startOffsetMs] en vez de reiniciarlo desde
 * el principio. Se usa desde onResume() cuando la app vuelve de segundo
 * plano o de un cambio de Activity mientras uno de estos clips sonaba.
 *
 * A diferencia de playUriWithTransition():
 *   - No hace FadeOut del clip "anterior" (ya no hay nada en pantalla,
 *     el VideoView quedó en alpha=0 desde onPause/setBugAlpha).
 *   - Hace seekTo(startOffsetMs) dentro de onPrepared, igual que
 *     beginProgramSegment(), porque Android puede haber liberado el
 *     surface del VideoView mientras la app estaba en segundo plano y
 *     seekTo() directo fuera de onPrepared se ignora silenciosamente.
 *   - El FadeOut de salida hacia el siguiente clip se reprograma con el
 *     tiempo que realmente queda (duration - elapsed), no con la duración
 *     completa, para no cortar el clip antes de tiempo.
 */
internal fun LiveDiscoveryKids.resumeUriWithSeek(
    uri: Uri,
    startOffsetMs: Int,
    fadeOutMs: Long = LiveDiscoveryKids.TRANSITION_FADE_OUT_MS,
    onComplete: () -> Unit
) {
    currentClipUri = uri
    currentClipPositionMs = startOffsetMs
    currentClipOnComplete = onComplete
    startPositionTracker()

    var transitionCompleted = false

    videoView.animate().cancel()
    videoView.alpha = 0f

    videoView.setOnPreparedListener { mp ->
        mp.isLooping = false
        if (startOffsetMs > 0) videoView.seekTo(startOffsetMs)
        videoView.start()
        videoView.animate()
            .alpha(1f)
            .setDuration(LiveDiscoveryKids.TRANSITION_FADE_IN_MS)
            .start()

        val duration = mp.duration
        val remaining = (duration - startOffsetMs).toLong().coerceAtLeast(0L)
        val fadeOutDelay = (remaining - fadeOutMs).coerceAtLeast(0L)
        post(fadeOutDelay) {
            if (!transitionCompleted) {
                transitionCompleted = true
                videoView.setOnCompletionListener(null)
                videoView.animate()
                    .alpha(0f)
                    .setDuration(fadeOutMs)
                    .withEndAction {
                        currentClipUri = null
                        currentClipOnComplete = null
                        onComplete()
                    }
                    .start()
            }
        }
    }

    videoView.setOnCompletionListener {
        if (!transitionCompleted) {
            transitionCompleted = true
            currentClipUri = null
            currentClipOnComplete = null
            onComplete()
        }
    }

    videoView.setVideoURI(uri)
    videoView.requestFocus()
}



// ── Antes en ChannelMediaResolver.kt ────────────────────────────────────────

/**
 * ChannelMediaResolver.kt — Preview 2006.4.1.0.21
 *
 * Funciones de extensión de LiveDiscoveryKids: resolución de URIs.
 *   - resolveProgram(): busca pro{N}.mp4 en la carpeta Movies primero
 *     (Android ≤ 9 o con MANAGE_EXTERNAL) y cae a una consulta MediaStore
 *     si no lo encuentra (necesario en Android 10+ con scoped storage).
 *   - rawUri(): construye el URI android.resource:// para recursos
 *     empaquetados en res/raw (bumpers, comerciales, enseguidas, etc).
 *
 * Reorganización 4.1.0.21 — código movido tal cual desde LiveDiscoveryKids.kt.
 * Sin cambios de comportamiento.
 */

// ══════════════════════════════════════════════════════════════════════════
// URI resolution – programs from Movies folder or MediaStore
// ══════════════════════════════════════════════════════════════════════════

internal fun LiveDiscoveryKids.resolveProgram(index: Int): Uri? {
    val fileName = "pro${index + 1}.mp4"

    // 1. Direct path in Movies directory (works on Android ≤ 9 or with MANAGE_EXTERNAL)
    val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
    val file = File(moviesDir, fileName)
    if (file.exists()) {
        Log.d(LiveDiscoveryKids.TAG, "Found via file path: ${file.absolutePath}")
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
                Log.d(LiveDiscoveryKids.TAG, "Found via MediaStore: $uri")
                uri
            } else null
        }
    } catch (e: Exception) {
        Log.e(LiveDiscoveryKids.TAG, "MediaStore query failed for $fileName", e)
        null
    }
}

// ══════════════════════════════════════════════════════════════════════════
// Utility
// ══════════════════════════════════════════════════════════════════════════

internal fun LiveDiscoveryKids.rawUri(resId: Int): Uri = Uri.parse("android.resource://$packageName/$resId")



// ── Antes en ChannelBackgroundMusic.kt ────────────────────────────────────────

/**
 * ChannelBackgroundMusic.kt — Preview 2006.4.1.0.21
 *
 * Funciones de extensión de LiveDiscoveryKids: música de fondo, activa
 * únicamente durante la reproducción de programas. Usa un MediaPlayer
 * independiente del VideoView para poder pausar/reanudar la música sin
 * afectar el video principal.
 *
 * Reorganización 4.1.0.21 — código movido tal cual desde LiveDiscoveryKids.kt.
 * Sin cambios de comportamiento.
 */

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
internal fun LiveDiscoveryKids.startBgMusic() {
    if (!SettingsManager.isBgMusicEnabled(this)) {  // Preview 2006.4.1.0.11
        Log.d(LiveDiscoveryKids.TAG, "BG Music SKIPPED – deshabilitada en Configuración")
        return
    }
    if (bgPlayer == null) {
        bgPlayer = MediaPlayer.create(this, R.raw.bg_music)?.apply {
            isLooping = false
            setVolume(0.08f, 0.08f)
            setOnCompletionListener { mp ->
                mp.seekTo(0)
                mp.start()
                Log.d(LiveDiscoveryKids.TAG, "BG Music LOOP (gapless restart)")
            }
            start()
            Log.d(LiveDiscoveryKids.TAG, "BG Music STARTED")
        }
    } else if (bgPlayer?.isPlaying == false) {
        bgPlayer?.start()
        Log.d(LiveDiscoveryKids.TAG, "BG Music RESUMED")
    }
}

/**
 * Detiene y libera el MediaPlayer de música de fondo.
 * Llamar en bumpers, comerciales y al destruir la Activity.
 */
internal fun LiveDiscoveryKids.stopBgMusic() {
    bgPlayer?.let {
        if (it.isPlaying) it.stop()
        it.release()
        Log.d(LiveDiscoveryKids.TAG, "BG Music STOPPED")
    }
    bgPlayer = null
}



// ── Antes en ChannelSessionState.kt ────────────────────────────────────────

/**
 * ChannelSessionState.kt — Preview 2006.4.1.0.21
 *
 * Funciones de extensión de LiveDiscoveryKids: arranque del canal, y
 * persistencia de sesión al cerrar la app (guardar/restaurar estado en
 * SharedPreferences, diálogo de "¿Continuar donde estabas?" y diálogo de
 * confirmación de salida).
 *
 * startChannel() decide entre arrancar desde cero o mostrar el diálogo de
 * reanudación; el resto de la lógica del playlist driver (advance(), etc.)
 * vive en ChannelPlaylist.kt.
 *
 * Reorganización 4.1.0.21 — código movido tal cual desde LiveDiscoveryKids.kt.
 * Sin cambios de comportamiento.
 */

// ══════════════════════════════════════════════════════════════════════════
// Arranque del canal
// ══════════════════════════════════════════════════════════════════════════

internal fun LiveDiscoveryKids.startChannel() {
    val prefs = getSharedPreferences(LiveDiscoveryKids.PREFS_NAME, Context.MODE_PRIVATE)
    if (prefs.getBoolean(LiveDiscoveryKids.PREF_HAS_STATE, false)) {
        // Hay sesión guardada → preguntar al usuario
        showResumeDialog(prefs)
    } else {
        // Sin sesión → arrancar desde el principio
        playlistIndex = 0
        advance()
    }
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
internal fun LiveDiscoveryKids.saveChannelState() {
    val posToSave = when {
        isInCommercialBlock -> commercialResumeMs
        else                -> pausedPositionMs
    }
    val breakQueueStr = breakQueue.joinToString(",")

    getSharedPreferences(LiveDiscoveryKids.PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
        putBoolean(LiveDiscoveryKids.PREF_HAS_STATE,     true)
        putInt    (LiveDiscoveryKids.PREF_PLAYLIST_IDX,  playlistIndex)
        putInt    (LiveDiscoveryKids.PREF_POSITION_MS,   posToSave)
        putInt    (LiveDiscoveryKids.PREF_PROGRAM_IDX,   currentProgramIndex)
        putString (LiveDiscoveryKids.PREF_ITEM_TYPE,     currentItemType)
        putInt    (LiveDiscoveryKids.PREF_COMMERCIAL_MS, commercialResumeMs)
        putInt    (LiveDiscoveryKids.PREF_SCREENBUG_RES, currentScreenBugRes)
        putString (LiveDiscoveryKids.PREF_BREAK_QUEUE,   breakQueueStr)
        putBoolean(LiveDiscoveryKids.PREF_HAS_PLAYED_PROGRAM, hasPlayedAnyProgram)   // Release 4.3.1
        apply()
    }
    Log.d(LiveDiscoveryKids.TAG, "Estado guardado: type=$currentItemType pos=${posToSave}ms breaks=$breakQueueStr")
}

/**
 * Muestra un AlertDialog preguntando si el usuario quiere continuar
 * donde estaba o empezar desde el principio.
 *
 * El mensaje describe qué estaba reproduciendo para que el usuario
 * pueda decidir con contexto.
 */
internal fun LiveDiscoveryKids.showResumeDialog(prefs: SharedPreferences) {
    val itemType     = prefs.getString(LiveDiscoveryKids.PREF_ITEM_TYPE, "bumper") ?: "bumper"
    val posMs        = prefs.getInt(LiveDiscoveryKids.PREF_POSITION_MS, 0)
    val progIdx      = prefs.getInt(LiveDiscoveryKids.PREF_PROGRAM_IDX, 0)
    val plIdx        = prefs.getInt(LiveDiscoveryKids.PREF_PLAYLIST_IDX, 0)
    val commMs       = prefs.getInt(LiveDiscoveryKids.PREF_COMMERCIAL_MS, 0)
    val screenbugRes = prefs.getInt(LiveDiscoveryKids.PREF_SCREENBUG_RES, R.drawable.screenbug)
    val breakQueueStr = prefs.getString(LiveDiscoveryKids.PREF_BREAK_QUEUE, "") ?: ""
    // Release 4.3.1 — si el estado guardado viene de una versión anterior sin
    // esta clave, se infiere a partir del tipo de ítem: si estaba en medio de
    // un programa o un comercial, necesariamente ya salió al aire un programa.
    val hasPlayedProgram = prefs.getBoolean(LiveDiscoveryKids.PREF_HAS_PLAYED_PROGRAM, itemType == "program" || itemType == "commercial")

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
            resumeSavedState(itemType, plIdx, progIdx, posMs, commMs, screenbugRes, breakQueueStr, hasPlayedProgram, prefs)
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
internal fun LiveDiscoveryKids.resumeSavedState(
    itemType: String,
    plIdx: Int,
    progIdx: Int,
    posMs: Int,
    commMs: Int,
    screenbugRes: Int,
    breakQueueStr: String,
    hasPlayedProgram: Boolean,
    prefs: SharedPreferences
) {
    clearSavedState()
    playlistIndex       = plIdx
    currentProgramIndex = progIdx
    currentScreenBugRes = screenbugRes
    hasPlayedAnyProgram = hasPlayedProgram   // Release 4.3.1

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
                Log.d(LiveDiscoveryKids.TAG, "Restaurando programa en ${posMs}ms, breaks pendientes: $breakQueue")
                beginProgramSegment(uri, startOffsetMs = posMs, isFirstPlay = false)
            } else {
                Log.w(LiveDiscoveryKids.TAG, "Restauración: pro${progIdx+1}.mp4 no encontrado, avanzando")
                playlistIndex = 0
                advance()
            }
        }
        "commercial" -> {
            val uri = resolveProgram(progIdx)
            if (uri != null) {
                currentProgramUri = uri
                breakQueue = restoredBreaks
                Log.d(LiveDiscoveryKids.TAG, "Restaurando post-comercial en ${commMs}ms, breaks pendientes: $breakQueue")
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
internal fun LiveDiscoveryKids.clearSavedState() {
    getSharedPreferences(LiveDiscoveryKids.PREFS_NAME, Context.MODE_PRIVATE).edit()
        .remove(LiveDiscoveryKids.PREF_HAS_STATE)
        .apply()
    Log.d(LiveDiscoveryKids.TAG, "Estado guardado borrado")
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
internal fun LiveDiscoveryKids.showExitConfirmationDialog() {
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
                scheduleSegmentLogic(pausedPositionMs, isNewSegment = false)
                Log.d(LiveDiscoveryKids.TAG, "Exit cancelled – resuming from ${pausedPositionMs}ms")
            }
        }
        .show()
}



// ── Antes en ChannelPositionTracker.kt ────────────────────────────────────────

/**
 * ChannelPositionTracker.kt — Preview 2006.4.1.0.21
 *
 * Funciones de extensión de LiveDiscoveryKids: position tracker (guarda la
 * posición del video cada 16 ms) y los helpers de scheduling de tareas
 * diferidas (post/cancelAllTasks) usados en todo el resto del flujo del canal.
 *
 * Reorganización 4.1.0.21 — código movido tal cual desde LiveDiscoveryKids.kt
 * (10 semanas desde el primer release, ver CHANGELOG). Sin cambios de
 * comportamiento: mismo estado de instancia, mismas funciones, solo
 * agrupadas por responsabilidad en su propio archivo.
 */

// ══════════════════════════════════════════════════════════════════════════
// Position tracker – guarda la posición del video cada 500 ms
// Resuelve el bug donde videoView.currentPosition devuelve 0 en onPause
// porque Android ya pausó el VideoView antes de llamar al callback.
// ══════════════════════════════════════════════════════════════════════════

/** Inicia el guardado continuo de posición. Llamar al arrancar un segmento de programa. */
internal fun LiveDiscoveryKids.startPositionTracker() {
    positionTrackerHandler.removeCallbacksAndMessages(null)
    positionTrackerHandler.post(positionTrackerRunnable)
    Log.d(LiveDiscoveryKids.TAG, "PositionTracker STARTED")
}

/** Detiene el guardado continuo de posición. Llamar en bumpers, comerciales y onPause. */
internal fun LiveDiscoveryKids.stopPositionTracker() {
    positionTrackerHandler.removeCallbacksAndMessages(null)
    val loggedPos = if (isInProgramSegment) pausedPositionMs else currentClipPositionMs
    Log.d(LiveDiscoveryKids.TAG, "PositionTracker STOPPED at ${loggedPos}ms (tipo=$currentItemType)")
}

// ══════════════════════════════════════════════════════════════════════════
// Task scheduling helpers
// ══════════════════════════════════════════════════════════════════════════

internal fun LiveDiscoveryKids.post(delayMs: Long, action: () -> Unit) {
    val r = Runnable(action)
    pendingTasks += r
    handler.postDelayed(r, delayMs)
}

internal fun LiveDiscoveryKids.cancelAllTasks() {
    pendingTasks.forEach { handler.removeCallbacks(it) }
    pendingTasks.clear()
}



// ── Antes en ChannelScreenBug.kt ────────────────────────────────────────

/**
 * ChannelScreenBug.kt — Preview 2006.4.1.0.21
 *
 * Funciones de extensión de LiveDiscoveryKids: animación de aparición/
 * desaparición del Screenbug (logo superpuesto). El cálculo de CUÁNDO
 * mostrarlo/ocultarlo vive en ChannelProgramPlayback.kt (scheduleSegmentLogic);
 * aquí solo está el cómo (fade in/out con Canvas+ViewPropertyAnimator).
 *
 * Reorganización 4.1.0.21 — código movido tal cual desde LiveDiscoveryKids.kt.
 * Sin cambios de comportamiento.
 */

// ══════════════════════════════════════════════════════════════════════════
// ScreenBug animation
// ══════════════════════════════════════════════════════════════════════════

internal fun LiveDiscoveryKids.fadeInBug() {
    Log.d(LiveDiscoveryKids.TAG, "ScreenBug FADE IN [res=$currentScreenBugRes]")
    screenBug.setImageResource(currentScreenBugRes)
    screenBug.animate()
        .alpha(1f)
        .setDuration(LiveDiscoveryKids.FADE_MS)
        .start()
}

internal fun LiveDiscoveryKids.fadeOutBug() {
    Log.d(LiveDiscoveryKids.TAG, "ScreenBug FADE OUT")
    screenBug.animate()
        .alpha(0f)
        .setDuration(LiveDiscoveryKids.FADE_MS)
        .start()
}

/** Instantly sets alpha without animation (used during transitions). */
internal fun LiveDiscoveryKids.setBugAlpha(alpha: Float) {
    screenBug.animate().cancel()
    screenBug.alpha = alpha
}



// ── Antes en ChannelUiHelpers.kt ────────────────────────────────────────

/**
 * ChannelUiHelpers.kt — Preview 2006.4.1.0.21
 *
 * Funciones de extensión de LiveDiscoveryKids: helpers de UI que no son
 * parte del flujo de reproducción del canal en sí — mostrar/ocultar los
 * botones de navegación (Prev/Next/Settings) al tocar la pantalla, pedir
 * el permiso de almacenamiento necesario para leer los programas, y forzar
 * pantalla completa (oculta status bar y nav bar del sistema).
 *
 * Los overrides de ciclo de vida que disparan estos helpers
 * (dispatchTouchEvent, onRequestPermissionsResult) permanecen en
 * LiveDiscoveryKids.kt porque son métodos de Activity, no funciones de
 * extensión.
 *
 * Reorganización 4.1.0.21 — código movido tal cual desde LiveDiscoveryKids.kt.
 * Sin cambios de comportamiento.
 */

// ══════════════════════════════════════════════════════════════════════════
// Toque de pantalla → mostrar / ocultar botones de navegación
// ══════════════════════════════════════════════════════════════════════════

/** Hace visibles los botones y programa su ocultado a los 3 segundos. */
internal fun LiveDiscoveryKids.showNavButtons() {
    prevButton.visibility = View.VISIBLE
    nextButton.visibility = View.VISIBLE
    settingsButton.visibility = View.VISIBLE
    resetNavHideTimer()
}

/** Cancela el temporizador anterior y lo reinicia desde cero (3 s). */
internal fun LiveDiscoveryKids.resetNavHideTimer() {
    navHideHandler.removeCallbacksAndMessages(null)
    navHideHandler.postDelayed({
        prevButton.visibility = View.GONE
        nextButton.visibility = View.GONE
        settingsButton.visibility = View.GONE
    }, 3_000L)
}

// ══════════════════════════════════════════════════════════════════════════
// Permissions
// ══════════════════════════════════════════════════════════════════════════

internal fun LiveDiscoveryKids.requestStoragePermission() {
    val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        android.Manifest.permission.READ_MEDIA_VIDEO
    else
        android.Manifest.permission.READ_EXTERNAL_STORAGE

    if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) {
        startChannel()
    } else {
        ActivityCompat.requestPermissions(this, arrayOf(perm), LiveDiscoveryKids.PERM_REQUEST)
    }
}

// ══════════════════════════════════════════════════════════════════════════
// Fullscreen
// ══════════════════════════════════════════════════════════════════════════

@Suppress("DEPRECATION")
internal fun LiveDiscoveryKids.goFullscreen() {
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



// ── Antes en ChannelDebugOverlay.kt ────────────────────────────────────────

/**
 * ChannelDebugOverlay.kt — Preview 2006.4.1.0.21
 *
 * Funciones de extensión de LiveDiscoveryKids: aplicación de las opciones
 * de Configuración sobre el estado en vivo del canal (applySettings), y el
 * overlay de debug que se muestra automáticamente en builds Preview
 * (versión, FPS, RAM disponible) y el texto de versión visible siempre.
 *
 * Reorganización 4.1.0.21 — código movido tal cual desde LiveDiscoveryKids.kt.
 * Sin cambios de comportamiento.
 */

// ══════════════════════════════════════════════════════════════════════════
// Configuración – Preview 2006.4.1.0.12
// ══════════════════════════════════════════════════════════════════════════

/**
 * Aplica las opciones guardadas en SettingsManager. Se llama en onCreate()
 * (antes de mostrar nada) y en onResume() (por si el usuario las cambió
 * en SettingsActivity y volvió). Música se resuelve sola en su próximo
 * ciclo (startBgMusic ya consulta SettingsManager directamente).
 *
 * Cambios Preview 4.1.0.12:
 *   - El modo debug ya NO es configurable: setupDebugInfo() es incondicional
 *     de nuevo (se muestra automático en builds Preview).
 *   - crtOverlay.effectEnabled reemplaza a brightnessMultiplier (antes slider
 *     0–100%, ahora on/off).
 *   - bugShowDelayMs y breakIntervalMin/MaxMs se leen de SettingsManager en
 *     lugar de ser const val fijas.
 *   - Forzar 4:3: controla los layoutParams del VideoView (no del contenedor).
 *     OFF (default) → match_parent (alto) / match_parent (ancho): el VideoView
 *     respeta su proporción real dentro del marco 4:3.
 *     ON → match_parent (alto) / wrap_content (ancho): el video se estira
 *     para llenar el ancho del marco 4:3 (comportamiento histórico).
 */
internal fun LiveDiscoveryKids.applySettings() {
    crtOverlay.effectEnabled = SettingsManager.isCrtEffectEnabled(this)
    bugShowDelayMs = SettingsManager.getScreenbugDelaySec(this) * 1_000L
    breakIntervalMinMs = SettingsManager.getCommercialMinMinutes(this) * 60 * 1_000L
    breakIntervalMaxMs = SettingsManager.getCommercialMaxMinutes(this) * 60 * 1_000L

    val params = videoView.layoutParams as FrameLayout.LayoutParams
    params.height = FrameLayout.LayoutParams.MATCH_PARENT
    params.width = if (SettingsManager.isForceAspectRatioEnabled(this)) {
        FrameLayout.LayoutParams.WRAP_CONTENT
    } else {
        FrameLayout.LayoutParams.MATCH_PARENT
    }
    videoView.layoutParams = params
}

//Modo Debug solo en Preview
internal fun LiveDiscoveryKids.setupDebugInfo() {
    debugTextView = findViewById(R.id.debugInfo)
    debugTextView.visibility = View.VISIBLE

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

internal fun LiveDiscoveryKids.startRamMonitor(
    versionName: String?,
    versionCode: Long,
    androidVersion: String,
    apiName: String,
    sdkInt: Int,
    manufacturer: String,
    model: String
) {
    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    val updateTask = object : Runnable {
        override fun run() {
            val memInfo = ActivityManager.MemoryInfo()
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

internal fun LiveDiscoveryKids.displayInfo() {
    versionInfo = findViewById(R.id.versionInfo)

    val packageInfo = packageManager.getPackageInfo(packageName, 0)
    val versionName = packageInfo.versionName
    val versionInfoText = "$versionName"

    versionInfo.text = versionInfoText
}

