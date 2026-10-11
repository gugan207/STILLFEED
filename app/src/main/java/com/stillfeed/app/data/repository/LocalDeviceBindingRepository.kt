package com.stillfeed.app.data.repository

import com.stillfeed.app.data.local.dao.DeviceBindingDao
import com.stillfeed.app.data.local.device.DeviceBindingProvider
import com.stillfeed.app.data.local.device.DeviceBindingVerificationResult
import com.stillfeed.app.data.local.mapper.toDomain
import com.stillfeed.app.data.local.mapper.toEntity
import com.stillfeed.app.domain.model.DeviceBinding
import com.stillfeed.app.domain.model.DeviceBindingStatus
import com.stillfeed.app.domain.repository.DeviceBindingRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Concrete implementation of [DeviceBindingRepository] decoupled via [DeviceBindingProvider].
 *
 * Product Requirements & Architectural Guarantees:
 * - History may restore after reinstall on the SAME device.
 * - History must NOT automatically restore on a DIFFERENT phone.
 * - App-private UUIDs are recognized as non-durable and cannot claim same-device guarantees.
 * - Replaces hard-coded mechanisms with provider abstraction.
 *
 * EXACT UNRESOLVED ARCHITECTURAL REQUIREMENT:
 * "Remote/sync restoration is deferred until the production authentication/cloud architecture is selected."
 */
class LocalDeviceBindingRepository(
    private val deviceBindingDao: DeviceBindingDao,
    private val deviceBindingProvider: DeviceBindingProvider,
    private val appVersion: String = "1.0.0",
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : DeviceBindingRepository {

    override fun observeBinding(): Flow<DeviceBinding> =
        deviceBindingDao.observeDeviceBinding().map { it.toDomain() }

    override suspend fun getCurrentBinding(): DeviceBinding = withContext(ioDispatcher) {
        val stored = deviceBindingDao.getDeviceBinding()
        if (stored != null) {
            return@withContext stored.toDomain()
        }

        // Establish binding using the configured platform provider
        val tokenResult = deviceBindingProvider.getOrCreateBindingToken()
        val initialStatus = if (tokenResult.token.isNotBlank()) {
            DeviceBindingStatus.BOUND
        } else {
            DeviceBindingStatus.INDETERMINATE
        }

        val initialBinding = DeviceBinding(
            bindingId = tokenResult.token,
            status = initialStatus,
            isDurableAcrossReinstall = tokenResult.isDurableAcrossReinstall,
            providerName = deviceBindingProvider.providerName,
            createdAtMillis = System.currentTimeMillis(),
            lastVerifiedAtMillis = System.currentTimeMillis(),
            appVersion = appVersion
        )
        deviceBindingDao.insertOrUpdate(initialBinding.toEntity())
        initialBinding
    }

    override suspend fun verifyBinding(bindingId: String): DeviceBindingVerificationResult =
        withContext(ioDispatcher) {
            val verificationResult = deviceBindingProvider.verifyBindingToken(bindingId)

            val updatedStatus = when (verificationResult) {
                is DeviceBindingVerificationResult.MatchSameDevice ->
                    DeviceBindingStatus.VERIFIED_SAME_DEVICE
                is DeviceBindingVerificationResult.MismatchDifferentDevice ->
                    DeviceBindingStatus.MISMATCH_DIFFERENT_DEVICE
                is DeviceBindingVerificationResult.IndeterminatePlatformLimitation ->
                    DeviceBindingStatus.INDETERMINATE
            }

            val currentStored = deviceBindingDao.getDeviceBinding()
            val createdAt = currentStored?.createdAtMillis ?: System.currentTimeMillis()

            val updatedBinding = DeviceBinding(
                bindingId = bindingId,
                status = updatedStatus,
                isDurableAcrossReinstall = deviceBindingProvider.isPlatformDurableAcrossReinstall,
                providerName = deviceBindingProvider.providerName,
                createdAtMillis = createdAt,
                lastVerifiedAtMillis = System.currentTimeMillis(),
                appVersion = appVersion
            )
            deviceBindingDao.insertOrUpdate(updatedBinding.toEntity())
            verificationResult
        }

    override suspend fun bindCurrentDevice(bindingId: String): DeviceBinding = withContext(ioDispatcher) {
        val verification = deviceBindingProvider.verifyBindingToken(bindingId)
        val status = when (verification) {
            is DeviceBindingVerificationResult.MatchSameDevice ->
                DeviceBindingStatus.BOUND
            is DeviceBindingVerificationResult.MismatchDifferentDevice ->
                DeviceBindingStatus.MISMATCH_DIFFERENT_DEVICE
            is DeviceBindingVerificationResult.IndeterminatePlatformLimitation ->
                DeviceBindingStatus.INDETERMINATE
        }

        val binding = DeviceBinding(
            bindingId = bindingId,
            status = status,
            isDurableAcrossReinstall = deviceBindingProvider.isPlatformDurableAcrossReinstall,
            providerName = deviceBindingProvider.providerName,
            createdAtMillis = System.currentTimeMillis(),
            lastVerifiedAtMillis = System.currentTimeMillis(),
            appVersion = appVersion
        )
        deviceBindingDao.insertOrUpdate(binding.toEntity())
        binding
    }

    override suspend fun clearBinding(): Unit = withContext(ioDispatcher) {
        deviceBindingDao.clearDeviceBinding()
        deviceBindingProvider.clearBindingToken()
    }
}
