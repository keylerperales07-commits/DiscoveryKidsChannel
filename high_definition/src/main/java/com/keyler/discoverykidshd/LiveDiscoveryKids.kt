/*
 ESTO TIENE LICENCIA GNU LICENSE APACHE
 
 NO ELIMINAR NINGUN COMENTARIO AGREGADO
*/
package com.keyler.discoverykidshd

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
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.keyler.discoverykidshd.R
import java.io.File

/**
 * Preview 2013.6.0.0.2
 *
 * Discovery Kids - TV Simulator • Era Doki 1.0 (2005–2009) • Preview Era 2006
 *
 * Playlist sequence (Intro/Programa/Créditos: corte directo, sin FadeOut/FadeIn
 * — ver Preview 2013.6.0.0.2 más abajo):
 *   [Intro] → Programa → [Créditos] → [Intro] → Programa → [Créditos] → ...
 *   (Intro/Créditos son opcionales por programa — ver hasValidIntro()/hasValidCreditos())
 *
 * Preview 2013.6.0.0.1 — EL BUMPER YA NO SALE DE PRIMERO: se quitó
 * PlayItem.Bumper del ciclo de arriba. Ahora el Bumper se inserta
 * aleatoriamente DENTRO de cada corte comercial, en uno de dos puntos
 * posibles (sorteado por corte): antes del comercial, o después del
 * comercial/antes del post-comercial — ver commercialBumperBeforeComercial /
 * playCommercial() / playCommercialBumper().
 *
 * Preview 2013.6.0.0.2 — SE ELIMINÓ POR COMPLETO "ya_regresa" (el clip
 * pre-comercial, determinístico por programa): el corte comercial ya no
 * arranca con un aviso propio, corta directo del Programa al Bumper/comercial.
 * El continuamos (post-comercial) ya NO se empareja con el ya_regresa de cada
 * programa: ahora depende de la hora real del dispositivo al momento del
 * corte (00:00–11:59 → continuamos_mananera1, 12:00–23:59 → continuamos_tardia1
 * — ver continuamosResForCurrentTime()). FadeOut/FadeIn quedan reservados
 * EXCLUSIVAMENTE al límite Programa↔bloque comercial: el Programa se apaga
 * (FadeOut) justo antes de cortar al bloque, y se enciende (FadeIn) al
 * retomarse justo después — TODO lo demás (Bumper, Intro, Créditos, y los
 * clips dentro del bloque comercial: comercial y continuamos) corta en seco.
 * Ver playCommercial() / beginProgramSegment() / playUriHardCut().
 *
 * Preview 2010.5.4.0.40 — REEMPLAZO DE ENSEGUIDA: el clip "enseguida" post-
 * programa (que antes ocupaba un ítem propio del playlist) se elimina. En su
 * lugar, "nextprogram" es un GIF que se superpone AL PROGRAMA MISMO cerca de
 * su final — ver NEXTPROGRAM_SHOW_BEFORE_MS / scheduleNextProgramBug().
 *
 * Release 5.4.0 — ELIMINACIÓN DE STANDALONECOMMERCIAL: los comerciales ya no
 * tienen un ítem propio del playlist entre Bumper y Programa; ahora SOLO
 * aparecen interrumpiendo un programa en curso (playCommercial/calcBreaks,
 * sin cambios). NUEVO: Intro y Créditos, opcionales por programa (activados
 * y elegidos por el usuario en Configuración de Programa, Discovery Kids
 * Launcher) — ver playIntro()/playCreditos()/resolveIntroUri()/resolveCreditosUri().
 *
 * Programs (pro1..pro4.mp4) are read from the user's Movies folder.
 * Bumpers (bumper.mp4–bumper5.mp4) son aleatorios, sin repetir el mismo dos veces seguidas.
 * Los comerciales dentro del programa: 4 comerciales (comercial1–4.mp4), aleatorios sin
 *   repetir el mismo dos veces seguidas. comercial1/comercial2 = Era 2006 (Preview 4.1.0.10);
 *   comercial3/comercial4 agregados en esta Preview (4.1.0.11).
 * Commercial scheduling: 1 break per every 3–9 minutes of program content, at random intervals.
 * Missing programs are skipped automatically.
 *
 * Configuración (Preview 4.1.0.12): SettingsActivity, accesible desde el botón ⚙️,
 * permite ajustar la duración del Screenbug y el intervalo aleatorio de
 * comerciales. Ver SettingsManager.
 * (El modo debug ya no es configurable: vuelve a ser automático en builds Preview).
 *
 * Release 2006.4.1.1 — BUG FIX: el Screenbug y los clips no-programa (bumper,
 * comercial) se reiniciaban al volver de segundo plano o de un
 * cambio de Activity. Ver scheduleSegmentLogic(), resumeUriHardCut() y
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
    // Release 2009.5.0.0: VideoView → DkVideoView (envoltorio propio, ver
    // DkVideoView.kt). Release 2009.5.2.1: el motor alternativo basado en
    // TextureView que tenía DkVideoView se eliminó por completo — vuelve a
    // ser un simple envoltorio de VideoView clásico (se mantiene la misma
    // API usada acá abajo: setVideoURI, seekTo, start, pause, stopPlayback,
    // isPlaying, currentPosition, setOnPreparedListener, setOnCompletionListener).
    internal lateinit var videoView: DkVideoView
    // Discovery Kids HD — videoContainer es un FrameLayout simple, SIN
    // ninguna proporción forzada (a diferencia de la app normal, que usa
    // AspectRatioFrameLayout para mantener el marco 4:3 clásico). Esta
    // variante HD no tiene ese concepto: el video ocupa la pantalla
    // completa, respetando su proporción real tal cual la reporte el
    // dispositivo. AspectRatioFrameLayout.kt NO existe en este proyecto.
    internal lateinit var videoContainer: FrameLayout
    internal lateinit var screenBug: ImageView
    // Release 5.8.0 — banner de clasificación (clasif_banner.gif), se
    // muestra brevemente al arrancar un programa nuevo.
    internal lateinit var clasifBanner: ImageView
    internal var clasifBannerGif: GifMovieDrawable? = null
    internal lateinit var versionInfo: TextView
    internal lateinit var debugTextView: TextView
    internal lateinit var prevButton: ImageButton
    internal lateinit var nextButton: ImageButton
    internal lateinit var settingsButton: ImageButton  // Preview 2006.4.1.0.11

    // ── Configuración (Preview 2006.4.1.0.12) ───────────────────────────────────
    // Antes eran `const val` en companion object; ahora son configurables desde
    // SettingsActivity, así que pasan a ser propiedades de instancia. Se inicializan
    // con el valor por defecto y se sobreescriben en applySettings() (onCreate/onResume).
    internal var bugShowDelayMs: Long = SettingsManager.DEFAULT_SCREENBUG_DELAY_SEC * 1_000L
    internal var breakIntervalMinMs: Long = SettingsManager.DEFAULT_COMMERCIAL_MIN_MINUTES * 60 * 1_000L
    internal var breakIntervalMaxMs: Long = SettingsManager.DEFAULT_COMMERCIAL_MAX_MINUTES * 60 * 1_000L


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
    //   TODO LO DEMÁS (bumper, comercial, continuamos) → desde Release
    //     2006.4.1.1 también se reanudan en su posición real (currentClipUri/
    //     currentClipPositionMs, ver más abajo), no se reinician. Ver
    //     resumeUriHardCut() y resumeCommercialBlock().
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
    // reconstruir exactamente dónde estábamos al reanudar.
    //
    // Preview 2013.6.0.0.2 — SE ELIMINÓ POR COMPLETO "ya_regresa" (el clip
    // pre-comercial): el bloque ya no arranca con un aviso propio, corta
    // directo del Programa al Bumper/comercial. CommercialStep.PRE_COMERCIAL
    // se elimina; el bloque ahora encadena 2 clips fijos (comercial →
    // continuamos), más el Bumper que le haya tocado a este corte en uno de
    // los dos puntos posibles (ver commercialBumperBeforeComercial).
    internal enum class CommercialStep { BUMPER_BEFORE_COMERCIAL, COMERCIAL, BUMPER_AFTER_COMERCIAL, POST_COMERCIAL }
    internal var commercialStep: CommercialStep        = CommercialStep.COMERCIAL
    // Release 2009.5.0.0 — Uri (no solo Int) porque continuamos puede ser un
    // video personalizado elegido por el usuario (ver resolveContinuamosUri()),
    // no solo un recurso empaquetado. commercialChosenCommercial sigue siendo
    // Int: el comercial en sí no es configurable por programa.
    internal var commercialChosenCommercial: Int        = -1
    internal var commercialChosenYaVolvemos: Uri?        = null
    // Preview 2013.6.0.0.1 — el Bumper ya no es el primer ítem del ciclo (ver
    // buildPlaylist()); ahora se inserta aleatoriamente DENTRO del corte
    // comercial, en uno de dos puntos posibles. commercialBumperBeforeComercial
    // se sortea una sola vez por corte, al arrancar playCommercial(), y decide
    // cuál de los dos puntos se usa en ESE corte:
    //   true  → al principio del corte, antes del comercial (antes: después
    //           del pre-comercial "ya_regresa", eliminado en la 2013.6.0.0.2).
    //   false → después del comercial, antes del post-comercial (continuamos).
    // commercialChosenBumperRes guarda el bumper elegido para ese corte, para
    // que resumeCommercialBlock() pueda reanudar el mismo clip si la app pasa
    // a segundo plano justo durante ese bumper.
    internal var commercialBumperBeforeComercial: Boolean = true
    internal var commercialChosenBumperRes: Int          = -1

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
        data class Intro(val programIndex: Int) : PlayItem()      // Release 5.4.0 — opcional, por programa
        data class Program(val index: Int) : PlayItem()   // 0-based → pro(n+1).mp4
        data class Creditos(val programIndex: Int) : PlayItem()   // Release 5.4.0 — opcional, por programa
    }

    // Release 2009.5.0.0 — antes era un `val` fijo de 4 programas. Ahora se
    // arma en onCreate() vía buildPlaylist(): se repite el ciclo
    // [Intro]→Programa→[Créditos] una vez por cada uno de los N programas
    // que eligió el usuario en Discovery Kids Launcher (SettingsManager.getProgramCount(),
    // 1–24 — RELEASE 2013.6.0.0: ya no depende de "Funciones experimentales",
    // ELIMINADO por completo). Intro/Créditos son opcionales — solo se
    // incluyen si el usuario los activó Y eligió un video (ver
    // hasValidIntro()/hasValidCreditos()).
    // Release 5.4.0: se quitó StandaloneCommercial del ciclo — los comerciales
    // ahora solo aparecen DENTRO de los programas (ver playCommercial()).
    internal var playlist: List<PlayItem> = emptyList()

    // Release 5.4.0 — BUG FIX: cantidad de programas con la que se construyó
    // `playlist` la última vez. Si LiveDiscoveryKids queda vivo en segundo
    // plano (el usuario vuelve al Launcher con el botón Atrás sin cerrar esta
    // Activity, cambia la cantidad de programas, y vuelve a esta misma
    // instancia por Recientes en vez de crear una nueva) `playlist` queda
    // armado con la cantidad VIEJA para siempre, porque onCreate() —donde se
    // llama buildPlaylist()— no se vuelve a ejecutar. Se compara contra
    // totalProgramCount() en onResume() y se reconstruye si cambió.
    internal var playlistBuiltForCount = -1

    internal var playlistIndex = 0
    internal var currentProgramIndex = 0
    // Release 5.8.0 — Episodios de Programa: qué episodio (0-based) del
    // programa actual se está reproduciendo. Se resetea a 0 cada vez que
    // arranca un PROGRAMA nuevo (playProgram()) — NO al pasar de un
    // episodio al siguiente del mismo programa (eso lo maneja
    // resumeProgramAfterCommercial(), incrementándolo él mismo).
    internal var currentEpisodeIndex = 0

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
    // Release 5.5.0 — duración real (ms) de la última Intro reproducida.
    // Se captura en playIntro() (onPrepared) y se consume una sola vez en
    // scheduleSegmentLogic() del primer segmento del Programa siguiente,
    // para RESTAURAR la fase de ScreenBug correcta (no re-disparar el
    // inicio desde cero) — ver comentario largo en scheduleMultipleScreenbugs().
    internal var lastIntroDurationMs: Int = 0
    // Release 5.5.0 — true mientras la fase 2 (mid, PNG estático) del
    // ScreenBug está actualmente visible. Se usa para restaurarla de
    // inmediato al empezar los Créditos (que suprimen su propia fase 1/2)
    // en vez de que se quede oculta desde el corte Programa→Créditos hasta
    // que le toque a la fase 3 — ver scheduleCreditosOverlays().
    internal var screenBugMidVisible: Boolean = false
    internal var currentScreenBugRes: Int = R.drawable.screenbug
    // Release (fix build: D8/R8 crash) — GifMovieDrawable propio (Movie API
    // nativa), cacheado una vez para no re-decodificar el GIF cada vez.
    internal var screenBugStartGif: GifMovieDrawable? = null
    internal var screenBugEndGif: GifMovieDrawable? = null
    // Release 2010.5.3.0 — ScreenBug de Navidad (1–24 de diciembre), mismos
    // 3 drawables y mismo comportamiento que el normal, solo distinto arte.
    internal var screenBugStartNavidadGif: GifMovieDrawable? = null
    internal var screenBugEndNavidadGif: GifMovieDrawable? = null
    // Preview 2013.6.0.0.2 — GIF "próximo programa" (proximo_programa_screenbug.gif):
    // sustituye brevemente al screenbug.png durante el programa, a mitad de
    // cada segmento — ver PROXIMO_PROGRAMA_* y scheduleMultipleScreenbugs().
    internal var proximoProgramaScreenbugGif: GifMovieDrawable? = null

    // ── NextProgram (Preview 2010.5.4.0.40) ─────────────────────────────────────
    // Overlay GIF que reemplaza a los "enseguida" post-programa: aparece
    // superpuesto sobre el programa mismo, NEXTPROGRAM_SHOW_BEFORE_MS antes
    // de su final real, anticipando qué sigue. Uno de los 4 GIFs
    // (LiveDiscoveryKids.NEXTPROGRAMS) se cachea por adelantado, igual que
    // screenBugStartGif/screenBugEndGif, para que no haya lag al mostrarlo.
    internal lateinit var nextProgramBug: ImageView
    internal var nextProgramGifs: Array<GifMovieDrawable?> = arrayOfNulls(LiveDiscoveryKids.NEXTPROGRAMS.size)

    // ── Constants ──────────────────────────────────────────────────────────────
    companion object {
        internal const val TAG = "DKids"

        /** Screenbug hides this many ms before segment end or commercial start. */
        internal const val BUG_HIDE_EARLY = 20_000L

        // Release 2009.4.6.1 — NUEVO: sistema de 3 screenbug con animaciones secuenciales
        // durante el programa. Los 3 son GIFs o imágenes (screenbug_start.gif, screenbug.png,
        // screenbug_end.gif) que se muestran en momentos diferentes para dar más dinamismo.
        // Timings:
        //   screenbug_start (GIF): mostrar 20s después de iniciar, ocultar 5s después (asumir)
        //   screenbug (PNG): mostrar 15s después de que start se oculta (40s total), ocultar
        //     cuando aparece screenbug_end (programDuration - 20s)
        //   screenbug_end (GIF): mostrar 20s antes del final, ocultar al final del programa
        internal const val SCREENBUG_START_DELAY_MS = 20_000L
        // Release 5.4.0: 5s → 4,9s. El GIF de screenbug_start/screenbug_end dura
        // ~5s exactos; a los 5s justos el sistema a veces ya arrancó el loop del
        // GIF de nuevo antes de que el alpha llegue a 0, mostrándose un
        // "salto" de un frame del inicio del loop siguiente. Ocultarlo 100ms
        // antes evita ese salto.
        //
        // RELEASE 2014.6.1.0 (HD) — vuelve a 4,9s (quedó en 8s en algún punto
        // sin que el comentario se actualizara). Mismo margen anti-salto de
        // 100ms que ya usa SCREENBUG_END_VISIBLE_DURATION_MS, para el mismo
        // GIF de 5s.
        internal const val SCREENBUG_START_ESTIMATED_DURATION_MS = 4_500L
        internal const val SCREENBUG_MID_DELAY_AFTER_START_MS = 0L           // El PNG aparece inmediatamente al ocultarse screenbug_start (antes: 15s de espera)
        // Preview 2010.5.4.0.40: 20s → 46s antes del final, para dejar lugar
        // al nextprogram (aparece 15s después, a los 31s antes del final).
        internal const val SCREENBUG_END_SHOW_BEFORE_MS = 46_000L          // Mostrar screenbug_end 46s antes del final
        internal const val SCREENBUG_END_VISIBLE_DURATION_MS = 5_000L      // Se oculta 4,9s después de mostrarse (antes 5s → 4,9s en 5.4.0, ver comentario arriba)

        // Preview 2013.6.0.0.2 — NUEVO: GIF "próximo programa"
        // (proximo_programa_screenbug.gif). Sustituye brevemente al
        // screenbug.png durante el programa — ver scheduleMultipleScreenbugs().
        // Nunca antes de 1 min de arrancado el segmento, ni de forma que
        // termine mostrándose dentro del último minuto antes de un corte
        // comercial o del final del programa/episodio.
        internal const val PROXIMO_PROGRAMA_MIN_START_MS = 60_000L        // No antes de 1 min de iniciado el segmento
        internal const val PROXIMO_PROGRAMA_MIN_BEFORE_END_MS = 60_000L   // Tiene que terminar de mostrarse con 1 min de margen antes del final del segmento
        internal const val PROXIMO_PROGRAMA_VISIBLE_MS = 15_000L          // Visible 15s, luego vuelve el screenbug.png

        // Preview 2010.5.4.0.40 — NUEVO: overlay "nextprogram" (GIF), reemplaza
        // a los enseguida post-programa. Se superpone sobre el programa mismo
        // (no es un clip aparte) y SOLO se programa en el último segmento del
        // programa (sin cortes comerciales pendientes) — ver scheduleNextProgramBug().
        //
        // RELEASE 2013.6.0.0 — BUG FIX: NextProgram estaba apareciendo más
        // segundos antes de lo que correspondía. Se partió la constante única
        // en una por GIF, mismo criterio que ya usa showVideoInBox() para los
        // recuadros (ver isFactoryNextProgram2) — nextprogram2.gif ahora
        // aparece exactamente 30s antes del final, tal como se pidió.
        // nextprogram1.gif no estaba reportado como con problema, se deja en
        // el valor que ya tenía.
        internal const val NEXTPROGRAM1_SHOW_BEFORE_MS = 30_000L  // Aparece 30,7s antes del final real del programa
        internal const val NEXTPROGRAM2_SHOW_BEFORE_MS = 30_000L  // Aparece 30s antes del final real del programa (RELEASE 2013.6.0.0)
        internal const val NEXTPROGRAM_ANIM_MS = 500L              // Duración del fade-in (imagen de referencia enviada por Keyler)

        /** No commercial break is scheduled within this many ms of the program end. */
        internal const val BREAK_CUTOFF_MS = 3 * 60 * 1_000L         // 3 min

        /** Programs shorter than this have zero commercial breaks. */
        internal const val MIN_DURATION_FOR_BREAKS = 3 * 60 * 1_000L  // 3 min

        /** Alpha-animation duration for screenbug fade. */
        internal const val FADE_MS = 500L

        /** FadeOut duration for video transitions (ms). Applied before every video change. Release 3.3.0: unified to 500 ms for all clip types. */
        internal const val TRANSITION_FADE_OUT_MS = 500L
        // Release 5.8.0 — banner de clasificación: cuánto queda visible
        // (a pantalla completa, antes de empezar el fade out) al arrancar un
        // programa. clasif_banner.gif dura 17s — se deja el banner puesto
        // ese tiempo exacto para que la animación se vea completa.
        internal const val CLASIF_BANNER_VISIBLE_MS = 17_000L

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
        // BUG FIX (2009.5.1.0) — "el logo se reinicia al cambiar de activity o
        // volver de segundo plano": currentSegmentStartMs vivía solo en memoria
        // y se perdía si el proceso se recreaba (común en boxes de Android TV
        // con poca RAM). Al restaurar sesión, beginProgramSegment() usaba su
        // default isNewSegment=true → elapsed se recalculaba en 0 → el ciclo
        // completo de 3 fases del ScreenBug arrancaba de cero, fuera de lugar.
        // Ahora se persiste el punto real donde arrancó el segmento.
        internal const val PREF_SEGMENT_START_MS = "segment_start_ms"
        internal const val PREF_HAS_PLAYED_PROGRAM = "has_played_program"   // Release 4.3.1

        /** Lista de comerciales disponibles; se elige uno al azar en cada corte. */
        internal val COMMERCIALS = listOf(R.raw.comercial1hd, R.raw.comercial2hd, R.raw.comercial3hd, R.raw.comercial4hd)

        /**
         * Lista de bumpers disponibles.
         * Se elige uno al azar antes de cada programa, evitando repetir el mismo dos veces seguidas.
         * Beta 2005.4.0.0.4: bumper6 reemplazado por nuevo bumper de aviso de la Era Doki
         * (Actualización La Era Doki / nuevo Discovery Kids).
         */
        internal val BUMPERS = listOf(
            R.raw.bumper1hd,
            R.raw.bumper2hd,
            R.raw.bumper3hd,
            R.raw.bumper4hd,
            R.raw.bumper5hd
            
        )

        /**
         * Preview 2010.5.4.0.40 — GIFs "nextprogram", uno por programa.
         * Reemplazan a los ENSEGUIDAS_POST_PROGRAMA (clip aparte, eliminados):
         * en vez de un ítem propio del playlist, ahora es un overlay que se
         * superpone al programa mismo cerca de su final.
         *
         * Asignación determinística por índice de programa (0-based); se
         * indexa directamente por currentProgramIndex (con módulo, ver
         * scheduleNextProgramBug()):
         *   programa 0 (pro1) → nextprogram1 | programa 1 (pro2) → nextprogram2
         *   programa 2 (pro3) → nextprogram3 | programa 3 (pro4) → nextprogram4
         */
        internal val NEXTPROGRAMS = listOf(
            R.drawable.nextprogram1,
            R.drawable.nextprogram2,
            R.drawable.nextprogram1,
            R.drawable.nextprogram2
        )

        /**
         * Preview 2013.6.0.0.2 — NUEVO: el continuamos (post-comercial) ya no
         * se asigna por programa ni se empareja con un ya_regresa (ELIMINADO
         * por completo, ver CommercialStep). Ahora depende únicamente de la
         * hora real del dispositivo al momento del corte:
         *   continuamos_mananera1 → 00:00 a 11:59
         *   continuamos_tardia1   → 12:00 a 23:59
         * Ver continuamosResForCurrentTime() / resolveContinuamosUri().
         *
         * NOTA: los nombres de recurso no llevan tilde/ñ (Android no permite
         * esos caracteres en res/raw) — "mananera" en vez de "mañanera".
         */
        internal fun continuamosResForCurrentTime(): Int {
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            return if (hour in 0..11) R.raw.continuamos1_mananerahd else R.raw.continuamos1_tardiahd
        }
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

        // Release 2009.5.0.0: el DkVideoView se crea en código y se agrega al
        // placeholder que dejó activity_main.xml — ver DkVideoView.kt.
        // Release 2009.5.2.1: ya no hay motor alternativo que elegir (se
        // eliminó TextureView por completo), así que se instancia directo.
        videoContainer = findViewById(R.id.videoContainer)
        val videoViewContainer = findViewById<FrameLayout>(R.id.videoViewContainer)
        videoView = DkVideoView(this)
        videoViewContainer.addView(
            videoView,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT).apply {
                gravity = android.view.Gravity.CENTER
            }
        )

        // Release 2009.5.0.0 — playlist dinámica: la cantidad de programas la
        // elige el usuario en Discovery Kids Launcher (1–24, RELEASE 2013.6.0.0:
        // ya no depende de "Funciones experimentales", ELIMINADO por completo).
        playlist = buildPlaylist()
        playlistBuiltForCount = totalProgramCount()

        screenBug = findViewById(R.id.screenBug)
        screenBug.alpha = 0f
        preloadScreenBugAssets()  // PERF FIX: precarga los GIFs para que se muestren sin lag
        clasifBanner = findViewById(R.id.clasifBanner)
        clasifBanner.alpha = 0f
        preloadClasifBannerGif()
        nextProgramBug = findViewById(R.id.nextProgramBug)
        nextProgramBug.alpha = 0f
        preloadNextProgramGifs()  // Preview 2010.5.4.0.40: mismo motivo que preloadScreenBugAssets()
        prevButton = findViewById(R.id.btnPrevious)
        nextButton = findViewById(R.id.btnNext)
        settingsButton = findViewById(R.id.btnSettings)  // Preview 2006.4.1.0.11

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

        checkForUpdateOnLaunch()
    }

    /**
     * Release 2009.5.0.0 — NO es experimental. Al entrar a la app se consulta
     * en silencio si hay una versión nueva en GitHub (mismo AppUpdater.checkForUpdate()
     * que ya usaba SettingsActivity → "Buscar actualizaciones", incluyendo el
     * switch "Habilitar versiones Preview"). Si hay novedad, se muestra un
     * AlertDialog PROPIO — fuera de Configuración y de UpdateActivity — con la
     * opción de ir al Actualizador ahora o más tarde. Si no hay novedad, o si
     * falla la consulta (sin internet, etc.), no se muestra nada: nunca
     * interrumpe la reproducción con un error.
     */
    private fun checkForUpdateOnLaunch() {
        AppUpdater.checkForUpdate(this, object : AppUpdater.CheckCallback {
            override fun onUpToDate() { /* silencioso */ }
            override fun onError(message: String) { /* silencioso — nunca interrumpe el canal */ }
            override fun onUpdateAvailable(remoteVersion: String, apkUrl: String, releaseNotesUrl: String) {
                if (isFinishing || isDestroyed) return
                AlertDialog.Builder(this@LiveDiscoveryKids)
                    .setTitle("Nueva actualización disponible")
                    .setMessage("Hay una nueva versión ($remoteVersion) de Discovery Kids lista para instalar.")
                    .setPositiveButton("Actualizar") { _, _ ->
                        startActivity(Intent(this@LiveDiscoveryKids, UpdateActivity::class.java))
                    }
                    .setNegativeButton("Más tarde", null)
                    .setCancelable(true)
                    .show()
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
     * TODO LO DEMÁS (bumper, comercial, continuamos): desde Release 2006.4.1.1
     *   se retoma el MISMO clip en su posición real vía resumeUriHardCut()/
     *   resumeCommercialBlock(), en vez de reiniciar con advance().
     *
     * COMERCIAL EN CURSO: retoma el paso exacto del bloque (commercialStep,
     *   ver resumeCommercialBlock()) en su posición real.
     */
    override fun onResume() {
        super.onResume()

        // Preview 2006.4.1.0.12: refresca configuración por si el usuario volvió
        // de SettingsActivity con cambios (intervalo comerciales, screenbug
        // delay). Se hace siempre, sin importar pausedByLifecycle,
        // porque abrir SettingsActivity también pasa por onPause()/onResume()
        // aunque no sea un backgrounding real del sistema.
        applySettings()

        // Release 5.4.0 — BUG FIX (ver comentario de playlistBuiltForCount):
        // si la cantidad de programas cambió mientras esta instancia seguía
        // viva en segundo plano, el playlist queda desactualizado. No se
        // toca el clip que esté sonando ahora mismo — solo se reconstruye
        // `playlist` para que el PRÓXIMO advance() ya use la cantidad nueva.
        val currentCount = totalProgramCount()
        if (playlist.isNotEmpty() && playlistBuiltForCount != currentCount) {
            Log.w(TAG, "Cantidad de programas cambió en segundo plano (antes=$playlistBuiltForCount, ahora=$currentCount) — reconstruyendo playlist")
            playlist = buildPlaylist()
            playlistBuiltForCount = currentCount
            if (playlist.isNotEmpty()) playlistIndex = playlistIndex % playlist.size
        }

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
                // Release 2006.4.1.1 — BUG FIX: bumper / intro / créditos
                // se reiniciaban siempre desde el principio al volver de segundo plano.
                // Ahora se reanuda el mismo clip (currentClipUri) en la posición donde
                // quedó (currentClipPositionMs) en vez de llamar advance().
                // Preview 2013.6.0.0.2 — resumeUriHardCut() (sin FadeIn): estos
                // clips ya cortan en seco, ver comentario largo en playBumper().
                val uri = currentClipUri!!
                val onComplete = currentClipOnComplete
                if (onComplete == null) {
                    Log.e(TAG, "onResume: currentClipUri sin onComplete registrado – advance()")
                    advance(); return
                }
                Log.d(TAG, "onResume – reanudando clip tipo=$currentItemType en ${currentClipPositionMs}ms")
                // Release 5.4.0: si lo que se estaba reanudando eran los
                // Créditos, hay que volver a agendar la fase 3 del ScreenBug
                // + NextProgram con la duración real de este clip (recién
                // disponible en su propio onPrepared) y el elapsed correcto
                // — mismo patrón de "restaurar si ya debería estar visible"
                // que el resto del sistema, para que no se note el paso por
                // segundo plano.
                val onPrepared: ((Int) -> Unit)? = if (currentItemType == "creditos") {
                    { durationMs -> scheduleCreditosOverlays(durationMs, elapsed = currentClipPositionMs.toLong()) }
                } else null
                resumeUriHardCut(uri, currentClipPositionMs, onPrepared = onPrepared, onComplete = onComplete)
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
    //   ChannelPlaylist.kt          → advance, playBumper,
    //                                  playIntro, playCreditos,
    //                                  goToAdjacentProgram, findAvailableProgramIndex
    //   ChannelProgramPlayback.kt   → playProgram, beginProgramSegment,
    //                                  scheduleSegmentLogic, calcBreaks
    //   ChannelCommercialBlock.kt   → playCommercial, playCommercialStepComercial,
    //                                  playCommercialStepPostComercial, resumeCommercialBlock
    //   ChannelVideoTransitions.kt  → playUri, playUriHardCut, resumeUriHardCut
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
    //
    // Preview 2010.5.4.0.40 — BUG FIX (investigación a fondo): los 11
    // archivos de arriba en realidad NUNCA se habían borrado del disco en la
    // 4.6.0 pese a lo que dice este comentario — seguían presentes en el
    // proyecto con las mismas funciones duplicadas letra por letra que las
    // reunificadas acá, lo que impedía compilar (redeclaración). Se borraron
    // recién ahora.
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
 * playlist (advance()), los tipos de clip "de relleno" entre programas
 * (Bumper, Intro, Créditos), y la navegación Prev/Next que salta
 * directo entre programas.
 *
 * Preview 2010.5.4.0.40: se quitó Enseguida post-programa de este grupo —
 * ver nextprogram (overlay sobre el programa, no un clip aparte).
 * Release 5.4.0: se quitó StandaloneCommercial (los comerciales ahora solo
 * interrumpen programas en curso) y se agregaron Intro/Créditos.
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
        is LiveDiscoveryKids.PlayItem.Bumper   -> playBumper()
        is LiveDiscoveryKids.PlayItem.Intro    -> playIntro(item.programIndex)
        is LiveDiscoveryKids.PlayItem.Program  -> playProgram(item.index)
        is LiveDiscoveryKids.PlayItem.Creditos -> playCreditos(item.programIndex)
    }
}

// ══════════════════════════════════════════════════════════════════════════
// Bumper playback
// ══════════════════════════════════════════════════════════════════════════

internal fun LiveDiscoveryKids.playBumper() {
    cancelAllTasks()
    setBugAlpha(0f)
    setNextProgramBugAlpha(0f)
    isInProgramSegment = false
    currentItemType = "bumper"

    val candidates = LiveDiscoveryKids.BUMPERS.filter { it != lastBumperRes }.ifEmpty { LiveDiscoveryKids.BUMPERS }
    val chosenBumper = candidates.random()
    lastBumperRes = chosenBumper

    Log.d(LiveDiscoveryKids.TAG, "▶ BUMPER [res=$chosenBumper]")

    // Preview 2013.6.0.0.2 — corte directo (sin FadeOut/FadeIn), ver
    // playUriHardCut(). El FadeOut/FadeIn queda solo en el límite
    // Programa↔bloque comercial.
    playUriHardCut(rawUri(chosenBumper)) {
        playlistIndex++
        advance()
    }
}

// ══════════════════════════════════════════════════════════════════════════
// Intro / Créditos — Release 5.4.0
// Clips opcionales por programa, elegidos por el usuario en Configuración de
// Programa (Discovery Kids Launcher). Sin lógica de cortes comerciales (los
// comerciales solo interrumpen Programas).
//
// ScreenBug / NextProgram (ver comentario largo en scheduleMultipleScreenbugs()):
//   - La Intro SÍ dispara la fase 1/2 del ScreenBug (screenbug_start/mid) al
//     arrancar — es el inicio real del bloque Intro→Programa→[Créditos].
//   - Los Créditos SÍ disparan la fase 3 (screenbug_end) + NextProgram al
//     acercarse SU final — porque son el verdadero final del bloque cuando
//     están activos (si no hay Créditos, esto sigue pasando en el Programa,
//     sin cambios).
//   - Ninguna de las dos cuentas se reinicia al cambiar de clip: la de 20s
//     de la Intro sigue en el Programa si la Intro fue más corta, y la de
//     46s de los Créditos usa la duración real de los créditos, no la del
//     programa.
//
// Solo terminan al terminar su propio video (nunca se cortan antes) — igual
// que cualquier otro clip reproducido con playUriHardCut(). Solo se agregan al playlist
// (ver buildPlaylist()) si el usuario los activó Y ya eligió un video; si el
// usuario borró la selección DESPUÉS de armado el playlist de esta sesión,
// el fallback de abajo (uri == null) los saltea sin romper el ciclo, igual
// que un programa sin archivo.
// ══════════════════════════════════════════════════════════════════════════

internal fun LiveDiscoveryKids.playIntro(programIndex: Int) {
    cancelAllTasks()
    setBugAlpha(0f)
    setNextProgramBugAlpha(0f)
    isInProgramSegment  = false
    isInCommercialBlock = false
    currentItemType      = "intro"

    val uri = resolveIntroUri(programIndex)
    if (uri == null) {
        Log.w(LiveDiscoveryKids.TAG, "Intro del programa ${programIndex + 1} sin video válido – skipping")
        playlistIndex++
        advance()
        return
    }

    Log.d(LiveDiscoveryKids.TAG, "▶ INTRO [programa=${programIndex + 1}, uri=$uri]")

    // Preview 2013.6.0.0.3 — el banner de clasificación ahora se muestra acá
    // (al arrancar la Intro) en vez de al arrancar el Programa, cuando el
    // programa tiene Intro activada — ver playProgram(), que ya no lo
    // dispara en ese caso para no mostrarlo dos veces seguidas.
    showClasifBanner()

    // Release 5.4.0/5.4.1/5.5.0: el ScreenBug de inicio (fase 1/2) arranca
    // acá — se pasa la duración REAL de la Intro como segmentEndMs para que
    // el clamp interno de scheduleMultipleScreenbugs() garantice que se
    // muestre incluso en Intros cortas.
    //
    // BUG FIX 5.5.0 ("se reinicia y vuelve a mostrar screenbug de inicio
    // sabiendo que ya se mostró en el intro"): además, se guarda la
    // duración real en lastIntroDurationMs — scheduleSegmentLogic() la usa
    // para el primer segmento del Programa (startMidElapsed = elapsed +
    // lastIntroDurationMs, en vez de arrancar de 0), así el Programa
    // RESTAURA la fase que corresponda (mid si la Intro ya mostró y ocultó
    // el start, o directamente el resto del delay si la Intro fue corta)
    // en vez de re-disparar el start desde cero.
    // Preview 2013.6.0.0.2 — corte directo (sin FadeOut/FadeIn), ver
    // playUriHardCut() / comentario largo en playBumper().
    playUriHardCut(
        uri,
        onPrepared = { durationMs ->
            lastIntroDurationMs = durationMs
            scheduleMultipleScreenbugs(
                segmentStartMs = 0,
                segmentEndMs = durationMs,
                elapsed = 0L,
                suppressEndPhase = true,
                allowProximoPrograma = false
            )
        }
    ) {
        playlistIndex++
        advance()
    }
}

/**
 * Release 5.4.0 — agenda el ScreenBug final (fase 3, suprimiendo 1/2 porque
 * ya se mostraron en la Intro o en el propio programa) y NextProgram sobre
 * los Créditos, usando la duración REAL de los créditos (recién conocida
 * en su propio onPrepared) en vez de la del programa. Compartida entre
 * playCreditos() (arranque normal) y el resume genérico de onResume()
 * (currentItemType == "creditos", ver resumeUriHardCut()).
 *
 * Release 5.4.1 — BUG FIX: antes se pasaba SCREENBUG_END_SHOW_BEFORE_MS
 * (46s) implícito, y en créditos más cortos que eso (lo más común) el
 * cálculo daba negativo y el ScreenBug final nunca llegaba a agendarse —
 * ver el comentario largo en scheduleMultipleScreenbugs() sobre el clamp
 * que lo soluciona ahí adentro (a partir de esta Release, automático para
 * cualquier caller — no hace falta calcular nada acá).
 *
 * Release 5.5.0 — BUG FIX ("lo mismo pasa en créditos con el screenbug"):
 * como acá suppressStartMidPhases=true (los Créditos nunca corren su
 * propia fase 1/2), si el Programa estaba mostrando la fase 2 (mid) justo
 * antes del corte, se quedaba oculta desde el arranque de los Créditos
 * hasta que le tocara a la fase 3 — un hueco sin ScreenBug que no debería
 * estar ahí. Ahora, si screenBugMidVisible ya venía en true, se restaura
 * de inmediato (mismo criterio que el resto: resetAnimation=false, sin
 * reiniciar nada) antes de agendar la fase 3 — que la va a reemplazar más
 * adelante en el momento que le corresponda.
 */
private fun LiveDiscoveryKids.scheduleCreditosOverlays(creditosDurationMs: Int, elapsed: Long) {
    if (screenBugMidVisible) {
        fadeInBugWithResource(currentMidScreenBugResource(), resetAnimation = false)
    }
    scheduleMultipleScreenbugs(
        segmentStartMs = 0,
        segmentEndMs = creditosDurationMs,
        elapsed = elapsed,
        suppressStartMidPhases = true
    )
    scheduleNextProgramBug(0, creditosDurationMs, elapsed, isFinalSegment = true)
}

internal fun LiveDiscoveryKids.playCreditos(programIndex: Int) {
    cancelAllTasks()
    setBugAlpha(0f)
    setNextProgramBugAlpha(0f)
    isInProgramSegment  = false
    isInCommercialBlock = false
    currentItemType      = "creditos"

    val uri = resolveCreditosUri(programIndex)
    if (uri == null) {
        Log.w(LiveDiscoveryKids.TAG, "Créditos del programa ${programIndex + 1} sin video válido – skipping")
        playlistIndex++
        advance()
        return
    }

    Log.d(LiveDiscoveryKids.TAG, "▶ CRÉDITOS [programa=${programIndex + 1}, uri=$uri]")

    // Preview 2013.6.0.0.2 — corte directo (sin FadeOut/FadeIn), ver
    // playUriHardCut() / comentario largo en playBumper().
    playUriHardCut(
        uri,
        onPrepared = { durationMs -> scheduleCreditosOverlays(durationMs, elapsed = 0L) }
    ) {
        playlistIndex++
        advance()
    }
}

// ══════════════════════════════════════════════════════════════════════════
// Navegación Prev / Next
//
// Release 3.4.1 — Prev / Next saltan directo al programa destino.
//
// Problema del enfoque anterior (iniciar desde el clip de relleno inicial):
//   playUriWithTransition() registra el timer del FadeOut en pendingTasks.
//   Cuando ese clip termina, su onComplete llama playBumper() → cancelAllTasks(),
//   que borra el timer del FadeOut del bumper antes de que corra. Además,
//   encadenar playUriWithTransition() dentro del onComplete de otro cancela la
//   animación del segundo via ViewPropertyAnimator (instancia única del videoView),
//   por lo que el withEndAction del FadeOut inicial nunca se ejecuta y el bumper
//   nunca arranca.
//
// Solución: Prev / Next se comportan como un cambio de canal — van directo al
// programa sin pasar por Bumper → [Intro]. Ese bloque ya ocurre
// naturalmente cuando el programa termine por su propio onCompletionListener.
// playlistIndex se fija en el PlayItem.Program para que advance() continúe
// correctamente desde el siguiente ciclo al terminar el programa.
//
// Preview 2010.5.4.0.40: el "Enseguida" post-programa que mencionaban las
// notas históricas de arriba ya no existe como ítem del playlist — ver
// nextprogram (overlay sobre el programa, en vez de un clip aparte).
// ══════════════════════════════════════════════════════════════════════════

/**
 * Release 2009.5.0.0 — cantidad de programas activa en esta sesión, la que
 * eligió el usuario en Discovery Kids Launcher (1–24, SettingsManager.getProgramCount()).
 *
 * RELEASE 2013.6.0.0 — ya no depende de "Funciones experimentales"
 * (ELIMINADO por completo): antes, con Experimental desactivado, siempre
 * devolvía 4 (comportamiento clásico, pro1–pro4.mp4) sin importar lo que
 * hubiera configurado el usuario. Ahora la configuración de Discovery Kids
 * Launcher siempre aplica.
 */
internal fun LiveDiscoveryKids.totalProgramCount(): Int =
    SettingsManager.getProgramCount(this)

/**
 * Release 5.4.0 — true si el programa [index] tiene una Intro válida para
 * reproducir: el usuario la activó Y ya eligió un video (a diferencia de
 * continuamos, Intro/Créditos NO tienen un video predeterminado incluido
 * en la app — si no hay Uri elegida, no hay nada que reproducir).
 *
 * RELEASE 2013.6.0.0 — ya no depende de "Funciones experimentales"
 * (ELIMINADO por completo, ver totalProgramCount()).
 */
internal fun LiveDiscoveryKids.hasValidIntro(index: Int): Boolean =
    SettingsManager.isIntroEnabled(this, index) &&
        !SettingsManager.getIntroUri(this, index).isNullOrBlank()

/** Release 5.4.0 — análogo a hasValidIntro() pero para Créditos. */
internal fun LiveDiscoveryKids.hasValidCreditos(index: Int): Boolean =
    SettingsManager.isCreditosEnabled(this, index) &&
        !SettingsManager.getCreditosUri(this, index).isNullOrBlank()

/**
 * Release 2009.5.0.0 — arma el playlist: Bumper → Programa(i), repetido una
 * vez por cada programa (ver totalProgramCount()). Reemplaza al `val
 * playlist` fijo de 4 ciclos que existía antes de esta Release.
 *
 * Preview 2010.5.4.0.40 — se quitó PlayItem.Enseguida del ciclo (el
 * "enseguida" post-programa ya no es un ítem del playlist; ver nextprogram,
 * el overlay que lo reemplaza dentro de playProgram/scheduleSegmentLogic).
 *
 * Release 5.4.0:
 *   - Se quitó PlayItem.StandaloneCommercial del ciclo — los comerciales
 *     ahora solo aparecen DENTRO de los programas (playCommercial).
 *   - Se agregan PlayItem.Intro / PlayItem.Creditos, condicionales por
 *     programa (ver hasValidIntro()/hasValidCreditos()): Bumper → [Intro] →
 *     Programa → [Créditos].
 *
 * Preview 2013.6.0.0.1 — se quitó PlayItem.Bumper del ciclo: el Bumper ya no
 * sale de primero antes del programa. Ahora se inserta aleatoriamente DENTRO
 * del corte comercial (ver commercialBumperBeforeComercial / playCommercial()
 * / playCommercialBumper()), así que el ciclo del playlist queda [Intro] →
 * Programa → [Créditos]. PlayItem.Bumper y su rama en advance() se dejan
 * intactos (no se borran) por si se necesitan más adelante — simplemente ya
 * no se agregan acá.
 */
internal fun LiveDiscoveryKids.buildPlaylist(): List<LiveDiscoveryKids.PlayItem> {
    val items = mutableListOf<LiveDiscoveryKids.PlayItem>()
    repeat(totalProgramCount()) { i ->
        if (hasValidIntro(i)) items.add(LiveDiscoveryKids.PlayItem.Intro(i))
        items.add(LiveDiscoveryKids.PlayItem.Program(i))
        if (hasValidCreditos(i)) items.add(LiveDiscoveryKids.PlayItem.Creditos(i))
    }
    return items
}

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
    setNextProgramBugAlpha(0f)
    stopPositionTracker()
    isInProgramSegment = false
    isInCommercialBlock = false
    // Release 2006.4.1.1: limpia el estado de reanudación de clips no-programa;
    // Prev/Next descarta cualquier bumper/enseguida/comercial en curso, así que
    // no debe quedar un currentClipUri obsoleto que onResume() intente reanudar.
    currentClipUri = null
    currentClipOnComplete = null
    videoView.stopPlayback()

    // Release 2009.4.6.1 — BUG FIX: busca el índice del PlayItem.Program destino
    // en el playlist, pero a partir de la posición ACTUAL (playlistIndex) en la
    // DIRECCIÓN de la navegación (Prev o Next), wrapeando si es necesario.
    // Antes usaba indexOfFirst (búsqueda siempre desde el inicio), lo que hacía
    // que si estabas reproduciendo Program(3) y presionabas Next, iría al Program(0)
    // encontrado en el índice 3 del playlist en vez de avanzar. Esto corrige
    // que Prev/Next ahora van en orden real a través de la lista.
    var searchIdx = playlistIndex
    val searchDirection = if (direction > 0) 1 else -1
    var found = false
    var attemptCounter = 0
    
    while (attemptCounter < playlist.size) {
        searchIdx = (searchIdx + searchDirection + playlist.size) % playlist.size
        val item = playlist[searchIdx]
        if (item is LiveDiscoveryKids.PlayItem.Program && item.index == target) {
            found = true
            break
        }
        attemptCounter++
    }
    
    val programIdx = if (found) searchIdx else 0

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

    val totalPrograms = totalProgramCount()   // Release 2009.5.0.0 — antes fijo en 4
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
    // Release 5.8.0 — Episodios de Programa: un programa NUEVO siempre
    // arranca en su episodio 0 (el primero). currentEpisodeIndex solo
    // avanza más adelante, dentro del mismo programa, vía
    // resumeProgramAfterCommercial() — nunca acá.
    currentEpisodeIndex = 0
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

    val uri = resolveProgramEpisode(idx, 0)
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
    // Release 5.8.0 — banner de clasificación: una vez por programa (no por
    // episodio ni al resumir desde una posición guardada).
    // Preview 2013.6.0.0.3 — si el programa tiene Intro activada, el banner
    // ya se mostró ahí (ver playIntro()) — no repetirlo acá.
    if (restartFromBeginning && !hasValidIntro(idx)) showClasifBanner()
}

/**
* Plays the program starting at [startOffsetMs].
* [isFirstPlay] = true  → recalculate breaks from scratch.
* [isFirstPlay] = false → breaks already trimmed; resume only.
*
* Preview 2013.6.0.0.2 — [fadeIn] controla si este arranque anima con FadeIn
* (1 s, TRANSITION_FADE_IN_MS) o corta en seco (alpha=1f inmediato). A partir
* de esta Preview, FadeIn queda reservado EXCLUSIVAMENTE al Programa
* retomando justo DESPUÉS de un corte comercial — por eso el default es
* false (arranque normal desde Bumper/Intro, o resume de sesión/segundo
* plano, que ya NO son "después de comerciales"): solo
* resumeProgramAfterCommercial() pasa fadeIn=true. Ver también el FadeOut de
* fin de segmento más abajo, que usa la misma regla en el sentido inverso
* (con willCutToCommercial).
*/
internal fun LiveDiscoveryKids.beginProgramSegment(
    uri: Uri,
    startOffsetMs: Int,
    isFirstPlay: Boolean,
    isNewSegment: Boolean = true,
    fadeIn: Boolean = false
) {
    cancelAllTasks()
    setBugAlpha(0f)
    setNextProgramBugAlpha(0f)
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
            // BUG FIX/NUEVO (Release 5.6.0) — "Activar comerciales" por
            // programa (Configuración de Programa, Predeterminado:
            // activado). Desactivado, este programa no agenda NINGÚN corte
            // — breakQueue vacía, se reproduce de punta a punta sin
            // interrupciones.
            breakQueue = if (SettingsManager.isCommercialsEnabled(this, currentProgramIndex)) {
                calcBreaks(programDuration).toMutableList()
            } else {
                mutableListOf()
            }
        }

        if (startOffsetMs > 0) videoView.seekTo(startOffsetMs)

        scheduleSegmentLogic(startOffsetMs, isNewSegment = isNewSegment, isFirstPlay = isFirstPlay)
        videoView.start()
        if (fadeIn) {
            videoView.alpha = 0f
            videoView.animate()
                .alpha(1f)
                .setDuration(LiveDiscoveryKids.TRANSITION_FADE_IN_MS)
                .start()
        } else {
            videoView.alpha = 1f
        }
        isInProgramSegment = true
        currentItemType = "program"
        startPositionTracker()

        // BUG FIX (2010.5.3.0 — investigación a fondo, el bug "seguía igual"
        // después del fix de la 2009.5.2.1): el corte programado acá SÍ se
        // agregó, pero el cálculo de CUÁNDO dispararlo estaba mal para
        // cualquier reanudación (isNewSegment=false: volver de Configuración,
        // de segundo plano, o restaurar sesión) — usaba "programDuration -
        // fadeOutMs" como si el video arrancara siempre desde el segundo 0,
        // ignorando por completo "startOffsetMs" (el punto real donde
        // arranca esta vez, potentialmente muy avanzado en el video). Eso
        // programaba el corte para un momento que, en un resume, ya había
        // quedado en el PASADO (el video real, arrancando adelantado, llega
        // a su fin real antes de que el timer mal calculado dispare) — así
        // que el video terminaba por el fallback en seco (onCompletionListener)
        // en vez de por el corte programado, y esa transición al siguiente
        // clip arrancaba desde un corte abrupto no planeado.
        // Mismo patrón que ya usa correctamente resumeUriHardCut():
        // "remaining = duration - startOffsetMs", NO "duration" a secas.
        //
        // Preview 2013.6.0.0.2 — willCutToCommercial decide si este final de
        // segmento anima FadeOut (500 ms, TRANSITION_FADE_OUT_MS) o corta en
        // seco: el FadeOut solo aplica si lo que sigue es un corte comercial
        // (quedan más episodios de este programa — ver resumeProgramAfterCommercial()/
        // onProgramSegmentFinished()); si el programa ya terminó del todo
        // (pasa a Créditos o al siguiente ítem del playlist), corta en seco.
        var transitionCompleted = false
        val fadeOutMs = LiveDiscoveryKids.TRANSITION_FADE_OUT_MS
        val remaining = (programDuration - startOffsetMs).toLong().coerceAtLeast(0L)
        val willCutToCommercial = (currentEpisodeIndex + 1) < SettingsManager.getEpisodeCount(this, currentProgramIndex)
        val cutDelay = if (willCutToCommercial) (remaining - fadeOutMs).coerceAtLeast(0L) else remaining
        post(cutDelay) {
            if (!transitionCompleted) {
                transitionCompleted = true
                videoView.setOnCompletionListener(null)
                if (willCutToCommercial) {
                    videoView.animate()
                        .alpha(0f)
                        .setDuration(fadeOutMs)
                        .withEndAction {
                            Log.d(LiveDiscoveryKids.TAG, "Program ended (fadeOut, va a corte comercial)")
                            setNextProgramBugAlpha(0f)
                            isInProgramSegment = false
                            stopPositionTracker()
                            pausedPositionMs = 0
                            onProgramSegmentFinished()
                        }
                        .start()
                } else {
                    Log.d(LiveDiscoveryKids.TAG, "Program ended (corte directo, sin comercial pendiente)")
                    setNextProgramBugAlpha(0f)
                    isInProgramSegment = false
                    stopPositionTracker()
                    pausedPositionMs = 0
                    onProgramSegmentFinished()
                }
            }
        }

        videoView.setOnCompletionListener {
            // Fallback: solo por si el programa termina ANTES de que corra
            // el post(cutDelay) de arriba (ej. clip más corto que fadeOutMs).
            // En el caso normal, este listener nunca llega a disparar porque
            // el de arriba ya lo desactiva primero.
            if (!transitionCompleted) {
                transitionCompleted = true
                Log.d(LiveDiscoveryKids.TAG, "Program ended (fallback onCompletion)")
                cancelAllTasks()
                setBugAlpha(0f)
                setNextProgramBugAlpha(0f)
                isInProgramSegment = false
                stopPositionTracker()
                pausedPositionMs = 0
                onProgramSegmentFinished()
            }
        }
    }
}

