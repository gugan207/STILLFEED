package com.stillfeed.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing persistent protection toggle state.
 */
@Entity(tableName = "protection_state")
data class ProtectionStateEntity(
    @PrimaryKey
    val id: String = SINGLE_ROW_ID,
    val isEnabled: Boolean,
    val updatedAtMillis: Long
) {
    companion object {
        const val SINGLE_ROW_ID = "protection_singleton"
    }
}
