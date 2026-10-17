package com.stillfeed.app.floating

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import com.stillfeed.app.domain.repository.AppPreferencesRepository
import kotlin.math.max
import kotlin.math.min

/**
 * Manages the screen position, boundary clamping, and persistence for the floating companion overlay.
 */
class FloatingPositionManager(
    private val context: Context,
    private val appPreferencesRepository: AppPreferencesRepository
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    data class Position(val x: Int, val y: Int)
    data class ScreenBounds(val minX: Int, val maxX: Int, val minY: Int, val maxY: Int)

    /**
     * Calculates the safe screen bounds for a floating view with the specified dimensions.
     */
    fun calculateSafeBounds(viewWidth: Int, viewHeight: Int): ScreenBounds {
        val screenSize = getScreenDimensions()
        val topClearance = getTopCutoutOrStatusBarHeight()
        val navBarHeight = getNavigationBarHeight()

        val minX = 0
        val maxX = max(0, screenSize.x - viewWidth)
        val minY = topClearance + dpToPx(4)
        val maxY = max(minY, screenSize.y - navBarHeight - viewHeight - dpToPx(8))

        return ScreenBounds(minX = minX, maxX = maxX, minY = minY, maxY = maxY)
    }

    /**
     * Clamps a given position inside the safe screen bounds.
     */
    fun clamp(pos: Position, viewWidth: Int, viewHeight: Int): Position {
        val bounds = calculateSafeBounds(viewWidth, viewHeight)
        val clampedX = min(bounds.maxX, max(bounds.minX, pos.x))
        val clampedY = min(bounds.maxY, max(bounds.minY, pos.y))
        return Position(clampedX, clampedY)
    }

    /**
     * Returns the default top-centered position placed directly below the front camera punch hole / cutout.
     */
    fun getDefaultPosition(viewWidth: Int, viewHeight: Int): Position {
        val screenSize = getScreenDimensions()
        val topClearance = getTopCutoutOrStatusBarHeight()

        val defaultX = (screenSize.x - viewWidth) / 2
        val defaultY = topClearance + dpToPx(8)

        return clamp(Position(defaultX, defaultY), viewWidth, viewHeight)
    }

    /**
     * Resolves the initial position, restoring saved coordinates from DataStore if valid.
     */
    suspend fun resolveInitialPosition(viewWidth: Int, viewHeight: Int): Position {
        val prefs = appPreferencesRepository.getPreferences()
        val savedX = prefs.floatingCompanionPositionX
        val savedY = prefs.floatingCompanionPositionY

        return if (savedX >= 0 && savedY >= 0) {
            clamp(Position(savedX, savedY), viewWidth, viewHeight)
        } else {
            getDefaultPosition(viewWidth, viewHeight)
        }
    }

    /**
     * Persists the last settled position into DataStore.
     */
    suspend fun savePosition(pos: Position) {
        appPreferencesRepository.updatePreferences { prefs ->
            prefs.copy(
                floatingCompanionPositionX = pos.x,
                floatingCompanionPositionY = pos.y
            )
        }
    }

    /**
     * Resets the saved position to default in DataStore.
     */
    suspend fun resetPositionToDefault() {
        appPreferencesRepository.updatePreferences { prefs ->
            prefs.copy(
                floatingCompanionPositionX = -1,
                floatingCompanionPositionY = -1
            )
        }
    }

    private fun getScreenDimensions(): Point {
        val point = Point()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val bounds = windowMetrics.bounds
            point.x = bounds.width()
            point.y = bounds.height()
        } else {
            @Suppress("DEPRECATION")
            val display = windowManager.defaultDisplay
            @Suppress("DEPRECATION")
            display.getRealSize(point)
        }
        return point
    }

    private fun getTopCutoutOrStatusBarHeight(): Int {
        var top = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val insets = windowManager.currentWindowMetrics.windowInsets
                val cutoutTop = insets.displayCutout?.safeInsetTop ?: 0
                val statusBars = insets.getInsetsIgnoringVisibility(android.view.WindowInsets.Type.statusBars()).top
                top = max(cutoutTop, statusBars)
            } catch (_: Exception) {}
        }
        if (top <= 0) {
            top = getStatusBarHeight()
        }
        return max(top, dpToPx(32))
    }

    private fun getStatusBarHeight(): Int {
        val resId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resId > 0) context.resources.getDimensionPixelSize(resId) else dpToPx(24)
    }

    private fun getNavigationBarHeight(): Int {
        val resId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resId > 0) context.resources.getDimensionPixelSize(resId) else dpToPx(48)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
