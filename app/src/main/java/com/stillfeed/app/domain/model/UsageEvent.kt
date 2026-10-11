package com.stillfeed.app.domain.model

/**
 * Surface categories classified by STILLFEED attention detection.
 */
enum class SurfaceCategory {
    REELS,
    SHORTS,
    FEED,
    UNKNOWN
}

/**
 * Lifecycle and intervention types for usage events.
 */
enum class UsageEventType {
    SESSION_START,
    SESSION_END,
    CONTENT_DETECTED,
    BLOCKED,
    UNLOCKED,
    HEARTBEAT
}

/**
 * Structured privacy-minimal domain model for recording application attention events.
 *
 * Privacy Guarantees:
 * - NO screenshots or graphical content.
 * - NO user messages, captions, or textual conversation data.
 * - NO video frames, audio, or media content.
 * - NO user credentials or passwords.
 * - Records solely package names, structural surface types, intervention states, and timestamps.
 */
data class UsageEvent(
    val id: String,
    val timestampMillis: Long,
    val packageName: String,
    val surfaceCategory: SurfaceCategory,
    val eventType: UsageEventType,
    val durationMillis: Long? = null,
    val isBlocked: Boolean = false,
    val isUnlocked: Boolean = false,
    val metadata: String? = null
)