/**
 * Release 5.8.0 — Episodios de Programa: se llama cada vez que un episodio
 * (o un programa clásico de 1 solo episodio, el caso de siempre) termina.
 * Si quedan más episodios para este programa (SettingsManager.getEpisodeCount()),
 * dispara el corte comercial de siempre (mismo look&feel: [Bumper] →
 * comercial → [Bumper] → continuamos — ver playCommercial()) y, al terminar ese
 * corte, arranca el SIGUIENTE episodio desde cero — NO retoma el mismo
 * video (ver resumeProgramAfterCommercial()). Si este era el último
 * episodio, sigue el flujo normal de siempre: Créditos si están
 * configurados para este programa, si no el próximo ítem del playlist.
 */
internal fun LiveDiscoveryKids.onProgramSegmentFinished() {
    val episodeCount = SettingsManager.getEpisodeCount(this, currentProgramIndex)
    if (currentEpisodeIndex + 1 < episodeCount) {
        Log.d(LiveDiscoveryKids.TAG, "Episodio ${currentEpisodeIndex + 1}/$episodeCount de programa $currentProgramIndex terminado — corte comercial antes del próximo episodio")
        // El 0 acá es un valor "dummy": resumeProgramAfterCommercial() (el
        // que se llama al final del corte) detecta que quedan episodios y
        // arranca el siguiente desde cero, ignorando este valor.
        playCommercial(0)
        return
    }
    playlistIndex++
    advance()
}

