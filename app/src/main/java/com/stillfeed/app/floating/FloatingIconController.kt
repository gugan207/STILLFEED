package com.stillfeed.app.floating

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/**
 * Controller providing a clean Kotlin API to orchestrate the floating companion lifecycle.
 */
class FloatingIconController(
    private val context: Context,
    private val overlayPermissionManager: OverlayPermissionManager
) {
    /**
     * Starts the floating companion overlay service if permission is granted.
     */
    fun startCompanion() {
        if (!overlayPermissionManager.hasOverlayPermission()) return

        val intent = Intent(context, FloatingOverlayService::class.java).apply {
            action = FloatingOverlayService.ACTION_START
        }
        ContextCompat.startForegroundService(context, intent)
    }

    /**
     * Stops the floating companion overlay service and removes the floating icon from the screen.
     */
    fun stopCompanion() {
        val intent = Intent(context, FloatingOverlayService::class.java).apply {
            action = FloatingOverlayService.ACTION_STOP
        }
        context.startService(intent)
    }

    /**
     * Resets the companion's screen position to the default top-centered location.
     */
    fun resetPosition() {
        val intent = Intent(context, FloatingOverlayService::class.java).apply {
            action = FloatingOverlayService.ACTION_RESET_POSITION
        }
        context.startService(intent)
    }

    /**
     * Broadcasts an immediate update request to refresh companion data.
     */
    fun requestUpdate() {
        val intent = Intent(context, FloatingOverlayService::class.java).apply {
            action = FloatingOverlayService.ACTION_UPDATE_DATA
        }
        context.startService(intent)
    }
}
