package com.eazydelivery.app.service

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.eazydelivery.app.util.Constants
import com.eazydelivery.app.util.ErrorHandler
import com.eazydelivery.app.util.WakeLockManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optimizes service behavior based on device state
 * Helps reduce battery consumption while maintaining functionality
 */
@Singleton
class ServiceOptimizer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val errorHandler: ErrorHandler,
    private val wakeLockManager: WakeLockManager
) {
    // Sampling intervals based on battery state
    private val highBatterySamplingIntervalMs = 1000L // 1 second
    private val mediumBatterySamplingIntervalMs = 2000L // 2 seconds
    private val lowBatterySamplingIntervalMs = 5000L // 5 seconds

    // Battery thresholds
    private val lowBatteryThreshold = 15 // 15%
    private val mediumBatteryThreshold = 30 // 30%

    // Current sampling interval
    private var currentSamplingIntervalMs = highBatterySamplingIntervalMs

    // Flag to indicate if optimization is enabled
    private var optimizationEnabled = true

    /**
     * Starts the service optimizer
     * Monitors device state and adjusts service behavior accordingly
     *
     * @param scope The coroutine scope to run the optimizer in
     */
    fun startOptimizer(scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            try {
                Timber.d("Starting service optimizer")

                while (true) {
                    // Check device state and adjust service behavior
                    adjustServiceBehavior()

                    // Wait for the current sampling interval
                    delay(currentSamplingIntervalMs)
                }
            } catch (e: Exception) {
                errorHandler.handleException("ServiceOptimizer.startOptimizer", e)
            }
        }
    }

    /**
     * Adjusts service behavior based on device state
     */
    private fun adjustServiceBehavior() {
        try {
            // Get battery level
            val batteryLevel = getBatteryLevel()

            // Get battery state
            val isCharging = isDeviceCharging()

            // Get device memory state
            val availableMemory = getAvailableMemory()
            val totalMemory = getTotalMemory()
            val memoryPercentage = (availableMemory.toFloat() / totalMemory.toFloat()) * 100

            // Adjust sampling interval based on battery level and charging state
            currentSamplingIntervalMs = when {
                isCharging -> highBatterySamplingIntervalMs
                batteryLevel <= lowBatteryThreshold -> lowBatterySamplingIntervalMs
                batteryLevel <= mediumBatteryThreshold -> mediumBatterySamplingIntervalMs
                else -> highBatterySamplingIntervalMs
            }

            // Adjust service behavior based on memory state
            if (memoryPercentage < 15) {
                // Low memory, reduce service activity
                Timber.d("Low memory detected (${memoryPercentage.toInt()}%), reducing service activity")
                reduceServiceActivity()
            }

            // Log current state
            Timber.d("Battery: $batteryLevel%, Charging: $isCharging, Memory: ${memoryPercentage.toInt()}%, Interval: ${currentSamplingIntervalMs}ms")
        } catch (e: Exception) {
            errorHandler.handleException("ServiceOptimizer.adjustServiceBehavior", e)
        }
    }

    /**
     * Gets the current battery level
     *
     * @return The battery level as a percentage (0-100)
     */
    private fun getBatteryLevel(): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    /**
     * Checks if the device is currently charging
     *
     * @return true if the device is charging, false otherwise
     */
    private fun isDeviceCharging(): Boolean {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.isCharging
    }

    /**
     * Gets the available memory in bytes
     *
     * @return The available memory in bytes
     */
    private fun getAvailableMemory(): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.availMem
    }

    /**
     * Gets the total memory in bytes
     *
     * @return The total memory in bytes
     */
    private fun getTotalMemory(): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.totalMem
    }

    /**
     * Reduces service activity to conserve resources
     */
    private fun reduceServiceActivity() {
        // Implement resource-saving measures here
        // For example, reduce sampling frequency, disable non-critical features, etc.
    }

    /**
     * Sets whether optimization is enabled
     *
     * @param enabled true to enable optimization, false to disable
     */
    fun setOptimizationEnabled(enabled: Boolean) {
        optimizationEnabled = enabled
        Timber.d("Service optimization ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Checks if optimization is enabled
     *
     * @return true if optimization is enabled, false otherwise
     */
    fun isOptimizationEnabled(): Boolean {
        return optimizationEnabled
    }

    /**
     * Acquires a wake lock to keep the CPU running
     * Should be used sparingly and released as soon as possible
     *
     * @param tag A tag for the wake lock
     * @param timeoutMs The timeout in milliseconds, or 0 for no timeout
     * @return The wake lock, or null if it could not be acquired
     */
    fun acquireWakeLock(tag: String, timeoutMs: Long = Constants.WAKE_LOCK_TIMEOUT_MS): PowerManager.WakeLock? {
        return wakeLockManager.acquireWakeLock("ServiceOptimizer:$tag", timeoutMs)
    }

    /**
     * Releases a wake lock
     *
     * @param wakeLock The wake lock to release
     * @param tag A tag for logging
     */
    fun releaseWakeLock(wakeLock: PowerManager.WakeLock?, tag: String) {
        wakeLockManager.releaseWakeLock(wakeLock, "ServiceOptimizer:$tag")
    }

    /**
     * Executes a code block with a wake lock that is automatically released
     * when the code block completes or throws an exception
     *
     * @param tag A tag for the wake lock
     * @param timeoutMs The timeout in milliseconds, or 0 for no timeout
     * @param block The code block to execute with the wake lock
     * @return The result of the code block
     */
    inline fun <T> withWakeLock(tag: String, timeoutMs: Long = Constants.WAKE_LOCK_TIMEOUT_MS, block: () -> T): T {
        return wakeLockManager.withWakeLock("ServiceOptimizer:$tag", timeoutMs, block)
    }
}