/**
 * Release 2009.4.6.1 — NUEVO: programa los 3 screenbug secuenciales durante un programa.
 *
 * Sistema de 3 fases que reemplaza la lógica simple anterior:
 *   1. screenbug_start (GIF): mostrar en SCREENBUG_START_DELAY_MS (20s),
 *      ocultar en SCREENBUG_START_DELAY_MS + SCREENBUG_START_ESTIMATED_DURATION_MS (24,9s)
 *   2. screenbug (PNG): mostrar SCREENBUG_MID_DELAY_AFTER_START_MS después de
 *      que start se oculta, ocultar cuando aparece screenbug_end
 *   3. screenbug_end (GIF): mostrar SCREENBUG_END_SHOW_BEFORE_MS (46s) antes del final,
 *      ocultar 4,9s después
 *
 * Release 5.4.0 — Intro / Créditos: si hay Intro válida, la fase 1/2
 * (screenbug_start/mid) TAMBIÉN corre durante la Intro (además del Programa,
 * sin cambios ahí); si hay Créditos válidos, la fase 3 (screenbug_end) +
 * NextProgram corren DURANTE los Créditos EN VEZ de en el Programa — ver
 * playIntro()/scheduleCreditosOverlays() y suppressEndPhase acá abajo.
 *
 * Release 5.4.1 — BUG FIX (causa raíz, "no aparece en la Intro/Créditos"):
 * el diseño original de la 5.4.0 usaba SCREENBUG_START_DELAY_MS (20s) /
 * SCREENBUG_END_SHOW_BEFORE_MS (46s) tal cual, calculados sobre la duración
 * de la Intro/Créditos — pero esos clips suelen ser bastante más cortos que
 * eso (créditos de 10-15s, por ejemplo). El cálculo daba negativo y la
 * condición de guarda (`segmentDuration > X`) hacía que directamente nunca
 * se agendara nada — no es que apareciera tarde, no aparecía NUNCA. Ahora:
 *   - startShowAt/endShowAt se calculan acá ADENTRO con clamp (coerceAtMost/
 *     coerceAtLeast) a la duración real de [segmentEndMs]-[segmentStartMs]:
 *     si el delay normal (20s / 46s) no entra completo en el clip, se corre
 *     lo antes posible para que SIEMPRE llegue a mostrarse algo. En clips
 *     largos (cualquier Programa normal) el resultado es idéntico a antes.
 *     playIntro() y scheduleCreditosOverlays() ya no necesitan calcular
 *     ningún ajuste — pasan la duración real del clip como segmentEndMs y
 *     el clamp de acá adentro hace el resto.
 *   - Se probó además suprimir la fase 1/2 en el primer segmento del
 *     Programa cuando hay Intro válida (asumiendo que la Intro "ya la
 *     dejaba mostrada") — pero beginProgramSegment() siempre resetea el
 *     ScreenBug a oculto al arrancar cualquier segmento nuevo, así que eso
 *     causaba un flash-y-desaparece-para-siempre justo al cortar de Intro a
 *     Programa. Se descartó: la fase 1/2 corre en AMBOS (Intro si aplica, y
 *     el Programa siempre) — en el peor caso se ve dos veces, pero nunca
 *     deja de aparecer.
 *
 * @param segmentStartMs posición en ms del programa donde arrancó el segmento
 * @param segmentEndMs posición en ms donde termina el segmento (siguiente break o final)
 * @param elapsed ms ya transcurridos DE ESTE CLIP — controla la fase 3 (screenbug_end)
 * @param startMidElapsed ms a considerar para las fases 1/2 (screenbug_start/mid) — por
 *   defecto igual a [elapsed]
 * @param suppressStartMidPhases true si las fases 1/2 no deben correr en este clip
 *   (Créditos: la fase 1/2 no tiene sentido ahí, ya corrió en la Intro y/o el Programa)
 * @param suppressEndPhase true si la fase 3 (+ NextProgram, agendado aparte) no debe
 *   correr en este clip (programa con Créditos válidos: se difiere a playCreditos())
 * @param allowProximoPrograma Preview 2013.6.0.0.2 — true si el GIF "próximo
 *   programa" (proximo_programa_screenbug.gif) puede aparecer en este clip.
 *   Solo tiene sentido durante el Programa en sí — playIntro() lo pasa en
 *   false (la Intro corre las fases 1/2 del ScreenBug normal, pero no es
 *   "el programa").
 */
