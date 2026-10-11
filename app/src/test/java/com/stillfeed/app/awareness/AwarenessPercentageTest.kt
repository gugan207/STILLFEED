package com.stillfeed.app.awareness

import com.stillfeed.app.domain.model.AwarenessPercentage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [AwarenessPercentage] (STEP 08 Section 3 & 10).
 */
class AwarenessPercentageTest {

    @Test
    fun supportedPercentages_onlyInclude_5_10_15_20() {
        assertEquals(setOf(5, 10, 15, 20), AwarenessPercentage.VALID_PERCENTAGES)

        assertTrue(AwarenessPercentage.isValid(5))
        assertTrue(AwarenessPercentage.isValid(10))
        assertTrue(AwarenessPercentage.isValid(15))
        assertTrue(AwarenessPercentage.isValid(20))

        assertEquals(AwarenessPercentage.FIVE, AwarenessPercentage.fromInt(5))
        assertEquals(AwarenessPercentage.TEN, AwarenessPercentage.fromInt(10))
        assertEquals(AwarenessPercentage.FIFTEEN, AwarenessPercentage.fromInt(15))
        assertEquals(AwarenessPercentage.TWENTY, AwarenessPercentage.fromInt(20))
    }

    @Test
    fun invalidPercentages_areStrictlyRejected() {
        val invalidCandidates = listOf(0, -5, -10, 1, 7, 12, 25, 50, 100)

        for (candidate in invalidCandidates) {
            assertFalse("Expected $candidate to be invalid", AwarenessPercentage.isValid(candidate))
            assertNull("Expected fromInt($candidate) to return null", AwarenessPercentage.fromInt(candidate))
        }
    }
}
