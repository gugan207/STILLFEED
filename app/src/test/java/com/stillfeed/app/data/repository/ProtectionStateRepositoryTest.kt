package com.stillfeed.app.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stillfeed.app.data.local.StillfeedDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProtectionStateRepositoryTest {

    private lateinit var database: StillfeedDatabase
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: LocalProtectionStateRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = StillfeedDatabase.buildInMemory(context)
        repository = LocalProtectionStateRepository(database.protectionStateDao(), testDispatcher)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun defaultState_isProtectionDisabled() = runTest(testDispatcher) {
        val state = repository.getProtectionState()
        assertFalse("Default protection should be disabled", state.isEnabled)
    }

    @Test
    fun enableState_and_disableState_updatesState() = runTest(testDispatcher) {
        repository.setProtectionEnabled(true)
        val enabledState = repository.getProtectionState()
        assertTrue("Protection should be enabled", enabledState.isEnabled)

        val observedEnabled = repository.observeProtectionState().first()
        assertTrue(observedEnabled.isEnabled)

        repository.setProtectionEnabled(false)
        val disabledState = repository.getProtectionState()
        assertFalse("Protection should be disabled", disabledState.isEnabled)
    }

    @Test
    fun persistenceAcrossRepositoryRecreation_preservesProtectionState() = runTest(testDispatcher) {
        repository.setProtectionEnabled(true)

        val newRepository = LocalProtectionStateRepository(database.protectionStateDao(), testDispatcher)
        val state = newRepository.getProtectionState()
        assertTrue("New repository instance must observe preserved enabled state", state.isEnabled)
    }
}
