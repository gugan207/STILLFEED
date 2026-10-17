package com.stillfeed.app.floating

import org.junit.Assert.assertEquals
import org.junit.Test

class BrainMoodResolverTest {

    @Test
    fun `resolveMood maps exact 5 reference tiers correctly`() {
        assertEquals(BrainMood.FRESH, BrainMoodResolver.resolveMood(0.0f))
        assertEquals(BrainMood.FRESH, BrainMoodResolver.resolveMood(0.24f))
        assertEquals(BrainMood.CALM, BrainMoodResolver.resolveMood(0.25f))
        assertEquals(BrainMood.CALM, BrainMoodResolver.resolveMood(0.49f))
        assertEquals(BrainMood.TIRED, BrainMoodResolver.resolveMood(0.50f))
        assertEquals(BrainMood.TIRED, BrainMoodResolver.resolveMood(0.74f))
        assertEquals(BrainMood.SAD, BrainMoodResolver.resolveMood(0.75f))
        assertEquals(BrainMood.SAD, BrainMoodResolver.resolveMood(0.99f))
        assertEquals(BrainMood.WORN_OUT, BrainMoodResolver.resolveMood(1.00f))
        assertEquals(BrainMood.WORN_OUT, BrainMoodResolver.resolveMood(1.50f))
    }

    @Test
    fun `calculateProgressRatio handles edge cases and standard allowances`() {
        assertEquals(0f, BrainMoodResolver.calculateProgressRatio(10L, 0L), 0.001f)
        assertEquals(0f, BrainMoodResolver.calculateProgressRatio(0L, 30L), 0.001f)
        assertEquals(0.5f, BrainMoodResolver.calculateProgressRatio(15L, 30L), 0.001f)
        assertEquals(1.0f, BrainMoodResolver.calculateProgressRatio(30L, 30L), 0.001f)
        assertEquals(1.5f, BrainMoodResolver.calculateProgressRatio(45L, 30L), 0.001f)
    }

    @Test
    fun `resolveDisplayAndProgress enforces data honesty contract when reel count is null`() {
        val (display, progress) = BrainMoodResolver.resolveDisplayAndProgress(
            reelCount = null,
            monitoredMinutes = 15L,
            dailyAllowance = 30
        )

        // Must display em-dash when reel count is unmeasured
        assertEquals("—", display)
        assertEquals(0.5f, progress, 0.001f)
    }

    @Test
    fun `resolveDisplayAndProgress formats verified reel counts correctly`() {
        val (display, progress) = BrainMoodResolver.resolveDisplayAndProgress(
            reelCount = 7,
            monitoredMinutes = 20L,
            dailyAllowance = 28
        )

        assertEquals("7", display)
        assertEquals(0.25f, progress, 0.001f)
    }
}
