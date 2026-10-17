package com.stillfeed.app.floating

/**
 * Reactive UI state representing the current floating companion overlay.
 */
data class FloatingIconUiState(
    val displayValue: String = "—",
    val progressRatio: Float = 0.0f,
    val mood: BrainMood = BrainMood.FRESH,
    val monitoredMinutes: Long = 0L,
    val dailyAllowance: Int = 30,
    val isProtectionActive: Boolean = true,
    val isAccessibilityGranted: Boolean = true,
    val isAnimationsEnabled: Boolean = true,
    val isOffline: Boolean = false,
    val isVisible: Boolean = true
) {
    val statusSummary: String
        get() = when {
            !isAccessibilityGranted || !isProtectionActive -> "Protection paused"
            else -> mood.editorialMessage
        }
}
