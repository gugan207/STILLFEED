package com.stillfeed.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stillfeed.app.data.local.entity.AwarenessMonthlyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AwarenessMonthlyDao {

    @Query("SELECT * FROM awareness_monthly_state WHERE monthIdentifier = :monthId LIMIT 1")
    fun observeMonthlyState(monthId: String): Flow<AwarenessMonthlyEntity?>

    @Query("SELECT * FROM awareness_monthly_state WHERE monthIdentifier = :monthId LIMIT 1")
    suspend fun getMonthlyState(monthId: String): AwarenessMonthlyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(state: AwarenessMonthlyEntity)

    @Query("SELECT * FROM awareness_monthly_state ORDER BY monthIdentifier DESC")
    suspend fun getAllMonthlyStates(): List<AwarenessMonthlyEntity>

    @Query("DELETE FROM awareness_monthly_state WHERE monthIdentifier = :monthId")
    suspend fun deleteMonthlyState(monthId: String): Int

    @Query("DELETE FROM awareness_monthly_state")
    suspend fun deleteAllMonthlyStates(): Int
}
