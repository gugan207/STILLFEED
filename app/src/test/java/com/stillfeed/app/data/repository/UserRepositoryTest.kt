package com.stillfeed.app.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stillfeed.app.data.local.StillfeedDatabase
import com.stillfeed.app.domain.model.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserRepositoryTest {

    private lateinit var database: StillfeedDatabase
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: LocalUserRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = StillfeedDatabase.buildInMemory(context)
        repository = LocalUserRepository(database.userDao(), testDispatcher)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertUser_and_readUser_succeeds() = runTest(testDispatcher) {
        val user = User(
            id = "user_persisted_1",
            email = "user1@stillfeed.app",
            displayName = "Persisted One",
            createdAtMillis = 1000L,
            updatedAtMillis = 1000L,
            deviceBindingId = "bind_device_001"
        )
        repository.saveUser(user)

        val retrieved = repository.getUser("user_persisted_1")
        assertNotNull(retrieved)
        assertEquals("user_persisted_1", retrieved?.id)
        assertEquals("user1@stillfeed.app", retrieved?.email)
        assertEquals("Persisted One", retrieved?.displayName)
        assertEquals("bind_device_001", retrieved?.deviceBindingId)

        val observed = repository.observeUser("user_persisted_1").first()
        assertEquals("user_persisted_1", observed?.id)
    }

    @Test
    fun updateUser_modifiesExistingUser() = runTest(testDispatcher) {
        val user = User(
            id = "user_persisted_2",
            email = "initial@stillfeed.app",
            displayName = "Initial Name",
            createdAtMillis = 1000L,
            updatedAtMillis = 1000L
        )
        repository.saveUser(user)

        val updated = user.copy(
            displayName = "Updated Name",
            updatedAtMillis = 2000L
        )
        repository.updateUser(updated)

        val retrieved = repository.getUser("user_persisted_2")
        assertEquals("Updated Name", retrieved?.displayName)
        assertEquals(2000L, retrieved?.updatedAtMillis)
    }

    @Test
    fun persistenceAcrossRepositoryRecreation_preservesData() = runTest(testDispatcher) {
        val user = User(
            id = "user_recreation_test",
            email = "recreate@stillfeed.app",
            displayName = "Recreation User",
            createdAtMillis = 5000L,
            updatedAtMillis = 5000L,
            deviceBindingId = "bind_abc"
        )
        repository.saveUser(user)

        // Instantiate a completely new repository instance over the same database
        val newRepositoryInstance = LocalUserRepository(database.userDao(), testDispatcher)

        val retrieved = newRepositoryInstance.getUser("user_recreation_test")
        assertNotNull(retrieved)
        assertEquals("user_recreation_test", retrieved?.id)
        assertEquals("recreate@stillfeed.app", retrieved?.email)
        assertEquals("bind_abc", retrieved?.deviceBindingId)
    }

    @Test
    fun deleteUser_removesSpecifiedUser() = runTest(testDispatcher) {
        val user = User(
            id = "user_to_delete",
            email = "delete@stillfeed.app",
            displayName = "Delete Me",
            createdAtMillis = 1000L,
            updatedAtMillis = 1000L
        )
        repository.saveUser(user)
        assertNotNull(repository.getUser("user_to_delete"))

        repository.deleteUser("user_to_delete")
        assertNull(repository.getUser("user_to_delete"))
    }
}