internal fun LiveDiscoveryKids.scheduleMultipleScreenbugs(
    segmentStartMs: Int,
    segmentEndMs: Int,
    elapsed: Long,
    startMidElapsed: Long = elapsed,
    suppressStartMidPhases: Boolean = false,
    suppressEndPhase: Boolean = false,
    allowProximoPrograma: Boolean = true
) {
    val segmentDuration = (segmentEndMs - segmentStartMs).toLong().coerceAtLeast(0)

    // Release 2010.5.3.0 — ScreenBug de Navidad (1 al 24 de diciembre):
    // mismo comportamiento de 3 fases que el normal, solo cambian los 3
    // drawables. Se elige una sola vez acá arriba y se usa en todo el resto
    // de la función — ver isChristmasScreenBugActive().
    val christmas = isChristmasScreenBugActive()
    val startRes = if (christmas) R.drawable.screenbug_start_navidad else R.drawable.screenbug_start
    val midRes = currentMidScreenBugResource()
    val endRes = if (christmas) R.drawable.screenbug_end_navidad else R.drawable.screenbug_end

    // --- PHASE 1: screenbug_start (GIF) ---
    // BUG FIX: estas constantes viven en el companion object de LiveDiscoveryKids;
    // como esta es una función de extensión top-level (fuera de la clase), hay que
    // calificarlas con "LiveDiscoveryKids." o el compilador las marca como
    // "Unresolved reference" (igual que TAG más abajo).
    //
    // Release 5.4.1 — BUG FIX (causa raíz real): SCREENBUG_START_DELAY_MS
    // (20s) se usaba TAL CUAL como el momento de aparición, sin importar la
    // duración real de [segmentDuration]. Si el clip (Intro/Créditos, ambos
    // suelen ser cortos) dura menos que eso, el show nunca "entraba" en la
    // ventana disponible y las guardas de abajo (segmentDuration > X) hacían
    // que no se agendara NADA — no es que apareciera tarde, no aparecía
    // nunca. Ahora se hace clamp: si el delay normal no entra completo
    // (con su duración visible incluida) dentro del clip, se corre lo antes
    // posible para que SIEMPRE llegue a mostrarse algo, incluso en clips muy
    // cortos. Para clips largos (el caso normal de todo Programa) el
    // resultado es IDÉNTICO a antes — el min()/max() no hace nada distinto
    // cuando sobra tiempo de sobra.
    val startShowAt = LiveDiscoveryKids.SCREENBUG_START_DELAY_MS.coerceAtMost(
        (segmentDuration - LiveDiscoveryKids.SCREENBUG_START_ESTIMATED_DURATION_MS).coerceAtLeast(0L)
    )
    val startHideAt = startShowAt + LiveDiscoveryKids.SCREENBUG_START_ESTIMATED_DURATION_MS
    val midShowAt = startShowAt + LiveDiscoveryKids.SCREENBUG_START_ESTIMATED_DURATION_MS + LiveDiscoveryKids.SCREENBUG_MID_DELAY_AFTER_START_MS
    // Release 5.4.0: si esta fase 3 se difiere a Créditos (suppressEndPhase), la
    // fase 2 (PNG estático) no debe autoocultarse al llegar al final DE ESTE
    // clip — tiene que quedarse fija hasta que Créditos la reemplace por su
    // propia fase 3. MAX_VALUE hace que las condiciones de abajo ("segmentDuration
    // > midHideAt") nunca se cumplan dentro de este clip.
    // Release 5.4.1 — mismo BUG FIX que en la fase 1: endShowAt se clampea a
    // 0 en vez de poder quedar negativo, para que SIEMPRE llegue a mostrarse
    // en clips más cortos que SCREENBUG_END_SHOW_BEFORE_MS (46s) — como los
    // Créditos (ver comentario largo más arriba en la fase 1).
    val endShowAt = (segmentDuration - LiveDiscoveryKids.SCREENBUG_END_SHOW_BEFORE_MS).coerceAtLeast(0L)
    val midHideAt = if (suppressEndPhase) Long.MAX_VALUE else endShowAt
    val endHideAt = endShowAt + LiveDiscoveryKids.SCREENBUG_END_VISIBLE_DURATION_MS

    // Preview 2013.6.0.0.2 — GIF "próximo programa": ventana de aparición a
    // mitad del segmento, con margen de PROXIMO_PROGRAMA_MIN_START_MS (1 min)
    // desde el arranque y PROXIMO_PROGRAMA_MIN_BEFORE_END_MS (1 min) antes
    // del final del segmento (próximo corte comercial, o fin de
    // programa/episodio). Si el segmento no tiene ese margen de sobra
    // (< 2min15s), directamente no se agenda — a diferencia de las fases
    // 1/2/3 (que siempre tienen que aparecer en algún punto), este GIF es
    // opcional: mejor no mostrarlo que mostrarlo pegado al arranque o al
    // corte comercial.
    val proximoWindowExists = allowProximoPrograma &&
        segmentDuration >= (LiveDiscoveryKids.PROXIMO_PROGRAMA_MIN_START_MS +
            LiveDiscoveryKids.PROXIMO_PROGRAMA_MIN_BEFORE_END_MS +
            LiveDiscoveryKids.PROXIMO_PROGRAMA_VISIBLE_MS)
    val proximoShowAt = LiveDiscoveryKids.PROXIMO_PROGRAMA_MIN_START_MS
    val proximoHideAt = proximoShowAt + LiveDiscoveryKids.PROXIMO_PROGRAMA_VISIBLE_MS

    // BUG FIX (investigación a fondo — "el ScreenBug se reinicia"): esta función
    // solo programaba eventos FUTUROS de show/hide según "elapsed". Pero
    // beginProgramSegment() llama setBugAlpha(0f) SIEMPRE al arrancar — incluso
    // en un simple resume (isNewSegment=false, mismo proceso: abrir
    // Configuración y volver, o un ratito en segundo plano) — así que si
    // "elapsed" caía DENTRO de la ventana visible de alguna fase (el ScreenBug
    // ya debería estar mostrándose en este punto del programa), quedaba oculto
    // hasta el próximo evento programado, varios segundos después. Eso es lo
    // que se percibía como "se reinicia": desaparece y reaparece más tarde
    // mostrando otra fase, fuera de lugar. Ahora, si corresponde, se restaura
    // la fase visible de inmediato — sin reiniciar el GIF al frame 0
    // (resetAnimation=false), para que no se note un salto.
    if (!suppressStartMidPhases) {
        when {
            startMidElapsed >= startShowAt && startMidElapsed < startHideAt && segmentDuration > startShowAt ->
                fadeInBugWithResource(startRes, resetAnimation = false)
            proximoWindowExists && startMidElapsed >= proximoShowAt && startMidElapsed < proximoHideAt -> {
                fadeInBugWithResource(R.drawable.proximo_programa_screenbug, resetAnimation = false)
                screenBugMidVisible = false
            }
            startMidElapsed >= midShowAt && startMidElapsed < midHideAt && segmentDuration > midShowAt && midShowAt < midHideAt -> {
                fadeInBugWithResource(midRes, resetAnimation = false)
                screenBugMidVisible = true
            }
        }

        if (startMidElapsed < startShowAt && segmentDuration > startShowAt) {
            val startDelay = (startShowAt - startMidElapsed).coerceAtLeast(0L)
            post(startDelay) {
                fadeInBugWithResource(startRes)
                Log.d(LiveDiscoveryKids.TAG, "ScreenBug PHASE 1: screenbug_start shown (navidad=$christmas)")
            }
        }

        if (startMidElapsed < startHideAt && segmentDuration > startHideAt) {
            val hideDelay = (startHideAt - startMidElapsed).coerceAtLeast(0L)
            // BUG FIX: antes usaba fadeOutBug() (con animación); el screenbug_start
            // debe desaparecer de golpe, sin fadeout.
            post(hideDelay) { setBugAlpha(0f) }
        }

        // --- PHASE 2: screenbug (PNG) ---
        if (startMidElapsed < midShowAt && segmentDuration > midShowAt && midShowAt < midHideAt) {
            val midDelay = (midShowAt - startMidElapsed).coerceAtLeast(0L)
            post(midDelay) {
                fadeInBugWithResource(midRes)
                screenBugMidVisible = true
                Log.d(LiveDiscoveryKids.TAG, "ScreenBug PHASE 2: screenbug shown (navidad=$christmas)")
            }
        }

        if (startMidElapsed < midHideAt && segmentDuration > midHideAt && midHideAt > midShowAt) {
            val hideDelay = (midHideAt - startMidElapsed).coerceAtLeast(0L)
            post(hideDelay) {
                fadeOutBug()
                screenBugMidVisible = false
            }
        }

        // --- Próximo programa (GIF, sustituye brevemente al screenbug.png) ---
        // Preview 2013.6.0.0.2. Mismo patrón de show/hide que las fases 1/2/3:
        // sustituye el drawable actual del ImageView por el GIF (instantáneo,
        // fadeInBugWithResource ya no anima — ver su doc), y a los 15s lo
        // vuelve a sustituir por el screenbug.png normal, también instantáneo.
        if (proximoWindowExists) {
            if (startMidElapsed < proximoShowAt && segmentDuration > proximoShowAt) {
                val showDelay = (proximoShowAt - startMidElapsed).coerceAtLeast(0L)
                post(showDelay) {
                    fadeInBugWithResource(R.drawable.proximo_programa_screenbug)
                    screenBugMidVisible = false
                    Log.d(LiveDiscoveryKids.TAG, "ScreenBug PRÓXIMO PROGRAMA: shown")
                }
            }
            if (startMidElapsed < proximoHideAt && segmentDuration > proximoHideAt) {
                val hideDelay = (proximoHideAt - startMidElapsed).coerceAtLeast(0L)
                post(hideDelay) {
                    fadeInBugWithResource(midRes)
                    screenBugMidVisible = true
                    Log.d(LiveDiscoveryKids.TAG, "ScreenBug PRÓXIMO PROGRAMA: hidden, vuelve screenbug.png")
                }
            }
        }
    }

    // --- PHASE 3: screenbug_end (GIF) ---
    // BUG FIX: antes endHideAt = segmentDuration, así que quedaba visible los
    // 20s completos de la ventana final y el GIF (más corto) se veía repetirse
    // en loop. Ahora se oculta 4,9s después de mostrarse, igual que screenbug_start.
    if (!suppressEndPhase) {
        if (elapsed >= endShowAt && elapsed < endHideAt && segmentDuration > endShowAt) {
            fadeInBugWithResource(endRes, resetAnimation = false)
            screenBugMidVisible = false
        }

        if (elapsed < endShowAt && segmentDuration > endShowAt) {
            val endDelay = (endShowAt - elapsed).coerceAtLeast(0L)
            post(endDelay) {
                fadeInBugWithResource(endRes)
                screenBugMidVisible = false
                Log.d(LiveDiscoveryKids.TAG, "ScreenBug PHASE 3: screenbug_end shown (navidad=$christmas)")
            }
        }

        if (elapsed < endHideAt && segmentDuration > endHideAt) {
            val hideDelay = (endHideAt - elapsed).coerceAtLeast(0L)
            post(hideDelay) { fadeOutBug() }
        }
    }
}

