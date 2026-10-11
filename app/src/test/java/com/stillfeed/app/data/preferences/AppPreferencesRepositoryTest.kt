package com.stillfeed.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppPreferencesRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var repository: DataStoreAppPreferencesRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        dataStoreScope = CoroutineScope(testDispatcher + Job())
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { context.preferencesDataStoreFile("test_prefs_${UUID.randomUUID()}") }
        )
        repository = DataStoreAppPreferencesRepository(testDataStore)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun defaultPreferences_haveExpectedValues() = runTest(testDispatcher) {
        val prefs = repository.getPreferences()
        assertTrue("Default isFirstLaunch must be true", prefs.isFirstLaunch)
        assertFalse("Default hasCompletedOnboarding must be false", prefs.hasCompletedOnboarding)
        assertEquals(0, prefs.onboardingStep)
        assertFalse("Default quickProtectionToggle must be false", prefs.quickProtectionToggle)
        assertTrue("Default dailyReminderEnabled must be true", prefs.dailyReminderEnabled)
        assertEquals("SYSTEM", prefs.preferredThemeMode)
    }

    @Test
    fun setOnboardingCompleted_updatesCorrectly() = runTest(testDispatcher) {
        repository.setOnboardingCompleted(true)
        val prefs = repository.getPreferences()
        assertTrue("Onboarding should be recorded as completed", prefs.hasCompletedOnboarding)
    }

    @Test
    fun setFirstLaunch_updatesCorrectly() = runTest(testDispatcher) {
        repository.setFirstLaunch(false)
        val prefs = repository.getPreferences()
        assertFalse("First launch should be updated to false", prefs.isFirstLaunch)
    }

    @Test
    fun setQuickProtectionToggle_updatesCorrectly() = runTest(testDispatcher) {
        repository.setQuickProtectionToggle(true)
        val prefs = repository.getPreferences()
        assertTrue("Quick toggle should be true", prefs.quickProtectionToggle)
    }

    @Test
    fun updatePreferences_transformsEntireState() = runTest(testDispatcher) {
        repository.updatePreferences { current ->
            current.copy(
                isFirstLaunch = false,
                hasCompletedOnboarding = true,
                onboardingStep = 3,
                quickProtectionToggle = true,
                preferredThemeMode = "DARK"
            )
        }

        val observed = repository.observePreferences().first()
        assertFalse(observed.isFirstLaunch)
        assertTrue(observed.hasCompletedOnboarding)
        assertEquals(3, observed.onboardingStep)
        assertTrue(observed.quickProtectionToggle)
        assertEquals("DARK", observed.preferredThemeMode)
    }

    @Test
    fun clearPreferences_resetsAllValues() = runTest(testDispatcher) {
        repository.setOnboardingCompleted(true)
        repository.clearPreferences()

        val resetPrefs = repository.getPreferences()
        assertTrue("Should revert to default isFirstLaunch", resetPrefs.isFirstLaunch)
        assertFalse("Should revert to default hasCompletedOnboarding", resetPrefs.hasCompletedOnboarding)
    }
}
