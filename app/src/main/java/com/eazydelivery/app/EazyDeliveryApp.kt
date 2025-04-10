package com.eazydelivery.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.StrictMode
import android.os.SystemClock
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.eazydelivery.app.accessibility.AccessibilityManager
import com.eazydelivery.app.accessibility.ThemeManager
import com.eazydelivery.app.analytics.AnalyticsManager
import com.eazydelivery.app.monitoring.AppLifecycleObserver
import com.eazydelivery.app.monitoring.CrashReportingManager
import com.eazydelivery.app.feedback.UserFeedbackManager
import com.eazydelivery.app.monitoring.NetworkMonitoringManager
import com.eazydelivery.app.network.NetworkOptimizer
import com.eazydelivery.app.monitoring.PerformanceMonitoringManager
import com.eazydelivery.app.security.SecurityManager
import com.eazydelivery.app.security.SecurePreferencesManager
import com.eazydelivery.app.service.ServiceMonitor
import com.eazydelivery.app.util.AppStateManager
import com.eazydelivery.app.util.BackgroundTaskManager
import com.eazydelivery.app.util.CrashRecoveryManager
import com.eazydelivery.app.util.MemoryManager
import com.eazydelivery.app.util.Constants
import com.eazydelivery.app.util.LocalizationHelper
import com.eazydelivery.app.util.SecureConstants
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Application class for EazyDelivery
 *
 * This is the main application class that initializes all core components
 * and services required by the application.
 */
@HiltAndroidApp
class EazyDeliveryApp : Application(), Configuration.Provider {

    // Performance and monitoring components
    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var performanceMonitoringManager: PerformanceMonitoringManager

    @Inject
    lateinit var crashReportingManager: CrashReportingManager

    @Inject
    lateinit var networkMonitoringManager: NetworkMonitoringManager

    @Inject
    lateinit var appLifecycleObserver: AppLifecycleObserver

    // Security components
    @Inject
    lateinit var securityManager: SecurityManager

    @Inject
    lateinit var securePreferencesManager: SecurePreferencesManager

    @Inject
    lateinit var secureConstants: SecureConstants

    // Accessibility components
    @Inject
    lateinit var accessibilityManager: AccessibilityManager

    @Inject
    lateinit var themeManager: ThemeManager

    // Localization
    @Inject
    lateinit var localizationHelper: LocalizationHelper

    // Service monitoring
    @Inject
    lateinit var serviceMonitor: ServiceMonitor

    // Background task management
    @Inject
    lateinit var backgroundTaskManager: BackgroundTaskManager

    // Memory management
    @Inject
    lateinit var memoryManager: MemoryManager

    // Network optimization
    @Inject
    lateinit var networkOptimizer: NetworkOptimizer

    // Crash recovery
    @Inject
    lateinit var crashRecoveryManager: CrashRecoveryManager

    // App state management
    @Inject
    lateinit var appStateManager: AppStateManager

    // User feedback management
    @Inject
    lateinit var userFeedbackManager: UserFeedbackManager

    // WorkManager
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private val appStartTime = SystemClock.elapsedRealtime()

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            // Enable StrictMode in debug builds
            enableStrictMode()
        } else {
            Timber.plant(CrashReportingTree())
        }

        // Start app startup trace
        performanceMonitoringManager.startTrace(
            traceName = PerformanceMonitoringManager.TRACE_APP_STARTUP
        )

        // Initialize secure constants
        secureConstants.initialize()

        // Apply language settings
        val languageCode = localizationHelper.getCurrentLanguage()
        localizationHelper.setAppLanguage(languageCode)
        Timber.d("Applied language settings: $languageCode")

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize crash reporting
        crashReportingManager.initialize(enableReporting = !BuildConfig.DEBUG)
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

        // Initialize analytics
        analyticsManager.initialize(userId = null)

        // Initialize network monitoring
        networkMonitoringManager.initialize()

        // Initialize app lifecycle observer
        appLifecycleObserver.initialize()

        // Initialize security components
        securityManager.initialize()
        securePreferencesManager.initialize()

        // Initialize accessibility components
        themeManager.initialize()

        // Create notification channels for Android O and above
        createNotificationChannels()

        // Initialize service monitor
        serviceMonitor.initialize()

        // Initialize background task manager
        backgroundTaskManager.initialize()

        // Initialize memory manager
        memoryManager.initialize()

        // Initialize network optimizer
        networkOptimizer.initialize()

        // Initialize crash recovery manager
        crashRecoveryManager.initialize()

        // Initialize app state manager
        appStateManager.initialize()

        // Initialize user feedback manager
        userFeedbackManager.initialize()

        // Set up uncaught exception handler
        setupUncaughtExceptionHandler()

        // Stop app startup trace
        val startupTime = SystemClock.elapsedRealtime() - appStartTime
        performanceMonitoringManager.stopTrace(
            traceName = PerformanceMonitoringManager.TRACE_APP_STARTUP
        )

        Timber.d("App startup completed in $startupTime ms")
    }

    /**
     * Enable StrictMode for detecting potential issues
     */
    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .detectCustomSlowCalls()
                .penaltyLog()
                .build()
        )

        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectActivityLeaks()
                .detectFileUriExposure()
                .penaltyLog()
                .build()
        )
    }

    /**
     * Create notification channels for Android O and above
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                Constants.CHANNEL_ID_FOREGROUND,
                "EazyDelivery Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Used for keeping the auto-accept service running"
                enableLights(false)
                enableVibration(false)
            }

            val notificationsChannel = NotificationChannel(
                Constants.CHANNEL_ID_NOTIFICATIONS,
                "EazyDelivery Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "App notifications for important updates"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannels(listOf(serviceChannel, notificationsChannel))
        }
    }

    /**
     * Set up uncaught exception handler to log crashes to Crashlytics
     * Note: This is a fallback handler. The CrashRecoveryManager sets up its own handler
     * that takes precedence, but we keep this as a safety measure.
     */
    private fun setupUncaughtExceptionHandler() {
        // Check if we already have a handler from CrashRecoveryManager
        val currentHandler = Thread.getDefaultUncaughtExceptionHandler()
        if (currentHandler?.javaClass?.name?.contains("CrashRecoveryManager") == true) {
            Timber.d("CrashRecoveryManager handler already installed, skipping fallback handler")
            return
        }

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Log to Crashlytics
            FirebaseCrashlytics.getInstance().recordException(throwable)

            // Log to Timber
            Timber.e(throwable, "Uncaught exception in thread ${thread.name}")

            // Call the default handler
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    /**
     * Provide WorkManager configuration
     */
    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }

    /**
     * Clean up resources when the application is terminated
     */
    override fun onTerminate() {
        super.onTerminate()

        // Clean up network optimizer
        try {
            networkOptimizer.cleanup()
        } catch (e: Exception) {
            Timber.e(e, "Error cleaning up network optimizer")
        }

        Timber.d("Application terminated")
    }

    /**
     * Custom Timber tree that logs to Crashlytics
     */
    private class CrashReportingTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority >= android.util.Log.WARN) {
                FirebaseCrashlytics.getInstance().log(message)

                if (t != null) {
                    FirebaseCrashlytics.getInstance().recordException(t)
                }
            }
        }
    }

    companion object {
        // Channel IDs moved to Constants class
    }
}
