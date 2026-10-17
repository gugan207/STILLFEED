package com.stillfeed.app.floating

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.stillfeed.app.MainActivity
import com.stillfeed.app.R
import com.stillfeed.app.StillfeedApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.hypot

/**
 * Foreground Service hosting the STILLFEED Floating Brain Companion Window Overlay.
 */
class FloatingOverlayService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var windowManager: WindowManager? = null
    private var companionView: FloatingCompanionView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private lateinit var positionManager: FloatingPositionManager
    private var overlayWidth: Int = 0
    private var overlayHeight: Int = 0

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> companionView?.pause()
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> companionView?.resume()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val app = application as StillfeedApplication
        positionManager = app.floatingPositionManager
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val density = resources.displayMetrics.density
        overlayWidth = (180 * density).toInt()
        overlayHeight = (110 * density).toInt()

        createNotificationChannel()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopOverlay()
                return START_NOT_STICKY
            }
            ACTION_RESET_POSITION -> {
                resetOverlayPosition()
                return START_STICKY
            }
            ACTION_UPDATE_DATA -> {
                // Triggered immediate state refresh
                refreshData()
                return START_STICKY
            }
            else -> {
                startOverlay()
                return START_STICKY
            }
        }
    }

    private fun startOverlay() {
        val app = application as StillfeedApplication
        if (!app.overlayPermissionManager.hasOverlayPermission()) {
            stopSelf()
            return
        }

        val notification = buildNotification("Companion active · Tap to open STILLFEED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        if (companionView == null) {
            setupFloatingView()
            observeStateAndData()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFloatingView() {
        val wm = windowManager ?: return

        serviceScope.launch {
            val initialPos = positionManager.resolveInitialPosition(overlayWidth, overlayHeight)

            val params = WindowManager.LayoutParams(
                overlayWidth,
                overlayHeight,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = initialPos.x
                y = initialPos.y
            }
            layoutParams = params

            val view = FloatingCompanionView(this@FloatingOverlayService).apply {
                onIconClickListener = {
                    openStillfeedHome()
                }
            }
            companionView = view

            setupTouchListener(view, params, wm)

            try {
                wm.addView(view, params)
            } catch (e: Exception) {
                // Handle WindowManager permission revocation or addView failure
                stopSelf()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchListener(
        view: View,
        params: WindowManager.LayoutParams,
        wm: WindowManager
    ) {
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop

        view.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY

                        if (!isDragging && hypot(dx, dy) > touchSlop) {
                            isDragging = true
                        }

                        if (isDragging) {
                            val candidateX = (initialX + dx).toInt()
                            val candidateY = (initialY + dy).toInt()

                            val clamped = positionManager.clamp(
                                FloatingPositionManager.Position(candidateX, candidateY),
                                overlayWidth,
                                overlayHeight
                            )

                            params.x = clamped.x
                            params.y = clamped.y
                            try {
                                wm.updateViewLayout(view, params)
                            } catch (_: Exception) {}
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isDragging) {
                            serviceScope.launch {
                                positionManager.savePosition(
                                    FloatingPositionManager.Position(params.x, params.y)
                                )
                            }
                        } else {
                            // Tapped: bounce and launch STILLFEED
                            companionView?.triggerTapBounce()
                            openStillfeedHome()
                        }
                        isDragging = false
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun observeStateAndData() {
        val app = application as StillfeedApplication

        // 1. Observe App Preferences for feature toggle
        serviceScope.launch {
            app.appPreferencesRepository.observePreferences().collectLatest { prefs ->
                if (!prefs.floatingCompanionEnabled) {
                    stopOverlay()
                    return@collectLatest
                }
                refreshData()
            }
        }

        // 2. Periodic usage refresh loop
        serviceScope.launch {
            while (isActive) {
                refreshData()
                delay(30_000L) // Refresh every 30 seconds while active
            }
        }
    }

    private fun refreshData() {
        val app = application as StillfeedApplication
        serviceScope.launch {
            val prefs = runCatching { app.appPreferencesRepository.getPreferences() }.getOrNull()
            val todaySummary = runCatching { app.usageAggregator.getTodayUsage() }.getOrNull()
            val protectionState = runCatching { app.protectionStateRepository.getProtectionState() }.getOrNull()
            val isAccessibility = app.accessibilityPermissionChecker.isAccessibilityPermissionGranted()

            val monitoredMinutes = ((todaySummary?.totalMonitoredMillis ?: 0L) / 60_000L).coerceAtLeast(0L)
            val allowance = prefs?.dailyReelsAllowance ?: 30
            val isProtection = protectionState?.isEnabled ?: true
            val isAnimations = prefs?.floatingCompanionAnimationsEnabled ?: true

            // DATA HONESTY: Reel count is null when unmeasured, causing em-dash "—" display
            val (displayVal, progressRatio) = BrainMoodResolver.resolveDisplayAndProgress(
                reelCount = null,
                monitoredMinutes = monitoredMinutes,
                dailyAllowance = allowance
            )

            val resolvedMood = BrainMoodResolver.resolveMood(progressRatio)

            val uiState = FloatingIconUiState(
                displayValue = displayVal,
                progressRatio = progressRatio,
                mood = resolvedMood,
                monitoredMinutes = monitoredMinutes,
                dailyAllowance = allowance,
                isProtectionActive = isProtection,
                isAccessibilityGranted = isAccessibility,
                isAnimationsEnabled = isAnimations,
                isOffline = false
            )

            companionView?.bindState(uiState)
        }
    }

    private fun resetOverlayPosition() {
        val wm = windowManager ?: return
        val view = companionView ?: return
        val params = layoutParams ?: return

        serviceScope.launch {
            val defaultPos = positionManager.getDefaultPosition(overlayWidth, overlayHeight)
            params.x = defaultPos.x
            params.y = defaultPos.y
            try {
                wm.updateViewLayout(view, params)
                positionManager.savePosition(defaultPos)
            } catch (_: Exception) {}
        }
    }

    private fun openStillfeedHome() {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("source", "floating_companion")
            putExtra("destination", "home")
        }
        startActivity(launchIntent)
    }

    private fun stopOverlay() {
        try {
            companionView?.let { view ->
                windowManager?.removeView(view)
                view.destroyView()
            }
        } catch (_: Exception) {}

        companionView = null
        layoutParams = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Exception) {}

        stopOverlay()
        serviceJob.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "STILLFEED Companion",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the mindful floating brain companion active above apps"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val tapIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("STILLFEED Companion")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "stillfeed_floating_companion"
        const val NOTIFICATION_ID = 2026

        const val ACTION_START = "com.stillfeed.app.floating.ACTION_START"
        const val ACTION_STOP = "com.stillfeed.app.floating.ACTION_STOP"
        const val ACTION_RESET_POSITION = "com.stillfeed.app.floating.ACTION_RESET_POSITION"
        const val ACTION_UPDATE_DATA = "com.stillfeed.app.floating.ACTION_UPDATE_DATA"
    }
}
