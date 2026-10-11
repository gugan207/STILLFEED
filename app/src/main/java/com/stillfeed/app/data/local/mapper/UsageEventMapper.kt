package com.stillfeed.app.data.local.mapper

import com.stillfeed.app.data.local.entity.UsageEventEntity
import com.stillfeed.app.domain.model.SurfaceCategory
import com.stillfeed.app.domain.model.UsageEvent
import com.stillfeed.app.domain.model.UsageEventType

fun UsageEventEntity.toDomain(): UsageEvent {
    val category = runCatching { SurfaceCategory.valueOf(surfaceCategory) }
        .getOrDefault(SurfaceCategory.UNKNOWN)
    val type = runCatching { UsageEventType.valueOf(eventType) }
        .getOrDefault(UsageEventType.CONTENT_DETECTED)

    return UsageEvent(
        id = id,
        timestampMillis = timestampMillis,
        packageName = packageName,
        surfaceCategory = category,
        eventType = type,
        durationMillis = durationMillis,
        isBlocked = isBlocked,
        isUnlocked = isUnlocked,
        metadata = metadata
    )
}

fun UsageEvent.toEntity(): UsageEventEntity = UsageEventEntity(
    id = id,
    timestampMillis = timestampMillis,
    packageName = packageName,
    surfaceCategory = surfaceCategory.name,
    eventType = eventType.name,
    durationMillis = durationMillis,
    isBlocked = isBlocked,
    isUnlocked = isUnlocked,
    metadata = metadata
)
