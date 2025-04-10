package com.eazydelivery.app.util

import android.content.Context
import android.os.BatteryManager
import android.os.PowerManager
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.eazydelivery.app.worker.OptimizationWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages background tasks and optimizes resource usage
 */
@Singleton
class BackgroundTaskManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val errorHandler: ErrorHandler
) {
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    companion object {
        private const val OPTIMIZATION_WORK_NAME = "background_optimization_work"
        private const val OPTIMIZATION_INTERVAL_MINUTES = 30L
        
        // Battery thresholds
        private const val LOW_BATTERY_THRESHOLD = 15
        private const val MEDIUM_BATTERY_THRESHOLD = 30
        
        // Sampling intervals based on battery level
        private const val LOW_BATTERY_SAMPLING_INTERVAL_MS = 5000L
        private const val MEDIUM_BATTERY_SAMPLING_INTERVAL_MS = 3000L
        private const val HIGH_BATTERY_SAMPLING_INTERVAL_MS = 1500L
    }
    
    // Current sampling interval
    private var currentSamplingIntervalMs = MEDIUM_BATTERY_SAMPLING_INTERVAL_MS
    
    /**
     * Initialize the background task manager
     */
    fun initialize() {
        try {
            Timber.d("Initializing background task manager")
            
            // Schedule periodic work for optimization
            scheduleOptimizationWork()
            
            // Perform initial optimization
            managerScope.launch {
                optimizeBackgroundTasks()
            }
        } catch (e: Exception) {
            errorHandler.handleException("BackgroundTaskManager.initialize", e)
        }
    }
    
    /**
     * Schedule periodic work for optimization
     */
    private fun scheduleOptimizationWork() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(false)
                .build()
            
            val workRequest = PeriodicWorkRequestBuilder<OptimizationWorker>(
                OPTIMIZATION_INTERVAL_MINUTES, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                OPTIMIZATION_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
            
            Timber.d("Scheduled optimization work")
        } catch (e: Exception) {
            errorHandler.handleException("BackgroundTaskManager.scheduleOptimizationWork", e)
        }
    }
    
    /**
     * Optimize background tasks based on device state
     */
    fun optimizeBackgroundTasks() {
        try {
            // Get battery level
            val batteryLevel = getBatteryLevel()
            
            // Get battery state
            val isCharging = isDeviceCharging()
            
            // Get device memory state
            val availableMemory = getAvailableMemory()
            val totalMemory = getTotalMemory()
            val memoryPercentage = (availableMemory.toFloat() / totalMemory.toFloat()) * 100
            
            Timber.d("Device state: Battery: $batteryLevel%, Charging: $isCharging, Memory: $memoryPercentage%")
            
            // Adjust sampling interval based on battery level and charging state
            currentSamplingIntervalMs = when {
                isCharging -> HIGH_BATTERY_SAMPLING_INTERVAL_MS
                batteryLevel <= LOW_BATTERY_THRESHOLD -> LOW_BATTERY_SAMPLING_INTERVAL_MS
                batteryLevel <= MEDIUM_BATTERY_THRESHOLD -> MEDIUM_BATTERY_SAMPLING_INTERVAL_MS
                else -> HIGH_BATTERY_SAMPLING_INTERVAL_MS
            }
            
            Timber.d("Adjusted sampling interval to $currentSamplingIntervalMs ms")
            
            // Check if we need to release memory
            if (memoryPercentage < 15) {
                Timber.w("Low memory detected, releasing memory")
                System.gc()
            }
        } catch (e: Exception) {
            errorHandler.handleException("BackgroundTaskManager.optimizeBackgroundTasks", e)
        }
    }
    
    /**
     * Get the current sampling interval
     */
    fun getCurrentSamplingInterval(): Long {
        return currentSamplingIntervalMs
    }
    
    /**
     * Get the battery level
     */
    private fun getBatteryLevel(): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
    
    /**
     * Check if the device is charging
     */
    private fun isDeviceCharging(): Boolean {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
        return status == BatteryManager.BATTERY_STATUS_CHARGING || 
               status == BatteryManager.BATTERY_STATUS_FULL
    }
    
    /**
     * Get the available memory in MB
     */
    private fun getAvailableMemory(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.freeMemory() + (runtime.maxMemory() - runtime.totalMemory())) / (1024 * 1024)
    }
    
    /**
     * Get the total memory in MB
     */
    private fun getTotalMemory(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.maxMemory() / (1024 * 1024)
    }
    
    /**
     * Request a wake lock to keep the CPU running
     * 
     * @param tag The tag for the wake lock
     * @param timeoutMs The timeout in milliseconds
     * @return The wake lock
     */
    fun requestWakeLock(tag: String, timeoutMs: Long): PowerManager.WakeLock? {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "EazyDelivery:$tag"
            )
            
            wakeLock.acquire(timeoutMs)
            Timber.d("Acquired wake lock for $tag with timeout $timeoutMs ms")
            
            return wakeLock
        } catch (e: Exception) {
            errorHandler.handleException("BackgroundTaskManager.requestWakeLock", e)
            return null
        }
    }
    
    /**
     * Release a wake lock
     * 
     * @param wakeLock The wake lock to release
     */
    fun releaseWakeLock(wakeLock: PowerManager.WakeLock?) {
        try {
            if (wakeLock != null && wakeLock.isHeld) {
                wakeLock.release()
                Timber.d("Released wake lock")
            }
        } catch (e: Exception) {
            errorHandler.handleException("BackgroundTaskManager.releaseWakeLock", e)
        }
    }
}
