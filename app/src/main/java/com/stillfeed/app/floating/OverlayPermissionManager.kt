package com.stillfeed.app.floating

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Manages checking and requesting the Android SYSTEM_ALERT_WINDOW (Overlay) permission.
 */
class OverlayPermissionManager(
    private val context: Context
) {
    /**
     * Checks if STILLFEED currently has permission to draw floating overlays over other apps.
     */
    fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Creates an Intent to navigate the user directly to the overlay permission grant screen.
     */
    fun createRequestOverlayPermissionIntent(): Intent {
        val packageUri = Uri.parse("package:${context.packageName}")
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // OEM compatibility fallback: some custom ROMs reject package-specific URIs
        return if (intent.resolveActivity(context.packageManager) != null) {
            intent
        } else {
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }
}
