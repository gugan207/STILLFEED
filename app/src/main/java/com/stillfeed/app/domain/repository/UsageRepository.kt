package com.stillfeed.app.domain.repository

import com.stillfeed.app.domain.model.UsageEvent
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for managing attention usage events.
 *
 * Privacy Guarantees:
 * - Only privacy-minimal structured metrics are persisted.
 * - Supports date-range queries for analytics.
 * - Supports fine-grained and full data retention clearing.
 */
interface UsageRepository {
    suspend fun recordEvent(event: UsageEvent)
    suspend fun recordEvents(events: List<UsageEvent>)
    suspend fun getEventById(id: String): UsageEvent?
    fun observeEventsInRange(startMillis: Long, endMillis: Long): Flow<List<UsageEvent>>
    suspend fun getEventsInRange(startMillis: Long, endMillis: Long): List<UsageEvent>
    suspend fun getEventCount(): Int
    suspend fun deleteEventsOlderThan(cutoffMillis: Long): Int
    suspend fun clearUsageHistory()
}
