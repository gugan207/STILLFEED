package com.stillfeed.app.data.local.mapper

import com.stillfeed.app.data.local.entity.UserEntity
import com.stillfeed.app.domain.model.User

fun UserEntity.toDomain(): User = User(
    id = id,
    email = email,
    displayName = displayName,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    deviceBindingId = deviceBindingId
)

fun User.toEntity(): UserEntity = UserEntity(
    id = id,
    email = email,
    displayName = displayName,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    deviceBindingId = deviceBindingId
)
