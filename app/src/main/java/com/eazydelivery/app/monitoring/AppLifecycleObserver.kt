package com.eazydelivery.app.monitoring

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import com.eazydelivery.app.analytics.AnalyticsEvents
import com.eazydelivery.app.analytics.AnalyticsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Observer for app lifecycle events
 */
@Singleton
class AppLifecycleObserver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyticsManager: AnalyticsManager,
    private val performanceMonitoringManager: PerformanceMonitoringManager
) : Application.ActivityLifecycleCallbacks {
    
    private var startTime: Long = 0
    private var foregroundTime: Long = 0
    private var backgroundTime: Long = 0
    private var isInForeground = false
    private var activityCount = 0
    private var currentActivity: Activity? = null
    
    /**
     * Initialize the observer
     */
    fun initialize() {
        try {
            // Register activity lifecycle callbacks
            (context.applicationContext as Application).registerActivityLifecycleCallbacks(this)
            
            // Initialize start time
            startTime = SystemClock.elapsedRealtime()
            
            Timber.d("App lifecycle observer initialized")
        } catch (e: Exception) {
            Timber.e(e, "Error initializing app lifecycle observer")
        }
    }
    
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        Timber.d("Activity created: ${activity.javaClass.simpleName}")
    }
    
    override fun onActivityStarted(activity: Activity) {
        // First activity started
        if (activityCount == 0) {
            // App is coming to foreground
            handleForeground()
        }
        
        activityCount++
        currentActivity = activity
        
        Timber.d("Activity started: ${activity.javaClass.simpleName}, count: $activityCount")
    }
    
    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
        
        // Start screen load trace
        performanceMonitoringManager.startTrace(
            traceName = "${PerformanceMonitoringManager.TRACE_SCREEN_LOAD}_${activity.javaClass.simpleName}",
            attributes = mapOf(
                "screen_name" to activity.javaClass.simpleName
            )
        )
        
        Timber.d("Activity resumed: ${activity.javaClass.simpleName}")
    }
    
    override fun onActivityPaused(activity: Activity) {
        // Stop screen load trace
        performanceMonitoringManager.stopTrace(
            traceName = "${PerformanceMonitoringManager.TRACE_SCREEN_LOAD}_${activity.javaClass.simpleName}"
        )
        
        Timber.d("Activity paused: ${activity.javaClass.simpleName}")
    }
    
    override fun onActivityStopped(activity: Activity) {
        activityCount--
        
        // Last activity stopped
        if (activityCount == 0) {
            // App is going to background
            handleBackground()
        }
        
        if (currentActivity == activity) {
            currentActivity = null
        }
        
        Timber.d("Activity stopped: ${activity.javaClass.simpleName}, count: $activityCount")
    }
    
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        Timber.d("Activity save instance state: ${activity.javaClass.simpleName}")
    }
    
    override fun onActivityDestroyed(activity: Activity) {
        Timber.d("Activity destroyed: ${activity.javaClass.simpleName}")
    }
    
    /**
     * Handle app coming to foreground
     */
    private fun handleForeground() {
        if (!isInForeground) {
            isInForeground = true
            
            // Record foreground time
            val now = SystemClock.elapsedRealtime()
            if (backgroundTime > 0) {
                val timeInBackground = now - backgroundTime
                
                // Track app foreground event
                analyticsManager.trackUserAction(
                    action = AnalyticsEvents.APP_FOREGROUND,
                    params = mapOf(
                        "time_in_background_ms" to timeInBackground
                    )
                )
            }
            
            foregroundTime = now
            
            Timber.d("App came to foreground")
        }
    }
    
    /**
     * Handle app going to background
     */
    private fun handleBackground() {
        if (isInForeground) {
            isInForeground = false
            
            // Record background time
            val now = SystemClock.elapsedRealtime()
            val timeInForeground = now - foregroundTime
            
            // Track app background event
            analyticsManager.trackUserAction(
                action = AnalyticsEvents.APP_BACKGROUND,
                params = mapOf(
                    "time_in_foreground_ms" to timeInForeground
                )
            )
            
            backgroundTime = now
            
            Timber.d("App went to background")
        }
    }
    
    /**
     * Get the current activity
     * 
     * @return The current activity, or null if no activity is in foreground
     */
    fun getCurrentActivity(): Activity? {
        return currentActivity
    }
    
    /**
     * Check if the app is in foreground
     * 
     * @return true if the app is in foreground, false otherwise
     */
    fun isInForeground(): Boolean {
        return isInForeground
    }
    
    /**
     * Get the time the app has been running
     * 
     * @return The time in milliseconds
     */
    fun getAppRunningTime(): Long {
        return SystemClock.elapsedRealtime() - startTime
    }
    
    /**
     * Get the time the app has been in foreground
     * 
     * @return The time in milliseconds, or 0 if the app is not in foreground
     */
    fun getTimeInForeground(): Long {
        return if (isInForeground) {
            SystemClock.elapsedRealtime() - foregroundTime
        } else {
            0
        }
    }
    
    /**
     * Get the time the app has been in background
     * 
     * @return The time in milliseconds, or 0 if the app is not in background
     */
    fun getTimeInBackground(): Long {
        return if (!isInForeground && backgroundTime > 0) {
            SystemClock.elapsedRealtime() - backgroundTime
        } else {
            0
        }
    }
}
