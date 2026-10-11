package com.stillfeed.app.data.repository

import com.stillfeed.app.data.local.dao.UserDao
import com.stillfeed.app.data.local.mapper.toDomain
import com.stillfeed.app.data.local.mapper.toEntity
import com.stillfeed.app.domain.model.User
import com.stillfeed.app.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LocalUserRepository(
    private val userDao: UserDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : UserRepository {

    override fun observeUser(userId: String): Flow<User?> =
        userDao.observeUserById(userId).map { it?.toDomain() }

    override fun observeCurrentUser(): Flow<User?> =
        userDao.observeFirstUser().map { it?.toDomain() }

    override suspend fun getUser(userId: String): User? = withContext(ioDispatcher) {
        userDao.getUserById(userId)?.toDomain()
    }

    override suspend fun getCurrentUser(): User? = withContext(ioDispatcher) {
        userDao.getFirstUser()?.toDomain()
    }

    override suspend fun saveUser(user: User): Unit = withContext(ioDispatcher) {
        userDao.insertUser(user.toEntity())
    }

    override suspend fun updateUser(user: User): Unit = withContext(ioDispatcher) {
        userDao.updateUser(user.toEntity())
    }

    override suspend fun deleteUser(userId: String): Unit = withContext(ioDispatcher) {
        userDao.deleteUserById(userId)
    }

    override suspend fun clearAllUsers(): Unit = withContext(ioDispatcher) {
        userDao.deleteAllUsers()
    }
}
