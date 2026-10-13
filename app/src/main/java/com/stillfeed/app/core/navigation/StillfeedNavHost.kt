package com.stillfeed.app.core.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import android.content.Intent
import com.stillfeed.app.StillfeedApplication
import com.stillfeed.app.auth.model.AuthState
import com.stillfeed.app.auth.repository.AuthRepository
import com.stillfeed.app.auth.ui.AuthGate
import com.stillfeed.app.auth.ui.AuthLoadingSplash
import com.stillfeed.app.auth.ui.AuthViewModel
import com.stillfeed.app.domain.repository.AppPreferencesRepository
import com.stillfeed.app.feature.awareness.AwarenessSelectionScreen
import com.stillfeed.app.feature.awareness.AwarenessViewModel
import com.stillfeed.app.feature.bootstrap.BootstrapScreen
import com.stillfeed.app.feature.edgestates.BatchFinishedScreen
import com.stillfeed.app.feature.edgestates.BatchFinishedViewModel
import com.stillfeed.app.feature.edgestates.FirstDayScreen
import com.stillfeed.app.feature.edgestates.FirstDayViewModel
import com.stillfeed.app.feature.edgestates.OfflineScreen
import com.stillfeed.app.feature.edgestates.OfflineViewModel
import com.stillfeed.app.feature.edgestates.PermissionDisabledScreen
import com.stillfeed.app.feature.edgestates.PermissionDisabledViewModel
import com.stillfeed.app.feature.edgestates.connectivity.AndroidNetworkConnectivityObserver
import com.stillfeed.app.feature.home.HomeScreen
import com.stillfeed.app.feature.insights.InsightsScreen
import com.stillfeed.app.feature.insights.InsightsViewModel
import com.stillfeed.app.feature.onboarding.OnboardingScreen
import com.stillfeed.app.feature.onboarding.OnboardingViewModel
import com.stillfeed.app.feature.settings.SettingsScreen
import com.stillfeed.app.feature.settings.SettingsViewModel
import com.stillfeed.app.protection.accessibility.StillfeedAccessibilityService
import com.stillfeed.app.protection.blocking.InterventionActivity
import com.stillfeed.app.protection.intervention.ui.InterventionStep

/**
 * Main application navigation host integrating the authentication navigation gate,
 * 5-screen onboarding sequence, production Home dashboard, monthly awareness selection,
 * historical Insights experience, and comprehensive Settings experience.
 */
