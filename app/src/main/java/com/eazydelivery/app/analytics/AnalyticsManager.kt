package com.eazydelivery.app.analytics

import android.content.Context
import android.os.Bundle
import com.eazydelivery.app.BuildConfig
import com.eazydelivery.app.util.error.AppError
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class for tracking analytics events and user properties
 */
@Singleton
class AnalyticsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        FirebaseAnalytics.getInstance(context)
    }
    
    private val crashlytics: FirebaseCrashlytics by lazy {
        FirebaseCrashlytics.getInstance()
    }
    
    /**
     * Initialize analytics with user information
     * 
     * @param userId The user ID
     * @param userProperties Map of user properties
     */
    fun initialize(userId: String?, userProperties: Map<String, String>? = null) {
        try {
            // Set user ID for analytics
            userId?.let {
                firebaseAnalytics.setUserId(it)
                crashlytics.setUserId(it)
            }
            
            // Set user properties
            userProperties?.forEach { (key, value) ->
                firebaseAnalytics.setUserProperty(key, value)
                crashlytics.setCustomKey(key, value)
            }
            
            // Set app version
            crashlytics.setCustomKey("app_version", BuildConfig.VERSION_NAME)
            crashlytics.setCustomKey("app_version_code", BuildConfig.VERSION_CODE.toString())
            
            // Set build type
            crashlytics.setCustomKey("build_type", BuildConfig.BUILD_TYPE)
            
            Timber.d("Analytics initialized with userId: $userId")
        } catch (e: Exception) {
            Timber.e(e, "Error initializing analytics")
        }
    }
    
    /**
     * Track a screen view
     * 
     * @param screenName The name of the screen
     * @param screenClass The class name of the screen
     */
    fun trackScreenView(screenName: String, screenClass: String? = null) {
        try {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                screenClass?.let { putString(FirebaseAnalytics.Param.SCREEN_CLASS, it) }
            }
            
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
            Timber.d("Screen view tracked: $screenName")
        } catch (e: Exception) {
            Timber.e(e, "Error tracking screen view: $screenName")
        }
    }
    
    /**
     * Track a user action
     * 
     * @param action The action name
     * @param params Additional parameters
     */
    fun trackUserAction(action: String, params: Map<String, Any>? = null) {
        try {
            val bundle = Bundle().apply {
                params?.forEach { (key, value) ->
                    when (value) {
                        is String -> putString(key, value)
                        is Int -> putInt(key, value)
                        is Long -> putLong(key, value)
                        is Double -> putDouble(key, value)
                        is Boolean -> putBoolean(key, value)
                        else -> putString(key, value.toString())
                    }
                }
            }
            
            firebaseAnalytics.logEvent(action, bundle)
            Timber.d("User action tracked: $action with params: $params")
        } catch (e: Exception) {
            Timber.e(e, "Error tracking user action: $action")
        }
    }
    
    /**
     * Track an order event
     * 
     * @param eventName The event name
     * @param orderId The order ID
     * @param platformName The platform name
     * @param amount The order amount
     * @param additionalParams Additional parameters
     */
    fun trackOrderEvent(
        eventName: String,
        orderId: String,
        platformName: String,
        amount: Double,
        additionalParams: Map<String, Any>? = null
    ) {
        try {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.TRANSACTION_ID, orderId)
                putString(FirebaseAnalytics.Param.ITEM_ID, orderId)
                putString(FirebaseAnalytics.Param.ITEM_NAME, "Order")
                putString(FirebaseAnalytics.Param.ITEM_CATEGORY, platformName)
                putDouble(FirebaseAnalytics.Param.VALUE, amount)
                putString(FirebaseAnalytics.Param.CURRENCY, "INR")
                
                // Add additional parameters
                additionalParams?.forEach { (key, value) ->
                    when (value) {
                        is String -> putString(key, value)
                        is Int -> putInt(key, value)
                        is Long -> putLong(key, value)
                        is Double -> putDouble(key, value)
                        is Boolean -> putBoolean(key, value)
                        else -> putString(key, value.toString())
                    }
                }
            }
            
            firebaseAnalytics.logEvent(eventName, bundle)
            Timber.d("Order event tracked: $eventName for order: $orderId on platform: $platformName")
        } catch (e: Exception) {
            Timber.e(e, "Error tracking order event: $eventName for order: $orderId")
        }
    }
    
    /**
     * Track a notification event
     * 
     * @param eventName The event name
     * @param notificationId The notification ID
     * @param platformName The platform name
     * @param additionalParams Additional parameters
     */
    fun trackNotificationEvent(
        eventName: String,
        notificationId: String,
        platformName: String,
        additionalParams: Map<String, Any>? = null
    ) {
        try {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.ITEM_ID, notificationId)
                putString(FirebaseAnalytics.Param.ITEM_NAME, "Notification")
                putString(FirebaseAnalytics.Param.ITEM_CATEGORY, platformName)
                
                // Add additional parameters
                additionalParams?.forEach { (key, value) ->
                    when (value) {
                        is String -> putString(key, value)
                        is Int -> putInt(key, value)
                        is Long -> putLong(key, value)
                        is Double -> putDouble(key, value)
                        is Boolean -> putBoolean(key, value)
                        else -> putString(key, value.toString())
                    }
                }
            }
            
            firebaseAnalytics.logEvent(eventName, bundle)
            Timber.d("Notification event tracked: $eventName for notification: $notificationId on platform: $platformName")
        } catch (e: Exception) {
            Timber.e(e, "Error tracking notification event: $eventName for notification: $notificationId")
        }
    }
    
    /**
     * Track an error event
     * 
     * @param error The error
     * @param additionalParams Additional parameters
     */
    fun trackError(error: AppError, additionalParams: Map<String, Any>? = null) {
        try {
            // Log to Crashlytics
            crashlytics.setCustomKey("error_type", error.javaClass.simpleName)
            crashlytics.setCustomKey("error_message", error.message)
            
            // Add additional parameters
            additionalParams?.forEach { (key, value) ->
                when (value) {
                    is String -> crashlytics.setCustomKey(key, value)
                    is Int -> crashlytics.setCustomKey(key, value)
                    is Long -> crashlytics.setCustomKey(key, value)
                    is Double -> crashlytics.setCustomKey(key, value)
                    is Boolean -> crashlytics.setCustomKey(key, value)
                    else -> crashlytics.setCustomKey(key, value.toString())
                }
            }
            
            // Log non-fatal exception to Crashlytics
            error.cause?.let { crashlytics.recordException(it) }
                ?: crashlytics.recordException(Exception(error.message))
            
            // Track error event in Firebase Analytics
            val bundle = Bundle().apply {
                putString("error_type", error.javaClass.simpleName)
                putString("error_message", error.message)
                
                // Add additional parameters
                additionalParams?.forEach { (key, value) ->
                    when (value) {
                        is String -> putString(key, value)
                        is Int -> putInt(key, value)
                        is Long -> putLong(key, value)
                        is Double -> putDouble(key, value)
                        is Boolean -> putBoolean(key, value)
                        else -> putString(key, value.toString())
                    }
                }
            }
            
            firebaseAnalytics.logEvent("app_error", bundle)
            Timber.d("Error tracked: ${error.javaClass.simpleName} - ${error.message}")
        } catch (e: Exception) {
            Timber.e(e, "Error tracking error: ${error.javaClass.simpleName} - ${error.message}")
        }
    }
    
    /**
     * Track a performance event
     * 
     * @param eventName The event name
     * @param durationMs The duration in milliseconds
     * @param additionalParams Additional parameters
     */
    fun trackPerformanceEvent(
        eventName: String,
        durationMs: Long,
        additionalParams: Map<String, Any>? = null
    ) {
        try {
            val bundle = Bundle().apply {
                putLong("duration_ms", durationMs)
                
                // Add additional parameters
                additionalParams?.forEach { (key, value) ->
                    when (value) {
                        is String -> putString(key, value)
                        is Int -> putInt(key, value)
                        is Long -> putLong(key, value)
                        is Double -> putDouble(key, value)
                        is Boolean -> putBoolean(key, value)
                        else -> putString(key, value.toString())
                    }
                }
            }
            
            firebaseAnalytics.logEvent("performance_$eventName", bundle)
            Timber.d("Performance event tracked: $eventName with duration: $durationMs ms")
        } catch (e: Exception) {
            Timber.e(e, "Error tracking performance event: $eventName")
        }
    }
    
    /**
     * Set a user property
     * 
     * @param key The property key
     * @param value The property value
     */
    fun setUserProperty(key: String, value: String) {
        try {
            firebaseAnalytics.setUserProperty(key, value)
            crashlytics.setCustomKey(key, value)
            Timber.d("User property set: $key = $value")
        } catch (e: Exception) {
            Timber.e(e, "Error setting user property: $key = $value")
        }
    }
    
    /**
     * Clear all user data
     */
    fun clearUserData() {
        try {
            // Clear user ID
            firebaseAnalytics.setUserId(null)
            
            // Clear user properties
            USER_PROPERTIES.forEach { property ->
                firebaseAnalytics.setUserProperty(property, null)
            }
            
            Timber.d("User data cleared")
        } catch (e: Exception) {
            Timber.e(e, "Error clearing user data")
        }
    }
    
    companion object {
        // Event names
        const val EVENT_ORDER_RECEIVED = "order_received"
        const val EVENT_ORDER_ACCEPTED = "order_accepted"
        const val EVENT_ORDER_REJECTED = "order_rejected"
        const val EVENT_ORDER_COMPLETED = "order_completed"
        const val EVENT_ORDER_CANCELLED = "order_cancelled"
        
        const val EVENT_NOTIFICATION_RECEIVED = "notification_received"
        const val EVENT_NOTIFICATION_CLICKED = "notification_clicked"
        const val EVENT_NOTIFICATION_DISMISSED = "notification_dismissed"
        
        const val EVENT_AUTO_ACCEPT_ENABLED = "auto_accept_enabled"
        const val EVENT_AUTO_ACCEPT_DISABLED = "auto_accept_disabled"
        
        const val EVENT_APP_FOREGROUND = "app_foreground"
        const val EVENT_APP_BACKGROUND = "app_background"
        
        // User properties
        private val USER_PROPERTIES = listOf(
            "user_type",
            "active_platforms",
            "auto_accept_enabled",
            "dark_mode_enabled",
            "notification_sound_enabled",
            "vibration_enabled",
            "minimum_order_amount",
            "maximum_distance",
            "active_hours_enabled"
        )
    }
}
