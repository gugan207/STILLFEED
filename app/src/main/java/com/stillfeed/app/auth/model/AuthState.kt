package com.stillfeed.app.auth.model

/**
 * Explicit representation of STILLFEED authentication lifecycle.
 *
 * Guarantees that the app cleanly distinguishes unauthenticated from authenticated states,
 * preventing unauthenticated anonymous users from entering protected screens.
 */
sealed interface AuthState {
    /**
     * Determining existing session upon launch or verifying credentials.
     */
    data object Loading : AuthState

    /**
     * User is signed out. Must enter via authentication flow.
     */
    data object SignedOut : AuthState

    /**
     * Sign-in operation actively in flight.
     */
    data object SigningIn : AuthState

    /**
     * Account creation operation actively in flight.
     */
    data object SigningUp : AuthState

    /**
     * User has an active, verified session.
     */
    data class SignedIn(val user: AuthUser) : AuthState

    /**
     * Previous session has expired; re-authentication is required.
     */
    data class SessionExpired(val message: String = "Your session has expired. Please sign in again.") : AuthState

    /**
     * Authentication error occurred.
     */
    data class Error(val error: AuthError) : AuthState
}
