package com.eazydelivery.app.util

import android.util.Log
import com.eazydelivery.app.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

/**
 * Enhanced utility class for controlling logging behavior
 * Provides better control over logging levels and frequency
 */
object LogUtils {
    // Log level constants
    const val LEVEL_VERBOSE = 1
    const val LEVEL_DEBUG = 2
    const val LEVEL_INFO = 3
    const val LEVEL_WARNING = 4
    const val LEVEL_ERROR = 5
    const val LEVEL_NONE = 6

    // Current log level - can be changed at runtime
    @Volatile
    private var currentLogLevel = if (BuildConfig.ENABLE_LOGS) LEVEL_DEBUG else LEVEL_WARNING

    // Rate limiting for logs - track last log time by tag
    private val lastLogTime = mutableMapOf<String, Long>()
    private const val MIN_LOG_INTERVAL_MS = 1000 // 1 second between similar logs

    /**
     * Set the current log level
     * @param level The log level to set
     */
    fun setLogLevel(level: Int) {
        currentLogLevel = level
    }

    /**
     * Get the current log level
     * @return The current log level
     */
    fun getLogLevel(): Int {
        return currentLogLevel
    }

    /**
     * Logs a verbose message
     */
    fun v(tag: String, message: String) {
        if (shouldLog(tag, LEVEL_VERBOSE)) {
            Timber.tag(tag).v(message)
        }
    }

    /**
     * Logs a debug message
     */
    fun d(tag: String, message: String) {
        if (shouldLog(tag, LEVEL_DEBUG)) {
            Timber.tag(tag).d(message)
        }
    }

    /**
     * Logs an info message
     */
    fun i(tag: String, message: String) {
        if (shouldLog(tag, LEVEL_INFO)) {
            Timber.tag(tag).i(message)
        }
    }

    /**
     * Logs a warning message
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (shouldLog(tag, LEVEL_WARNING)) {
            if (throwable != null) {
                Timber.tag(tag).w(throwable, message)
            } else {
                Timber.tag(tag).w(message)
            }
        }
    }

    /**
     * Logs an error message and reports to Crashlytics
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        // Errors are always logged regardless of rate limiting
        if (currentLogLevel <= LEVEL_ERROR) {
            if (throwable != null) {
                Timber.tag(tag).e(throwable, message)
                // Report to Crashlytics
                FirebaseCrashlytics.getInstance().apply {
                    log("$tag: $message")
                    recordException(throwable)
                }
            } else {
                Timber.tag(tag).e(message)
                // Report to Crashlytics
                FirebaseCrashlytics.getInstance().log("$tag: $message")
            }
        }
    }

    /**
     * Determines if a log message should be shown based on level and rate limiting
     */
    private fun shouldLog(tag: String, level: Int): Boolean {
        // Check if the level is enabled
        if (level < currentLogLevel) {
            return false
        }

        // Apply rate limiting for non-error logs
        if (level < LEVEL_ERROR) {
            val now = System.currentTimeMillis()
            val lastTime = lastLogTime[tag] ?: 0L

            // If we've logged this tag recently, skip it
            if (now - lastTime < MIN_LOG_INTERVAL_MS) {
                return false
            }

            // Update the last log time
            lastLogTime[tag] = now
        }

        return true
    }
}
