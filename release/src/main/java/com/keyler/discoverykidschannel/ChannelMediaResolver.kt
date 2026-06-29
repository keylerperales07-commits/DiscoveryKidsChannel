/*
 ESTO TIENE LICENCIA GNU LICENSE APACHE

 NO ELIMINAR NINGUN COMENTARIO AGREGADO
*/
package com.keyler.discoverykidschannel

import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File

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
