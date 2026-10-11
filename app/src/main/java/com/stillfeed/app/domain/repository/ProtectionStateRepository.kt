package com.stillfeed.app.domain.repository

import com.stillfeed.app.domain.model.ProtectionState
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for persisting and observing protection state.
 */
interface ProtectionStateRepository {
    fun observeProtectionState(): Flow<ProtectionState>
    suspend fun getProtectionState(): ProtectionState
    suspend fun setProtectionEnabled(enabled: Boolean)
    suspend fun clearProtectionState()
}
