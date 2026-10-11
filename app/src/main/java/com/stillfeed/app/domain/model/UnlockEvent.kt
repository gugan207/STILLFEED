package com.stillfeed.app.domain.model

/**
 * Domain model representing an intentional friction-unlock event.
 *
 * NOTE: The unlock business rules, cooldown gates, and limit checks
 * are deferred to subsequent verified steps.
 */
data class UnlockEvent(
    val id: String,
    val timestampMillis: Long,
    val allowanceMinutes: Int,
    val reason: String,
    val relatedUsageEventId: String? = null
)
