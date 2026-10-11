package com.stillfeed.app.data.repository

import com.stillfeed.app.data.local.dao.AwarenessMonthlyDao
import com.stillfeed.app.data.local.mapper.toDomain
import com.stillfeed.app.data.local.mapper.toEntity
import com.stillfeed.app.domain.model.AwarenessMonthlyState
import com.stillfeed.app.domain.repository.AwarenessRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LocalAwarenessRepository(
    private val awarenessMonthlyDao: AwarenessMonthlyDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AwarenessRepository {

    override fun observeMonthlyState(monthIdentifier: String): Flow<AwarenessMonthlyState?> =
        awarenessMonthlyDao.observeMonthlyState(monthIdentifier).map { it?.toDomain() }

    override suspend fun getMonthlyState(monthIdentifier: String): AwarenessMonthlyState? =
        withContext(ioDispatcher) {
            awarenessMonthlyDao.getMonthlyState(monthIdentifier)?.toDomain()
        }

    override suspend fun saveMonthlyState(state: AwarenessMonthlyState): Unit =
        withContext(ioDispatcher) {
            awarenessMonthlyDao.insertOrUpdate(state.toEntity())
        }

    override suspend fun getAllMonthlyStates(): List<AwarenessMonthlyState> =
        withContext(ioDispatcher) {
            awarenessMonthlyDao.getAllMonthlyStates().map { it.toDomain() }
        }

    override suspend fun deleteMonthlyState(monthIdentifier: String): Unit =
        withContext(ioDispatcher) {
            awarenessMonthlyDao.deleteMonthlyState(monthIdentifier)
        }

    override suspend fun clearAwarenessData(): Unit = withContext(ioDispatcher) {
        awarenessMonthlyDao.deleteAllMonthlyStates()
    }
}
