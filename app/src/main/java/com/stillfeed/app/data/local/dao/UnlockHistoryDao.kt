package com.stillfeed.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stillfeed.app.data.local.entity.UnlockEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UnlockHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnlockEvent(event: UnlockEventEntity)

    @Query("SELECT * FROM unlock_events WHERE id = :id LIMIT 1")
    suspend fun getUnlockEventById(id: String): UnlockEventEntity?

    @Query("SELECT * FROM unlock_events WHERE timestampMillis BETWEEN :startTime AND :endTime ORDER BY timestampMillis ASC")
    fun observeUnlockEventsInRange(startTime: Long, endTime: Long): Flow<List<UnlockEventEntity>>

    @Query("SELECT * FROM unlock_events WHERE timestampMillis BETWEEN :startTime AND :endTime ORDER BY timestampMillis ASC")
    suspend fun getUnlockEventsInRange(startTime: Long, endTime: Long): List<UnlockEventEntity>

    @Query("DELETE FROM unlock_events")
    suspend fun deleteAllUnlockEvents(): Int
}
