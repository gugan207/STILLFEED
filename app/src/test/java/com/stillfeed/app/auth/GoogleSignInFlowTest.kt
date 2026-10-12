package com.stillfeed.app.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stillfeed.app.auth.google.GoogleAuthConfig
import com.stillfeed.app.auth.google.GoogleSignInHelper
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GoogleSignInFlowTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        GoogleAuthConfig.serverClientId = ""
    }

    @After
    fun tearDown() {
        GoogleAuthConfig.serverClientId = GoogleAuthConfig.DEFAULT_SERVER_CLIENT_ID
    }

    @Test
    fun getGoogleIdToken_whenUnconfigured_returnsDescriptiveConfigurationError() = runTest {
        val helper = GoogleSignInHelper(context)
        val result = helper.getGoogleIdToken()

        assertTrue(result.isFailure)
        val errorMsg = result.exceptionOrNull()?.message ?: ""
        assertTrue(
            "Expected configuration instruction in error message",
            errorMsg.contains("Google Cloud Web Client ID") || errorMsg.contains("serverClientId")
        )
    }

    @Test
    fun googleAuthConfig_serverClientId_canBeConfigured() {
        GoogleAuthConfig.serverClientId = "test-web-client-id.apps.googleusercontent.com"
        assertTrue(GoogleAuthConfig.serverClientId.isNotBlank())
    }

    @Test
    fun googleAuthConfig_defaultServerClientId_matchesProductionWebClientId() {
        org.junit.Assert.assertEquals(
            "92258811699-dj7fh1fc8lh460h1rtm1f8k0f20pdk4m.apps.googleusercontent.com",
            GoogleAuthConfig.DEFAULT_SERVER_CLIENT_ID
        )
    }
}