/**
 * Release 2010.5.3.0 — true entre el 1 y el 24 de diciembre (inclusive),
 * cualquier año. Decide si scheduleMultipleScreenbugs() usa el set de
 * ScreenBug de Navidad (screenbug_start_navidad.gif / screenbug_navidad.png
 * / screenbug_end_navidad.gif) en vez del normal.
 *
 * Release 5.8.0 — BUG FIX (diseño): antes tenía su propio switch de
 * activar/desactivar (isNavidadScreenBugEnabled()). Ahora usa el sistema
 * unificado de Configuración: SettingsManager.isEventsEnabled() (switch
 * maestro "Activar eventos") + SettingsManager.getSelectedEvent(). Si el
 * usuario forzó manualmente "Navidad" en el selector, es true sin importar
 * la fecha real (para poder previsualizar/probar). Si el selector está en
 * "Normal", decide sola por fecha (comportamiento de siempre). Si el
 * maestro está desactivado, siempre false.
 */
internal fun LiveDiscoveryKids.isChristmasScreenBugActive(): Boolean {
    if (!SettingsManager.isEventsEnabled(this)) return false
    val selected = SettingsManager.getSelectedEvent(this)
    if (selected == SettingsManager.EVENT_NAVIDAD) return true
    if (selected != SettingsManager.EVENT_NORMAL) return false
    val cal = java.util.Calendar.getInstance()
    val month = cal.get(java.util.Calendar.MONTH)  // 0-indexado: Calendar.DECEMBER == 11
    val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
    return month == java.util.Calendar.DECEMBER && day in 1..24
}

/**
 * RELEASE 2014.6.1.0 — true el 31 de octubre (Halloween), cualquier año.
 * Mismo sistema unificado que isChristmasScreenBugActive() (switch maestro +
 * selector), pero mismo criterio que tenían Año Nuevo/Pascua/Día de la
 * Tierra (ELIMINADOS esta misma Release): solo reemplaza la fase 2
 * (screenbug.png por screenbug_halloween.png) — las fases 1/3
 * (screenbug_start/end) siguen siendo siempre las normales.
 */
internal fun LiveDiscoveryKids.isHalloweenScreenBugActive(): Boolean {
    if (!SettingsManager.isEventsEnabled(this)) return false
    val selected = SettingsManager.getSelectedEvent(this)
    if (selected == SettingsManager.EVENT_HALLOWEEN) return true
    if (selected != SettingsManager.EVENT_NORMAL) return false
    val cal = java.util.Calendar.getInstance()
    val month = cal.get(java.util.Calendar.MONTH)
    val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
    return month == java.util.Calendar.OCTOBER && day == 31
}

/**
 * Release 5.5.0 — resuelve cuál PNG usar para la fase 2 (mid) del ScreenBug
 * en este momento, según qué evento esté activo (y habilitado por el
 * usuario). Compartida entre scheduleMultipleScreenbugs() y
 * scheduleCreditosOverlays() (para restaurar la fase 2 correcta al entrar a
 * Créditos sin reiniciarla).
 *
 * RELEASE 2014.6.1.0 — ELIMINADOS por completo Año Nuevo, Pascua y Día de la
 * Tierra (con isAnoNuevoScreenBugActive()/isPascuaScreenBugActive()/
 * isDiaTierraScreenBugActive() y PASCUA_MES_DIA_POR_ANIO). Quedan solo
 * Navidad y el nuevo Halloween.
 */
internal fun LiveDiscoveryKids.currentMidScreenBugResource(): Int = when {
    isChristmasScreenBugActive() -> R.drawable.screenbug_navidad
    isHalloweenScreenBugActive() -> R.drawable.screenbug_halloween
    else -> R.drawable.screenbug
}

/**
 * Schedule screenbug show/hide and the next commercial break
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
internal fun LiveDiscoveryKids.scheduleSegmentLogic(segmentStartMs: Int, isNewSegment: Boolean, isFirstPlay: Boolean) {
    val previousSegmentStartMs = currentSegmentStartMs
    if (isNewSegment) {
        currentSegmentStartMs = segmentStartMs
    }

    val segmentEndMs = if (breakQueue.isNotEmpty()) breakQueue[0] else programDuration
    val segmentDuration = (segmentEndMs - segmentStartMs).toLong().coerceAtLeast(0)

    Log.d(LiveDiscoveryKids.TAG, "Segment: ${segmentStartMs}ms → ${segmentEndMs}ms (${segmentDuration}ms)")

    val baseSegmentStartMs = if (isNewSegment) segmentStartMs else previousSegmentStartMs
    val elapsed = (segmentStartMs - baseSegmentStartMs).toLong().coerceAtLeast(0L)

    // Release 5.5.0 — BUG FIX (causa raíz real, "el ScreenBug se reinicia y
    // vuelve a mostrar el de inicio sabiendo que ya se mostró en el intro"):
    // la 5.4.1 hacía correr la fase 1/2 COMPLETA de nuevo en el primer
    // segmento del Programa (elapsed=0 fresco) aunque ya hubiera corrido en
    // la Intro, razonando que "peor es que no aparezca nunca". Pero eso es
    // exactamente lo que Keyler reportó como bug: se ve el screenbug_start
    // (el GIF de aparición) DE NUEVO en el Programa, como si se hubiera
    // reiniciado, en vez de continuar en la fase que ya le tocaba (mid, si
    // la Intro ya alcanzó a mostrar y ocultar el start).
    //
    // Se vuelve al enfoque de la 5.4.0 (carry-over), esta vez SIN el bug de
    // cálculo negativo que tenía esa versión: [startMidElapsed] pasa acá la
    // duración real de la Intro (lastIntroDurationMs) sumada al elapsed
    // fresco de este segmento, en vez de 0 — scheduleMultipleScreenbugs()
    // (con el clamp interno ya corregido en la 5.4.1) usa ese valor para
    // decidir qué fase RESTAURAR de inmediato (mid, si ya tocaba) o cuánto
    // falta del delay original (si la Intro fue corta) — nunca vuelve a
    // programar el start desde cero si ya se mostró.
    val startMidElapsed = if (isFirstPlay && hasValidIntro(currentProgramIndex)) {
        (elapsed + lastIntroDurationMs).also {
            Log.d(LiveDiscoveryKids.TAG, "ScreenBug: primer segmento con Intro previa — se restaura/continúa en ${it}ms (Intro duró ${lastIntroDurationMs}ms), no se reinicia")
        }
    } else {
        elapsed
    }

    // Preview 2013.6.0.0.3 — screenbug_end (fase 3) se elimina por completo
    // del ÚLTIMO segmento del programa, tenga o no Créditos válidos: antes
    // solo se difería (deferToCreditos) cuando SÍ había Créditos; ahora,
    // haya o no Créditos, screenbug_end nunca se muestra si lo que sigue es
    // el final del programa — el screenbug.png (fase 2) simplemente sigue
    // mostrándose sin cambios hasta que arrancan los Créditos (ahí
    // desaparece, sin animación — ver playCreditos()/scheduleMultipleScreenbugs()
    // con suppressStartMidPhases=true) o, si no hay Créditos, hasta que el
    // programa corta al siguiente ítem del playlist. screenbug_end sigue
    // apareciendo normalmente antes de un corte comercial a mitad del
    // programa (isFinalSegment=false ahí) — eso no cambió.
    //
    // RELEASE 2014.6.1.0 — BUG FIX ("NextProgram aparece entre episodios en
    // vez de comercial + continuamos"): breakQueue.isEmpty() por sí solo NO
    // alcanza para saber si esto es realmente el final del programa — solo
    // dice que no quedan MÁS CORTES DENTRO de este video puntual. Si el
    // programa tiene Episodios (SettingsManager.getEpisodeCount() > 1) y
    // todavía queda al menos un episodio más después de este, breakQueue
    // vacío acá NO significa "se terminó el programa": significa "se
    // terminó ESTE episodio", y lo que sigue es un corte comercial normal
    // (con su continuamos) antes de arrancar el próximo episodio — ver
    // resumeProgramAfterCommercial(). isFinalSegment ahora exige además que
    // no queden más episodios pendientes, así que tanto screenbug_end como
    // NextProgram (más abajo) dejan de dispararse entre episodios y vuelven
    // a comportarse como un corte comercial cualquiera.
    val hasMoreEpisodes = (currentEpisodeIndex + 1) < SettingsManager.getEpisodeCount(this, currentProgramIndex)
    val isFinalSegment = breakQueue.isEmpty() && !hasMoreEpisodes
    val deferToCreditos = isFinalSegment && hasValidCreditos(currentProgramIndex)

    // Release 2009.4.6.1 — NUEVO: reemplazo de la lógica simple de screenbug
    // por el sistema de 3 screenbug secuenciales. Usa la nueva función
    // scheduleMultipleScreenbugs() que maneja los timings de los 3 drawables.
    scheduleMultipleScreenbugs(
        segmentStartMs, segmentEndMs, elapsed,
        startMidElapsed = startMidElapsed,
        suppressEndPhase = isFinalSegment
    )

    // Preview 2010.5.4.0.40 — NUEVO: overlay "nextprogram". Solo tiene
    // sentido en el ÚLTIMO segmento del programa (sin cortes comerciales
    // pendientes en este punto, breakQueue vacío ⇒ segmentEndMs == programDuration);
    // no debe aparecer antes de un corte comercial a mitad del programa.
    // Release 5.4.0: si hay Créditos válidos, tampoco corre acá — se difiere
    // a playCreditos() (NextProgram debe anticipar lo que sigue en el CANAL,
    // tiene sentido que aparezca sobre los créditos, no sobre el programa).
    scheduleNextProgramBug(segmentStartMs, segmentEndMs, elapsed, isFinalSegment = isFinalSegment && !deferToCreditos)

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
 * Preview 2010.5.4.0.40 — programa el overlay "nextprogram" para el ÚLTIMO
 * segmento del programa: aparece NEXTPROGRAM_SHOW_BEFORE_MS (31s) antes del
 * final real, con un fade-in de NEXTPROGRAM_ANIM_MS (500 ms). Reemplaza al
 * "enseguida" post-programa, que antes era un clip aparte entre el fin del
 * programa y el StandaloneCommercial.
 *
 * Mismo patrón de "restaurar si ya debería estar visible" que
 * scheduleMultipleScreenbugs(): si [elapsed] ya superó el punto de aparición
 * (ej. se restaura sesión a mitad del segmento final), aparece de inmediato
 * sin animación en vez de esperar a un timer que ya pasó.
 *
 * @param isFinalSegment true si este segmento termina en el final real del
 *   programa (breakQueue vacío al momento de llamar). Si es false (el
 *   segmento termina en un corte comercial), no se programa nada — el
 *   nextprogram nunca debe aparecer antes de un corte a mitad de programa.
 */
/**
 * Release 5.4.0 — agenda el overlay NextProgram (el marco decorativo) para
 * el ÚLTIMO segmento del bloque (Créditos si existen, si no el Programa).
 *
 * Release 5.4.1 — BUG FIX: [showAt] se calculaba como
 * `segmentDuration - NEXTPROGRAM_SHOW_BEFORE_MS` (31s) y si eso daba
 * negativo (clip más corto que 31s — muy común en Créditos) la función
 * cortaba con `return` sin agendar NADA, así que NextProgram nunca
 * aparecía. Ahora se hace clamp a 0: si el clip es más corto que 31s,
 * aparece apenas arranca en vez de no aparecer nunca.
 *
 * Release 5.4.1 — además de mostrar el marco (fadeInNextProgramBug()),
 * ahora también dispara showVideoInBox(): lo que va DENTRO del recuadro es
 * el VideoView del programa mismo, sin estirar — ver showVideoInBox().
 *
 * RELEASE 2013.6.0.0.01-HD — BUG FIX: al aparecer NextProgram (tanto acá,
 * cuando aparece de entrada, como en fadeInNextProgramBug()) ahora se oculta
 * el Screenbug (setBugAlpha(0f)) — antes quedaba visible superpuesto al
 * marco NextProgram. Vuelve a mostrarse solo, sin cambios acá, en el
 * arranque del siguiente segmento (beginProgramSegment() ya llama
 * setBugAlpha(0f) SIEMPRE al empezar, y las fases start/mid/end lo
 * reactivan según corresponda — ver fadeInBugWithResource()).
 */
internal fun LiveDiscoveryKids.scheduleNextProgramBug(
    segmentStartMs: Int,
    segmentEndMs: Int,
    elapsed: Long,
    isFinalSegment: Boolean
) {
    if (!isFinalSegment) return

    val segmentDuration = (segmentEndMs - segmentStartMs).toLong().coerceAtLeast(0)
    // RELEASE 2013.6.0.0 — BUG FIX: antes usaba un único NEXTPROGRAM_SHOW_BEFORE_MS
    // para los dos GIFs — ver comentario en la declaración de las constantes.
    val showBeforeMs = if (isCurrentNextProgramFactory2()) {
        LiveDiscoveryKids.NEXTPROGRAM2_SHOW_BEFORE_MS
    } else {
        LiveDiscoveryKids.NEXTPROGRAM1_SHOW_BEFORE_MS
    }
    val showAt = (segmentDuration - showBeforeMs).coerceAtLeast(0L)

    if (elapsed >= showAt) {
        Log.d(LiveDiscoveryKids.TAG, "NextProgramBug: elapsed=${elapsed}ms >= showAt(${showAt}ms) → aparece inmediatamente")
        setBugAlpha(0f)
        showNextProgramResource()
        setNextProgramBugAlpha(1f)
        showVideoInBox()
    } else {
        val delay = (showAt - elapsed).coerceAtLeast(0L)
        post(delay) { fadeInNextProgramBug() }
    }
}

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
 * interrumpe un programa en curso.
 *
 * Preview 2013.6.0.0.2 — SE ELIMINÓ POR COMPLETO "ya_regresa" (el clip
 * pre-comercial, determinístico por programa): el bloque ya no arranca con
 * un aviso propio. Secuencia actual: [Bumper] → comercial (aleatorio) →
 * [Bumper] → continuamos (según la hora del dispositivo, ver
 * continuamosResForCurrentTime()) → retoma el programa. El Bumper sale en
 * uno de los dos puntos posibles marcados con [Bumper] (ver
 * commercialBumperBeforeComercial), nunca en los dos a la vez.
 *
 * FadeOut/FadeIn: a partir de esta Preview quedan reservados EXCLUSIVAMENTE
 * al límite Programa↔bloque comercial — el FadeOut de playCommercial() (justo
 * abajo, al cortar el Programa) y el FadeIn que hace beginProgramSegment()
 * al retomar el programa (ver resumeProgramAfterCommercial()). Todo lo que
 * pasa DENTRO del bloque (Bumper, comercial, continuamos) corta en seco vía
 * playUriHardCut()/resumeUriHardCut() — sin animación de por medio.
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
// Secuencia: [Bumper] → comercial(es) → [Bumper] → continuamos → programa
// ══════════════════════════════════════════════════════════════════════════

internal fun LiveDiscoveryKids.playCommercial(resumeProgramAtMs: Int) {
    cancelAllTasks()
    setBugAlpha(0f)
    setNextProgramBugAlpha(0f)
    isInProgramSegment = false
    isInCommercialBlock = true
    commercialResumeMs = resumeProgramAtMs
    currentItemType = "commercial"

    val commercialCandidates = LiveDiscoveryKids.COMMERCIALS.filter { it != lastCommercialRes }
        .ifEmpty { LiveDiscoveryKids.COMMERCIALS }
    val chosenCommercial = commercialCandidates.random()
    lastCommercialRes = chosenCommercial

    // Preview 2013.6.0.0.2 — resolveContinuamosUri() ya no depende de qué
    // ya_regresa le tocaba a este programa (ELIMINADO): usa el video
    // PERSONALIZADO del usuario si activó "Personalizado" para este programa,
    // o si no, continuamosResForCurrentTime() según la hora real del
    // dispositivo.
    val chosenYaVolvemos = resolveContinuamosUri(currentProgramIndex)

    // Release 2006.4.1.1 — se promueven a campos de instancia para que
    // onResume() pueda reconstruir exactamente este bloque comercial (qué
    // clips se eligieron y en qué paso estaba) si la app pasa a segundo
    // plano a mitad del bloque, en vez de volver a sortear.
    commercialChosenCommercial   = chosenCommercial
    commercialChosenYaVolvemos   = chosenYaVolvemos

    // Preview 2013.6.0.0.1 — se sortea, una vez por corte, en cuál de los dos
    // puntos posibles va a salir el Bumper de este corte (ver comentario en
    // la declaración de commercialBumperBeforeComercial). commercialChosenBumperRes
    // se resetea acá y recién se fija cuando playCommercialBumper() elige el
    // bumper, más adelante en el flujo.
    commercialBumperBeforeComercial = listOf(true, false).random()
    commercialChosenBumperRes = -1

    Log.d(LiveDiscoveryKids.TAG, "▶ Corte comercial → continuamos [uri=$chosenYaVolvemos] (bumper ${if (commercialBumperBeforeComercial) "antes" else "después"} del comercial)")

    // Preview 2013.6.0.0.2 — este es el ÚNICO FadeOut que queda en todo el
    // bloque comercial: el Programa se apaga acá (500 ms, TRANSITION_FADE_OUT_MS)
    // y todo lo que sigue (Bumper/comercial/continuamos) corta en seco.
    videoView.animate()
        .alpha(0f)
        .setDuration(LiveDiscoveryKids.TRANSITION_FADE_OUT_MS)
        .withEndAction {
            if (commercialBumperBeforeComercial) {
                playCommercialBumper(LiveDiscoveryKids.CommercialStep.BUMPER_BEFORE_COMERCIAL) {
                    playCommercialStepComercial(chosenCommercial, chosenYaVolvemos, resumeProgramAtMs)
                }
            } else {
                playCommercialStepComercial(chosenCommercial, chosenYaVolvemos, resumeProgramAtMs)
            }
        }
        .start()
}

