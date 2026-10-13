package com.stillfeed.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stillfeed.app.data.local.entity.ActiveUnlockSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for persisting and querying active unlock sessions (STEP 19).
 */
@Dao
interface ActiveUnlockSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(session: ActiveUnlockSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateSync(session: ActiveUnlockSessionEntity)

    @Query("SELECT * FROM active_unlock_sessions WHERE package_name = :packageName LIMIT 1")
    suspend fun getSessionByPackage(packageName: String): ActiveUnlockSessionEntity?

    @Query("SELECT * FROM active_unlock_sessions WHERE package_name = :packageName LIMIT 1")
    fun getSessionByPackageSync(packageName: String): ActiveUnlockSessionEntity?

    @Query("SELECT * FROM active_unlock_sessions")
    suspend fun getAllSessions(): List<ActiveUnlockSessionEntity>

    @Query("SELECT * FROM active_unlock_sessions")
    fun getAllSessionsSync(): List<ActiveUnlockSessionEntity>

    @Query("SELECT * FROM active_unlock_sessions WHERE package_name = :packageName LIMIT 1")
    fun observeSessionByPackage(packageName: String): Flow<ActiveUnlockSessionEntity?>

    @Query("DELETE FROM active_unlock_sessions WHERE package_name = :packageName")
    suspend fun deleteSessionByPackage(packageName: String): Int

    @Query("DELETE FROM active_unlock_sessions WHERE package_name = :packageName")
    fun deleteSessionByPackageSync(packageName: String): Int

    @Query("DELETE FROM active_unlock_sessions")
    suspend fun deleteAllSessions(): Int

    @Query("DELETE FROM active_unlock_sessions")
    fun deleteAllSessionsSync(): Int
}
