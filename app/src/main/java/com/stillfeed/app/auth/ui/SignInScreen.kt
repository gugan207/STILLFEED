package com.stillfeed.app.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stillfeed.app.core.ui.components.StillfeedScreenHeader
import com.stillfeed.app.core.ui.theme.StillfeedTheme

/**
 * STILLFEED Sign In Screen (00A — Sign In).
 *
 * Implements the exact finalized Figma screen (Node 265:36):
 * - Canvas: 390 x 844 dp
 * - Background fill: StillfeedWarmPaper (#F5F0E7)
 * - Header: SKILLFEED brand wordmark, Step indicator "01 / 02", Section "ACCOUNT"
 * - Title: "Welcome back." (Inter SemiBold 42sp)
 * - Subtitle: "Sign in to continue with STILLFEED." (Inter Regular 14sp)
 * - Email field (334 x 62 dp, 18dp corner radius, CardCream fill, 1dp SandBorder)
 * - Password field with independent eye toggle (44dp touch target, "Show password" / "Hide password")
 * - Primary CTA: "SIGN IN" (294 x 52 dp, 26dp radius, Charcoal fill)
 * - Secondary action: "New here?  CREATE ACCOUNT"
 *
 * NO social logins, NO forgot-password UI, NO extra cards or gradients.
 */
@Composable
fun SignInScreen(
    viewModel: AuthViewModel,
    onNavigateToCreateAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StillfeedTheme.colors.backgroundPrimary
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = StillfeedTheme.spacing.screenMargin)
                .padding(top = StillfeedTheme.spacing.lg, bottom = StillfeedTheme.spacing.xxl),
            horizontalAlignment = Alignment.Start
        ) {
            // Screen Header: Brand wordmark (left) + "01 / 02" (right) + "ACCOUNT"
            StillfeedScreenHeader(
                sectionLabel = "ACCOUNT",
                stepCounter = "01 / 02"
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Screen Title
            Text(
                text = "Welcome back.",
                style = StillfeedTheme.typography.headlineLarge,
                color = StillfeedTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Screen Subtitle
            Text(
                text = "Sign in to continue with STILLFEED.",
                style = StillfeedTheme.typography.bodyMedium,
                color = StillfeedTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Email Input Field
            AuthInputField(
                label = "Email",
                value = uiState.emailInput,
                onValueChange = { viewModel.onEmailChanged(it) },
                placeholder = "Your email",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                enabled = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Password Input Field with Eye Toggle
            AuthInputField(
                label = "Password",
                value = uiState.passwordInput,
                onValueChange = { viewModel.onPasswordChanged(it) },
                placeholder = "••••••••",
                visualTransformation = if (isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        viewModel.submitSignIn()
                    }
                ),
                enabled = !uiState.isLoading,
                trailingContent = {
                    PasswordVisibilityToggle(
                        isPasswordVisible = isPasswordVisible,
                        onToggle = { isPasswordVisible = !isPasswordVisible },
                        enabled = !uiState.isLoading
                    )
                }
            )

            // Respectful error feedback
            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = uiState.errorMessage.orEmpty(),
                    style = StillfeedTheme.typography.bodySmall,
                    color = StillfeedTheme.colors.destructive,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Centered Primary CTA & Secondary Action
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AuthPrimaryButton(
                    text = "SIGN IN",
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.submitSignIn()
                    },
                    isLoading = uiState.isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                AuthSecondaryAction(
                    prefixText = "New here?",
                    actionText = "CREATE ACCOUNT",
                    onClick = {
                        focusManager.clearFocus()
                        onNavigateToCreateAccount()
                    },
                    enabled = !uiState.isLoading
                )
            }
        }
    }
}
