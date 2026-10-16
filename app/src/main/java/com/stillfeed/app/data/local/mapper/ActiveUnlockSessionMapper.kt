package com.stillfeed.app.data.local.mapper

import com.stillfeed.app.data.local.entity.ActiveUnlockSessionEntity
import com.stillfeed.app.domain.model.BatchUnlockSession

fun ActiveUnlockSessionEntity.toDomain(): BatchUnlockSession = BatchUnlockSession(
    id = id,
    packageName = packageName,
    selectedBatchAllowance = selectedBatchAllowance,
    remainingBatchAllowance = remainingBatchAllowance,
    activityType = activityType,
    verifiedActivityCompletion = verifiedActivityCompletion,
    createdAtMillis = createdAtMillis,
    expiresAtMillis = expiresAtMillis,
    isIndeterminateTracking = isIndeterminateTracking
)

fun BatchUnlockSession.toEntity(): ActiveUnlockSessionEntity = ActiveUnlockSessionEntity(
    id = id,
    packageName = packageName,
    selectedBatchAllowance = selectedBatchAllowance,
    remainingBatchAllowance = remainingBatchAllowance,
    activityType = activityType,
    verifiedActivityCompletion = verifiedActivityCompletion,
    createdAtMillis = createdAtMillis,
    expiresAtMillis = expiresAtMillis,
    isIndeterminateTracking = isIndeterminateTracking
)
