/*
 ESTO TIENE LICENCIA GNU LICENSE APACHE

 NO ELIMINAR NINGUN COMENTARIO AGREGADO
*/
package com.keyler.discoverykidschannel

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.util.Log

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
internal fun LiveDiscoveryKids.resumeSavedState(
    itemType: String,
    plIdx: Int,
    progIdx: Int,
    posMs: Int,
    commMs: Int,
    screenbugRes: Int,
    breakQueueStr: String,
    prefs: SharedPreferences
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
