package com.stillfeed.app.auth.model

/**
 * Domain authentication errors.
 *
 * All user-facing error messages adhere to STILLFEED communication principles:
 * - Calm and respectful tone.
 * - No shame, guilt, blame, or behavioral scolding.
 * - No internal backend technical details leaked to users.
 */
sealed class AuthError(open val userMessage: String) {
    data class InvalidCredentials(
        override val userMessage: String = "The email or password does not match our records. Please try again."
    ) : AuthError(userMessage)

    data class AccountAlreadyExists(
        override val userMessage: String = "An account with this email already exists. Please sign in instead."
    ) : AuthError(userMessage)

    data class InvalidEmail(
        override val userMessage: String = "Please enter a valid email address."
    ) : AuthError(userMessage)

    data class WeakPassword(
        override val userMessage: String = "Please choose a password with at least 8 characters."
    ) : AuthError(userMessage)

    data class NetworkFailure(
        override val userMessage: String = "Unable to connect. Please check your connection and try again."
    ) : AuthError(userMessage)

    data class SessionExpired(
        override val userMessage: String = "Your session has ended. Please sign in again to continue."
    ) : AuthError(userMessage)

    data class AccountCreationFailed(
        override val userMessage: String = "We could not complete your account creation right now. Please try again shortly."
    ) : AuthError(userMessage)

    data class Unknown(
        override val userMessage: String = "Something went wrong while signing in. Please try again in a moment."
    ) : AuthError(userMessage)
}
