package com.stillfeed.app.auth.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * STILLFEED Forgot Password Screen (Figma Frame: 00D — Forgot Password, Node 289:770).
 *
 * Exact specifications:
 * - Brand Header: Focus mark + SKILLFEED wordmark
 * - Title: "Reset your password." (Inter SemiBold 42sp, #1F2421)
 * - Subtitle: "Enter the email you signed up with and we’ll send you a link to choose a new password." (Inter Regular 15sp, #6D746E)
 * - Email field: Surface #FBF8F2, 1dp border #D9D4CA
 * - Primary CTA: "Send reset link" (pill 56dp, #1F2421)
 * - Navigation link: "Back to sign in"
 */
@Composable
fun ForgotPasswordScreen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

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
                text = "Reset your password.",
                fontSize = 42.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2421),
                lineHeight = 46.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter the email you signed up with and we’ll send you a link to choose a new password.",
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF6D746E),
                lineHeight = 22.sp
            )

            // Respectful error feedback
            if (uiState.errorMessage != null) {
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
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        viewModel.submitForgotPassword()
                    }
                ),
                enabled = !uiState.isLoading,
                hasError = uiState.emailError != null
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Primary CTA: Send reset link
            AuthPrimaryButton(
                text = if (uiState.isLoading) "Sending link…" else "Send reset link",
                onClick = {
                    focusManager.clearFocus()
                    viewModel.submitForgotPassword()
                },
                isLoading = uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Back to sign in link
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
                                onNavigateBack()
                            }
                        )
                        .padding(vertical = 4.dp)
                        .semantics { this.role = Role.Button }
                )
            }
        }
    }
}
