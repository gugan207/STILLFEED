package com.stillfeed.app.domain.repository

import com.stillfeed.app.data.local.device.DeviceBindingVerificationResult
import com.stillfeed.app.domain.model.DeviceBinding
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for managing device-bound restore metadata.
 *
 * Product Requirement:
 * - History may be restored after reinstall on the SAME device.
 * - History must NOT automatically restore to a NEW device.
 * - Does not claim stronger guarantees than the underlying platform supports.
 *
 * EXACT UNRESOLVED ARCHITECTURAL REQUIREMENT:
 * "Remote/sync restoration is deferred until the production authentication/cloud architecture is selected."
 */
interface DeviceBindingRepository {
    fun observeBinding(): Flow<DeviceBinding>
    suspend fun getCurrentBinding(): DeviceBinding
    suspend fun verifyBinding(bindingId: String): DeviceBindingVerificationResult
    suspend fun bindCurrentDevice(bindingId: String): DeviceBinding
    suspend fun clearBinding()
}
