package com.stillfeed.app

import android.app.Application
import com.stillfeed.app.auth.repository.AuthRepository
import com.stillfeed.app.data.auth.LocalAuthRepository
import com.stillfeed.app.data.auth.Pbkdf2PasswordHasher
import com.stillfeed.app.data.auth.SecurePreferencesAuthSessionStorage
import com.stillfeed.app.data.local.StillfeedDatabase
import com.stillfeed.app.data.local.device.AndroidIdDeviceBindingProvider
import com.stillfeed.app.data.preferences.DataStoreAppPreferencesRepository
import com.stillfeed.app.data.preferences.appPreferencesDataStore
import com.stillfeed.app.data.repository.LocalAwarenessRepository
import com.stillfeed.app.data.repository.LocalDeviceBindingRepository
import com.stillfeed.app.data.repository.LocalProtectionStateRepository
import com.stillfeed.app.data.repository.LocalUnlockHistoryRepository
import com.stillfeed.app.data.repository.LocalUsageRepository
import com.stillfeed.app.data.repository.LocalUserRepository
import com.stillfeed.app.domain.repository.AppPreferencesRepository
import com.stillfeed.app.domain.repository.AwarenessRepository
import com.stillfeed.app.domain.repository.DeviceBindingRepository
import com.stillfeed.app.domain.repository.ProtectionStateRepository
import com.stillfeed.app.domain.repository.UnlockHistoryRepository
import com.stillfeed.app.domain.repository.UsageRepository
import com.stillfeed.app.domain.repository.UserRepository

import com.stillfeed.app.data.auth.SecureSessionRepository
import com.stillfeed.app.domain.repository.SessionRepository

class StillfeedApplication : Application() {

    lateinit var database: StillfeedDatabase
        private set

    lateinit var userRepository: UserRepository
        private set

    lateinit var protectionStateRepository: ProtectionStateRepository
        private set

    lateinit var usageRepository: UsageRepository
        private set

    lateinit var awarenessRepository: AwarenessRepository
        private set

    lateinit var unlockHistoryRepository: UnlockHistoryRepository
        private set

    lateinit var deviceBindingRepository: DeviceBindingRepository
        private set

    lateinit var appPreferencesRepository: AppPreferencesRepository
        private set

    lateinit var sessionRepository: SessionRepository
        private set

    lateinit var authRepository: AuthRepository
        private set

    val unlockSessionRepository: com.stillfeed.app.domain.repository.UnlockSessionRepository by lazy {
        com.stillfeed.app.data.repository.DefaultUnlockSessionRepository(
            activeUnlockSessionDao = database.activeUnlockSessionDao(),
            unlockHistoryRepository = unlockHistoryRepository
        )
    }

    val protectionEngine: com.stillfeed.app.protection.engine.ProtectionEngine by lazy {
        com.stillfeed.app.protection.engine.DefaultProtectionEngine(
            protectionStateRepository = protectionStateRepository,
            usageRepository = usageRepository,
            classifier = com.stillfeed.app.protection.detection.CompositeSurfaceClassifier(),
            unlockSessionRepository = unlockSessionRepository
        )
    }

    val usageStatsDataSource: com.stillfeed.app.usage.stats.UsageStatsDataSource by lazy {
        com.stillfeed.app.usage.stats.AndroidUsageStatsDataSource(this)
    }

    val usageSyncManager: com.stillfeed.app.usage.sync.UsageSyncManager by lazy {
        com.stillfeed.app.usage.sync.UsageSyncManager(
            usageStatsDataSource = usageStatsDataSource,
            sessionAnalyzer = com.stillfeed.app.usage.analysis.UsageSessionAnalyzer(),
            reconciler = com.stillfeed.app.usage.reconciliation.UsageReconciler(usageRepository),
            preferencesRepository = appPreferencesRepository
        )
    }

    val usageAggregator: com.stillfeed.app.usage.aggregation.UsageAggregator by lazy {
        com.stillfeed.app.usage.aggregation.UsageAggregator(usageRepository)
    }

    val awarenessManager: com.stillfeed.app.awareness.AwarenessManager by lazy {
        com.stillfeed.app.awareness.AwarenessManager(
            awarenessRepository = awarenessRepository,
            usageAggregator = usageAggregator,
            usageStatsDataSource = usageStatsDataSource
        )
    }

    val accessibilityPermissionChecker: com.stillfeed.app.protection.permission.AccessibilityPermissionChecker by lazy {
        com.stillfeed.app.protection.permission.AndroidAccessibilityPermissionChecker(this)
    }

