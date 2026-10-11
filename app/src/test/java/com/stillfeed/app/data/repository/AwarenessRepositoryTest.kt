package com.stillfeed.app.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stillfeed.app.data.local.StillfeedDatabase
import com.stillfeed.app.domain.model.AwarenessMonthlyState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AwarenessRepositoryTest {

    private lateinit var database: StillfeedDatabase
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: LocalAwarenessRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = StillfeedDatabase.buildInMemory(context)
        repository = LocalAwarenessRepository(database.awarenessMonthlyDao(), testDispatcher)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createMonthlyState_and_readMonthlyState_succeeds() = runTest(testDispatcher) {
        val state = AwarenessMonthlyState(
            monthIdentifier = "2026-10",
            targetReductionPercentage = 15,
            baselineMinutes = 480,
            currentProgressMinutes = 45,
            createdAtMillis = 1000L,
            updatedAtMillis = 1000L
        )
        repository.saveMonthlyState(state)

        val retrieved = repository.getMonthlyState("2026-10")
        assertNotNull(retrieved)
        assertEquals("2026-10", retrieved?.monthIdentifier)
        assertEquals(15, retrieved?.targetReductionPercentage)
        assertEquals(480, retrieved?.baselineMinutes)
        assertEquals(45, retrieved?.currentProgressMinutes)

        val observed = repository.observeMonthlyState("2026-10").first()
        assertEquals(15, observed?.targetReductionPercentage)
    }

    @Test
    fun updateMonthlyState_modifiesExistingRecord() = runTest(testDispatcher) {
        val initial = AwarenessMonthlyState(
            monthIdentifier = "2026-10",
            targetReductionPercentage = 10,
            baselineMinutes = 500,
            currentProgressMinutes = 10,
            createdAtMillis = 1000L,
            updatedAtMillis = 1000L
        )
        repository.saveMonthlyState(initial)

        val updated = initial.copy(
            targetReductionPercentage = 20,
            currentProgressMinutes = 80,
            updatedAtMillis = 2000L
        )
        repository.saveMonthlyState(updated)

        val retrieved = repository.getMonthlyState("2026-10")
        assertEquals(20, retrieved?.targetReductionPercentage)
        assertEquals(80, retrieved?.currentProgressMinutes)
        assertEquals(2000L, retrieved?.updatedAtMillis)
    }

    @Test
    fun differentMonthsRemainSeparate_correctlyIsolatesData() = runTest(testDispatcher) {
        val august = AwarenessMonthlyState(
            monthIdentifier = "2026-08",
            targetReductionPercentage = 5,
            baselineMinutes = 600,
            currentProgressMinutes = 300
        )
        val september = AwarenessMonthlyState(
            monthIdentifier = "2026-09",
            targetReductionPercentage = 10,
            baselineMinutes = 570,
            currentProgressMinutes = 250
        )
        val october = AwarenessMonthlyState(
            monthIdentifier = "2026-10",
            targetReductionPercentage = 15,
            baselineMinutes = 510,
            currentProgressMinutes = 100
        )

        repository.saveMonthlyState(august)
        repository.saveMonthlyState(september)
        repository.saveMonthlyState(october)

        val all = repository.getAllMonthlyStates()
        assertEquals(3, all.size)

        val retrievedAugust = repository.getMonthlyState("2026-08")
        val retrievedSeptember = repository.getMonthlyState("2026-09")
        val retrievedOctober = repository.getMonthlyState("2026-10")

        assertEquals(5, retrievedAugust?.targetReductionPercentage)
        assertEquals(10, retrievedSeptember?.targetReductionPercentage)
        assertEquals(15, retrievedOctober?.targetReductionPercentage)
    }
}
