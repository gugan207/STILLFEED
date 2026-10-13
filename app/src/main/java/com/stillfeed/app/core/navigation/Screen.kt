package com.stillfeed.app.core.navigation

sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object Bootstrap : Screen("bootstrap")
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object AwarenessSelection : Screen("awareness_selection")
    data object Insights : Screen("insights")
    data object Settings : Screen("settings")
    data object ResetActivity : Screen("reset_activity")
}
