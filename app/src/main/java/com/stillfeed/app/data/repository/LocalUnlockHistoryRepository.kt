package com.stillfeed.app.data.repository

import com.stillfeed.app.data.local.dao.UnlockHistoryDao
import com.stillfeed.app.data.local.mapper.toDomain
import com.stillfeed.app.data.local.mapper.toEntity
import com.stillfeed.app.domain.model.UnlockEvent
import com.stillfeed.app.domain.repository.UnlockHistoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LocalUnlockHistoryRepository(
    private val unlockHistoryDao: UnlockHistoryDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : UnlockHistoryRepository {

    override suspend fun recordUnlock(event: UnlockEvent): Unit = withContext(ioDispatcher) {
        unlockHistoryDao.insertUnlockEvent(event.toEntity())
    }

    override suspend fun getUnlockById(id: String): UnlockEvent? = withContext(ioDispatcher) {
        unlockHistoryDao.getUnlockEventById(id)?.toDomain()
    }

    override fun observeUnlocksInRange(startMillis: Long, endMillis: Long): Flow<List<UnlockEvent>> =
        unlockHistoryDao.observeUnlockEventsInRange(startMillis, endMillis).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getUnlocksInRange(startMillis: Long, endMillis: Long): List<UnlockEvent> =
        withContext(ioDispatcher) {
            unlockHistoryDao.getUnlockEventsInRange(startMillis, endMillis).map { it.toDomain() }
        }

    override suspend fun clearUnlockHistory(): Unit = withContext(ioDispatcher) {
        unlockHistoryDao.deleteAllUnlockEvents()
    }
}
