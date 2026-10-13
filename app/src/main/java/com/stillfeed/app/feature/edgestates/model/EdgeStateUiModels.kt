package com.stillfeed.app.feature.edgestates.model

/**
 * UI state for Screen 15 — Daily Limit / Batch Finished (Figma Node 70:198).
 *
 * Requirements:
 * - Displays only upon a verified batch-expiration event from Step 19.
 * - Distinguishes batch expiration from daily allowance and monthly targets.
 * - Reel count is displayed only when supplied by a reliable source.
 * - Never substitutes monitored minutes or fabricated counters for Reels watched.
 * - Does not independently grant another unlock.
 */
data class BatchFinishedUiState(
    val isBatchExpired: Boolean = true,
    val batchUnlockedReelsCount: Int? = null, // e.g. 20 (supplied by Step 19 if reliable)
    val batchDurationMinutes: Int? = null,     // Duration of the expired batch
    val todayReelsWatched: Int? = null,        // e.g. 32 (supplied if reliable)
    val todayMonitoredMinutes: Int = 0,
    val isReelsCountAvailable: Boolean = false,// Strictly false until verified single-reel tracking
    val dailyAllowanceReels: Int = 30,         // Persisted daily baseline from onboarding
    val monthlyTargetPercentage: Int? = null,  // Independent monthly target (e.g. 10%)
    val headline: String = "That batch is done.",
    val body: String = if (isReelsCountAvailable && batchUnlockedReelsCount != null) {
        "You watched $batchUnlockedReelsCount reels in this unlock. Take a reset if you want another small batch."
    } else {
        "Your unlocked batch is done. Take a reset if you want another small batch."
    },
    val mindStateLabel: String = "Your mind could use a little space.",
    val footerNote: String = "You stay in control of the next unlock.",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * UI state for Screen 16 — Permission Disabled (Figma Node 70:221).
 *
 * Requirements:
 * - Represents Accessibility Service and Usage Access separately.
 * - Explains which capability is unavailable for each missing permission.
 * - Never claims protection is active without verifying required permissions.
 * - Preserves access to the rest of STILLFEED.
 */
data class PermissionDisabledUiState(
    val isAccessibilityGranted: Boolean = false,
    val isUsageAccessGranted: Boolean = false,
    val isProtectionPaused: Boolean = !isAccessibilityGranted || !isUsageAccessGranted,
    val headline: String = "Your protection is paused.",
    val body: String = "SKILLFEED needs permission to keep short video blocked.",
    val cardTitle: String = "Access needs attention.",
    val cardDetail: String = "Your Stories and DMs remain yours.",
    val accessibilityTitle: String = "Accessibility Service",
    val accessibilityDescription: String = "Required to recognize Reels & Shorts surfaces and keep your boundary.",
    val usageAccessTitle: String = "Usage Access",
    val usageAccessDescription: String = "Required to monitor attention duration without private data.",
    val safeNote: String = "Nothing is lost while protection is paused."
)

/**
 * UI state for Screen 17 — No Internet / Offline State (Figma Node 70:241).
 *
 * Requirements:
 * - Distinguishes network connectivity from local on-device protection.
 * - Does not disable local protection merely because the internet is unavailable.
 * - Claims "STILL PROTECTED" only when local permissions and accessibility service are verified.
 * - Retry action only triggers verifiable network evaluation; zero fake APIs.
 */
data class OfflineUiState(
    val isOnline: Boolean = false,
    val isLocallyProtected: Boolean = true, // Verified against local permission checkers
    val todayReelsWatched: Int? = null,
    val todayMonitoredMinutes: Int = 0,
    val isReelsCountAvailable: Boolean = false,
    val headline: String = "You’re offline. That’s okay.",
    val body: String = "Your current protection and today’s count stay on this device.",
    val detailText: String = "Reconnect when you’re ready.",
    val offlineNote: String = "Awareness cards already saved can still be shown.",
    val isRetrying: Boolean = false,
    val feedbackMessage: String? = null
)

/**
 * UI state for Screen 18 — First Day / Empty State (Figma Node 70:261).
 *
 * Requirements:
 * - Supports a genuine new-user or no-history state.
 * - Explains what will appear as reliable usage data becomes available.
 * - Does not fabricate daily or historical Reel counts.
 * - Does not interpret missing data as a verified zero.
 * - Distinguishes unavailable Reel-count metrics from valid monitored-time metrics.
 */
data class FirstDayUiState(
    val isFirstDay: Boolean = true,
    val todayReelsWatched: Int? = null, // 0 if reliable zero, null if unintercepted
    val todayMonitoredMinutes: Int = 0,
    val isReelsCountAvailable: Boolean = false,
    val headline: String = "Start with noticing.",
    val body: String = "There’s no baseline yet. SKILLFEED will learn your rhythm without judging it.",
    val mindStateLabel: String = "Your mind looks fresh.",
    val referenceNote: String = "Your first week becomes your reference point."
)
