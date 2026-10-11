package com.stillfeed.app.auth.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stillfeed.app.core.ui.theme.StillfeedTheme

/**
 * STILLFEED Create Account Screen (Figma Frames: 00B — Create Account, 00B-E — Validation, Nodes 289:517, 289:595).
 *
 * Exact specifications:
 * - Brand Header: Focus mark + SKILLFEED wordmark
 * - Title: "Create your account." (Inter SemiBold 42sp, #1F2421)
 * - Subtitle: "A simple account to continue with SKILLFEED." (Inter Regular 15sp, #6D746E)
 * - Form inputs: Email, Password, Confirm Password with eye toggles
 * - Validation feedback: "Use at least 10 characters.", "Passwords don’t match."
 * - Primary CTA: "Create account" (pill 56dp, #1F2421)
 * - Legal text: "By creating an account, you agree to the Terms of Service and Privacy Policy."
 * - Divider: "or"
 * - Secondary CTA: "Continue with Google"
 * - Footer: "Already have an account?  Sign in"
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
        containerColor = Color(0xFFF5F0E7)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Brand Header
            AuthBrandHeader()

            Spacer(modifier = Modifier.height(24.dp))

            // Screen Title & Subtitle
            Text(
                text = "Create your account.",
                fontSize = 42.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2421),
                lineHeight = 46.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "A simple account to continue with SKILLFEED.",
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF6D746E)
            )

            // Respectful error feedback
            if (uiState.errorMessage != null && uiState.passwordError == null && uiState.confirmPasswordError == null) {
                Spacer(modifier = Modifier.height(20.dp))
                AuthAlertCard(message = uiState.errorMessage ?: "")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Email Input Field
            AuthInputField(
                label = "Email",
                value = uiState.emailInput,
                onValueChange = { viewModel.onEmailChanged(it) },
                placeholder = "you@example.com",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                enabled = !uiState.isLoading,
                hasError = uiState.emailError != null
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Password Input Field with Eye Toggle
            Column {
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
                    hasError = uiState.passwordError != null,
                    trailingContent = {
                        PasswordVisibilityToggle(
                            isPasswordVisible = isPasswordVisible,
                            onToggle = { isPasswordVisible = !isPasswordVisible },
                            enabled = !uiState.isLoading
                        )
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Password hint or validation error
                Text(
                    text = uiState.passwordError ?: "Use at least 10 characters.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (uiState.passwordError != null) Color(0xFFB3261E) else Color(0xFF6D746E),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Confirm Password Input Field with Independent Eye Toggle
            Column {
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
                    hasError = uiState.confirmPasswordError != null,
                    trailingContent = {
                        PasswordVisibilityToggle(
                            isPasswordVisible = isConfirmPasswordVisible,
                            onToggle = { isConfirmPasswordVisible = !isConfirmPasswordVisible },
                            enabled = !uiState.isLoading
                        )
                    }
                )

                if (uiState.confirmPasswordError != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uiState.confirmPasswordError ?: "",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFFB3261E),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary CTA: Create Account
            AuthPrimaryButton(
                text = if (uiState.isLoading) "Creating account…" else "Create account",
                onClick = {
                    focusManager.clearFocus()
                    viewModel.submitSignUp()
                },
                isLoading = uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Legal notice
            Text(
                text = "By creating an account, you agree to the Terms of Service and Privacy Policy.",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF6D746E),
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // OR Divider
            AuthDivider()

            Spacer(modifier = Modifier.height(20.dp))

            // Google Sign-In CTA
            GoogleSignInButton(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.submitGoogleSignIn()
                },
                isLoading = false,
                enabled = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Bottom Navigation Prompt: "Already have an account?  Sign in"
            val interactionSource = remember { MutableInteractionSource() }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = !uiState.isLoading,
                        role = Role.Button,
                        onClick = {
                            focusManager.clearFocus()
                            onNavigateToSignIn()
                        }
                    )
                    .semantics { this.role = Role.Button },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account?  ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF6D746E)
                )
                Text(
                    text = "Sign in",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2421)
                )
            }
        }
    }
}
