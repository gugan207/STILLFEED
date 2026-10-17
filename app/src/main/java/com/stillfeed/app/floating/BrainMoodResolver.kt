package com.stillfeed.app.floating

/**
 * Resolves the floating companion's mood and honest display value according to verified local data.
 *
 * Data Honesty Contract:
 * - We NEVER fabricate reel counts or convert monitored minutes into estimated reel numbers.
 * - When per-reel counts are unmeasured, the counter displays an em-dash ("—") or explicit minute indicator.
 * - Progress ratio is computed from actual measured activity relative to the user's daily allowance.
 */
object BrainMoodResolver {

    /**
     * Maps a normalized progress ratio (0.0 to 1.0+) to one of the 5 reference moods.
     */
    fun resolveMood(progressRatio: Float): BrainMood {
        return when {
            progressRatio < 0.25f -> BrainMood.FRESH
            progressRatio < 0.50f -> BrainMood.CALM
            progressRatio < 0.75f -> BrainMood.TIRED
            progressRatio < 1.00f -> BrainMood.SAD
            else -> BrainMood.WORN_OUT
        }
    }

    /**
     * Calculates the normalized progress ratio safely.
     */
    fun calculateProgressRatio(current: Long, allowance: Long): Float {
        if (allowance <= 0L) return 0f
        return (current.toFloat() / allowance.toFloat()).coerceAtLeast(0f)
    }

    /**
     * Resolves the display string and progress according to verified available data.
     */
    fun resolveDisplayAndProgress(
        reelCount: Int?,
        monitoredMinutes: Long,
        dailyAllowance: Int
    ): Pair<String, Float> {
        val allowance = dailyAllowance.coerceAtLeast(1).toLong()

        return if (reelCount != null) {
            val progress = calculateProgressRatio(reelCount.toLong(), allowance)
            Pair(reelCount.toString(), progress)
        } else {
            // Data honesty: reel count is unmeasured, so display honest em-dash "—"
            val progress = calculateProgressRatio(monitoredMinutes, allowance)
            Pair("—", progress)
        }
    }
}
