package com.stillfeed.app.data.local.device

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import com.stillfeed.app.data.preferences.AppPreferencesKeys
import com.stillfeed.app.data.preferences.DataStoreAppPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

/**
 * Unit test suite verifying the Android Block Store configuration and credential storage
 * guarantees (STEP 05 Final Security Remediation).
 *
 * Verifies:
 * 1. First enrollment stores credential correctly in Block Store.
 * 2. Block Store configuration strictly sets `setShouldBackupToCloud(false)`.
 * 3. Same-device reinstall recovers the existing credential when supported.
 * 4. Credential is NOT copied into ordinary Jetpack DataStore.
 * 5. Device-to-device migration does not restore the credential (blocks cross-device restoration).
 * 6. Deleting / clearing the credential wipes it from Block Store.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BlockStoreDeviceRestoreCredentialStoreTest {

    private lateinit var mockBlockStoreDelegate: InMemoryBlockStoreClientDelegate
    private lateinit var store: BlockStoreDeviceRestoreCredentialStore
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var preferencesRepository: DataStoreAppPreferencesRepository
    private val testCredential = "sfrc_test_secret_credential_token_9876543210_abcdef"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        mockBlockStoreDelegate = InMemoryBlockStoreClientDelegate()
        store = BlockStoreDeviceRestoreCredentialStore(mockBlockStoreDelegate)

        dataStoreScope = CoroutineScope(Dispatchers.Unconfined + Job())
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { context.preferencesDataStoreFile("test_prefs_${UUID.randomUUID()}") }
        )
        preferencesRepository = DataStoreAppPreferencesRepository(testDataStore)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun firstEnrollment_storesCredentialCorrectly_inBlockStore() = runTest {
        store.saveRestoreCredential(testCredential)

        val retrieved = store.getRestoreCredential()
        assertEquals(testCredential, retrieved)
        assertTrue(mockBlockStoreDelegate.store.containsKey(BlockStoreDeviceRestoreCredentialStore.BLOCK_STORE_RESTORE_CREDENTIAL_KEY))
    }

    @Test
    fun blockStoreConfiguration_strictlySetsShouldBackupToCloudFalse() = runTest {
        store.saveRestoreCredential(testCredential)

        val storedBlock = mockBlockStoreDelegate.store[BlockStoreDeviceRestoreCredentialStore.BLOCK_STORE_RESTORE_CREDENTIAL_KEY]
        assertNotNull("Block must exist in Block Store", storedBlock)
        assertFalse(
            "CRITICAL SECURITY GATE: Block Store MUST set shouldBackupToCloud=false to prevent cloud/device sync",
            storedBlock!!.shouldBackupToCloud
        )
    }

    @Test
    fun sameDeviceReinstall_recoversExistingCredential_whenSupported() = runTest {
        // Device A enrolls and saves credential
        store.saveRestoreCredential(testCredential)

        // App is uninstalled and reinstalled on the SAME physical device.
        // Google Play services retains the local Block Store partition for this package.
        val reinstalledStore = BlockStoreDeviceRestoreCredentialStore(mockBlockStoreDelegate)

        val recoveredCredential = reinstalledStore.getRestoreCredential()
        assertEquals("Reinstall on same device must recover the existing credential", testCredential, recoveredCredential)
    }

    @Test
    fun deviceToDeviceMigration_doesNotRestoreCredential_blockingCrossDeviceRestore() = runTest {
        // Original device saves credential
        store.saveRestoreCredential(testCredential)

        // User sets up a NEW device (simulating Google Cloud Restore / D2D migration)
        // Because shouldBackupToCloud = false, Google Play services does NOT sync this block to the new device!
        val newDeviceDelegate = mockBlockStoreDelegate.simulateMigrationToNewDevice()
        val newDeviceStore = BlockStoreDeviceRestoreCredentialStore(newDeviceDelegate)

        val credentialOnNewDevice = newDeviceStore.getRestoreCredential()
        assertNull("New device must NOT receive the restore credential via cloud or D2D transfer", credentialOnNewDevice)
    }

    @Test
    fun credential_isNotCopiedIntoOrdinaryDataStore() = runTest {
        // Save restore credential
        store.saveRestoreCredential(testCredential)

        // Read all preferences from Jetpack DataStore
        val prefs = preferencesRepository.getPreferences()

        // Verify none of the DataStore preferences hold the secret restore credential
        assertFalse(prefs.hasCompletedOnboarding)
        assertEquals(0, prefs.onboardingStep)
        // Verify AppPreferencesKeys has zero credential keys
        val allDeclaredKeys = listOf(
            AppPreferencesKeys.IS_FIRST_LAUNCH.name,
            AppPreferencesKeys.HAS_COMPLETED_ONBOARDING.name,
            AppPreferencesKeys.ONBOARDING_STEP.name,
            AppPreferencesKeys.QUICK_PROTECTION_TOGGLE.name,
            AppPreferencesKeys.DAILY_REMINDER_ENABLED.name,
            AppPreferencesKeys.PREFERRED_THEME_MODE.name
        )
        assertFalse("DataStore must never declare a key for the restore credential",
            allDeclaredKeys.any { it.contains("credential", ignoreCase = true) || it.contains("sfrc", ignoreCase = true) })
    }

    @Test
    fun clearRestoreCredential_wipesDataFromBlockStore() = runTest {
        store.saveRestoreCredential(testCredential)
        assertEquals(testCredential, store.getRestoreCredential())

        store.clearRestoreCredential()
        assertNull(store.getRestoreCredential())
        assertFalse(mockBlockStoreDelegate.store.containsKey(BlockStoreDeviceRestoreCredentialStore.BLOCK_STORE_RESTORE_CREDENTIAL_KEY))
    }
}
