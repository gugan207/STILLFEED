package com.stillfeed.app.usage.analysis

import com.stillfeed.app.domain.model.SurfaceCategory
import java.util.UUID

/**
 * Domain model representing an analyzed contiguous foreground application session.
 *
 * Privacy Guarantees:
 * - NO user text, captions, messages, or screen recordings.
 * - Stores solely structural package attribution, session boundary timestamps, and duration.
 */
data class ForegroundSession(
    val id: String = UUID.randomUUID().toString(),
    val packageName: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val durationMillis: Long,
    val surfaceCategory: SurfaceCategory = SurfaceCategory.FEED,
    val isIntervened: Boolean = false
)
