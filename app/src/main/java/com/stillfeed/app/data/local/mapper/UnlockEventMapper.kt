package com.stillfeed.app.data.local.mapper

import com.stillfeed.app.data.local.entity.UnlockEventEntity
import com.stillfeed.app.domain.model.UnlockEvent

fun UnlockEventEntity.toDomain(): UnlockEvent = UnlockEvent(
    id = id,
    timestampMillis = timestampMillis,
    allowanceMinutes = allowanceMinutes,
    reason = reason,
    relatedUsageEventId = relatedUsageEventId
)

fun UnlockEvent.toEntity(): UnlockEventEntity = UnlockEventEntity(
    id = id,
    timestampMillis = timestampMillis,
    allowanceMinutes = allowanceMinutes,
    reason = reason,
    relatedUsageEventId = relatedUsageEventId
)
