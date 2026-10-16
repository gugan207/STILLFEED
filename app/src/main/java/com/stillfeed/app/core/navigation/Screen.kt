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
    data object BatchFinished : Screen("batch_finished")
    data object PermissionDisabled : Screen("permission_disabled")
    data object Offline : Screen("offline")
    data object FirstDay : Screen("first_day")

    // Flow 05: Reports
    data object WeeklyReport : Screen("weekly_report")
    data object MonthlyReview : Screen("monthly_review")
    data object YearlyReport : Screen("yearly_report")

    // Flow 06: Account & Privacy
    data object ProfileSecurity : Screen("profile_security")
    data object PrivacyCenter : Screen("privacy_center")
    data object DataExport : Screen("data_export")

    // Flow 07: Security & Support
    data object AppLock : Screen("app_lock")
    data object Support : Screen("support")

    // Flow 08: Deletion
    data object DeleteAccount : Screen("delete_account")
    data object DeletionPending : Screen("deletion_pending")

    // Flow 09: Supporting Settings States
    data object EditProfile : Screen("edit_profile")
    data object ChangePassword : Screen("change_password")
    data object NotificationSettings : Screen("notification_settings")
    data object AppearanceSettings : Screen("appearance_settings")
    data object ActiveSessions : Screen("active_sessions")
    data object DataExportReady : Screen("data_export_ready")
}
