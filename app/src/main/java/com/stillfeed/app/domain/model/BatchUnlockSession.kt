package com.stillfeed.app.domain.model

/**
 * Domain model representing an active batch viewing unlock session (STEP 19).
 *
 * Requirements:
 * 1. Represents intentional batch viewing unlocked after completing a Reset Activity
 *    and Awareness Cards reflection.
 * 2. Scoped to a source [packageName] (e.g., Instagram Reels or YouTube Shorts).
 * 3. Atomic remaining allowance tracking.
 * 4. Separate from the default daily allowance established during onboarding.
 * 5. Lifecycle-bounded with an expiration window.
 */
data class BatchUnlockSession(
    val id: String,
    val packageName: String,
    val selectedBatchAllowance: Int,
    val remainingBatchAllowance: Int,
    val activityType: String,
    val verifiedActivityCompletion: Boolean,
    val createdAtMillis: Long,
    val expiresAtMillis: Long = createdAtMillis + DEFAULT_EXPIRATION_WINDOW_MS,
    val isIndeterminateTracking: Boolean = false
) {
    val isExhausted: Boolean
        get() = remainingBatchAllowance <= 0

    val isExpired: Boolean
        get() = System.currentTimeMillis() > expiresAtMillis

    val isValid: Boolean
        get() = verifiedActivityCompletion && !isExhausted && !isExpired

    companion object {
        const val DEFAULT_EXPIRATION_WINDOW_MS = 60 * 60 * 1000L // 1 hour validity

        val SUPPORTED_BATCH_OPTIONS = listOf(5, 10, 20, 30)
    }
}
