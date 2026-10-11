package com.stillfeed.app.domain.repository

import com.stillfeed.app.domain.model.AwarenessMonthlyState
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for persisting monthly awareness configurations and progress.
 */
interface AwarenessRepository {
    fun observeMonthlyState(monthIdentifier: String): Flow<AwarenessMonthlyState?>
    suspend fun getMonthlyState(monthIdentifier: String): AwarenessMonthlyState?
    suspend fun saveMonthlyState(state: AwarenessMonthlyState)
    suspend fun getAllMonthlyStates(): List<AwarenessMonthlyState>
    suspend fun deleteMonthlyState(monthIdentifier: String)
    suspend fun clearAwarenessData()
}
