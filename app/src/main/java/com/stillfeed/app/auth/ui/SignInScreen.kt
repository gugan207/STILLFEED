package com.stillfeed.app.auth.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.runtime.rememberCoroutineScope
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.stillfeed.app.auth.google.GoogleSignInHelper
import kotlinx.coroutines.launch
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stillfeed.app.core.ui.theme.StillfeedTheme

/**
 * STILLFEED Sign In Screen (Figma Frames: 00A — Sign In, 00A-L — Loading, 00A-E — Error, Nodes 289:307, 289:375, 289:443).
 *
 * Exact specifications:
 * - Brand Header: Focus mark + SKILLFEED wordmark positioned in the top left
 * - Title: "Welcome back." (Inter SemiBold 42sp, #1F2421)
 * - Subtitle: "Sign in to continue with SKILLFEED." (Inter Regular 15sp, #6D746E)
 * - Form inputs: Email and Password with eye visibility toggle
 * - Link: "Forgot password?"
 * - Alert card on error: "Email or password is incorrect. Check them and try again."
 * - Primary CTA: "Sign in" / "Signing in…" (pill 56dp, #1F2421)
 * - Divider: "or"
 * - Secondary CTA: "Continue with Google"
 * - Footer: "New to SKILLFEED?  Create an account"
 */
@Composable
fun SignInScreen(
    viewModel: AuthViewModel,
    onNavigateToCreateAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val googleSignInHelper = remember(context) { GoogleSignInHelper(context) }
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }

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
                // Screen Title & Subtitle (Figma Left-Aligned)
                Text(
                    text = "Welcome back.",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2421),
                    lineHeight = 42.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Sign in to continue with SKILLFEED.",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF6D746E)
                )

                // Respectful error feedback
                if (uiState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    AuthAlertCard(message = uiState.errorMessage ?: "")
                }

                Spacer(modifier = Modifier.height(18.dp))

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

                Spacer(modifier = Modifier.height(10.dp))

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
                    hasError = uiState.passwordError != null,
                    trailingContent = {
                        PasswordVisibilityToggle(
                            isPasswordVisible = isPasswordVisible,
                            onToggle = { isPasswordVisible = !isPasswordVisible },
                            enabled = !uiState.isLoading
                        )
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Forgot Password Link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Forgot password?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2421),
                        modifier = Modifier
                            .clickable(enabled = !uiState.isLoading) {
                                focusManager.clearFocus()
                                viewModel.navigateToForgotPassword()
                            }
                            .padding(vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Primary CTA: Sign In / Signing In
                AuthPrimaryButton(
                    text = if (uiState.isLoading) "Signing in…" else "Sign in",
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.submitSignIn()
                    },
                    isLoading = uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // OR Divider
                AuthDivider()

                Spacer(modifier = Modifier.height(12.dp))

                // Google Sign-In CTA
                GoogleSignInButton(
                    onClick = {
                        focusManager.clearFocus()
                        coroutineScope.launch {
                            val tokenResult = googleSignInHelper.getGoogleIdToken()
                            if (tokenResult.isSuccess) {
                                viewModel.submitGoogleSignIn(tokenResult.getOrThrow())
                            } else {
                                val err = tokenResult.exceptionOrNull()
                                if (err !is GetCredentialCancellationException) {
                                    val msg = err?.message ?: "Google Sign-In failed"
                                    viewModel.showErrorMessage(msg)
                                }
                            }
                        }
                    },
                    isLoading = false,
                    enabled = !uiState.isLoading
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Navigation Prompt: "New to SKILLFEED?  Create an account"
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            enabled = !uiState.isLoading,
                            role = Role.Button,
                            onClick = {
                                focusManager.clearFocus()
                                onNavigateToCreateAccount()
                            }
                        )
                        .semantics { this.role = Role.Button },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "New to SKILLFEED?  ",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF6D746E)
                        )
                        Text(
                            text = "Create an account",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1F2421)
                        )
                    }
                }
            }
        }
    }
}
