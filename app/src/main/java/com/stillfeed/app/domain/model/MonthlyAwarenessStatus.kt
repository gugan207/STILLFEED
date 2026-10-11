package com.stillfeed.app.domain.model

/**
 * Calm, non-judgmental monthly awareness state indicators (STEP 08 Section 8).
 *
 * PRODUCT CONSTRAINTS:
 * - NEVER score out of 100.
 * - NEVER use shame, guilt, failure, lazy, streak penalties, or punishment language.
 */
enum class MonthlyAwarenessStatus(val displayLabel: String) {
    BASELINE_FORMING("Baseline forming"),
    ON_TRACK("On track"),
    REDUCING("Reducing"),
    TARGET_REACHED("Target reached"),
    DATA_UNAVAILABLE("Data unavailable");

    companion object {
        fun fromString(value: String): MonthlyAwarenessStatus =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: BASELINE_FORMING
    }
}
