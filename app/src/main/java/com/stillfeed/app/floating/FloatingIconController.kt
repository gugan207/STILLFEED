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
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (_: Exception) {}
    }

    /**
     * Stops the floating companion overlay service and removes the floating icon from the screen.
     */
    fun stopCompanion() {
        val intent = Intent(context, FloatingOverlayService::class.java).apply {
            action = FloatingOverlayService.ACTION_STOP
        }
        try {
            context.startService(intent)
        } catch (_: Exception) {}
    }

    /**
     * Controls visibility of the floating companion specifically for Instagram Reels sessions.
     * When Reels is open, shows the icon at top-center. When exited, hides the icon.
     */
    fun setReelsActive(active: Boolean) {
        if (!overlayPermissionManager.hasOverlayPermission()) return

        val intent = Intent(context, FloatingOverlayService::class.java).apply {
            action = FloatingOverlayService.ACTION_SET_REELS_ACTIVE
            putExtra(FloatingOverlayService.EXTRA_REELS_ACTIVE, active)
        }
        try {
            if (active) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        } catch (_: Exception) {}
    }

    /**
     * Updates the calculated organic reel count on the floating icon in real time.
     */
    fun updateReelCount(count: Int) {
        val intent = Intent(context, FloatingOverlayService::class.java).apply {
            action = FloatingOverlayService.ACTION_UPDATE_REEL_COUNT
            putExtra(FloatingOverlayService.EXTRA_REEL_COUNT, count)
        }
        try {
            context.startService(intent)
        } catch (_: Exception) {}
    }

    /**
     * Resets the companion's screen position to the default top-centered location.
     */
    fun resetPosition() {
        val intent = Intent(context, FloatingOverlayService::class.java).apply {
            action = FloatingOverlayService.ACTION_RESET_POSITION
        }
        try {
            context.startService(intent)
        } catch (_: Exception) {}
    }

    /**
     * Broadcasts an immediate update request to refresh companion data.
     */
    fun requestUpdate() {
        val intent = Intent(context, FloatingOverlayService::class.java).apply {
            action = FloatingOverlayService.ACTION_UPDATE_DATA
        }
        try {
            context.startService(intent)
        } catch (_: Exception) {}
    }
}