@Composable
fun StillfeedNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    authRepository: AuthRepository? = null,
    appPreferencesRepository: AppPreferencesRepository? = null,
    awarenessViewModel: AwarenessViewModel? = null,
    insightsViewModel: InsightsViewModel? = null,
    settingsViewModel: SettingsViewModel? = null,
    onboardingViewModel: OnboardingViewModel? = null,
    startDestination: String? = null
) {
    val app = LocalContext.current.applicationContext as? StillfeedApplication
    val repository = authRepository ?: app?.authRepository
        ?: throw IllegalStateException("AuthRepository must be provided or StillfeedApplication initialized.")

    val authVm: AuthViewModel = viewModel(
        factory = AuthViewModel.provideFactory(repository)
    )

    val pendingDeepLink by (app?.pendingDeepLink?.collectAsState() ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(null) })
    androidx.compose.runtime.LaunchedEffect(pendingDeepLink) {
        val link = pendingDeepLink
        if (!link.isNullOrBlank()) {
            authVm.handleDeepLink(link)
            app?.pendingDeepLink?.value = null
        }
    }

    val prefRepo = appPreferencesRepository ?: app?.appPreferencesRepository
    val preferences by (prefRepo?.observePreferences()?.collectAsState(initial = null)
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(null) })

    val resolvedAwarenessVm: AwarenessViewModel? = awarenessViewModel ?: app?.let {
        viewModel(
            factory = AwarenessViewModel.provideFactory(
                awarenessManager = it.awarenessManager,
                usageAggregator = it.usageAggregator,
                usageStatsDataSource = it.usageStatsDataSource,
                protectionStateRepository = it.protectionStateRepository,
                appPreferencesRepository = it.appPreferencesRepository
            )
        )
    }

    val resolvedInsightsVm: InsightsViewModel? = insightsViewModel ?: app?.let {
        viewModel(
            factory = InsightsViewModel.provideFactory(
                usageAggregator = it.usageAggregator,
                awarenessManager = it.awarenessManager,
                usageStatsDataSource = it.usageStatsDataSource
            )
        )
    }

    val resolvedSettingsVm: SettingsViewModel? = settingsViewModel ?: app?.let {
        viewModel(
            factory = SettingsViewModel.provideFactory(
                protectionStateRepository = it.protectionStateRepository,
                appPreferencesRepository = it.appPreferencesRepository,
                awarenessManager = it.awarenessManager,
                awarenessRepository = it.awarenessRepository,
                authRepository = it.authRepository,
                usageRepository = it.usageRepository,
                unlockHistoryRepository = it.unlockHistoryRepository,
                userRepository = it.userRepository,
                accessibilityPermissionChecker = it.accessibilityPermissionChecker,
                usageStatsDataSource = it.usageStatsDataSource,
                dataExportService = it.dataExportService
            )
        )
    }

    val resolvedOnboardingVm: OnboardingViewModel? = onboardingViewModel ?: app?.let { appInstance ->
        prefRepo?.let { preferencesRepository ->
            viewModel(
                factory = OnboardingViewModel.provideFactory(
                    appPreferencesRepository = preferencesRepository,
                    accessibilityPermissionChecker = appInstance.accessibilityPermissionChecker,
                    usageStatsDataSource = appInstance.usageStatsDataSource,
                    protectionStateRepository = appInstance.protectionStateRepository
                )
            )
        }
    }

    val authState by authVm.currentAuthState.collectAsState()

    when (authState) {
        is AuthState.Loading -> {
            // Cold launch session restoration splash screen
            AuthLoadingSplash(modifier = modifier)
        }
        is AuthState.SignedIn -> {
            // Tab navigation helper: enforces single-top and anchors backstack to Home
            val navigateToTab: (String) -> Unit = { targetRoute ->
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                if (currentRoute != targetRoute) {
                    navController.navigate(targetRoute) {
                        popUpTo(Screen.Home.route) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }

            val hasCompletedOnboarding = preferences?.hasCompletedOnboarding ?: false
            val effectiveStartDestination = when {
                startDestination != null && startDestination != Screen.Auth.route -> startDestination
                hasCompletedOnboarding -> Screen.Home.route
                else -> Screen.Onboarding.route
            }

            NavHost(
                navController = navController,
                startDestination = effectiveStartDestination,
                modifier = modifier,
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None }
            ) {
                composable(route = Screen.Onboarding.route) {
                    resolvedOnboardingVm?.let { vm ->
                        OnboardingScreen(
                            viewModel = vm,
                            onNavigateToHome = {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Onboarding.route) {
                                        inclusive = true
                                    }
                                    launchSingleTop = true
                                }
                            }
                        )
                    } ?: BootstrapScreen()
                }
                composable(route = Screen.Home.route) {
                    resolvedAwarenessVm?.let { vm ->
                        HomeScreen(
                            viewModel = vm,
                            onNavigateToAwarenessSelection = {
                                navController.navigate(Screen.AwarenessSelection.route)
                            },
                            onNavigateToInsights = {
                                navigateToTab(Screen.Insights.route)
                            },
                            onNavigateToSettings = {
                                navigateToTab(Screen.Settings.route)
                            },
                            onResetActivity = {
                                navController.navigate(Screen.ResetActivity.route)
                            },
                            onNavigateToPermissionDisabled = {
                                navController.navigate(Screen.PermissionDisabled.route)
                            },
                            onNavigateToOffline = {
                                navController.navigate(Screen.Offline.route)
                            },
                            onNavigateToFirstDay = {
                                navController.navigate(Screen.FirstDay.route)
                            }
                        )
                    } ?: BootstrapScreen()
                }

                composable(
                    route = Screen.AwarenessSelection.route,
                    enterTransition = { fadeIn(animationSpec = tween(150)) },
                    exitTransition = { fadeOut(animationSpec = tween(150)) },
                    popEnterTransition = { fadeIn(animationSpec = tween(150)) },
                    popExitTransition = { fadeOut(animationSpec = tween(150)) }
                ) {
                    resolvedAwarenessVm?.let { vm ->
                        AwarenessSelectionScreen(
                            viewModel = vm,
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }

                composable(route = Screen.Insights.route) {
                    resolvedInsightsVm?.let { vm ->
                        InsightsScreen(
                            viewModel = vm,
                            onNavigateToHome = {
                                navigateToTab(Screen.Home.route)
                            },
                            onNavigateToSettings = {
                                navigateToTab(Screen.Settings.route)
                            }
                        )
                    }
                }

                composable(route = Screen.Settings.route) {
                    resolvedSettingsVm?.let { vm ->
                        SettingsScreen(
                            viewModel = vm,
                            onNavigateToHome = {
                                navigateToTab(Screen.Home.route)
                            },
                            onNavigateToInsights = {
                                navigateToTab(Screen.Insights.route)
                            },
                            onNavigateToAwarenessSelection = {
                                navController.navigate(Screen.AwarenessSelection.route)
                            },
                            onSignOutOrDelete = {
                                // Handled declaratively via authState transition to SignedOut
                            }
                        )
                    }
                }

                composable(route = Screen.ResetActivity.route) {
                    val context = LocalContext.current
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        val intent = Intent(context, InterventionActivity::class.java).apply {
                            putExtra(InterventionActivity.EXTRA_SURFACE_CATEGORY, "ResetActivity")
                            putExtra(InterventionActivity.EXTRA_START_STEP, InterventionStep.RESET_SELECTION.name)
                        }
                        context.startActivity(intent)
                        navController.popBackStack()
                    }
                }

                composable(route = Screen.BatchFinished.route) {
                    val expiredSession = app?.unlockSessionRepository?.getLatestExpiredSession()
                    if (expiredSession == null) {
                        // Never open Screen 15 merely because the user visits its route without a verified expired batch
                        androidx.compose.runtime.LaunchedEffect(Unit) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.BatchFinished.route) { inclusive = true }
                            }
                        }
                    } else {
                        val durationMins = ((System.currentTimeMillis() - expiredSession.createdAtMillis) / 60000L).toInt().coerceAtLeast(1)
                        val batchFinishedVm: BatchFinishedViewModel = viewModel(
                            factory = BatchFinishedViewModel.provideFactory(
                                usageAggregator = app?.usageAggregator,
                                initialDailyAllowanceReels = preferences?.dailyReelsAllowance ?: 30,
                                initialBatchReels = expiredSession.selectedBatchAllowance,
                                initialBatchDurationMinutes = durationMins,
                                initialIsExpired = true
                            )
                        )
                        BatchFinishedScreen(
                            viewModel = batchFinishedVm,
                            onChooseReset = {
                                navController.navigate(Screen.ResetActivity.route)
                            },
                            onKeepChatting = {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Home.route) { inclusive = true }
                                }
                            },
                            onNavigateToHome = { navigateToTab(Screen.Home.route) },
                            onNavigateToInsights = { navigateToTab(Screen.Insights.route) },
                            onNavigateToSettings = { navigateToTab(Screen.Settings.route) }
                        )
                    }
                }

                composable(route = Screen.PermissionDisabled.route) {
                    val permissionDisabledVm: PermissionDisabledViewModel = viewModel(
                        factory = PermissionDisabledViewModel.provideFactory(
                            accessibilityPermissionChecker = app?.accessibilityPermissionChecker
                                ?: com.stillfeed.app.protection.permission.AndroidAccessibilityPermissionChecker(LocalContext.current),
                            usageStatsDataSource = app?.usageStatsDataSource
                                ?: com.stillfeed.app.usage.stats.AndroidUsageStatsDataSource(LocalContext.current)
                        )
                    )
                    PermissionDisabledScreen(
                        viewModel = permissionDisabledVm,
                        onNavigateToHome = { navigateToTab(Screen.Home.route) },
                        onNavigateToInsights = { navigateToTab(Screen.Insights.route) },
                        onNavigateToSettings = { navigateToTab(Screen.Settings.route) }
                    )
                }

                composable(route = Screen.Offline.route) {
                    val context = LocalContext.current
                    val offlineVm: OfflineViewModel = viewModel(
                        factory = OfflineViewModel.provideFactory(
                            networkConnectivityObserver = app?.let {
                                AndroidNetworkConnectivityObserver(it)
                            } ?: AndroidNetworkConnectivityObserver(context),
                            accessibilityPermissionChecker = app?.accessibilityPermissionChecker
                                ?: com.stillfeed.app.protection.permission.AndroidAccessibilityPermissionChecker(context),
                            usageStatsDataSource = app?.usageStatsDataSource
                                ?: com.stillfeed.app.usage.stats.AndroidUsageStatsDataSource(context),
                            protectionStateRepository = app?.protectionStateRepository,
                            protectionEngine = app?.protectionEngine,
                            isServiceRunningProvider = { StillfeedAccessibilityService.isRunning },
                            usageAggregator = app?.usageAggregator
                        )
                    )
                    OfflineScreen(
                        viewModel = offlineVm,
                        onNavigateToHome = { navigateToTab(Screen.Home.route) },
                        onNavigateToInsights = { navigateToTab(Screen.Insights.route) },
                        onNavigateToSettings = { navigateToTab(Screen.Settings.route) }
                    )
                }

                composable(route = Screen.FirstDay.route) {
                    val firstDayVm: FirstDayViewModel = viewModel(
                        factory = FirstDayViewModel.provideFactory(
                            usageAggregator = app?.usageAggregator,
                            usageStatsDataSource = app?.usageStatsDataSource,
                            usageRepository = app?.usageRepository
                        )
                    )
                    FirstDayScreen(
                        viewModel = firstDayVm,
                        onOpenStillfeed = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.FirstDay.route) { inclusive = true }
                            }
                        },
                        onNavigateToHome = { navigateToTab(Screen.Home.route) },
                        onNavigateToInsights = { navigateToTab(Screen.Insights.route) },
                        onNavigateToSettings = { navigateToTab(Screen.Settings.route) }
                    )
                }

                composable(route = Screen.Bootstrap.route) {
                    BootstrapScreen()
                }

                composable(route = Screen.Auth.route) {
                    AuthGate(viewModel = authVm) {
                        // In case Screen.Auth is explicitly queried in graph
                    }
                }
            }
        }
        else -> {
            // Unauthenticated state: SignedOut, SessionExpired, Error, NeedsEmailVerification
            AuthGate(viewModel = authVm) {
                // When sign in succeeds, authState becomes SignedIn and switches to the branch above
            }
        }
    }
}
