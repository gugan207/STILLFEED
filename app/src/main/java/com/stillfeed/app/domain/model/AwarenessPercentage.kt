package com.stillfeed.app.domain.model

/**
 * Supported monthly awareness reduction percentages (STEP 08 Section 3).
 *
 * Product Rule:
 * - Supported choices are strictly: 5%, 10%, 15%, 20%.
 * - No other percentage may be silently introduced or accepted.
 */
enum class AwarenessPercentage(val value: Int) {
    FIVE(5),
    TEN(10),
    FIFTEEN(15),
    TWENTY(20);

    companion object {
        val VALID_PERCENTAGES: Set<Int> = entries.map { it.value }.toSet()

        /**
         * Returns true if [percentage] matches one of the 4 allowed values (5, 10, 15, 20).
         */
        fun isValid(percentage: Int): Boolean = percentage in VALID_PERCENTAGES

        /**
         * Resolves the matching [AwarenessPercentage] enum or null if invalid.
         */
        fun fromInt(percentage: Int): AwarenessPercentage? =
            entries.find { it.value == percentage }
    }
}
