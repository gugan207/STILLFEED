package com.stillfeed.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stillfeed.app.data.local.entity.UsageEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: UsageEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<UsageEventEntity>)

    @Query("SELECT * FROM usage_events WHERE id = :id LIMIT 1")
    suspend fun getEventById(id: String): UsageEventEntity?

    @Query("SELECT * FROM usage_events WHERE timestampMillis BETWEEN :startTime AND :endTime ORDER BY timestampMillis ASC")
    fun observeEventsInRange(startTime: Long, endTime: Long): Flow<List<UsageEventEntity>>

    @Query("SELECT * FROM usage_events WHERE timestampMillis BETWEEN :startTime AND :endTime ORDER BY timestampMillis ASC")
    suspend fun getEventsInRange(startTime: Long, endTime: Long): List<UsageEventEntity>

    @Query("SELECT COUNT(*) FROM usage_events")
    suspend fun getEventCount(): Int

    @Query("DELETE FROM usage_events WHERE timestampMillis < :cutoffMillis")
    suspend fun deleteEventsOlderThan(cutoffMillis: Long): Int

    @Query("DELETE FROM usage_events")
    suspend fun deleteAllEvents(): Int
}
