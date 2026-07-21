/*
 ESTO TIENE LICENCIA GNU LICENSE APACHE

 NO ELIMINAR NINGUN COMENTARIO AGREGADO
*/
package com.keyler.discoverykidschannel

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.view.View
import android.widget.FrameLayout

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
