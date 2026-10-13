package com.stillfeed.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    startDestination: String = Screen.Auth.route
) {
    val app = LocalContext.current.applicationContext as? StillfeedApplication
    val repository = authRepository ?: app?.authRepository
        ?: throw IllegalStateException("AuthRepository must be provided or StillfeedApplication initialized.")

    val authVm: AuthViewModel = viewModel(
        factory = AuthViewModel.provideFactory(repository)
    )

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

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(route = Screen.Auth.route) {
            val authState by authVm.currentAuthState.collectAsState()
            LaunchedEffect(authState) {
                if (authState is AuthState.SignedIn) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            AuthGate(viewModel = authVm) { _ ->
                com.stillfeed.app.auth.ui.AuthLoadingSplash()
            }
        }

        composable(route = Screen.Home.route) {
            val authState by authVm.currentAuthState.collectAsState()
            LaunchedEffect(authState) {
                if (authState !is AuthState.SignedIn && authState !is AuthState.Loading) {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }

            resolvedAwarenessVm?.let { vm ->
                HomeScreen(
                    viewModel = vm,
                    onNavigateToAwarenessSelection = {
                        navController.navigate(Screen.AwarenessSelection.route)
                    },
                    onNavigateToInsights = {
                        navController.navigate(Screen.Insights.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route) {
                            launchSingleTop = true
                        }
                    }
                )
            } ?: BootstrapScreen()
        }

        composable(route = Screen.AwarenessSelection.route) {
            val authState by authVm.currentAuthState.collectAsState()
            LaunchedEffect(authState) {
                if (authState !is AuthState.SignedIn && authState !is AuthState.Loading) {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }

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
            val authState by authVm.currentAuthState.collectAsState()
            LaunchedEffect(authState) {
                if (authState !is AuthState.SignedIn && authState !is AuthState.Loading) {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }

            resolvedInsightsVm?.let { vm ->
                InsightsScreen(
                    viewModel = vm,
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        }

        composable(route = Screen.Settings.route) {
            val authState by authVm.currentAuthState.collectAsState()
            LaunchedEffect(authState) {
                if (authState !is AuthState.SignedIn && authState !is AuthState.Loading) {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }

            resolvedSettingsVm?.let { vm ->
                SettingsScreen(
                    viewModel = vm,
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToInsights = {
                        navController.navigate(Screen.Insights.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToAwarenessSelection = {
                        navController.navigate(Screen.AwarenessSelection.route)
                    },
                    onSignOutOrDelete = {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(route = Screen.Bootstrap.route) {
            BootstrapScreen()
        }
    }
}
