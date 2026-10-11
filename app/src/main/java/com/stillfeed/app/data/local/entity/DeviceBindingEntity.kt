package com.stillfeed.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing local device-bound installation metadata.
 */
@Entity(tableName = "device_binding")
data class DeviceBindingEntity(
    @PrimaryKey
    val id: String = SINGLE_ROW_ID,
    val bindingId: String,
    val status: String,
    val isDurableAcrossReinstall: Boolean = false,
    val providerName: String = "UNKNOWN",
    val createdAtMillis: Long,
    val lastVerifiedAtMillis: Long,
    val appVersion: String
) {
    companion object {
        const val SINGLE_ROW_ID = "device_binding_singleton"
    }
}
