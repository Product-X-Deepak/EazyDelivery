package com.eazydelivery.app.monitoring

import android.content.Context
import android.os.Build
import com.eazydelivery.app.BuildConfig
import com.eazydelivery.app.util.error.AppError
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class for crash reporting
 */
@Singleton
class CrashReportingManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val crashlytics: FirebaseCrashlytics by lazy {
        FirebaseCrashlytics.getInstance()
    }
    
    /**
     * Initialize crash reporting
     * 
     * @param userId The user ID
     * @param enableReporting Whether to enable crash reporting
     */
    fun initialize(userId: String? = null, enableReporting: Boolean = !BuildConfig.DEBUG) {
        try {
            // Enable or disable crash reporting
            crashlytics.setCrashlyticsCollectionEnabled(enableReporting)
            
            // Set user ID
            userId?.let { crashlytics.setUserId(it) }
            
            // Set app version
            crashlytics.setCustomKey("app_version", BuildConfig.VERSION_NAME)
            crashlytics.setCustomKey("app_version_code", BuildConfig.VERSION_CODE.toString())
            
            // Set build type
            crashlytics.setCustomKey("build_type", BuildConfig.BUILD_TYPE)
            
            // Set device information
            crashlytics.setCustomKey("device_manufacturer", Build.MANUFACTURER)
            crashlytics.setCustomKey("device_model", Build.MODEL)
            crashlytics.setCustomKey("android_version", Build.VERSION.RELEASE)
            crashlytics.setCustomKey("android_sdk", Build.VERSION.SDK_INT.toString())
            
            // Set session ID
            val sessionId = UUID.randomUUID().toString()
            crashlytics.setCustomKey("session_id", sessionId)
            
            Timber.d("Crash reporting initialized with userId: $userId, enableReporting: $enableReporting")
        } catch (e: Exception) {
            Timber.e(e, "Error initializing crash reporting")
        }
    }
    
    /**
     * Log a non-fatal exception
     * 
     * @param throwable The throwable to log
     * @param customMessage Optional custom message
     * @param customAttributes Optional custom attributes
     */
    fun logException(
        throwable: Throwable,
        customMessage: String? = null,
        customAttributes: Map<String, String>? = null
    ) {
        try {
            // Set custom message if provided
            customMessage?.let { crashlytics.log("Custom message: $it") }
            
            // Set custom attributes if provided
            customAttributes?.forEach { (key, value) ->
                crashlytics.setCustomKey(key, value)
            }
            
            // Log the exception
            crashlytics.recordException(throwable)
            
            Timber.e(throwable, "Logged exception: ${throwable.message}")
        } catch (e: Exception) {
            Timber.e(e, "Error logging exception: ${throwable.message}")
        }
    }
    
    /**
     * Log an app error
     * 
     * @param error The app error to log
     * @param customAttributes Optional custom attributes
     */
    fun logAppError(error: AppError, customAttributes: Map<String, String>? = null) {
        try {
            // Set error type and message
            crashlytics.setCustomKey("error_type", error.javaClass.simpleName)
            crashlytics.setCustomKey("error_message", error.message)
            
            // Set custom attributes if provided
            customAttributes?.forEach { (key, value) ->
                crashlytics.setCustomKey(key, value)
            }
            
            // Log the error
            error.cause?.let { crashlytics.recordException(it) }
                ?: crashlytics.recordException(Exception(error.message))
            
            Timber.e(error.cause, "Logged app error: ${error.javaClass.simpleName} - ${error.message}")
        } catch (e: Exception) {
            Timber.e(e, "Error logging app error: ${error.javaClass.simpleName} - ${error.message}")
        }
    }
    
    /**
     * Log a message
     * 
     * @param message The message to log
     */
    fun logMessage(message: String) {
        try {
            crashlytics.log(message)
            Timber.d("Logged message: $message")
        } catch (e: Exception) {
            Timber.e(e, "Error logging message: $message")
        }
    }
    
    /**
     * Set a custom key
     * 
     * @param key The key
     * @param value The value
     */
    fun setCustomKey(key: String, value: String) {
        try {
            crashlytics.setCustomKey(key, value)
            Timber.d("Set custom key: $key = $value")
        } catch (e: Exception) {
            Timber.e(e, "Error setting custom key: $key = $value")
        }
    }
    
    /**
     * Set a custom key
     * 
     * @param key The key
     * @param value The value
     */
    fun setCustomKey(key: String, value: Boolean) {
        try {
            crashlytics.setCustomKey(key, value)
            Timber.d("Set custom key: $key = $value")
        } catch (e: Exception) {
            Timber.e(e, "Error setting custom key: $key = $value")
        }
    }
    
    /**
     * Set a custom key
     * 
     * @param key The key
     * @param value The value
     */
    fun setCustomKey(key: String, value: Int) {
        try {
            crashlytics.setCustomKey(key, value)
            Timber.d("Set custom key: $key = $value")
        } catch (e: Exception) {
            Timber.e(e, "Error setting custom key: $key = $value")
        }
    }
    
    /**
     * Set a custom key
     * 
     * @param key The key
     * @param value The value
     */
    fun setCustomKey(key: String, value: Long) {
        try {
            crashlytics.setCustomKey(key, value)
            Timber.d("Set custom key: $key = $value")
        } catch (e: Exception) {
            Timber.e(e, "Error setting custom key: $key = $value")
        }
    }
    
    /**
     * Set a custom key
     * 
     * @param key The key
     * @param value The value
     */
    fun setCustomKey(key: String, value: Float) {
        try {
            crashlytics.setCustomKey(key, value)
            Timber.d("Set custom key: $key = $value")
        } catch (e: Exception) {
            Timber.e(e, "Error setting custom key: $key = $value")
        }
    }
    
    /**
     * Set a custom key
     * 
     * @param key The key
     * @param value The value
     */
    fun setCustomKey(key: String, value: Double) {
        try {
            crashlytics.setCustomKey(key, value)
            Timber.d("Set custom key: $key = $value")
        } catch (e: Exception) {
            Timber.e(e, "Error setting custom key: $key = $value")
        }
    }
    
    /**
     * Set the user ID
     * 
     * @param userId The user ID
     */
    fun setUserId(userId: String) {
        try {
            crashlytics.setUserId(userId)
            Timber.d("Set user ID: $userId")
        } catch (e: Exception) {
            Timber.e(e, "Error setting user ID: $userId")
        }
    }
    
    /**
     * Clear the user ID
     */
    fun clearUserId() {
        try {
            crashlytics.setUserId("")
            Timber.d("Cleared user ID")
        } catch (e: Exception) {
            Timber.e(e, "Error clearing user ID")
        }
    }
}
