package com.stillfeed.app.data.local.mapper

import com.stillfeed.app.data.local.entity.ProtectionStateEntity
import com.stillfeed.app.domain.model.ProtectionState

fun ProtectionStateEntity?.toDomain(): ProtectionState =
    if (this == null) {
        ProtectionState(isEnabled = false, updatedAtMillis = System.currentTimeMillis())
    } else {
        ProtectionState(isEnabled = isEnabled, updatedAtMillis = updatedAtMillis)
    }

fun ProtectionState.toEntity(): ProtectionStateEntity = ProtectionStateEntity(
    id = ProtectionStateEntity.SINGLE_ROW_ID,
    isEnabled = isEnabled,
    updatedAtMillis = updatedAtMillis
)
