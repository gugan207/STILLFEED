package com.stillfeed.app.widget.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetUiStateTest {

    @Test
    fun testDisplayCountFormatting() {
        val unmeasuredState = WidgetUiState(reelCount = null)
        assertEquals("—", unmeasuredState.displayCount)

        val measuredState = WidgetUiState(reelCount = 14)
        assertEquals("14", measuredState.displayCount)

        val zeroState = WidgetUiState(reelCount = 0)
        assertEquals("0", zeroState.displayCount)
    }

    @Test
    fun testFormattedMonitoredTime() {
        val state = WidgetUiState(monitoredMinutes = 35L)
        assertEquals("35m monitored", state.formattedMonitoredTime)
    }

    @Test
    fun testIsFullyProtectedLogic() {
        // Both active
        assertTrue(WidgetUiState(isProtectionActive = true, isAccessibilityGranted = true).isFullyProtected)

        // Protection toggled off
        assertFalse(WidgetUiState(isProtectionActive = false, isAccessibilityGranted = true).isFullyProtected)

        // Accessibility revoked
        assertFalse(WidgetUiState(isProtectionActive = true, isAccessibilityGranted = false).isFullyProtected)

        // Both inactive
        assertFalse(WidgetUiState(isProtectionActive = false, isAccessibilityGranted = false).isFullyProtected)
    }
}
