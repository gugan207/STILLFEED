package com.stillfeed.app.domain.model

/**
 * Domain model representing the current persistent protection toggle state.
 *
 * NOTE: The actual blocking engine, accessibility monitor, and active interception
 * are deferred to subsequent verified steps.
 */
data class ProtectionState(
    val isEnabled: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis()
)
