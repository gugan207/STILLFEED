package com.stillfeed.app.auth.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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

/**
 * STILLFEED Reset Password Screen.
 *
 * Provides fields to enter and confirm a new password after opening a valid Supabase recovery deep link.
 * Adheres strictly to STILLFEED design system and calmness principles:
 * - Brand Header: Focus mark + SKILLFEED wordmark
 * - Title: "Create new password." (38sp SemiBold, #1F2421)
 * - Subtitle: "Choose a strong password with at least 8 characters, including 1 capital letter and 1 symbol/number."
 * - Primary CTA: "Update password" (pill 56dp, #1F2421)
 * - Respectful validation feedback without shaming
 */
@Composable
fun ResetPasswordScreen(
    viewModel: AuthViewModel,
    onNavigateToSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    var isNewPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFF5F0E7),
        topBar = {
            AuthBrandHeader(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 8.dp)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Create new password.",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2421),
                    lineHeight = 42.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Choose a strong password with at least 8 characters, including 1 capital letter and 1 symbol/number.",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF6D746E),
                    lineHeight = 22.sp
                )

                if (uiState.isResetPasswordSuccess) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = uiState.resetPasswordSuccessMessage
                            ?: "Your password has been reset successfully! Please sign in with your new password.",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2E6930),
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    AuthPrimaryButton(
                        text = "Sign in",
                        onClick = { onNavigateToSignIn() },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    if (uiState.errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        AuthAlertCard(message = uiState.errorMessage ?: "")
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // New Password Field
                    Column {
                        AuthInputField(
                            label = "New password",
                            value = uiState.newPasswordInput,
                            onValueChange = { viewModel.onNewPasswordChanged(it) },
                            placeholder = "••••••••",
                            visualTransformation = if (isNewPasswordVisible) {
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
                            hasError = uiState.newPasswordError != null,
                            trailingContent = {
                                PasswordVisibilityToggle(
                                    isPasswordVisible = isNewPasswordVisible,
                                    onToggle = { isNewPasswordVisible = !isNewPasswordVisible },
                                    enabled = !uiState.isLoading
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = uiState.newPasswordError ?: "Use at least 8 characters with 1 capital letter & 1 symbol/number.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = if (uiState.newPasswordError != null) Color(0xFFBA1A1A) else Color(0xFF6D746E),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Confirm New Password Field
                    Column {
                        AuthInputField(
                            label = "Confirm new password",
                            value = uiState.confirmNewPasswordInput,
                            onValueChange = { viewModel.onConfirmNewPasswordChanged(it) },
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
                                    viewModel.submitResetPassword()
                                }
                            ),
                            enabled = !uiState.isLoading,
                            hasError = uiState.confirmNewPasswordError != null,
                            trailingContent = {
                                PasswordVisibilityToggle(
                                    isPasswordVisible = isConfirmPasswordVisible,
                                    onToggle = { isConfirmPasswordVisible = !isConfirmPasswordVisible },
                                    enabled = !uiState.isLoading
                                )
                            }
                        )

                        if (uiState.confirmNewPasswordError != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uiState.confirmNewPasswordError ?: "",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFFBA1A1A),
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    AuthPrimaryButton(
                        text = if (uiState.isLoading) "Updating password…" else "Update password",
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.submitResetPassword()
                        },
                        isLoading = uiState.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Back to sign in",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1F2421),
                            modifier = Modifier
                                .clickable(
                                    enabled = !uiState.isLoading,
                                    role = Role.Button,
                                    onClick = {
                                        focusManager.clearFocus()
                                        onNavigateToSignIn()
                                    }
                                )
                                .padding(vertical = 4.dp)
                                .semantics { this.role = Role.Button }
                        )
                    }
                }
            }
        }
    }
}
