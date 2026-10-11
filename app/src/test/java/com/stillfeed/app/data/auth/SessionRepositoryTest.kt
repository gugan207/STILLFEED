package com.stillfeed.app.data.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stillfeed.app.domain.model.AuthSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SessionRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: SecureSessionRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Clear prefs before test
        context.getSharedPreferences("test_secure_session_tokens", Context.MODE_PRIVATE).edit().clear().commit()
        repository = SecureSessionRepository(context, "test_secure_session_tokens")
    }

    @Test
    fun saveSession_persistsTokensAndRetrievesCorrectly() = runTest {
        val session = AuthSession(
            userId = "user_abc_123",
            accessToken = "jwt_access_alpha",
            refreshToken = "jwt_refresh_beta",
            expiresAtMillis = System.currentTimeMillis() + 3600000L,
            userEmail = "user@stillfeed.app",
            deviceBindingId = "bind_device_token_77"
        )

        repository.saveSession(session)

        val retrieved = repository.getSession()
        assertNotNull(retrieved)
        assertEquals("user_abc_123", retrieved?.userId)
        assertEquals("jwt_access_alpha", retrieved?.accessToken)
        assertEquals("jwt_refresh_beta", retrieved?.refreshToken)
        assertEquals("user@stillfeed.app", retrieved?.userEmail)
        assertEquals("bind_device_token_77", retrieved?.deviceBindingId)
        assertFalse(retrieved?.isExpired ?: true)
    }

    @Test
    fun isSessionValid_withActiveToken_returnsTrue() = runTest {
        val session = AuthSession(
            userId = "user_valid",
            accessToken = "valid_token",
            expiresAtMillis = System.currentTimeMillis() + 60000L
        )
        repository.saveSession(session)

        assertTrue(repository.isSessionValid())
    }

    @Test
    fun isSessionValid_withExpiredToken_returnsFalse() = runTest {
        val session = AuthSession(
            userId = "user_expired",
            accessToken = "expired_token",
            expiresAtMillis = System.currentTimeMillis() - 1000L
        )
        repository.saveSession(session)

        assertFalse(repository.isSessionValid())
    }

    @Test
    fun clearSession_wipesAllTokensAndFlowEmitsNull() = runTest {
        val session = AuthSession(
            userId = "user_wipe",
            accessToken = "token_wipe",
            expiresAtMillis = System.currentTimeMillis() + 60000L
        )
        repository.saveSession(session)
        assertNotNull(repository.getSession())

        repository.clearSession()

        assertNull(repository.getSession())
        assertFalse(repository.isSessionValid())
        assertNull(repository.observeSession().first())
    }

    @Test
    fun inMemorySessionRepository_conformsToSessionRepositoryContract() = runTest {
        val inMemoryRepo = InMemorySessionRepository()
        assertNull(inMemoryRepo.getSession())
        assertFalse(inMemoryRepo.isSessionValid())

        val session = AuthSession(
            userId = "mem_user",
            accessToken = "mem_token",
            expiresAtMillis = System.currentTimeMillis() + 60000L
        )
        inMemoryRepo.saveSession(session)

        assertEquals("mem_user", inMemoryRepo.getSession()?.userId)
        assertTrue(inMemoryRepo.isSessionValid())

        inMemoryRepo.clearSession()
        assertNull(inMemoryRepo.getSession())
        assertFalse(inMemoryRepo.isSessionValid())
    }
}
