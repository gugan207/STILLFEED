package com.stillfeed.app.data.local.mapper

import com.stillfeed.app.data.local.entity.DeviceBindingEntity
import com.stillfeed.app.domain.model.DeviceBinding
import com.stillfeed.app.domain.model.DeviceBindingStatus

fun DeviceBindingEntity?.toDomain(): DeviceBinding {
    if (this == null) {
        return DeviceBinding(
            bindingId = "",
            status = DeviceBindingStatus.UNBOUND,
            isDurableAcrossReinstall = false,
            providerName = "NONE",
            createdAtMillis = System.currentTimeMillis(),
            lastVerifiedAtMillis = System.currentTimeMillis()
        )
    }
    val parsedStatus = runCatching { DeviceBindingStatus.valueOf(status) }
        .getOrDefault(DeviceBindingStatus.BOUND)

    return DeviceBinding(
        bindingId = bindingId,
        status = parsedStatus,
        isDurableAcrossReinstall = isDurableAcrossReinstall,
        providerName = providerName,
        createdAtMillis = createdAtMillis,
        lastVerifiedAtMillis = lastVerifiedAtMillis,
        appVersion = appVersion
    )
}

fun DeviceBinding.toEntity(): DeviceBindingEntity = DeviceBindingEntity(
    id = DeviceBindingEntity.SINGLE_ROW_ID,
    bindingId = bindingId,
    status = status.name,
    isDurableAcrossReinstall = isDurableAcrossReinstall,
    providerName = providerName,
    createdAtMillis = createdAtMillis,
    lastVerifiedAtMillis = lastVerifiedAtMillis,
    appVersion = appVersion
)
