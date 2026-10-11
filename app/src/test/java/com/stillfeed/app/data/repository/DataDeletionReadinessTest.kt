package com.stillfeed.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import com.stillfeed.app.data.local.StillfeedDatabase
import com.stillfeed.app.data.local.device.InMemoryDeviceBindingProvider
import com.stillfeed.app.data.preferences.DataStoreAppPreferencesRepository
import com.stillfeed.app.domain.model.AwarenessMonthlyState
import com.stillfeed.app.domain.model.DeviceBindingStatus
import com.stillfeed.app.domain.model.SurfaceCategory
import com.stillfeed.app.domain.model.UnlockEvent
import com.stillfeed.app.domain.model.UsageEvent
import com.stillfeed.app.domain.model.UsageEventType
import com.stillfeed.app.domain.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataDeletionReadinessTest {

    private lateinit var database: StillfeedDatabase
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var dataStoreScope: CoroutineScope

    private lateinit var userRepository: LocalUserRepository
    private lateinit var protectionRepository: LocalProtectionStateRepository
    private lateinit var usageRepository: LocalUsageRepository
    private lateinit var awarenessRepository: LocalAwarenessRepository
    private lateinit var unlockRepository: LocalUnlockHistoryRepository

    private lateinit var deviceBindingRepository: LocalDeviceBindingRepository
    private lateinit var preferencesRepository: DataStoreAppPreferencesRepository
    private lateinit var deviceBindingProvider: InMemoryDeviceBindingProvider

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = StillfeedDatabase.buildInMemory(context)

        userRepository = LocalUserRepository(database.userDao(), testDispatcher)
        protectionRepository = LocalProtectionStateRepository(database.protectionStateDao(), testDispatcher)
        usageRepository = LocalUsageRepository(database.usageEventDao(), testDispatcher)
        awarenessRepository = LocalAwarenessRepository(database.awarenessMonthlyDao(), testDispatcher)
        unlockRepository = LocalUnlockHistoryRepository(database.unlockHistoryDao(), testDispatcher)

        deviceBindingProvider = InMemoryDeviceBindingProvider(currentToken = "device_token_xyz")
        deviceBindingRepository = LocalDeviceBindingRepository(
            deviceBindingDao = database.deviceBindingDao(),
            deviceBindingProvider = deviceBindingProvider,
            ioDispatcher = testDispatcher
        )

        dataStoreScope = CoroutineScope(testDispatcher + Job())
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { context.preferencesDataStoreFile("test_deletion_prefs_${System.currentTimeMillis()}") }
        )
        preferencesRepository = DataStoreAppPreferencesRepository(testDataStore)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
        database.close()
    }

    @Test
    fun deleteUserData_removesAccountWithoutAffectingOthers() = runTest(testDispatcher) {
        val user1 = User("u1", "u1@stillfeed.app", "User 1", 1000L, 1000L)
        val user2 = User("u2", "u2@stillfeed.app", "User 2", 1000L, 1000L)
        userRepository.saveUser(user1)
        userRepository.saveUser(user2)

        userRepository.deleteUser("u1")

        assertNull("Deleted user should be gone", userRepository.getUser("u1"))
        assertEquals("Remaining user should persist", "u2", userRepository.getUser("u2")?.id)

        userRepository.clearAllUsers()
        assertNull(userRepository.getUser("u2"))
    }

    @Test
    fun deleteUsageHistory_clearsEventsDeterministically() = runTest(testDispatcher) {
        val eventOld = UsageEvent("e_old", 1000L, "com.instagram.android", SurfaceCategory.REELS, UsageEventType.BLOCKED)
        val eventRecent = UsageEvent("e_rec", 5000L, "com.instagram.android", SurfaceCategory.REELS, UsageEventType.BLOCKED)
        usageRepository.recordEvents(listOf(eventOld, eventRecent))
        assertEquals(2, usageRepository.getEventCount())

        // Selective cutoff deletion
        val deletedCount = usageRepository.deleteEventsOlderThan(3000L)
        assertEquals(1, deletedCount)
        assertEquals(1, usageRepository.getEventCount())
        assertNull(usageRepository.getEventById("e_old"))
        assertEquals("e_rec", usageRepository.getEventById("e_rec")?.id)

        // Complete usage history purge
        usageRepository.clearUsageHistory()
        assertEquals(0, usageRepository.getEventCount())
    }

    @Test
    fun deleteUnlockHistory_clearsAllUnlocks() = runTest(testDispatcher) {
        val unlock = UnlockEvent("unl_1", 2000L, 5, "manual_request")
        unlockRepository.recordUnlock(unlock)
        assertEquals(1, unlockRepository.getUnlocksInRange(1000L, 3000L).size)

        unlockRepository.clearUnlockHistory()
        assertEquals(0, unlockRepository.getUnlocksInRange(1000L, 3000L).size)
    }

    @Test
    fun deleteAwarenessData_clearsSpecificMonthAndAllData() = runTest(testDispatcher) {
        val month1 = AwarenessMonthlyState("2026-09", 10, 600, 200)
        val month2 = AwarenessMonthlyState("2026-10", 15, 540, 100)
        awarenessRepository.saveMonthlyState(month1)
        awarenessRepository.saveMonthlyState(month2)
        assertEquals(2, awarenessRepository.getAllMonthlyStates().size)

        // Delete single month
        awarenessRepository.deleteMonthlyState("2026-09")
        assertNull(awarenessRepository.getMonthlyState("2026-09"))
        assertEquals(1, awarenessRepository.getAllMonthlyStates().size)

        // Clear all awareness data
        awarenessRepository.clearAwarenessData()
        assertEquals(0, awarenessRepository.getAllMonthlyStates().size)
    }

    @Test
    fun clearDeviceBinding_resetsDeviceStatus() = runTest(testDispatcher) {
        val binding = deviceBindingRepository.getCurrentBinding()
        assertEquals(DeviceBindingStatus.BOUND, binding.status)

        deviceBindingRepository.clearBinding()
        // Next retrieval creates fresh unbound/rebound state with new installation token
        deviceBindingProvider.currentToken = "brand_new_token"
        val newBinding = deviceBindingRepository.getCurrentBinding()
        assertEquals("brand_new_token", newBinding.bindingId)
    }

    @Test
    fun clearProtectionAndPreferences_resetsToDefaults() = runTest(testDispatcher) {
        protectionRepository.setProtectionEnabled(true)
        assertTrue(protectionRepository.getProtectionState().isEnabled)
        protectionRepository.clearProtectionState()
        assertFalse(protectionRepository.getProtectionState().isEnabled)

        preferencesRepository.setOnboardingCompleted(true)
        assertTrue(preferencesRepository.getPreferences().hasCompletedOnboarding)
        preferencesRepository.clearPreferences()
        assertFalse(preferencesRepository.getPreferences().hasCompletedOnboarding)
    }
}
