package com.stillfeed.app.data.repository

import com.stillfeed.app.data.local.dao.ProtectionStateDao
import com.stillfeed.app.data.local.mapper.toDomain
import com.stillfeed.app.data.local.mapper.toEntity
import com.stillfeed.app.domain.model.ProtectionState
import com.stillfeed.app.domain.repository.ProtectionStateRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge

class LocalProtectionStateRepository(
    private val protectionStateDao: ProtectionStateDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ProtectionStateRepository {

    private val localUpdates = MutableSharedFlow<ProtectionState>(replay = 1)

    override fun observeProtectionState(): Flow<ProtectionState> =
        flow {
            emit(getProtectionState())
            emitAll(
                merge(
                    protectionStateDao.observeProtectionState().map { it.toDomain() },
                    localUpdates
                )
            )
        }.distinctUntilChanged()

    override suspend fun getProtectionState(): ProtectionState = withContext(ioDispatcher) {
        protectionStateDao.getProtectionState().toDomain()
    }

    override suspend fun setProtectionEnabled(enabled: Boolean): Unit = withContext(ioDispatcher) {
        val updated = ProtectionState(
            isEnabled = enabled,
            updatedAtMillis = System.currentTimeMillis()
        )
        protectionStateDao.insertOrUpdate(updated.toEntity())
        localUpdates.emit(updated)
    }

    override suspend fun clearProtectionState(): Unit = withContext(ioDispatcher) {
        protectionStateDao.clearProtectionState()
        val defaultState = ProtectionState(isEnabled = false, updatedAtMillis = 0L)
        localUpdates.emit(defaultState)
    }
}