    val dataExportService: com.stillfeed.app.feature.settings.export.DataExportService by lazy {
        com.stillfeed.app.feature.settings.export.DataExportService(
            authRepository = authRepository,
            usageRepository = usageRepository,
            awarenessRepository = awarenessRepository,
            preferencesRepository = appPreferencesRepository
        )
    }

    val supabaseClient: com.stillfeed.app.data.remote.supabase.SupabaseHttpClient by lazy {
        com.stillfeed.app.data.remote.supabase.SupabaseHttpClient(
            baseUrl = com.stillfeed.app.data.remote.supabase.SupabaseConfig.PROJECT_URL,
            anonKey = com.stillfeed.app.data.remote.supabase.SupabaseConfig.ANON_KEY
        )
    }

    val remoteAuthDataSource: com.stillfeed.app.data.remote.RemoteAuthDataSource by lazy {
        com.stillfeed.app.data.remote.supabase.SupabaseRemoteAuthDataSource(supabaseClient)
    }

    val restoreAuthorizationService: com.stillfeed.app.data.remote.RestoreAuthorizationService by lazy {
        com.stillfeed.app.data.remote.supabase.SupabaseRestoreAuthorizationService(supabaseClient)
    }

    val remoteHistoryRepository: com.stillfeed.app.domain.repository.RemoteHistoryRepository by lazy {
        com.stillfeed.app.data.remote.supabase.SupabaseRemoteHistoryRepository(supabaseClient)
    }

    val widgetDataRepository: com.stillfeed.app.widget.data.WidgetDataRepository by lazy {
        com.stillfeed.app.widget.data.WidgetDataRepository(
            usageAggregator = usageAggregator,
            protectionStateRepository = protectionStateRepository,
            appPreferencesRepository = appPreferencesRepository,
            accessibilityPermissionChecker = accessibilityPermissionChecker
        )
    }

    val widgetUpdateCoordinator: com.stillfeed.app.widget.update.WidgetUpdateCoordinator by lazy {
        com.stillfeed.app.widget.update.WidgetUpdateCoordinator(this)
    }

    val overlayPermissionManager: com.stillfeed.app.floating.OverlayPermissionManager by lazy {
        com.stillfeed.app.floating.OverlayPermissionManager(this)
    }

    val floatingPositionManager: com.stillfeed.app.floating.FloatingPositionManager by lazy {
        com.stillfeed.app.floating.FloatingPositionManager(this, appPreferencesRepository)
    }

    val floatingIconController: com.stillfeed.app.floating.FloatingIconController by lazy {
        com.stillfeed.app.floating.FloatingIconController(this, overlayPermissionManager)
    }

    val pendingDeepLink = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Room Local Database
        database = StillfeedDatabase.getInstance(this)

        // 2. Initialize Repositories
        userRepository = LocalUserRepository(database.userDao())
        protectionStateRepository = LocalProtectionStateRepository(database.protectionStateDao())
        usageRepository = LocalUsageRepository(database.usageEventDao())
        awarenessRepository = LocalAwarenessRepository(database.awarenessMonthlyDao())
        unlockHistoryRepository = LocalUnlockHistoryRepository(database.unlockHistoryDao())

        val deviceBindingProvider = com.stillfeed.app.data.local.device.AndroidIdDeviceBindingProvider(this)
        deviceBindingRepository = LocalDeviceBindingRepository(
            deviceBindingDao = database.deviceBindingDao(),
            deviceBindingProvider = deviceBindingProvider
        )

        appPreferencesRepository = DataStoreAppPreferencesRepository(appPreferencesDataStore)
        sessionRepository = SecureSessionRepository(this)

        // 3. Initialize Production Authentication Repository backed by Supabase
        val remoteUserRepo = com.stillfeed.app.data.remote.supabase.SupabaseRemoteUserRepository(supabaseClient)
        val appScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
        authRepository = com.stillfeed.app.data.auth.ProductionAuthRepository(
            remoteAuthDataSource = remoteAuthDataSource,
            sessionRepository = sessionRepository,
            remoteUserRepository = remoteUserRepo,
            deviceBindingRepository = deviceBindingRepository,
            userRepository = userRepository,
            coroutineScope = appScope
        )

        // 4. Background Usage Analysis WorkManager Scheduling
        try {
            com.stillfeed.app.usage.worker.UsageAnalysisWorker.schedulePeriodicSync(this)
        } catch (_: Exception) {
            // Fail-safe in environments where WorkManager is uninitialized
        }
    }
}