/**
 * Preview 2013.6.0.0.2 — paso del bloque comercial: el comercial en sí.
 * Corte directo (playUriHardCut()), sin FadeOut/FadeIn.
 */
internal fun LiveDiscoveryKids.playCommercialStepComercial(
    chosenCommercial: Int,
    chosenYaVolvemos: Uri,
    resumeProgramAtMs: Int
) {
    Log.d(LiveDiscoveryKids.TAG, "▶ COMMERCIAL [res=$chosenCommercial] (resumes program at ${resumeProgramAtMs}ms)")
    commercialStep = LiveDiscoveryKids.CommercialStep.COMERCIAL

    playUriHardCut(rawUri(chosenCommercial)) {
        // Preview 2013.6.0.0.1 — si a este corte le tocó el Bumper DESPUÉS
        // del comercial, se inserta acá, antes del post-comercial (continuamos).
        if (!commercialBumperBeforeComercial) {
            playCommercialBumper(LiveDiscoveryKids.CommercialStep.BUMPER_AFTER_COMERCIAL) {
                playCommercialStepPostComercial(chosenYaVolvemos, resumeProgramAtMs)
            }
        } else {
            playCommercialStepPostComercial(chosenYaVolvemos, resumeProgramAtMs)
        }
    }
}

/**
 * Preview 2013.6.0.0.2 — paso del bloque comercial: continuamos
 * (post-comercial). Corte directo (playUriHardCut()), sin FadeOut/FadeIn —
 * el FadeIn de vuelta al programa lo hace beginProgramSegment() en
 * resumeProgramAfterCommercial(), no este paso.
 */
internal fun LiveDiscoveryKids.playCommercialStepPostComercial(
    chosenYaVolvemos: Uri,
    resumeProgramAtMs: Int
) {
    Log.d(LiveDiscoveryKids.TAG, "▶ CONTINUAMOS post-comercial [uri=$chosenYaVolvemos]")
    commercialStep = LiveDiscoveryKids.CommercialStep.POST_COMERCIAL

    playUriHardCut(chosenYaVolvemos) {
        Log.d(LiveDiscoveryKids.TAG, "Continuamos done – resuming program at ${resumeProgramAtMs}ms")
        isInCommercialBlock = false
        resumeProgramAfterCommercial(resumeProgramAtMs)
    }
}

/**
 * Preview 2013.6.0.0.1 — Bumper insertado DENTRO del corte comercial, en el
 * punto que le haya tocado a este corte (ver commercialBumperBeforeComercial).
 * [step] es BUMPER_BEFORE_COMERCIAL o BUMPER_AFTER_COMERCIAL — se guarda en
 * commercialStep para que resumeCommercialBlock() sepa exactamente en qué
 * punto reanudar si la app pasa a segundo plano durante este bumper.
 * commercialChosenBumperRes guarda el bumper elegido por la misma razón,
 * igual que commercialChosenCommercial/YaVolvemos.
 *
 * A diferencia del Bumper "clásico" (playBumper(), usado ANTES de esta
 * Preview como primer ítem del ciclo del playlist, antes del programa —
 * ver buildPlaylist()), este no llama advance() al terminar: sigue
 * encadenado dentro del corte comercial vía [onDone].
 *
 * Preview 2013.6.0.0.2 — corte directo (playUriHardCut()), sin FadeOut/FadeIn.
 */
internal fun LiveDiscoveryKids.playCommercialBumper(
    step: LiveDiscoveryKids.CommercialStep,
    onDone: () -> Unit
) {
    commercialStep = step

    val candidates = LiveDiscoveryKids.BUMPERS.filter { it != lastBumperRes }.ifEmpty { LiveDiscoveryKids.BUMPERS }
    val chosenBumper = candidates.random()
    lastBumperRes = chosenBumper
    commercialChosenBumperRes = chosenBumper

    Log.d(LiveDiscoveryKids.TAG, "▶ BUMPER (dentro del corte comercial, $step) [res=$chosenBumper]")

    playUriHardCut(rawUri(chosenBumper), onComplete = onDone)
}

/**
 * Release 5.8.0 — Episodios de Programa: se llama al final de CUALQUIER
 * corte comercial (arranque normal en playCommercialStepPostComercial(), o
 * reanudado tras pasar a segundo plano en resumeCommercialBlock()) para
 * decidir qué sigue. Si el programa actual tiene más episodios pendientes
 * (SettingsManager.getEpisodeCount()), arranca el SIGUIENTE episodio desde
 * cero — [resumeAtMs] se ignora en ese caso, porque no tiene sentido
 * "retomar" un video nuevo en una posición pensada para el anterior. Si no
 * quedan más episodios (o el programa es de 1 solo, el caso de siempre),
 * retoma el mismo video en [resumeAtMs], tal como siempre.
 *
 * Preview 2013.6.0.0.2 — fadeIn=true en los dos casos: acá es, literalmente,
 * "el programa arrancando después de un corte comercial" — el único lugar
 * donde FadeIn sigue existiendo (ver beginProgramSegment()).
 */
internal fun LiveDiscoveryKids.resumeProgramAfterCommercial(resumeAtMs: Int) {
    val episodeCount = SettingsManager.getEpisodeCount(this, currentProgramIndex)
    if (currentEpisodeIndex + 1 < episodeCount) {
        currentEpisodeIndex++
        val nextUri = resolveProgramEpisode(currentProgramIndex, currentEpisodeIndex)
        if (nextUri != null) {
            Log.d(LiveDiscoveryKids.TAG, "▶ Próximo episodio ${currentEpisodeIndex + 1}/$episodeCount de programa $currentProgramIndex [uri=$nextUri]")
            currentProgramUri = nextUri
            beginProgramSegment(nextUri, startOffsetMs = 0, isFirstPlay = true, isNewSegment = true, fadeIn = true)
            return
        }
        Log.w(LiveDiscoveryKids.TAG, "Episodio ${currentEpisodeIndex + 1} sin Uri resuelta — sigo con el flujo normal (Créditos/avanzar)")
    }
    val uri = currentProgramUri ?: run {
        Log.e(LiveDiscoveryKids.TAG, "No currentProgramUri – advancing")
        playlistIndex++
        advance()
        return
    }
    beginProgramSegment(uri, startOffsetMs = resumeAtMs, isFirstPlay = false, fadeIn = true)
}

/**
 * Release 2006.4.1.1 — Reanuda el bloque comercial exactamente en el paso
 * y la posición donde estaba al pasar a segundo plano, usando los mismos
 * recursos ya elegidos (commercialChosenCommercial/YaVolvemos) en vez de
 * volver a sortear. Se llama desde onResume().
 *
 * Release 2009.5.0.0 — commercialChosenYaVolvemos ahora es Uri? (nullable
 * porque es un campo de instancia con valor inicial null); si por algún
 * motivo es null acá (no debería pasar en un resume real, ya que
 * playCommercial() siempre lo fija primero), se recalcula con
 * resolveContinuamosUri() como fallback defensivo.
 *
 * Preview 2013.6.0.0.2 — CommercialStep.PRE_COMERCIAL (ya_regresa) ELIMINADO;
 * el primer paso posible del bloque ahora es BUMPER_BEFORE_COMERCIAL o
 * COMERCIAL. Todos los resumes acá usan resumeUriHardCut() (sin FadeIn) —
 * el FadeIn solo pasa al volver al Programa, en resumeProgramAfterCommercial().
 */
internal fun LiveDiscoveryKids.resumeCommercialBlock(startOffsetMs: Int) {
    val yaVolvemosUri = commercialChosenYaVolvemos ?: resolveContinuamosUri(currentProgramIndex)

    when (commercialStep) {
        LiveDiscoveryKids.CommercialStep.BUMPER_BEFORE_COMERCIAL -> {
            // Preview 2013.6.0.0.1
            Log.d(LiveDiscoveryKids.TAG, "onResume – reanudando bumper (antes del comercial) en ${startOffsetMs}ms")
            resumeUriHardCut(rawUri(commercialChosenBumperRes), startOffsetMs) {
                playCommercialStepComercial(commercialChosenCommercial, yaVolvemosUri, commercialResumeMs)
            }
        }
        LiveDiscoveryKids.CommercialStep.COMERCIAL -> {
            Log.d(LiveDiscoveryKids.TAG, "onResume – reanudando comercial en ${startOffsetMs}ms")
            resumeUriHardCut(rawUri(commercialChosenCommercial), startOffsetMs) {
                // Preview 2013.6.0.0.1 — respeta si a este corte le toca Bumper
                // DESPUÉS del comercial (ver commercialBumperBeforeComercial).
                if (!commercialBumperBeforeComercial) {
                    playCommercialBumper(LiveDiscoveryKids.CommercialStep.BUMPER_AFTER_COMERCIAL) {
                        playCommercialStepPostComercial(yaVolvemosUri, commercialResumeMs)
                    }
                } else {
                    playCommercialStepPostComercial(yaVolvemosUri, commercialResumeMs)
                }
            }
        }
        LiveDiscoveryKids.CommercialStep.BUMPER_AFTER_COMERCIAL -> {
            // Preview 2013.6.0.0.1
            Log.d(LiveDiscoveryKids.TAG, "onResume – reanudando bumper (después del comercial) en ${startOffsetMs}ms")
            resumeUriHardCut(rawUri(commercialChosenBumperRes), startOffsetMs) {
                playCommercialStepPostComercial(yaVolvemosUri, commercialResumeMs)
            }
        }
        LiveDiscoveryKids.CommercialStep.POST_COMERCIAL -> {
            Log.d(LiveDiscoveryKids.TAG, "onResume – reanudando continuamos en ${startOffsetMs}ms")
            resumeUriHardCut(yaVolvemosUri, startOffsetMs) {
                isInCommercialBlock = false
                resumeProgramAfterCommercial(commercialResumeMs)
            }
        }
    }
}



// ── Antes en ChannelVideoTransitions.kt ────────────────────────────────────────

/**
 * ChannelVideoTransitions.kt — Preview 2006.4.1.0.21
 *
 * Funciones de extensión de LiveDiscoveryKids: los helpers de bajo nivel
 * que reproducen un clip en el VideoView, usados por TODOS los cambios de
 * video del canal (bumper, intro, créditos, comerciales, continuamos,
 * retoma de programa tras un corte).
 *
 *   - playUri(): reproducción simple sin transición ni bookkeeping de resume
 *     (usada internamente por rutas que manejan todo por su cuenta).
 *   - playUriHardCut(): corte directo (sin FadeOut/FadeIn) con bookkeeping
 *     de resume — el helper estándar a partir de la Preview 2013.6.0.0.2.
 *   - resumeUriHardCut(): igual que playUriHardCut() pero retoma un clip en
 *     una posición ya avanzada (seekTo dentro de onPrepared), usado al
 *     volver de segundo plano (Release 2006.4.1.1).
 *
 * Preview 2013.6.0.0.2 — SE ELIMINARON POR COMPLETO playUriWithTransition()
 * y resumeUriWithSeek() (el FadeOut/FadeIn genérico para cualquier cambio de
 * clip): a partir de esta Preview, FadeOut/FadeIn quedan reservados
 * EXCLUSIVAMENTE al límite Programa↔bloque comercial (ver playCommercial() y
 * beginProgramSegment()); todo lo demás usa playUriHardCut()/resumeUriHardCut().
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
 * Preview 2013.6.0.0.2 — NUEVO: corte directo, sin FadeOut/FadeIn. A partir
 * de esta Preview, FadeOut/FadeIn quedan reservados EXCLUSIVAMENTE al límite
 * Programa↔bloque comercial (ver playCommercial() y beginProgramSegment());
 * todo lo demás (Bumper, Intro, Créditos, y los clips DENTRO del bloque
 * comercial: comercial y continuamos) corta en seco. A diferencia de
 * playUri() (que no lleva bookkeeping de resume), esta función sí guarda
 * currentClipUri/currentClipPositionMs/currentClipOnComplete y arranca
 * startPositionTracker(), igual que hacía playUriWithTransition(), para que
 * onResume() pueda reanudar este clip en su posición real si la app pasa a
 * segundo plano mientras suena.
 */
internal fun LiveDiscoveryKids.playUriHardCut(
    uri: Uri,
    onPrepared: ((durationMs: Int) -> Unit)? = null,
    onComplete: () -> Unit
) {
    currentClipUri = uri
    currentClipPositionMs = 0
    currentClipOnComplete = onComplete
    startPositionTracker()

    // Corte en seco real: se deja alpha=0 hasta que el clip nuevo ya está
    // listo para arrancar (onPrepared) — así nunca se llega a ver, ni un
    // frame, el último cuadro congelado del clip anterior. El salto a
    // alpha=1f es instantáneo (sin animate()), es decir, un corte.
    videoView.animate().cancel()
    videoView.alpha = 0f

    videoView.setOnPreparedListener { mp ->
        mp.isLooping = false
        videoView.start()
        videoView.alpha = 1f
        onPrepared?.invoke(mp.duration)
    }
    videoView.setOnCompletionListener {
        currentClipUri = null
        currentClipOnComplete = null
        onComplete()
    }
    videoView.setVideoURI(uri)
    videoView.requestFocus()
}

/**
 * Preview 2013.6.0.0.2 — análogo a playUriHardCut() pero retomando [uri] en
 * [startOffsetMs] (seekTo dentro de onPrepared, mismo motivo que
 * resumeUriWithSeek()). Reemplaza a resumeUriWithSeek() en todos los cortes
 * que ya no llevan FadeIn (bumper/comercial/continuamos y el resume genérico
 * de onResume()).
 */
