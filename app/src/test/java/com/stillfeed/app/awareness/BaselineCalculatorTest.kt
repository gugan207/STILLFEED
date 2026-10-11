package com.stillfeed.app.awareness

import com.stillfeed.app.awareness.baseline.BaselineCalculator
import com.stillfeed.app.usage.aggregation.DailyUsageSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [BaselineCalculator] (STEP 08 Section 4 & 10).
 */
class BaselineCalculatorTest {

    private lateinit var calculator: BaselineCalculator

    @Before
    fun setUp() {
        calculator = BaselineCalculator()
    }

    @Test
    fun sufficientBaselineData_calculatesAccurateAverageMinutes() {
        // 7 days with varying usage: 50, 60, 70, 80, 50, 60, 50 mins (sum = 420 mins, avg = 60 mins)
        val dailySummaries = listOf(
            createSummary("2026-09-24", 50),
            createSummary("2026-09-25", 60),
            createSummary("2026-09-26", 70),
            createSummary("2026-09-27", 80),
            createSummary("2026-09-28", 50),
            createSummary("2026-09-29", 60),
            createSummary("2026-09-30", 50)
        )

        val result = calculator.calculateBaseline(dailySummaries)
        assertTrue(result.isSufficient)
        assertEquals(7, result.qualifiedDaysCount)
        assertEquals(60, result.baselineMinutes)
    }

    @Test
    fun insufficientData_lessThanSevenDays_returnsZeroBaselineAndFormingStatus() {
        val dailySummaries = listOf(
            createSummary("2026-09-27", 60),
            createSummary("2026-09-28", 60),
            createSummary("2026-09-29", 60),
            createSummary("2026-09-30", 60)
        )

        val result = calculator.calculateBaseline(dailySummaries)
        assertFalse(result.isSufficient)
        assertEquals(4, result.qualifiedDaysCount)
        assertEquals(0, result.baselineMinutes)
    }

    @Test
    fun zeroUsage_acrossAllDays_returnsZeroBaseline() {
        val dailySummaries = (1..7).map { day ->
            createSummary("2026-09-0$day", 0)
        }

        val result = calculator.calculateBaseline(dailySummaries)
        assertTrue(result.isSufficient)
        assertEquals(7, result.qualifiedDaysCount)
        assertEquals(0, result.baselineMinutes)
    }

    @Test
    fun incompleteDays_todayExcludedFromBaselineFormation() {
        // 8 days provided, but today (2026-10-01) is incomplete
        val dailySummaries = (24..30).map { day ->
            createSummary("2026-09-$day", 60)
        } + listOf(createSummary("2026-10-01", 10))

        val result = calculator.calculateBaseline(dailySummaries, todayDateString = "2026-10-01")
        assertTrue(result.isSufficient)
        assertEquals(7, result.qualifiedDaysCount)
        assertEquals(60, result.baselineMinutes)
    }

    @Test
    fun outlierHandling_extremeAutoplayDayIsClampedAgainstMedian() {
        // 6 days of 60 mins, 1 day of 1080 mins (18 hours autoplay)
        val dailySummaries = listOf(
            createSummary("2026-09-24", 60),
            createSummary("2026-09-25", 60),
            createSummary("2026-09-26", 60),
            createSummary("2026-09-27", 60),
            createSummary("2026-09-28", 60),
            createSummary("2026-09-29", 60),
            createSummary("2026-09-30", 1080) // extreme outlier
        )

        val result = calculator.calculateBaseline(dailySummaries)
        assertTrue(result.isSufficient)
        // Median is 60. Outlier cap = 2.5 * 60 = 150 mins.
        // Clamped sum = 6 * 60 + 150 = 360 + 150 = 510 mins.
        // Average = (510 + 3) / 7 = 73 mins.
        assertEquals(73, result.baselineMinutes)
    }

    @Test
    fun deterministicRepeatedCalculation_yieldsIdenticalResults() {
        val dailySummaries = (1..14).map { day ->
            createSummary("2026-09-%02d".format(day), 45 + (day * 2))
        }

        val run1 = calculator.calculateBaseline(dailySummaries)
        val run2 = calculator.calculateBaseline(dailySummaries)
        val run3 = calculator.calculateBaseline(dailySummaries)

        assertEquals(run1.baselineMinutes, run2.baselineMinutes)
        assertEquals(run1.baselineMinutes, run3.baselineMinutes)
        assertEquals(run1.qualifiedDaysCount, run2.qualifiedDaysCount)
    }

    private fun createSummary(dateString: String, minutes: Int): DailyUsageSummary {
        val millis = minutes * 60000L
        return DailyUsageSummary(
            dateString = dateString,
            instagramUsageMillis = millis / 2,
            youtubeUsageMillis = millis / 2,
            totalMonitoredMillis = millis,
            blockedInterventionCount = 0,
            sessionCount = 1
        )
    }
}
