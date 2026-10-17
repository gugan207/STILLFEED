package com.stillfeed.app.widget.model

/**
 * UI State for STILLFEED Home Screen Widgets.
 *
 * Requirements enforced:
 * 1. Data Honesty: [reelCount] is non-null ONLY when genuine, verified Reel count is available.
 *    If unmeasured, it remains null and renders an em-dash ("—"). Never fabricates Reel numbers.
 * 2. Real Monitored Time: [monitoredMinutes] reflects verified duration from Room.
 * 3. Editorial Copy: [editorialMessage] and [editorialHeadline] match the calm, human tone from Figma.
 * 4. Protection & Permissions: Accurately reflects [isProtectionActive] and [isAccessibilityGranted].
 */
data class WidgetUiState(
    val theme: WidgetTimeTheme = WidgetTimeTheme.DAY,
    val reelCount: Int? = null,
    val monitoredMinutes: Long = 0L,
    val dailyAllowanceMinutes: Int = 30,
    val blockedInterventions: Int = 0,
    val isProtectionActive: Boolean = true,
    val isAccessibilityGranted: Boolean = true,
    val editorialMessage: String = "Your mind looks fresh.",
    val editorialHeadline: String = "Your attention is still yours.",
    val isOffline: Boolean = false
) {
    /**
     * Formatted reel count string: displays genuine number if available, or em-dash "—"
     */
    val displayCount: String
        get() = reelCount?.toString() ?: "—"

    /**
     * Monitored time label (e.g. "12m monitored")
     */
    val formattedMonitoredTime: String
        get() = "${monitoredMinutes}m monitored"

    /**
     * True if protection is fully operational
     */
    val isFullyProtected: Boolean
        get() = isProtectionActive && isAccessibilityGranted
}
