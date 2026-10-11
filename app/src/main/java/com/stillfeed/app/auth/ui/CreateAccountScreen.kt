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
 * STILLFEED Create Account Screen (00B — Create Account).
 *
 * Implements the exact finalized Figma screen (Node 265:55):
 * - Canvas: 390 x 844 dp
 * - Background fill: StillfeedWarmPaper (#F5F0E7)
 * - Header: SKILLFEED brand wordmark, Step indicator "02 / 02", Section "ACCOUNT"
 * - Title: "Create your account." (Inter SemiBold 42sp)
 * - Subtitle: "A simple account to continue with STILLFEED." (Inter Regular 14sp)
 * - Email field (334 x 62 dp, 18dp corner radius, CardCream fill, 1dp SandBorder)
 * - Password field with independent eye toggle
 * - Confirm Password field with independent eye toggle
 * - Primary CTA: "CREATE ACCOUNT" (294 x 52 dp, 26dp radius, Charcoal fill)
 * - Secondary action: "Already have an account?  SIGN IN"
 *
 * NO extra profile fields, NO unnecessary inputs.
 */
@Composable
fun CreateAccountScreen(
    viewModel: AuthViewModel,
    onNavigateToSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var isConfirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

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
            // Screen Header: Brand wordmark (left) + "02 / 02" (right) + "ACCOUNT"
            StillfeedScreenHeader(
                sectionLabel = "ACCOUNT",
                stepCounter = "02 / 02"
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Screen Title
            Text(
                text = "Create your account.",
                style = StillfeedTheme.typography.headlineLarge,
                color = StillfeedTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Screen Subtitle
            Text(
                text = "A simple account to continue with STILLFEED.",
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

            // Password Input Field with Independent Eye Toggle
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
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
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

            Spacer(modifier = Modifier.height(14.dp))

            // Confirm Password Input Field with Independent Eye Toggle
            AuthInputField(
                label = "Confirm password",
                value = uiState.confirmPasswordInput,
                onValueChange = { viewModel.onConfirmPasswordChanged(it) },
                placeholder = "••••••••",
                visualTransformation = if (isConfirmPasswordVisible) {
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
                        viewModel.submitSignUp()
                    }
                ),
                enabled = !uiState.isLoading,
                trailingContent = {
                    PasswordVisibilityToggle(
                        isPasswordVisible = isConfirmPasswordVisible,
                        onToggle = { isConfirmPasswordVisible = !isConfirmPasswordVisible },
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

            Spacer(modifier = Modifier.height(28.dp))

            // Centered Primary CTA & Secondary Action
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AuthPrimaryButton(
                    text = "CREATE ACCOUNT",
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.submitSignUp()
                    },
                    isLoading = uiState.isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                AuthSecondaryAction(
                    prefixText = "Already have an account?",
                    actionText = "SIGN IN",
                    onClick = {
                        focusManager.clearFocus()
                        onNavigateToSignIn()
                    },
                    enabled = !uiState.isLoading
                )
            }
        }
    }
}
