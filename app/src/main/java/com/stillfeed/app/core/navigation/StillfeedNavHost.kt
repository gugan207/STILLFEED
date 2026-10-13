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
import com.stillfeed.app.StillfeedApplication
import com.stillfeed.app.auth.model.AuthState
import com.stillfeed.app.auth.repository.AuthRepository
import com.stillfeed.app.auth.ui.AuthGate
import com.stillfeed.app.auth.ui.AuthLoadingSplash
import com.stillfeed.app.auth.ui.AuthViewModel
import com.stillfeed.app.feature.awareness.AwarenessSelectionScreen
import com.stillfeed.app.feature.awareness.AwarenessViewModel
import com.stillfeed.app.feature.bootstrap.BootstrapScreen
import com.stillfeed.app.feature.home.HomeScreen
import com.stillfeed.app.feature.insights.InsightsScreen
import com.stillfeed.app.feature.insights.InsightsViewModel
import com.stillfeed.app.feature.settings.SettingsScreen
import com.stillfeed.app.feature.settings.SettingsViewModel

/**
 * Main application navigation host integrating the authentication navigation gate,
 * production Home dashboard, monthly awareness selection, historical Insights experience,
 * and comprehensive Settings experience.
 */
@Composable
fun StillfeedNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    authRepository: AuthRepository? = null,
    awarenessViewModel: AwarenessViewModel? = null,
    insightsViewModel: InsightsViewModel? = null,
    settingsViewModel: SettingsViewModel? = null,
    startDestination: String = Screen.Home.route
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

    val resolvedAwarenessVm: AwarenessViewModel? = awarenessViewModel ?: app?.let {
        viewModel(
            factory = AwarenessViewModel.provideFactory(
                awarenessManager = it.awarenessManager,
                usageAggregator = it.usageAggregator,
                usageStatsDataSource = it.usageStatsDataSource,
                protectionStateRepository = it.protectionStateRepository
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

            val effectiveStartDestination = if (startDestination == Screen.Auth.route) {
                Screen.Home.route
            } else {
                startDestination
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
