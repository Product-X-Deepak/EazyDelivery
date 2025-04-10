package com.eazydelivery.app.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import com.eazydelivery.app.MainActivity
import com.eazydelivery.app.security.SecurePreferencesManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages crash recovery and provides mechanisms to recover from crashes gracefully
 */
@Singleton
class CrashRecoveryManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securePreferencesManager: SecurePreferencesManager,
    private val errorHandler: ErrorHandler
) {
    private val recoveryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    companion object {
        private const val CRASH_TIMESTAMP_KEY = "last_crash_timestamp"
        private const val CRASH_COUNT_KEY = "crash_count"
        private const val CRASH_TYPE_KEY = "last_crash_type"
        
        // Crash recovery thresholds
        private const val MAX_CRASHES_IN_PERIOD = 3
        private const val CRASH_PERIOD_MS = 5 * 60 * 1000 // 5 minutes
        
        // Crash types
        const val CRASH_TYPE_UNKNOWN = "unknown"
        const val CRASH_TYPE_ANR = "anr"
        const val CRASH_TYPE_NATIVE = "native"
        const val CRASH_TYPE_RUNTIME = "runtime"
        const val CRASH_TYPE_OOM = "out_of_memory"
    }
    
    /**
     * Initialize the crash recovery manager
     */
    fun initialize() {
        try {
            Timber.d("Initializing crash recovery manager")
            
            // Set up custom uncaught exception handler
            setupUncaughtExceptionHandler()
            
            // Check for previous crashes
            checkPreviousCrashes()
            
            Timber.d("Crash recovery manager initialized")
        } catch (e: Exception) {
            errorHandler.handleException("CrashRecoveryManager.initialize", e)
        }
    }
    
    /**
     * Set up custom uncaught exception handler
     */
    private fun setupUncaughtExceptionHandler() {
        try {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                try {
                    // Log the crash
                    Timber.e(throwable, "Uncaught exception in thread ${thread.name}")
                    
                    // Save crash information
                    saveCrashInfo(throwable)
                    
                    // Report to Firebase Crashlytics
                    FirebaseCrashlytics.getInstance().recordException(throwable)
                    
                    // Attempt to recover
                    if (attemptRecovery(throwable)) {
                        // If recovery was successful, don't call the default handler
                        // This prevents the app from showing the "App has stopped" dialog
                        restartApp()
                    } else {
                        // If recovery failed, call the default handler
                        defaultHandler?.uncaughtException(thread, throwable)
                    }
                } catch (e: Exception) {
                    // If our handler fails, call the default handler
                    Timber.e(e, "Error in custom uncaught exception handler")
                    defaultHandler?.uncaughtException(thread, throwable)
                }
            }
        } catch (e: Exception) {
            errorHandler.handleException("CrashRecoveryManager.setupUncaughtExceptionHandler", e)
        }
    }
    
    /**
     * Check for previous crashes
     */
    private fun checkPreviousCrashes() {
        recoveryScope.launch {
            try {
                // Get last crash timestamp
                val lastCrashTimestamp = securePreferencesManager.getLong(CRASH_TIMESTAMP_KEY, 0)
                val currentTime = System.currentTimeMillis()
                
                // If last crash was within the crash period, increment crash count
                if (currentTime - lastCrashTimestamp < CRASH_PERIOD_MS) {
                    val crashCount = securePreferencesManager.getInt(CRASH_COUNT_KEY, 0)
                    
                    if (crashCount >= MAX_CRASHES_IN_PERIOD) {
                        // Too many crashes in a short period, reset app state
                        Timber.w("Too many crashes detected ($crashCount in ${CRASH_PERIOD_MS / 1000} seconds), resetting app state")
                        resetAppState()
                    }
                } else {
                    // Reset crash count if last crash was outside the period
                    securePreferencesManager.putInt(CRASH_COUNT_KEY, 0)
                }
                
                // Clean up old crash logs
                cleanupOldCrashLogs()
            } catch (e: Exception) {
                errorHandler.handleException("CrashRecoveryManager.checkPreviousCrashes", e)
            }
        }
    }
    
    /**
     * Save crash information
     */
    private fun saveCrashInfo(throwable: Throwable) {
        recoveryScope.launch {
            try {
                // Save crash timestamp
                val currentTime = System.currentTimeMillis()
                securePreferencesManager.putLong(CRASH_TIMESTAMP_KEY, currentTime)
                
                // Increment crash count
                val crashCount = securePreferencesManager.getInt(CRASH_COUNT_KEY, 0)
                securePreferencesManager.putInt(CRASH_COUNT_KEY, crashCount + 1)
                
                // Determine crash type
                val crashType = determineCrashType(throwable)
                securePreferencesManager.putString(CRASH_TYPE_KEY, crashType)
                
                // Save crash log to file
                saveCrashLogToFile(throwable, crashType)
                
                // Add crash info to Crashlytics
                val crashlytics = FirebaseCrashlytics.getInstance()
                crashlytics.setCustomKey("crash_count", crashCount + 1)
                crashlytics.setCustomKey("crash_type", crashType)
                crashlytics.setCustomKey("device_uptime", android.os.SystemClock.elapsedRealtime())
                
                Timber.d("Saved crash info: type=$crashType, count=${crashCount + 1}")
            } catch (e: Exception) {
                Timber.e(e, "Error saving crash info")
            }
        }
    }
    
    /**
     * Determine the type of crash
     */
    private fun determineCrashType(throwable: Throwable): String {
        return when {
            throwable is OutOfMemoryError -> CRASH_TYPE_OOM
            throwable.stackTrace.any { it.className.contains("android.app.ActivityThread") && it.methodName.contains("performResumeActivity") } -> CRASH_TYPE_ANR
            throwable.stackTrace.any { it.isNativeMethod } -> CRASH_TYPE_NATIVE
            else -> CRASH_TYPE_RUNTIME
        }
    }
    
    /**
     * Save crash log to file
     */
    private fun saveCrashLogToFile(throwable: Throwable, crashType: String) {
        try {
            val crashLogsDir = File(context.filesDir, "crash_logs")
            if (!crashLogsDir.exists()) {
                crashLogsDir.mkdirs()
            }
            
            // Create timestamp for filename
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val crashLogFile = File(crashLogsDir, "crash_${timestamp}_${crashType}.txt")
            
            // Write crash log to file
            FileOutputStream(crashLogFile).use { fos ->
                PrintWriter(fos).use { pw ->
                    pw.println("Crash Type: $crashType")
                    pw.println("Timestamp: ${Date()}")
                    pw.println("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                    pw.println("Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                    pw.println("App Version: ${context.packageManager.getPackageInfo(context.packageName, 0).versionName}")
                    pw.println("\nStack Trace:")
                    throwable.printStackTrace(pw)
                }
            }
            
            Timber.d("Saved crash log to ${crashLogFile.absolutePath}")
        } catch (e: Exception) {
            Timber.e(e, "Error saving crash log to file")
        }
    }
    
    /**
     * Clean up old crash logs
     */
    private fun cleanupOldCrashLogs() {
        try {
            val crashLogsDir = File(context.filesDir, "crash_logs")
            if (!crashLogsDir.exists()) {
                return
            }
            
            // Get all crash log files
            val crashLogFiles = crashLogsDir.listFiles { file ->
                file.isFile && file.name.startsWith("crash_")
            } ?: return
            
            // Sort by last modified (oldest first)
            val sortedFiles = crashLogFiles.sortedBy { it.lastModified() }
            
            // Keep only the 10 most recent crash logs
            if (sortedFiles.size > 10) {
                sortedFiles.take(sortedFiles.size - 10).forEach { file ->
                    file.delete()
                    Timber.d("Deleted old crash log: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error cleaning up old crash logs")
        }
    }
    
    /**
     * Attempt to recover from a crash
     * 
     * @param throwable The throwable that caused the crash
     * @return true if recovery was attempted, false otherwise
     */
    private fun attemptRecovery(throwable: Throwable): Boolean {
        try {
            // Get crash count
            val crashCount = securePreferencesManager.getInt(CRASH_COUNT_KEY, 0)
            
            // If we've had too many crashes, don't attempt recovery
            if (crashCount > MAX_CRASHES_IN_PERIOD) {
                Timber.w("Too many crashes ($crashCount), not attempting recovery")
                return false
            }
            
            // Determine crash type
            val crashType = determineCrashType(throwable)
            
            // Attempt recovery based on crash type
            return when (crashType) {
                CRASH_TYPE_OOM -> {
                    // For OOM, clear caches and restart
                    Timber.d("Attempting recovery from OOM")
                    clearCaches()
                    true
                }
                CRASH_TYPE_ANR -> {
                    // For ANR, just restart
                    Timber.d("Attempting recovery from ANR")
                    true
                }
                CRASH_TYPE_RUNTIME -> {
                    // For runtime exceptions, attempt recovery if not too severe
                    val isSevere = isSevereCrash(throwable)
                    if (!isSevere) {
                        Timber.d("Attempting recovery from runtime exception")
                        true
                    } else {
                        Timber.w("Severe runtime exception, not attempting recovery")
                        false
                    }
                }
                else -> {
                    // For other crashes, don't attempt recovery
                    Timber.w("Unknown crash type, not attempting recovery")
                    false
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in attemptRecovery")
            return false
        }
    }
    
    /**
     * Check if a crash is severe (not recoverable)
     */
    private fun isSevereCrash(throwable: Throwable): Boolean {
        // Check for specific severe exceptions
        return throwable is SecurityException ||
                throwable is NoClassDefFoundError ||
                throwable is IllegalStateException && throwable.message?.contains("Activity") == true
    }
    
    /**
     * Clear caches to free up memory
     */
    private fun clearCaches() {
        try {
            // Clear internal cache
            context.cacheDir.deleteRecursively()
            
            // Clear external cache if available
            context.externalCacheDir?.deleteRecursively()
            
            Timber.d("Cleared app caches")
        } catch (e: Exception) {
            Timber.e(e, "Error clearing caches")
        }
    }
    
    /**
     * Reset app state
     */
    private fun resetAppState() {
        recoveryScope.launch {
            try {
                Timber.w("Resetting app state due to repeated crashes")
                
                // Clear crash count
                securePreferencesManager.putInt(CRASH_COUNT_KEY, 0)
                
                // Clear caches
                clearCaches()
                
                // Reset preferences (but keep authentication)
                val authToken = securePreferencesManager.getString(SecurePreferencesManager.KEY_AUTH_TOKEN)
                val refreshToken = securePreferencesManager.getString(SecurePreferencesManager.KEY_REFRESH_TOKEN)
                val userId = securePreferencesManager.getString(SecurePreferencesManager.KEY_USER_ID)
                
                // Clear all preferences
                securePreferencesManager.clearAll()
                
                // Restore authentication if available
                if (authToken != null && refreshToken != null && userId != null) {
                    securePreferencesManager.storeAuthData(userId, authToken, refreshToken)
                }
                
                Timber.d("App state reset completed")
            } catch (e: Exception) {
                errorHandler.handleException("CrashRecoveryManager.resetAppState", e)
            }
        }
    }
    
    /**
     * Restart the app
     */
    private fun restartApp() {
        try {
            Timber.d("Restarting app after crash")
            
            // Create intent to restart the app
            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra("crash_recovery", true)
            }
            
            // Start the activity
            context.startActivity(intent)
            
            // Kill the current process
            Process.killProcess(Process.myPid())
            System.exit(10)
        } catch (e: Exception) {
            Timber.e(e, "Error restarting app")
        }
    }
    
    /**
     * Get crash logs
     * 
     * @return List of crash log files
     */
    fun getCrashLogs(): List<File> {
        try {
            val crashLogsDir = File(context.filesDir, "crash_logs")
            if (!crashLogsDir.exists()) {
                return emptyList()
            }
            
            // Get all crash log files
            return crashLogsDir.listFiles { file ->
                file.isFile && file.name.startsWith("crash_")
            }?.sortedByDescending { it.lastModified() } ?: emptyList()
        } catch (e: Exception) {
            errorHandler.handleException("CrashRecoveryManager.getCrashLogs", e)
            return emptyList()
        }
    }
    
    /**
     * Get crash statistics
     * 
     * @return Map of crash statistics
     */
    fun getCrashStatistics(): Map<String, Any> {
        try {
            val crashCount = securePreferencesManager.getInt(CRASH_COUNT_KEY, 0)
            val lastCrashTimestamp = securePreferencesManager.getLong(CRASH_TIMESTAMP_KEY, 0)
            val lastCrashType = securePreferencesManager.getString(CRASH_TYPE_KEY) ?: CRASH_TYPE_UNKNOWN
            
            return mapOf(
                "crash_count" to crashCount,
                "last_crash_timestamp" to lastCrashTimestamp,
                "last_crash_type" to lastCrashType,
                "crash_logs_count" to getCrashLogs().size
            )
        } catch (e: Exception) {
            errorHandler.handleException("CrashRecoveryManager.getCrashStatistics", e)
            return emptyMap()
        }
    }
}
