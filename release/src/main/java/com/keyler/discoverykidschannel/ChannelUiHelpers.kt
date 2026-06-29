/*
 ESTO TIENE LICENCIA GNU LICENSE APACHE

 NO ELIMINAR NINGUN COMENTARIO AGREGADO
*/
package com.keyler.discoverykidschannel

import android.os.Build
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

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
