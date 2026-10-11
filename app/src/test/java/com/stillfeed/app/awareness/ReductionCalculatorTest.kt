package com.stillfeed.app.awareness

import com.stillfeed.app.awareness.reduction.ReductionCalculator
import com.stillfeed.app.domain.model.MonthlyAwarenessStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ReductionCalculator] (STEP 08 Section 5, 8 & 10).
 */
class ReductionCalculatorTest {

    private lateinit var calculator: ReductionCalculator

    @Before
    fun setUp() {
        calculator = ReductionCalculator()
    }

    @Test
    fun targetCalculation_forSupportedTiers_matchesFormula() {
        val baseline = 600 // 10 hours

        // 5% tier
        val reduction5 = calculator.calculateReductionAmount(baseline, 5)
        val target5 = calculator.calculateTargetUsage(baseline, 5)
        assertEquals(30, reduction5)
        assertEquals(570, target5)

        // 10% tier
        val reduction10 = calculator.calculateReductionAmount(baseline, 10)
        val target10 = calculator.calculateTargetUsage(baseline, 10)
        assertEquals(60, reduction10)
        assertEquals(540, target10)

        // 15% tier
        val reduction15 = calculator.calculateReductionAmount(baseline, 15)
        val target15 = calculator.calculateTargetUsage(baseline, 15)
        assertEquals(90, reduction15)
        assertEquals(510, target15)

        // 20% tier
        val reduction20 = calculator.calculateReductionAmount(baseline, 20)
        val target20 = calculator.calculateTargetUsage(baseline, 20)
        assertEquals(120, reduction20)
        assertEquals(480, target20)
    }

    @Test
    fun reductionCalculation_roundingIsIntegerSafeAndExact() {
        // 485 mins at 15% -> 485 * 0.15 = 72.75 -> half-up rounds to 73 mins
        val baseline = 485
        val reduction = calculator.calculateReductionAmount(baseline, 15)
        val target = calculator.calculateTargetUsage(baseline, 15)

        assertEquals(73, reduction)
        assertEquals(412, target)
        assertEquals(baseline, reduction + target)
    }

    @Test
    fun largeValues_doNotCauseIntegerOverflow() {
        // High multi-day minute value
        val largeBaseline = 50_000 // 50,000 mins
        val reduction = calculator.calculateReductionAmount(largeBaseline, 20)
        val target = calculator.calculateTargetUsage(largeBaseline, 20)

        assertEquals(10_000, reduction)
        assertEquals(40_000, target)
    }

    @Test
    fun zeroBaseline_producesZeroTargetAndZeroReductionSafely() {
        val reduction = calculator.calculateReductionAmount(0, 15)
        val target = calculator.calculateTargetUsage(0, 15)

        assertEquals(0, reduction)
        assertEquals(0, target)
    }

    @Test
    fun validatePercentage_rejectsUnsupportedValues() {
        assertThrows(IllegalArgumentException::class.java) {
            calculator.validatePercentage(0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            calculator.validatePercentage(7)
        }
        assertThrows(IllegalArgumentException::class.java) {
            calculator.validatePercentage(25)
        }
    }

    @Test
    fun determineStatus_resolvesCalmSemanticsWithoutJudgment() {
        // 1. Permission missing
        val permMissing = calculator.determineStatus(
            hasPermission = false,
            isBaselineSufficient = true,
            baselineMinutes = 600,
            percentage = 15,
            currentProgressMinutes = 100,
            targetUsageMinutes = 510
        )
        assertEquals(MonthlyAwarenessStatus.DATA_UNAVAILABLE, permMissing)

        // 2. Baseline forming
        val forming = calculator.determineStatus(
            hasPermission = true,
            isBaselineSufficient = false,
            baselineMinutes = 0,
            percentage = 15,
            currentProgressMinutes = 100,
            targetUsageMinutes = 0
        )
        assertEquals(MonthlyAwarenessStatus.BASELINE_FORMING, forming)

        // 3. Reducing (current usage is lower than historical baseline)
        val reducing = calculator.determineStatus(
            hasPermission = true,
            isBaselineSufficient = true,
            baselineMinutes = 600,
            percentage = 15,
            currentProgressMinutes = 400,
            targetUsageMinutes = 510
        )
        assertEquals(MonthlyAwarenessStatus.REDUCING, reducing)

        // 4. Target reached
        val reached = calculator.determineStatus(
            hasPermission = true,
            isBaselineSufficient = true,
            baselineMinutes = 600,
            percentage = 15,
            currentProgressMinutes = 515,
            targetUsageMinutes = 510
        )
        assertEquals(MonthlyAwarenessStatus.TARGET_REACHED, reached)
    }
}
