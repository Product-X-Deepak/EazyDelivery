package com.eazydelivery.app.util

import android.content.Context
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class to safely manage wake locks
 */
@Singleton
class WakeLockManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val errorHandler: ErrorHandler
) {
    /**
     * Acquires a wake lock with the specified tag and timeout
     *
     * @param tag The tag for the wake lock
     * @param timeoutMs The timeout in milliseconds (0 for no timeout)
     * @return The acquired wake lock or null if acquisition failed
     */
    fun acquireWakeLock(tag: String, timeoutMs: Long = Constants.WAKE_LOCK_TIMEOUT_MS): PowerManager.WakeLock? {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "EazyDelivery:$tag"
            )
            
            // Set a timeout if specified
            if (timeoutMs > 0) {
                wakeLock.acquire(timeoutMs)
            } else {
                wakeLock.acquire()
            }
            
            Timber.d("Wake lock acquired: $tag")
            wakeLock
        } catch (e: Exception) {
            errorHandler.handleException("WakeLockManager.acquireWakeLock", e)
            null
        }
    }
    
    /**
     * Safely releases a wake lock
     *
     * @param wakeLock The wake lock to release
     * @param tag The tag for logging
     */
    fun releaseWakeLock(wakeLock: PowerManager.WakeLock?, tag: String) {
        try {
            if (wakeLock != null && wakeLock.isHeld) {
                wakeLock.release()
                Timber.d("Wake lock released: $tag")
            }
        } catch (e: Exception) {
            errorHandler.handleException("WakeLockManager.releaseWakeLock", e)
        }
    }
    
    /**
     * Use this function to execute code with a wake lock that is automatically released
     * when the code block completes or throws an exception
     *
     * @param tag The tag for the wake lock
     * @param timeoutMs The timeout in milliseconds (0 for no timeout)
     * @param block The code block to execute with the wake lock
     * @return The result of the code block
     */
    inline fun <T> withWakeLock(tag: String, timeoutMs: Long = Constants.WAKE_LOCK_TIMEOUT_MS, block: () -> T): T {
        val wakeLock = acquireWakeLock(tag, timeoutMs)
        try {
            return block()
        } finally {
            releaseWakeLock(wakeLock, tag)
        }
    }
}
