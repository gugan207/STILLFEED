package com.stillfeed.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stillfeed.app.data.local.entity.DeviceBindingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceBindingDao {

    @Query("SELECT * FROM device_binding WHERE id = :id LIMIT 1")
    fun observeDeviceBinding(id: String = DeviceBindingEntity.SINGLE_ROW_ID): Flow<DeviceBindingEntity?>

    @Query("SELECT * FROM device_binding WHERE id = :id LIMIT 1")
    suspend fun getDeviceBinding(id: String = DeviceBindingEntity.SINGLE_ROW_ID): DeviceBindingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(binding: DeviceBindingEntity)

    @Query("DELETE FROM device_binding")
    suspend fun clearDeviceBinding(): Int
}
