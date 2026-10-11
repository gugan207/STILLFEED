package com.stillfeed.app.data.local.mapper

import com.stillfeed.app.data.local.entity.AwarenessMonthlyEntity
import com.stillfeed.app.domain.model.AwarenessMonthlyState
import com.stillfeed.app.domain.model.MonthlyAwarenessStatus

fun AwarenessMonthlyEntity.toDomain(): AwarenessMonthlyState {
    val target = if (targetUsageMinutes > 0) targetUsageMinutes else AwarenessMonthlyState.calculateTargetUsage(baselineMinutes, targetReductionPercentage)
    val reduction = if (reductionAmountMinutes > 0) reductionAmountMinutes else AwarenessMonthlyState.calculateReductionAmount(baselineMinutes, targetReductionPercentage)
    return AwarenessMonthlyState(
        monthIdentifier = monthIdentifier,
        targetReductionPercentage = targetReductionPercentage,
        baselineMinutes = baselineMinutes,
        currentProgressMinutes = currentProgressMinutes,
        targetUsageMinutes = target,
        reductionAmountMinutes = reduction,
        status = MonthlyAwarenessStatus.fromString(status),
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

fun AwarenessMonthlyState.toEntity(): AwarenessMonthlyEntity = AwarenessMonthlyEntity(
    monthIdentifier = monthIdentifier,
    targetReductionPercentage = targetReductionPercentage,
    baselineMinutes = baselineMinutes,
    currentProgressMinutes = currentProgressMinutes,
    targetUsageMinutes = targetUsageMinutes,
    reductionAmountMinutes = reductionAmountMinutes,
    status = status.name,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)
