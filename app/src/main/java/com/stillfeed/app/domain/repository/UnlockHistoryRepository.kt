package com.stillfeed.app.domain.repository

import com.stillfeed.app.domain.model.UnlockEvent
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for managing unlock events and allowance history.
 */
interface UnlockHistoryRepository {
    suspend fun recordUnlock(event: UnlockEvent)
    suspend fun getUnlockById(id: String): UnlockEvent?
    fun observeUnlocksInRange(startMillis: Long, endMillis: Long): Flow<List<UnlockEvent>>
    suspend fun getUnlocksInRange(startMillis: Long, endMillis: Long): List<UnlockEvent>
    suspend fun clearUnlockHistory()
}
