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
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

/**
 * Discovery Kids Channel – TV Simulator
 *
 * Playlist sequence:
 *   bumper → pro1 → bumper → pro2 → bumper → pro3 → bumper → pro4 → bumper  (then loops)
 *
 * Programs (pro1..pro4.mp4) are read from the user's Movies folder.
 * Bumper (bumper.mp4) and commercials (comercial1.mp4) are bundled in res/raw.
 * ScreenBug (screenbug.png) is an overlay image bundled in res/drawable.
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
    private lateinit var debugTextView: android.widget.TextView

    // ── Scheduling ─────────────────────────────────────────────────────────────
    private val handler = Handler(Looper.getMainLooper())
    private val pendingTasks = mutableListOf<Runnable>()
    private val debugHandler = Handler(Looper.getMainLooper())

    // ── Playlist definition ────────────────────────────────────────────────────
    private sealed class PlayItem {
        object Bumper : PlayItem()
        data class Program(val index: Int) : PlayItem()   // 0-based → pro(n+1).mp4
    }

    private val playlist = listOf(
        PlayItem.Bumper,
        PlayItem.Program(0),
        PlayItem.Bumper,
        PlayItem.Program(1),
        PlayItem.Bumper,
        PlayItem.Program(2),
        PlayItem.Bumper,
        PlayItem.Program(3),
        PlayItem.Bumper
    )

    private var playlistIndex = 0

    // ── Program state (persisted across commercial breaks) ─────────────────────
    private var currentProgramUri: Uri? = null
    private var programDuration  = 0          // total ms
    private var breakQueue       = mutableListOf<Int>()   // upcoming break positions in ms

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
        private const val MIN_DURATION_FOR_BREAKS = 2 * 60 * 1_000L  // 2 min

        /** Alpha-animation duration for screenbug fade. */
        private const val FADE_MS = 1_000L

        private const val PERM_REQUEST = 42
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

        requestStoragePermission()
        
        setupDebugInfo()

    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) goFullscreen()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelAllTasks()
        videoView.stopPlayback()
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
            is PlayItem.Bumper  -> playBumper()
            is PlayItem.Program -> playProgram(item.index)
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Bumper playback
    // ══════════════════════════════════════════════════════════════════════════

    private fun playBumper() {
        cancelAllTasks()
        setBugAlpha(0f)
        Log.d(TAG, "▶ BUMPER")

        playUri(rawUri(R.raw.bumper)) {
            playlistIndex++
            advance()
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Program playback
    // ══════════════════════════════════════════════════════════════════════════

    private fun playProgram(idx: Int) {
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

        // First play: prepare, calculate breaks, start from 0
        beginProgramSegment(uri, startOffsetMs = 0, isFirstPlay = true)
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
        }
        videoView.setOnCompletionListener {
            Log.d(TAG, "Program ended")
            cancelAllTasks()
            setBugAlpha(0f)
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

    // ══════════════════════════════════════════════════════════════════════════
    // Commercial playback
    // ══════════════════════════════════════════════════════════════════════════

    private fun playCommercial(resumeProgramAtMs: Int) {
        cancelAllTasks()
        setBugAlpha(0f)
        Log.d(TAG, "▶ YA VOLVEMOS (pre-commercial)")

        // Secuencia: ya_volvemos → comercial → ya_volvemos → programa
        playUri(rawUri(R.raw.ya_volvemos)) {
            Log.d(TAG, "▶ COMMERCIAL (resumes program at ${resumeProgramAtMs}ms)")

            playUri(rawUri(R.raw.comercial1)) {
                Log.d(TAG, "▶ YA VOLVEMOS (post-commercial)")

                playUri(rawUri(R.raw.ya_volvemos)) {
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
                "SDK: $sdkInt, $manufacturer $model, RAM Total: ${totalRamMB}MB, RAM Disponible: ${availableRamMB}MB"

                debugTextView.text = debugText

                debugHandler.postDelayed(this, 1000) // cada 1 segundo
            }
        }

        debugHandler.post(updateTask)
    }
}