internal fun LiveDiscoveryKids.resumeUriHardCut(
    uri: Uri,
    startOffsetMs: Int,
    onPrepared: ((durationMs: Int) -> Unit)? = null,
    onComplete: () -> Unit
) {
    currentClipUri = uri
    currentClipPositionMs = startOffsetMs
    currentClipOnComplete = onComplete
    startPositionTracker()

    videoView.animate().cancel()
    videoView.alpha = 0f

    videoView.setOnPreparedListener { mp ->
        mp.isLooping = false
        if (startOffsetMs > 0) videoView.seekTo(startOffsetMs)
        videoView.start()
        videoView.alpha = 1f
        onPrepared?.invoke(mp.duration)
    }
    videoView.setOnCompletionListener {
        currentClipUri = null
        currentClipOnComplete = null
        onComplete()
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

/**
 * Release 5.8.0 — Episodios de Programa: resuelve la Uri del episodio
 * [episodeIndex] (0-based) del programa [programIndex]. El episodio 0 es
 * EXACTAMENTE resolveProgram(programIndex) — mismo comportamiento de
 * siempre (Uri elegida por el usuario, o pro{N}.mp4 clásico como fallback)
 * — así que un programa de 1 solo episodio (el caso de siempre) no cambia
 * en nada. Los episodios 1+ solo existen como Uri elegida por el usuario
 * (SettingsManager.getEpisodeUri) — no tienen un fallback tipo "pro1_2.mp4"
 * clásico, ya que son un concepto nuevo sin equivalente en la Era clásica.
 */
internal fun LiveDiscoveryKids.resolveProgramEpisode(programIndex: Int, episodeIndex: Int): Uri? {
    if (episodeIndex <= 0) return resolveProgram(programIndex)
    val customUri = SettingsManager.getEpisodeUri(this, programIndex, episodeIndex)
    if (customUri.isNullOrBlank()) return null
    return try {
        Uri.parse(customUri)
    } catch (e: Exception) {
        Log.e(LiveDiscoveryKids.TAG, "Uri de episodio $episodeIndex de programa $programIndex inválida: $customUri", e)
        null
    }
}

internal fun LiveDiscoveryKids.resolveProgram(index: Int): Uri? {
    // Release 2009.5.0.0 — si el usuario eligió un video propio para este
    // programa desde Discovery Kids Launcher (SAF, sin necesidad de
    // renombrar/copiar nada a Movies), se usa ese Uri directamente. La Uri
    // se persiste con takePersistableUriPermission() al elegirla (ver
    // DiscoveryKidsLauncherActivity.pickProgramVideo()), así que sigue
    // siendo válida entre reinicios de la app.
    // RELEASE 2013.6.0.0 — ya no depende de "Funciones experimentales"
    // (ELIMINADO por completo).
    run {
        val customUri = SettingsManager.getProgramUri(this, index)
        if (!customUri.isNullOrBlank()) {
            return try {
                Uri.parse(customUri)
            } catch (e: Exception) {
                Log.e(LiveDiscoveryKids.TAG, "Uri de programa $index inválida: $customUri", e)
                null
            }
        }
    }

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

/**
 * Preview 2013.6.0.0.2 — resuelve el continuamos (post-comercial) que le
 * toca a ESTE corte. Si el usuario marcó "Personalizado" para el programa
 * [programIndex] (SettingsManager.isContinuamosCustom), devuelve el video
 * que eligió; si no, cae al comportamiento clásico:
 * continuamosResForCurrentTime(), según la hora real del dispositivo (ya NO
 * se empareja con un ya_regresa — ELIMINADO por completo).
 * RELEASE 2013.6.0.0 — ya no depende de "Funciones experimentales"
 * (ELIMINADO por completo).
 */
internal fun LiveDiscoveryKids.resolveContinuamosUri(programIndex: Int): Uri {
    if (SettingsManager.isContinuamosCustom(this, programIndex)) {
        val customUri = SettingsManager.getContinuamosUri(this, programIndex)
        if (!customUri.isNullOrBlank()) {
            try { return Uri.parse(customUri) } catch (e: Exception) {
                Log.e(LiveDiscoveryKids.TAG, "Uri de continuamos personalizado inválida (programa $programIndex)", e)
            }
        }
    }
    return rawUri(LiveDiscoveryKids.continuamosResForCurrentTime())
}

/**
 * Release 5.4.0 — resuelve la Intro del programa [programIndex]. A
 * diferencia de continuamos, NO hay un video predeterminado
 * incluido en la app: si el usuario no activó Intro para este programa o no
 * eligió un video, devuelve null (y quien llame debe saltear, ver
 * playIntro()). buildPlaylist() ya se asegura de esto vía hasValidIntro()
 * al armar el playlist, pero se vuelve a validar acá por si la
 * configuración cambió después de armado el playlist de esta sesión.
 */
internal fun LiveDiscoveryKids.resolveIntroUri(programIndex: Int): Uri? {
    if (!hasValidIntro(programIndex)) return null
    val customUri = SettingsManager.getIntroUri(this, programIndex)
    return try {
        Uri.parse(customUri)
    } catch (e: Exception) {
        Log.e(LiveDiscoveryKids.TAG, "Uri de Intro inválida (programa $programIndex): $customUri", e)
        null
    }
}

/** Release 5.4.0 — análogo a resolveIntroUri() pero para Créditos. */
internal fun LiveDiscoveryKids.resolveCreditosUri(programIndex: Int): Uri? {
    if (!hasValidCreditos(programIndex)) return null
    val customUri = SettingsManager.getCreditosUri(this, programIndex)
    return try {
        Uri.parse(customUri)
    } catch (e: Exception) {
        Log.e(LiveDiscoveryKids.TAG, "Uri de Créditos inválida (programa $programIndex): $customUri", e)
        null
    }
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
    val plIdx = prefs.getInt(LiveDiscoveryKids.PREF_PLAYLIST_IDX, 0)
    val progIdx = prefs.getInt(LiveDiscoveryKids.PREF_PROGRAM_IDX, 0)
    // Release 2009.5.0.0 — la cantidad de programas (y por lo tanto el
    // tamaño del playlist) puede cambiar entre sesiones si el usuario la
    // modificó en Discovery Kids Launcher. Si el estado guardado de una
    // sesión anterior ya no encaja en el playlist actual (índice fuera de
    // rango), se descarta en vez de arriesgar un crash al indexar
    // playlist[plIdx] — se arranca desde cero.
    val savedStateFitsCurrentPlaylist = plIdx < playlist.size && progIdx < totalProgramCount()

    if (prefs.getBoolean(LiveDiscoveryKids.PREF_HAS_STATE, false) && savedStateFitsCurrentPlaylist) {
        // Hay sesión guardada → preguntar al usuario
        showResumeDialog(prefs)
    } else {
        // Sin sesión (o sesión inválida para el playlist actual) → arrancar desde el principio
        clearSavedState()
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
    // Preview 2013.6.0.0.3 — BUG FIX ("Intro y Créditos no tienen pausa: se
    // reinician al cambiar de Activity"). Investigación a fondo: la causa
    // real no estaba en onPause()/onResume() (esos ya reanudaban bien en
    // memoria, ver el bloque "currentClipUri != null" de onResume()) sino
    // acá — cuando el proceso muere de verdad (común en el dispositivo
    // físico, con poca memoria) y hay que restaurar desde SharedPreferences,
    // el fallback "pausedPositionMs" es la posición del position tracker
    // SOLO para isInProgramSegment=true (ver positionTrackerRunnable) — para
    // Intro/Créditos/Bumper, la posición real vive en currentClipPositionMs.
    // Antes se guardaba pausedPositionMs igual (desactualizada — 0, o lo que
    // quedó de un programa previo), así que "restaurar" reproducía Intro/
    // Créditos con una posición basura. Ahora que ya se guarda bien, ver
    // también resumeSavedState() para el otro lado del bug (esa posición ni
    // siquiera se estaba usando para Intro/Créditos).
    val posToSave = when {
        isInCommercialBlock -> commercialResumeMs
        isInProgramSegment  -> pausedPositionMs
        else                -> currentClipPositionMs
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
        putInt    (LiveDiscoveryKids.PREF_SEGMENT_START_MS, currentSegmentStartMs)
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
    // BUG FIX (2009.5.1.0): default = posMs (mismo comportamiento que antes)
    // por si el estado guardado viene de una versión anterior sin esta clave.
    val segmentStartMs = prefs.getInt(LiveDiscoveryKids.PREF_SEGMENT_START_MS, posMs)
    // Release 4.3.1 — si el estado guardado viene de una versión anterior sin
    // esta clave, se infiere a partir del tipo de ítem: si estaba en medio de
    // un programa o un comercial, necesariamente ya salió al aire un programa.
    val hasPlayedProgram = prefs.getBoolean(LiveDiscoveryKids.PREF_HAS_PLAYED_PROGRAM, itemType == "program" || itemType == "commercial")

    val whereStr = when (itemType) {
        "program"    -> getString(R.string.resume_where_program, progIdx + 1, posMs / 60_000, (posMs % 60_000) / 1_000)
        "commercial" -> getString(R.string.resume_where_commercial, progIdx + 1)
        "bumper"     -> getString(R.string.resume_where_bumper)
        "enseguida"  -> getString(R.string.resume_where_enseguida)
        "intro"      -> getString(R.string.resume_where_intro, progIdx + 1)
        "creditos"   -> getString(R.string.resume_where_creditos, progIdx + 1)

        else         -> getString(R.string.resume_where_unknown)
    }

    AlertDialog.Builder(this)
        .setTitle(getString(R.string.dialog_resume_title))
        .setMessage(getString(R.string.dialog_resume_message, whereStr))
        .setCancelable(false)
        .setPositiveButton(getString(R.string.dialog_resume_positive)) { _, _ ->
            pausedPositionMs = 0
            resumeSavedState(itemType, plIdx, progIdx, posMs, commMs, screenbugRes, breakQueueStr, hasPlayedProgram, segmentStartMs, prefs)
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
 * - intro/creditos (Preview 2013.6.0.0.3): reinician desde el principio, mismo
 *               razonamiento que bumper/enseguida — ver comentario en el when de abajo.
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
    segmentStartMs: Int,
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
                // BUG FIX (2009.5.1.0): restaurar el punto real donde arrancó
                // el segmento y pasar isNewSegment=false, para que elapsed se
                // calcule correctamente (posMs - segmentStartMs) en vez de 0.
                // Antes, al faltar isNewSegment explícito, se usaba el default
                // (true) y el ciclo de 3 fases del ScreenBug arrancaba de cero
                // como si el segmento recién empezara en el punto de resume.
                currentSegmentStartMs = segmentStartMs
                Log.d(LiveDiscoveryKids.TAG, "Restaurando programa en ${posMs}ms (segmento arrancó en ${segmentStartMs}ms), breaks pendientes: $breakQueue")
                beginProgramSegment(uri, startOffsetMs = posMs, isFirstPlay = false, isNewSegment = false)
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
                // El programa retoma justo después del comercial: acá sí es
                // legítimamente un segmento nuevo que arranca en commMs.
                // Preview 2013.6.0.0.2 — fadeIn=true: es, literalmente, "el
                // programa arrancando después de un corte comercial".
                beginProgramSegment(uri, startOffsetMs = commMs, isFirstPlay = false, isNewSegment = true, fadeIn = true)
            } else {
                playlistIndex = 0
                advance()
            }
        }
        // Preview 2013.6.0.0.3 — BUG FIX ("Intro y Créditos no tienen
        // pausa"): antes "intro"/"creditos" no tenían rama acá y caían al
        // "else" de abajo, que resetea playlistIndex=0 — es decir, ante la
        // muerte del proceso a mitad de una Intro o unos Créditos, la app no
        // solo reiniciaba ESE clip: reiniciaba TODO el canal desde el
        // principio. playlistIndex ya se restauró arriba (línea ~2808), así
        // que playlist[playlistIndex] ya apunta al mismo PlayItem.Intro/
        // Creditos de antes — advance() lo vuelve a reproducir correctamente.
        // Igual que "bumper"/"enseguida": reinicia el clip desde el
        // principio (son cortos, no vale la pena reconstruir un seek acá).
        "intro", "creditos" -> {
            advance()
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
                startPositionTracker()
                scheduleSegmentLogic(pausedPositionMs, isNewSegment = false, isFirstPlay = false)
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

/**
 * BUG FIX: ImageView.setImageResource() con un .gif solo decodifica y muestra
 * el primer frame (no anima). screenbug_start.gif y screenbug_end.gif se veían
 * congelados por esto, mientras que screenbug.png (estático) se veía bien.
 *
 * Fix: usar Glide, que detecta GIFs animados y los reproduce en el ImageView
 * automáticamente vía .load(resId). Para el PNG estático (fase 2) Glide
 * también funciona igual de bien, así que se unifica el path para las 3 fases.
 */
/**
 * BUG FIX (histórico): ImageView.setImageResource() con un .gif solo decodifica
 * el primer frame (no anima). screenbug_start.gif y screenbug_end.gif se veían
 * congelados por esto.
 *
 * Fix (librería GIF liviana): android-gif-drawable decodifica el GIF nativo y
 * expone un Drawable Animatable normal — sin el overhead de un pipeline de
 * imagen genérico como Glide. Los GifDrawable de start/end se cachean una vez
 * en preloadScreenBugAssets() y aquí solo se reinician (seekTo(0) + start())
 * para que reproduzcan desde el primer frame cada vez que se muestran.
 */
internal fun LiveDiscoveryKids.fadeInBug() {
    Log.d(LiveDiscoveryKids.TAG, "ScreenBug SHOW [res=$currentScreenBugRes]")
    showScreenBugResource(currentScreenBugRes)
    setBugAlpha(1f)
}

/** Release 2009.4.6.1 — variante de fadeInBug() que toma un resource específico (para screenbug de 3 fases).
 *  [resetAnimation]: false cuando se está RESTAURANDO una fase que ya debería estar visible
 *  (ver scheduleMultipleScreenbugs) — no reinicia el GIF al frame 0, solo lo hace visible
 *  donde sea que esté (el GifMovieDrawable sigue su propio reloj interno de todos modos). */
internal fun LiveDiscoveryKids.fadeInBugWithResource(res: Int, resetAnimation: Boolean = true) {
    Log.d(LiveDiscoveryKids.TAG, "ScreenBug SHOW [res=$res, reset=$resetAnimation]")
    currentScreenBugRes = res
    showScreenBugResource(res, resetAnimation)
    setBugAlpha(1f)
}

/** Aplica el resource correcto al ImageView: PNG estático directo, GIF cacheado (reiniciado desde el frame 0 solo si [resetAnimation]). */
private fun LiveDiscoveryKids.showScreenBugResource(res: Int, resetAnimation: Boolean = true) {
    when (res) {
        R.drawable.screenbug, R.drawable.screenbug_navidad ->
            screenBug.setImageResource(res)  // PNG estático, sin animación
        R.drawable.screenbug_start -> screenBugStartGif?.let {
            if (resetAnimation) it.seekToStart()
            it.start()
            screenBug.setImageDrawable(it)
        } ?: screenBug.setImageResource(res)  // fallback si el preload aún no corrió
        R.drawable.screenbug_end -> screenBugEndGif?.let {
            if (resetAnimation) it.seekToStart()
            it.start()
            screenBug.setImageDrawable(it)
        } ?: screenBug.setImageResource(res)
        R.drawable.screenbug_start_navidad -> screenBugStartNavidadGif?.let {
            if (resetAnimation) it.seekToStart()
            it.start()
            screenBug.setImageDrawable(it)
        } ?: screenBug.setImageResource(res)
        R.drawable.screenbug_end_navidad -> screenBugEndNavidadGif?.let {
            if (resetAnimation) it.seekToStart()
            it.start()
            screenBug.setImageDrawable(it)
        } ?: screenBug.setImageResource(res)
        R.drawable.proximo_programa_screenbug -> proximoProgramaScreenbugGif?.let {
            if (resetAnimation) it.seekToStart()
            it.start()
            screenBug.setImageDrawable(it)
        } ?: screenBug.setImageResource(res)
        else -> screenBug.setImageResource(res)
    }
}

/**
 * Precarga y cachea los GifMovieDrawable de screenbug_start / screenbug_end
 * (y, desde la 2010.5.3.0, sus variantes de Navidad) en memoria una sola
 * vez, para no re-decodificar el GIF cada vez que se muestra. Llamar una
 * sola vez en onCreate(), antes de que arranque cualquier programa.
 *
 * Release 5.4.0 — BUG FIX (ANR "Discovery Kids no responde" al abrir la
 * app): Movie.decodeStream() es una decodificación de imagen bit a bit,
 * relativamente lenta, y esta función decodificaba 4 GIFs de forma
 * SINCRÓNICA en el hilo principal dentro de onCreate() — junto con los 4
 * GIFs más de preloadNextProgramGifs() (Preview 2010.5.4.0.40, agregados
 * DESPUÉS de esta función, sumando aún más trabajo al mismo hilo), esto
 * podía superar los ~5s que tolera el sistema antes de mostrar el diálogo
 * "no responde". GifMovieDrawable no toca vistas ni depende del hilo desde
 * el que se construye (ver GifMovieDrawable.kt: solo crea un Handler
 * apuntando al Looper principal, lo cual es válido desde cualquier hilo) —
 * así que ahora la decodificación entera corre en un hilo aparte, y solo la
 * asignación final a los campos de la Activity se posta de vuelta al hilo
 * principal con runOnUiThread().
 */
@Suppress("DEPRECATION")
internal fun LiveDiscoveryKids.preloadScreenBugAssets() {
    Thread({
        try {
            val start = resources.openRawResource(R.drawable.screenbug_start).use { stream ->
                android.graphics.Movie.decodeStream(stream)?.let { GifMovieDrawable(it) }
            }
            val end = resources.openRawResource(R.drawable.screenbug_end).use { stream ->
                android.graphics.Movie.decodeStream(stream)?.let { GifMovieDrawable(it) }
            }
            val startNavidad = resources.openRawResource(R.drawable.screenbug_start_navidad).use { stream ->
                android.graphics.Movie.decodeStream(stream)?.let { GifMovieDrawable(it) }
            }
            val endNavidad = resources.openRawResource(R.drawable.screenbug_end_navidad).use { stream ->
                android.graphics.Movie.decodeStream(stream)?.let { GifMovieDrawable(it) }
            }
            val proximoPrograma = resources.openRawResource(R.drawable.proximo_programa_screenbug).use { stream ->
                android.graphics.Movie.decodeStream(stream)?.let { GifMovieDrawable(it) }
            }
            runOnUiThread {
                screenBugStartGif = start
                screenBugEndGif = end
                screenBugStartNavidadGif = startNavidad
                screenBugEndNavidadGif = endNavidad
                proximoProgramaScreenbugGif = proximoPrograma
            }
        } catch (e: Exception) {
            Log.e(LiveDiscoveryKids.TAG, "Error precargando GIFs de screenbug", e)
        }
    }, "preload-screenbug-gifs").start()
}

/**
 * Release 5.8.0 — precarga clasif_banner.gif en un hilo aparte, mismo
 * motivo/patrón que preloadScreenBugAssets() (evitar ANR — ver ese
 * comentario para el detalle completo).
 */
@Suppress("DEPRECATION")
internal fun LiveDiscoveryKids.preloadClasifBannerGif() {
    Thread({
        try {
            val gif = resources.openRawResource(R.drawable.clasif_banner).use { stream ->
                android.graphics.Movie.decodeStream(stream)?.let { GifMovieDrawable(it) }
            }
            runOnUiThread { clasifBannerGif = gif }
        } catch (e: Exception) {
            Log.e(LiveDiscoveryKids.TAG, "Error precargando clasif_banner.gif", e)
        }
    }, "preload-clasif-banner-gif").start()
}

/**
 * Release 5.8.0 — "Añadir un banner de clasificación usando
 * clasif_banner.gif y se ejecute al iniciar el programa": lo muestra
 * (fade in) y lo oculta solo (fade out) pasados [CLASIF_BANNER_VISIBLE_MS].
 * Se llama una vez por PROGRAMA (no por episodio — ver playProgram()), tal
 * como un banner de clasificación real de TV, que aparece al arrancar el
 * contenido y no se repite en cada corte.
 */
internal fun LiveDiscoveryKids.showClasifBanner() {
    val gif = clasifBannerGif ?: return
    gif.seekToStart()
    gif.start()
    clasifBanner.setImageDrawable(gif)
    clasifBanner.animate().cancel()
    clasifBanner.alpha = 1f
    post(LiveDiscoveryKids.CLASIF_BANNER_VISIBLE_MS) {
        clasifBanner.animate()
            .alpha(0f)
            .setDuration(LiveDiscoveryKids.TRANSITION_FADE_OUT_MS)
            .start()
    }
}

internal fun LiveDiscoveryKids.fadeOutBug() {
    Log.d(LiveDiscoveryKids.TAG, "ScreenBug HIDE")
    setBugAlpha(0f)
}

/** Instantly sets alpha without animation (used during transitions). */
internal fun LiveDiscoveryKids.setBugAlpha(alpha: Float) {
    screenBug.animate().cancel()
    screenBug.alpha = alpha
}


// ══════════════════════════════════════════════════════════════════════════
// NextProgram overlay (Preview 2010.5.4.0.40, corregido en Release 5.4.1)
// ══════════════════════════════════════════════════════════════════════════
// Reemplaza a los "enseguida" post-programa: en vez de un clip aparte entre
// el fin del programa y el StandaloneCommercial, es un marco decorativo
// (GIF, pantalla completa: logo, texto, borde amarillo del recuadro) que se
// superpone cerca del final del bloque (ver scheduleNextProgramBug()).
//
// Release 5.4.1 — CORRECCIÓN: el recuadro del marco NO trae el contenido
// dentro (el GIF lo deja transparente/vacío ahí) — lo que se ve adentro es
// el VIDEO DEL PROGRAMA MISMO, reposicionado y encogido para entrar en el
// recuadro sin estirarse (ver showVideoInBox()/restoreVideoFullScreen()),
// no otro GIF. fadeInNextProgramBug() dispara ambas cosas juntas: el marco
// (fade-in) y el reposicionamiento del video (instantáneo).
//
// Mismo patrón de caché que screenBugStartGif/screenBugEndGif (GifMovieDrawable
// decodificado una sola vez por GIF, seekToStart()+start() en cada aparición)
// para que no haya lag al mostrarlo.

/** Aparición animada (fade-in, NEXTPROGRAM_ANIM_MS) del marco NextProgram + reposicionamiento del video en el recuadro. */
internal fun LiveDiscoveryKids.fadeInNextProgramBug() {
    Log.d(LiveDiscoveryKids.TAG, "NextProgramBug FADE IN [program=$currentProgramIndex]")
    setBugAlpha(0f)
    showNextProgramResource()
    nextProgramBug.animate().cancel()
    nextProgramBug.alpha = 0f
    nextProgramBug.animate()
        .alpha(1f)
        .setDuration(LiveDiscoveryKids.NEXTPROGRAM_ANIM_MS)
        .start()
    showVideoInBox()
}

/**
 * Aplica el GIF correcto (indexado por currentProgramIndex, con módulo —
 * ver NEXTPROGRAMS) al ImageView, reiniciándolo desde el primer frame.
 * Usa el GifMovieDrawable cacheado por preloadNextProgramGifs(); si por
 * algún motivo el preload todavía no corrió, cae a setImageResource()
 * (se ve el primer frame congelado, pero no crashea).
 *
 * Release 5.5.0 — NUEVO: si el usuario activó un NextProgram personalizado
 * para este programa (Configuración de Programa) y eligió un archivo, se
 * usa ese en vez del de fábrica. Admite GIF animado o una imagen estática
 * (PNG/JPG) — se intenta decodificar como GIF primero (Movie), y si no es
 * un GIF válido, cae a Bitmap estático.
 *
 * ⚠️ Nota de rendimiento: a diferencia de los 4 nextprogramN.gif de
 * fábrica (precargados en un hilo aparte en onCreate(), ver
 * preloadNextProgramGifs()), el archivo personalizado se lee y decodifica
 * acá mismo, en el momento en que NextProgram tiene que aparecer — si el
 * usuario elige un archivo muy pesado, podría notarse un pequeño
 * tranco justo en ese instante. No se resolvió en esta Release por
 * mantener el alcance acotado; si se nota en la práctica, se puede
 * precargar el personalizado también en un hilo aparte más adelante.
 */
private fun LiveDiscoveryKids.showNextProgramResource() {
    val customUriString = if (SettingsManager.isNextProgramCustom(this, currentProgramIndex))
        SettingsManager.getNextProgramUri(this, currentProgramIndex)
    else null

    if (!customUriString.isNullOrBlank()) {
        try {
            val bytes = contentResolver.openInputStream(Uri.parse(customUriString))?.use { it.readBytes() }
            if (bytes != null) {
                val movie = android.graphics.Movie.decodeByteArray(bytes, 0, bytes.size)
                if (movie != null) {
                    nextProgramBug.setImageDrawable(GifMovieDrawable(movie))
                } else {
                    val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp != null) {
                        nextProgramBug.setImageBitmap(bmp)
                    } else {
                        Log.w(LiveDiscoveryKids.TAG, "NextProgram personalizado (programa $currentProgramIndex): archivo no es GIF ni imagen válida")
                    }
                }
                return
            }
        } catch (e: Exception) {
            Log.e(LiveDiscoveryKids.TAG, "Error cargando NextProgram personalizado (programa $currentProgramIndex) — se usa el de fábrica", e)
            // sigue abajo y cae al de fábrica
        }
    }

    val index = currentProgramIndex % LiveDiscoveryKids.NEXTPROGRAMS.size
    val res = LiveDiscoveryKids.NEXTPROGRAMS[index]
    val cached = nextProgramGifs.getOrNull(index)
    if (cached != null) {
        cached.seekToStart()
        cached.start()
        nextProgramBug.setImageDrawable(cached)
    } else {
        nextProgramBug.setImageResource(res)
    }
}

/**
 * Precarga y cachea los GifMovieDrawable de los 4 nextprogram en memoria una
 * sola vez, igual que preloadScreenBugAssets(). Llamar una sola vez en
 * onCreate(), antes de que arranque cualquier programa.
 *
 * Release 5.4.0 — mismo BUG FIX de ANR que preloadScreenBugAssets(): corre
 * en un hilo aparte, ver comentario ahí.
 */
@Suppress("DEPRECATION")
internal fun LiveDiscoveryKids.preloadNextProgramGifs() {
    Thread({
        LiveDiscoveryKids.NEXTPROGRAMS.forEachIndexed { index, res ->
            try {
                val decoded = resources.openRawResource(res).use { stream ->
                    android.graphics.Movie.decodeStream(stream)?.let { GifMovieDrawable(it) }
                }
                if (decoded != null) runOnUiThread { nextProgramGifs[index] = decoded }
            } catch (e: Exception) {
                Log.e(LiveDiscoveryKids.TAG, "Error precargando nextprogram GIF #$index", e)
            }
        }
    }, "preload-nextprogram-gifs").start()
}

/**
 * Instantly sets alpha without animation (usado en transiciones y al cortar
 * junto con el fin del programa). Release 5.4.1: al ocultar (alpha=0),
 * también restaura el VideoView a pantalla completa — ver
 * restoreVideoFullScreen(). Si alpha>0 no toca el video: quien muestra el
 * marco (fadeInNextProgramBug()/scheduleNextProgramBug()) es responsable de
 * llamar showVideoInBox() por su cuenta.
 */
internal fun LiveDiscoveryKids.setNextProgramBugAlpha(alpha: Float) {
    nextProgramBug.animate().cancel()
    nextProgramBug.alpha = alpha
    if (alpha == 0f) restoreVideoFullScreen()
}

// ══════════════════════════════════════════════════════════════════════════
// Video en el recuadro de NextProgram — Release 5.4.1
// ══════════════════════════════════════════════════════════════════════════
// Mientras el marco NextProgram está visible, el VIDEO DEL PROGRAMA (no un
// GIF aparte) se ve DENTRO del recuadro que deja transparente el marco,
// encogido pero SIN estirarse ni deformarse.
//
// Discovery Kids HD — ELIMINADO por completo AspectRatioFrameLayout (y
// AspectRatioFrameLayout.kt, que ni siquiera existe en este proyecto):
// videoContainer es un FrameLayout simple, sin ninguna proporción forzada.
// Como consecuencia, el "marco" que usan las fracciones de abajo para
// calcular el recuadro deja de ser una reconstrucción del viejo marco 4:3
// pillarboxeado de la app normal — acá el marco ES la pantalla completa,
// tal cual la reporte el dispositivo (root.width × root.height, sin
// pillarboxing ni offset).
//
// Cómo funciona: videoContainer normalmente es match_parent/match_parent,
// centrado. Achicarlo a un ancho/alto explícito (en vez de match_parent)
// hace que quede posicionado en el recuadro que le corresponda; el video
// adentro (DkVideoView, dentro de videoViewContainer) ya sabe encajarse
// dentro de lo que le dé este contenedor sin estirarse. Solo hace falta
// fijar ancho, alto y posición (margins + gravity) para que ese rectángulo
// más chico caiga exactamente en el recuadro de la imagen de referencia.
//
// Release 5.5.0 — ajuste fino ("le falta poco para ubicarse"): remedidas
// sobre la nueva imagen de referencia (12160.jpg, con el video YA
// mostrándose adentro) en vez de la estimación original a ojo. Marco 4:3
// detectado en x:[325,1269] / y:[0,720]; borde amarillo del recuadro en
// x:[765,1210] / y:[60,378] → fracciones sobre el marco: left=0.466,
// top=0.083, bottom=0.525.
//
// Release 5.6.0 — BUG FIX ("cambiar la posición del VideoView en el
// NextProgram"): remedido con precisión de píxel sobre 12397.jpg. LEFT/TOP/
// BOTTOM ya estaban bien calibrados (coinciden con el nuevo margen de
// Preview 2013.6.0.0.3 — al actualizarse los GIFs de NextProgram, se
// confirmó que nextprogram1.gif y nextprogram2.gif NO comparten el mismo
// recuadro — son dos diseños distintos (nextprogram1: un solo recuadro
// grande, a la izquierda. nextprogram2: recuadro chico a la izquierda +
// una animación propia ya incluida en el GIF a la derecha, que no es
// nuestra — el video en vivo va en el recuadro chico). El shift-en-dp de
// la 5.8.0 (aproximado, "por si no queda perfecto") se reemplaza por
// coordenadas medidas directamente sobre capturas de referencia de cada
// GIF, igual criterio que ya usaba el recuadro original.
//
// ⚠️ Discovery Kids HD — las fracciones de abajo se heredaron TAL CUAL de
// la app normal (calibradas sobre capturas del marco 4:3 pillarboxeado de
// ESA app). Si esta variante HD usa los mismos GIFs de NextProgram que la
// app normal, van a quedar mal alineadas porque acá el marco de referencia
// ya no es 4:3 pillarboxeado sino la pantalla completa — hace falta
// remedirlas sobre una captura real de esta app HD antes de dar esto por
// calibrado.
//
// nextprogram1.gif — recuadro grande.
private const val NEXTPROGRAM1_BOX_LEFT_FRACTION = 0.1705f
private const val NEXTPROGRAM1_BOX_RIGHT_FRACTION = 0.6902f
private const val NEXTPROGRAM1_BOX_TOP_FRACTION = 0.1335f
private const val NEXTPROGRAM1_BOX_BOTTOM_FRACTION = 0.6287f

// nextprogram2.gif — recuadro chico.
private const val NEXTPROGRAM2_BOX_LEFT_FRACTION = 0.0687f
private const val NEXTPROGRAM2_BOX_RIGHT_FRACTION = 0.3434f
private const val NEXTPROGRAM2_BOX_TOP_FRACTION = 0.3324f
private const val NEXTPROGRAM2_BOX_BOTTOM_FRACTION = 0.6023f

/**
 * Achica y reposiciona videoContainer para que el video quede dentro del
 * recuadro del marco NextProgram, sin estirarse. Idempotente — llamarla de
 * nuevo mientras ya está en el recuadro no hace nada raro.
 *
 * Discovery Kids HD — los % (NEXTPROGRAM1/2_BOX_*_FRACTION) se aplican
 * directamente sobre la pantalla completa (root.width × root.height): acá
 * NO hay un marco 4:3 pillarboxeado separado de la pantalla como en la app
 * normal (ver comentario largo arriba de este archivo — ELIMINADO por
 * completo AspectRatioFrameLayout).
 */
/**
 * RELEASE 2013.6.0.0 — extraída de showVideoInBox() para poder reusarla
 * también en scheduleNextProgramBug() (necesita saber cuál GIF está activo
 * para elegir NEXTPROGRAM1_SHOW_BEFORE_MS vs NEXTPROGRAM2_SHOW_BEFORE_MS).
 * true si el NextProgram de currentProgramIndex es nextprogram2.gif DE
 * FÁBRICA (no personalizado) — ver NEXTPROGRAMS.
 */
internal fun LiveDiscoveryKids.isCurrentNextProgramFactory2(): Boolean =
    !SettingsManager.isNextProgramCustom(this, currentProgramIndex) &&
        (currentProgramIndex % LiveDiscoveryKids.NEXTPROGRAMS.size) == 1

internal fun LiveDiscoveryKids.showVideoInBox() {
    val root = videoContainer.parent as? View ?: return
    val rootWidth = root.width
    val rootHeight = root.height
    if (rootWidth <= 0 || rootHeight <= 0) {
        // Layout todavía no midió (carrera rara al arrancar la Activity) —
        // reintenta en el próximo frame en vez de no hacer nada.
        videoContainer.post { showVideoInBox() }
        return
    }

    // Discovery Kids HD — el marco de referencia para las fracciones ES la
    // pantalla completa, sin reconstrucción de un 4:3 pillarboxeado (eso ya
    // no existe en esta variante — ver comentario largo arriba).
    val frameWidth = rootWidth
    val frameHeight = rootHeight

    // Preview 2013.6.0.0.3 — qué recuadro usar: nextprogram2.gif (índice 1
    // en NEXTPROGRAMS) tiene su propio recuadro chico; cualquier otro caso
    // (nextprogram1.gif de fábrica, o un NextProgram personalizado — ahí no
    // hay forma de saber su alineación real, así que se usa el recuadro
    // grande de nextprogram1 como default razonable) usa el recuadro grande.
    val isFactoryNextProgram2 = isCurrentNextProgramFactory2()

    val leftFraction = if (isFactoryNextProgram2) NEXTPROGRAM2_BOX_LEFT_FRACTION else NEXTPROGRAM1_BOX_LEFT_FRACTION
    val rightFraction = if (isFactoryNextProgram2) NEXTPROGRAM2_BOX_RIGHT_FRACTION else NEXTPROGRAM1_BOX_RIGHT_FRACTION
    val topFraction = if (isFactoryNextProgram2) NEXTPROGRAM2_BOX_TOP_FRACTION else NEXTPROGRAM1_BOX_TOP_FRACTION
    val bottomFraction = if (isFactoryNextProgram2) NEXTPROGRAM2_BOX_BOTTOM_FRACTION else NEXTPROGRAM1_BOX_BOTTOM_FRACTION

    val boxHeightPx = ((bottomFraction - topFraction) * frameHeight).toInt()
    val boxWidthPx = ((rightFraction - leftFraction) * frameWidth).toInt()
    val boxTopPx = (topFraction * frameHeight).toInt()
    val boxLeftPx = (leftFraction * frameWidth).toInt()

    val lp = FrameLayout.LayoutParams(boxWidthPx, boxHeightPx)
    lp.gravity = Gravity.TOP or Gravity.START
    lp.leftMargin = boxLeftPx
    lp.topMargin = boxTopPx
    videoContainer.layoutParams = lp
}

/** Vuelve videoContainer a pantalla completa (estado original de activity_main.xml). */
internal fun LiveDiscoveryKids.restoreVideoFullScreen() {
    val current = videoContainer.layoutParams as? FrameLayout.LayoutParams ?: return
    if (current.width == FrameLayout.LayoutParams.MATCH_PARENT && current.height == FrameLayout.LayoutParams.MATCH_PARENT) {
        return   // ya está a pantalla completa, no reasignar layoutParams sin necesidad (evita un re-layout de más)
    }
    val lp = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
    lp.gravity = Gravity.CENTER
    videoContainer.layoutParams = lp
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
 * en SettingsActivity y volvió).
 */
internal fun LiveDiscoveryKids.applySettings() {
    bugShowDelayMs = SettingsManager.getScreenbugDelaySec(this) * 1_000L
    breakIntervalMinMs = SettingsManager.getCommercialMinMinutes(this) * 60 * 1_000L
    breakIntervalMaxMs = SettingsManager.getCommercialMaxMinutes(this) * 60 * 1_000L

    // BUG FIX (2009.5.2.1 — investigación a fondo, corrige un error de diseño
    // de la 2009.5.1.0/2009.5.2.0): en la app normal, videoContainer
    // (AspectRatioFrameLayout) SIEMPRE está en 4:3 — ya no tiene ningún
    // toggle, no hace falta tocarlo acá.
    //
    // RELEASE 2013.6.0.0-HD — Discovery Kids HD ELIMINÓ por completo el
    // switch "Forzar 4:3" Y AspectRatioFrameLayout: videoContainer es un
    // FrameLayout simple sin ninguna caja 4:3 forzada, el video ocupa la
    // pantalla completa respetando su proporción real — ver DkVideoView.kt.
    // No hace falta sincronizar nada acá.
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