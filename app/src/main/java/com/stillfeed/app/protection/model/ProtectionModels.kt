package com.stillfeed.app.protection.model

import com.stillfeed.app.domain.model.SurfaceCategory

/**
 * Privacy-safe structural window snapshot extracted from an [android.view.accessibility.AccessibilityEvent]
 * and active window hierarchy.
 *
 * PRIVACY GUARANTEES (STEP 06 Phase 9):
 * - NO user message bodies, chat text, or captions.
 * - NO search queries, input fields, or passwords.
 * - NO user account handles or personal information.
 * - NO video frames, image data, screenshots, or audio.
 * - Retains ONLY structural view resource IDs and class names necessary for layout surface classification.
 */
data class AccessibilityWindowContext(
    val packageName: String,
    val rootClassName: String? = null,
    val activeViewIds: Set<String> = emptySet(),
    val structuralTags: Set<String> = emptySet(),
    val windowId: Int = 0,
    val timestampMillis: Long = System.currentTimeMillis(),
    val eventType: Int = 0,
    val contentTransitionToken: String? = null
) {
    /**
     * Checks whether any of the collected view IDs contains the given [substring].
     */
    fun hasViewIdContaining(substring: String): Boolean {
        return activeViewIds.any { it.contains(substring, ignoreCase = true) }
    }

    /**
     * Checks whether any collected structural tag contains the given [substring].
     */
    fun hasTagContaining(substring: String): Boolean {
        return structuralTags.any { it.contains(substring, ignoreCase = true) }
    }
}

/**
 * Result of surface classification by a [com.stillfeed.app.protection.detection.SurfaceClassifier].
 */
data class SurfaceClassification(
    val surfaceCategory: SurfaceCategory,
    val isProtectedShortForm: Boolean,
    val confidence: Float = 1.0f,
    val matchedSignal: String? = null
) {
    companion object {
        fun allowed(surfaceCategory: SurfaceCategory = SurfaceCategory.UNKNOWN, signal: String? = null) =
            SurfaceClassification(
                surfaceCategory = surfaceCategory,
                isProtectedShortForm = false,
                confidence = 1.0f,
                matchedSignal = signal
            )

        fun blocked(surfaceCategory: SurfaceCategory, signal: String) =
            SurfaceClassification(
                surfaceCategory = surfaceCategory,
                isProtectedShortForm = true,
                confidence = 1.0f,
                matchedSignal = signal
            )
    }
}

/**
 * Outcome of a protection evaluation by [com.stillfeed.app.protection.engine.ProtectionEngine].
 */
sealed class ProtectionDecision {
    /**
     * The visible surface is a protected short-form algorithmic video surface (Reels or Shorts),
     * and protection is currently enabled. Immediate intervention must occur.
     */
    data class Block(
        val packageName: String,
        val surfaceCategory: SurfaceCategory,
        val reason: String,
        val isBatchExpired: Boolean = false,
        val batchAllowance: Int? = null,
        val batchDurationMinutes: Int? = null
    ) : ProtectionDecision()

    /**
     * The visible surface is permitted (e.g. DM, Story, Feed, Profile, standard YouTube video, or non-target app).
     */
    data class Allow(
        val packageName: String,
        val surfaceCategory: SurfaceCategory,
        val reason: String = "Allowed surface"
    ) : ProtectionDecision()

    /**
     * Protection is currently toggled OFF by the user. Content is allowed through without intervention.
     */
    data object ProtectionDisabled : ProtectionDecision()
}
