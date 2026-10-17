package com.stillfeed.app.floating

import org.junit.Assert.assertEquals
import org.junit.Test

class FloatingIconUiStateTest {

    @Test
    fun `statusSummary reflects protection pause when accessibility or protection is inactive`() {
        val pausedAccessibilityState = FloatingIconUiState(
            displayValue = "—",
            mood = BrainMood.FRESH,
            isProtectionActive = true,
            isAccessibilityGranted = false
        )
        assertEquals("Protection paused", pausedAccessibilityState.statusSummary)

        val pausedProtectionState = FloatingIconUiState(
            displayValue = "—",
            mood = BrainMood.FRESH,
            isProtectionActive = false,
            isAccessibilityGranted = true
        )
        assertEquals("Protection paused", pausedProtectionState.statusSummary)
    }

    @Test
    fun `statusSummary displays editorial message when active`() {
        val activeFresh = FloatingIconUiState(
            displayValue = "—",
            mood = BrainMood.FRESH,
            isProtectionActive = true,
            isAccessibilityGranted = true
        )
        assertEquals("Your mind looks fresh.", activeFresh.statusSummary)

        val activeCalm = FloatingIconUiState(
            displayValue = "—",
            mood = BrainMood.CALM,
            isProtectionActive = true,
            isAccessibilityGranted = true
        )
        assertEquals("Your companion looks calm.", activeCalm.statusSummary)

        val activeWorn = FloatingIconUiState(
            displayValue = "—",
            mood = BrainMood.WORN_OUT,
            isProtectionActive = true,
            isAccessibilityGranted = true
        )
        assertEquals("Your companion is worn out. That is the limit for today.", activeWorn.statusSummary)
    }
}
