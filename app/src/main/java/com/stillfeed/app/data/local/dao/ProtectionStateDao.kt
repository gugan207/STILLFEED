package com.stillfeed.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stillfeed.app.data.local.entity.ProtectionStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProtectionStateDao {

    @Query("SELECT * FROM protection_state WHERE id = :id LIMIT 1")
    fun observeProtectionState(id: String = ProtectionStateEntity.SINGLE_ROW_ID): Flow<ProtectionStateEntity?>

    @Query("SELECT * FROM protection_state WHERE id = :id LIMIT 1")
    suspend fun getProtectionState(id: String = ProtectionStateEntity.SINGLE_ROW_ID): ProtectionStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(state: ProtectionStateEntity)

    @Query("DELETE FROM protection_state")
    suspend fun clearProtectionState(): Int
}
