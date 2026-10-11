package com.stillfeed.app.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stillfeed.app.data.local.StillfeedDatabase
import com.stillfeed.app.data.local.device.AndroidIdDeviceBindingProvider
import com.stillfeed.app.data.local.device.DeviceBindingVerificationResult
import com.stillfeed.app.data.local.device.InMemoryDeviceBindingProvider
import com.stillfeed.app.domain.model.DeviceBindingStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DeviceBindingRepositoryTest {

    private lateinit var database: StillfeedDatabase
    private lateinit var mockProvider: InMemoryDeviceBindingProvider
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: LocalDeviceBindingRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = StillfeedDatabase.buildInMemory(context)
        mockProvider = InMemoryDeviceBindingProvider(
            currentToken = "durable_platform_token_alpha",
            isPlatformDurableAcrossReinstall = true
        )
        repository = LocalDeviceBindingRepository(
            deviceBindingDao = database.deviceBindingDao(),
            deviceBindingProvider = mockProvider,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    /**
     * Requirement 10.1: First installation binding
     */
    @Test
    fun firstInstallationBinding_initializesBindingAndMarksBound() = runTest(testDispatcher) {
        val binding = repository.getCurrentBinding()
        assertNotNull(binding)
        assertEquals("durable_platform_token_alpha", binding.bindingId)
        assertEquals(DeviceBindingStatus.BOUND, binding.status)
        assertTrue("Provider must be marked durable", binding.isDurableAcrossReinstall)

        val observed = repository.observeBinding().first()
        assertEquals("durable_platform_token_alpha", observed.bindingId)
        assertEquals(DeviceBindingStatus.BOUND, observed.status)
    }

    /**
     * Requirement 10.2: Normal same-installation match
     */
    @Test
    fun normalSameInstallationMatch_returnsMatchSameDevice() = runTest(testDispatcher) {
        repository.getCurrentBinding()

        val verification = repository.verifyBinding("durable_platform_token_alpha")
        assertTrue(
            "Expected MatchSameDevice on matching token",
            verification is DeviceBindingVerificationResult.MatchSameDevice
        )

        val updated = repository.getCurrentBinding()
        assertEquals(DeviceBindingStatus.VERIFIED_SAME_DEVICE, updated.status)
    }

    /**
     * Requirement 10.3: Reinstall/restore behavior supported by chosen mechanism
     * (Simulates uninstall where Room DB is wiped, but durable platform token persists)
     */
    @Test
    fun reinstallRestoreBehavior_supportedByChosenMechanism_restoresSameDeviceMatch() = runTest(testDispatcher) {
        val registeredAccountToken = "durable_platform_token_alpha"

        // 1. First installation established binding
        repository.getCurrentBinding()

        // 2. Simulate complete uninstall/reinstall: database is wiped
        database.close()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val reinstalledDatabase = StillfeedDatabase.buildInMemory(context)

        // Platform mechanism (e.g. Scoped ANDROID_ID) still returns the same hardware-scoped token
        val reinstalledRepository = LocalDeviceBindingRepository(
            deviceBindingDao = reinstalledDatabase.deviceBindingDao(),
            deviceBindingProvider = mockProvider,
            ioDispatcher = testDispatcher
        )

        // 3. User signs in; app verifies against previously registered account token
        val verification = reinstalledRepository.verifyBinding(registeredAccountToken)
        assertTrue(
            "Same-device reinstall must succeed when supported by durable platform signal",
            verification is DeviceBindingVerificationResult.MatchSameDevice
        )
        assertEquals(
            DeviceBindingStatus.VERIFIED_SAME_DEVICE,
            reinstalledRepository.getCurrentBinding().status
        )
        reinstalledDatabase.close()
    }

    /**
     * Requirement 10.4: Different-device rejection where platform can reliably enforce it
     */
    @Test
    fun differentDeviceRejection_whenPlatformCanReliablyEnforceIt_rejectsWithMismatch() = runTest(testDispatcher) {
        repository.getCurrentBinding()

        // Account was registered on a different phone
        val foreignPhoneToken = "durable_foreign_phone_token_beta"
        val verification = repository.verifyBinding(foreignPhoneToken)

        assertTrue(
            "Cross-device restore must be strictly rejected",
            verification is DeviceBindingVerificationResult.MismatchDifferentDevice
        )
        assertEquals(
            DeviceBindingStatus.MISMATCH_DIFFERENT_DEVICE,
            repository.getCurrentBinding().status
        )
    }

    /**
     * Requirement 10.5: Inability to claim same-device restoration when required platform signal is unavailable
     * (Architecture refuses to fake guarantees when mechanism is non-durable or signal is missing)
     */
    @Test
    fun inabilityToClaimSameDeviceRestoration_whenRequiredPlatformSignalIsUnavailable_yieldsIndeterminate() =
        runTest(testDispatcher) {
            // Configure provider as non-durable (e.g. app-private UUID fallback or missing OS signal)
            mockProvider.isPlatformDurableAcrossReinstall = false

            repository.getCurrentBinding()

            val verification = repository.verifyBinding("some_previous_token")
            assertTrue(
                "Must report IndeterminatePlatformLimitation instead of faking a guarantee",
                verification is DeviceBindingVerificationResult.IndeterminatePlatformLimitation
            )
            assertEquals(
                DeviceBindingStatus.INDETERMINATE,
                repository.getCurrentBinding().status
            )
        }

    /**
     * Platform test for AndroidIdDeviceBindingProvider with valid platform ANDROID_ID
     */
    @Test
    fun androidIdDeviceBindingProvider_derivesAppScopedHash_andEnforcesMatching() = runTest(testDispatcher) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Simulate real Android 8.0+ device where ANDROID_ID is present
        android.provider.Settings.Secure.putString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID,
            "device_hardware_unique_hex_12345"
        )
        val provider = AndroidIdDeviceBindingProvider(context)

        val tokenResult = provider.getOrCreateBindingToken()
        assertNotNull(tokenResult.token)
        assertTrue(tokenResult.token.startsWith("dev_bind_"))
        assertTrue("Android ID provider is platform durable on Android 8.0+", tokenResult.isDurableAcrossReinstall)

        val matchResult = provider.verifyBindingToken(tokenResult.token)
        assertTrue(matchResult is DeviceBindingVerificationResult.MatchSameDevice)

        val foreignResult = provider.verifyBindingToken("dev_bind_foreign_token_9999")
        assertTrue(foreignResult is DeviceBindingVerificationResult.MismatchDifferentDevice)
    }

    /**
     * Platform test for AndroidIdDeviceBindingProvider when platform signal is missing or invalid
     */
    @Test
    fun androidIdDeviceBindingProvider_whenAndroidIdUnavailable_returnsIndeterminateResult() = runTest(testDispatcher) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Simulate emulator dummy ID "9774d56d682e549c"
        android.provider.Settings.Secure.putString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID,
            "9774d56d682e549c"
        )
        val provider = AndroidIdDeviceBindingProvider(context)

        val tokenResult = provider.getOrCreateBindingToken()
        assertFalse("Must not claim durability on dummy/invalid ANDROID_ID", tokenResult.isDurableAcrossReinstall)

        val verification = provider.verifyBindingToken("any_registered_token")
        assertTrue(
            "Must report IndeterminatePlatformLimitation when platform signal is invalid",
            verification is DeviceBindingVerificationResult.IndeterminatePlatformLimitation
        )
    }
}
