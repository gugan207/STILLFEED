package com.stillfeed.app.data.repository

import com.stillfeed.app.data.local.dao.UsageEventDao
import com.stillfeed.app.data.local.mapper.toDomain
import com.stillfeed.app.data.local.mapper.toEntity
import com.stillfeed.app.domain.model.UsageEvent
import com.stillfeed.app.domain.repository.UsageRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LocalUsageRepository(
    private val usageEventDao: UsageEventDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : UsageRepository {

    override suspend fun recordEvent(event: UsageEvent): Unit = withContext(ioDispatcher) {
        usageEventDao.insertEvent(event.toEntity())
    }

    override suspend fun recordEvents(events: List<UsageEvent>): Unit = withContext(ioDispatcher) {
        usageEventDao.insertEvents(events.map { it.toEntity() })
    }

    override suspend fun getEventById(id: String): UsageEvent? = withContext(ioDispatcher) {
        usageEventDao.getEventById(id)?.toDomain()
    }

    override fun observeEventsInRange(startMillis: Long, endMillis: Long): Flow<List<UsageEvent>> =
        usageEventDao.observeEventsInRange(startMillis, endMillis).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getEventsInRange(startMillis: Long, endMillis: Long): List<UsageEvent> =
        withContext(ioDispatcher) {
            usageEventDao.getEventsInRange(startMillis, endMillis).map { it.toDomain() }
        }

    override suspend fun getEventCount(): Int = withContext(ioDispatcher) {
        usageEventDao.getEventCount()
    }

    override suspend fun deleteEventsOlderThan(cutoffMillis: Long): Int = withContext(ioDispatcher) {
        usageEventDao.deleteEventsOlderThan(cutoffMillis)
    }

    override suspend fun clearUsageHistory(): Unit = withContext(ioDispatcher) {
        usageEventDao.deleteAllEvents()
    }
}
