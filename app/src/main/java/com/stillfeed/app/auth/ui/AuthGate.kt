package com.stillfeed.app.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stillfeed.app.auth.model.AuthState
import com.stillfeed.app.auth.model.AuthUser
import com.stillfeed.app.core.ui.components.StillfeedWordmark
import com.stillfeed.app.core.ui.theme.StillfeedTheme

/**
 * Authentication Navigation Gate.
 *
 * Enforces that unauthenticated anonymous users CANNOT enter the protected application area.
 * Lifecycle:
 * App launch -> Check session (Loading) -> SignedOut OR SignedIn.
 *
 * If SignedOut (or in sign-in flow/error): renders authentication UI.
 * If SignedIn: renders [authenticatedContent].
 */
@Composable
fun AuthGate(
    viewModel: AuthViewModel,
    authenticatedContent: @Composable (AuthUser) -> Unit
) {
    val authState by viewModel.currentAuthState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    when (val state = authState) {
        is AuthState.Loading -> {
            AuthLoadingSplash()
        }
        is AuthState.SignedIn -> {
            authenticatedContent(state.user)
        }
        is AuthState.NeedsEmailVerification,
        is AuthState.SignedOut,
        is AuthState.SigningIn,
        is AuthState.SigningUp,
        is AuthState.Error,
        is AuthState.SessionExpired -> {
            when (uiState.currentMode) {
                AuthScreenMode.SIGN_IN -> {
                    SignInScreen(
                        viewModel = viewModel,
                        onNavigateToCreateAccount = { viewModel.navigateToCreateAccount() }
                    )
                }
                AuthScreenMode.CREATE_ACCOUNT -> {
                    CreateAccountScreen(
                        viewModel = viewModel,
                        onNavigateToSignIn = { viewModel.navigateToSignIn() }
                    )
                }
                AuthScreenMode.VERIFY_CODE -> {
                    VerifyCodeScreen(
                        viewModel = viewModel,
                        onNavigateBack = {
                            if (uiState.isSignUpMode) {
                                viewModel.navigateToCreateAccount()
                            } else {
                                viewModel.navigateToSignIn()
                            }
                        }
                    )
                }
                AuthScreenMode.FORGOT_PASSWORD -> {
                    ForgotPasswordScreen(
                        viewModel = viewModel,
                        onNavigateBack = { viewModel.navigateToSignIn() }
                    )
                }
                AuthScreenMode.RESET_LINK_SENT -> {
                    ResetLinkSentScreen(
                        viewModel = viewModel,
                        onNavigateToSignIn = { viewModel.navigateToSignIn() }
                    )
                }
                AuthScreenMode.RESET_PASSWORD -> {
                    ResetPasswordScreen(
                        viewModel = viewModel,
                        onNavigateToSignIn = { viewModel.navigateToSignIn() }
                    )
                }
            }
        }
    }
}

@Composable
internal fun AuthLoadingSplash(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StillfeedTheme.colors.backgroundPrimary)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            StillfeedWordmark()
            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))
            Text(
                text = "YOUR ATTENTION IS STILL YOURS",
                style = StillfeedTheme.typography.label,
                color = StillfeedTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))
            CircularProgressIndicator(
                color = StillfeedTheme.colors.accent,
                strokeWidth = 2.dp
            )
        }
    }
}
