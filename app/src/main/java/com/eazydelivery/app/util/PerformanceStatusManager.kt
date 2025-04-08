package com.eazydelivery.app.util

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.Process
import com.eazydelivery.app.ui.component.PerformanceStatusView
import com.eazydelivery.app.util.error.AppError
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages performance status updates for the UI
 */
@Singleton
class PerformanceStatusManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val errorHandler: ErrorHandler,
    private val serviceOptimizer: com.eazydelivery.app.service.ServiceOptimizer
) {
    // Coroutine scope for background operations
    private val statusScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Handler for UI operations
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // Flag to indicate if monitoring is enabled
    private var monitoringEnabled = false
    
    // Performance status view to update
    private var performanceStatusView: PerformanceStatusView? = null
    
    // Update interval in milliseconds
    private val updateIntervalMs = 2000L
    
    /**
     * Starts monitoring and updating the performance status
     * 
     * @param statusView The view to update with performance status
     */
    fun startMonitoring(statusView: PerformanceStatusView) {
        if (monitoringEnabled) return
        
        performanceStatusView = statusView
        monitoringEnabled = true
        
        statusScope.launch {
            try {
                while (monitoringEnabled && performanceStatusView != null) {
                    updatePerformanceStatus()
                    delay(updateIntervalMs)
                }
            } catch (e: Exception) {
                val appError = errorHandler.handleException("PerformanceStatusManager.startMonitoring", e)
                if (appError !is AppError.Unexpected) {
                    // Restart monitoring if it was a recoverable error
                    delay(TimeUnit.SECONDS.toMillis(5))
                    performanceStatusView?.let { startMonitoring(it) }
                }
            }
        }
    }
    
    /**
     * Stops monitoring and updating the performance status
     */
    fun stopMonitoring() {
        monitoringEnabled = false
        performanceStatusView = null
    }
    
    /**
     * Updates the performance status view with current metrics
     */
    private suspend fun updatePerformanceStatus() {
        try {
            // Get CPU usage
            val cpuUsage = getCpuUsage()
            
            // Get memory usage
            val memoryUsage = getMemoryUsage()
            
            // Get battery optimization status
            val batteryOptimized = isBatteryOptimized()
            
            // Update the view on the main thread
            mainHandler.post {
                performanceStatusView?.apply {
                    this.cpuUsage = cpuUsage
                    this.memoryUsage = memoryUsage
                    this.batteryOptimized = batteryOptimized
                }
            }
        } catch (e: Exception) {
            errorHandler.handleException("PerformanceStatusManager.updatePerformanceStatus", e)
        }
    }
    
    /**
     * Gets the current CPU usage as a percentage
     * 
     * @return CPU usage as a percentage (0-100)
     */
    private fun getCpuUsage(): Float {
        try {
            val pid = Process.myPid()
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            
            // Get CPU usage for the app process
            val pids = intArrayOf(pid)
            val cpuInfo = activityManager.getProcessCpuStateInfo(pids)
            
            if (cpuInfo != null && cpuInfo.isNotEmpty()) {
                return cpuInfo[0].cpuPercent
            }
            
            // Fallback method if the above doesn't work
            val process = Runtime.getRuntime().exec("top -n 1")
            process.waitFor()
            
            val reader = process.inputStream.bufferedReader()
            var line: String?
            var cpuTotal = 0f
            var cpuApp = 0f
            
            while (reader.readLine().also { line = it } != null) {
                if (line?.contains("CPU:") == true) {
                    val parts = line?.split("\\s+".toRegex())
                    if (parts != null && parts.size > 1) {
                        for (part in parts) {
                            if (part.endsWith("%")) {
                                val value = part.substring(0, part.length - 1).toFloatOrNull()
                                if (value != null) {
                                    cpuTotal += value
                                }
                            }
                        }
                    }
                } else if (line?.contains(pid.toString()) == true) {
                    val parts = line?.split("\\s+".toRegex())
                    if (parts != null && parts.size > 7) {
                        val value = parts[7].toFloatOrNull()
                        if (value != null) {
                            cpuApp = value
                        }
                    }
                }
            }
            
            return if (cpuTotal > 0) (cpuApp / cpuTotal) * 100f else 0f
        } catch (e: Exception) {
            Timber.e(e, "Error getting CPU usage")
            return 0f
        }
    }
    
    /**
     * Gets the current memory usage as a percentage
     * 
     * @return Memory usage as a percentage (0-100)
     */
    private fun getMemoryUsage(): Float {
        try {
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val maxMemory = runtime.maxMemory()
            
            return (usedMemory.toFloat() / maxMemory.toFloat()) * 100f
        } catch (e: Exception) {
            Timber.e(e, "Error getting memory usage")
            return 0f
        }
    }
    
    /**
     * Checks if battery optimization is enabled
     * 
     * @return true if battery optimization is enabled, false otherwise
     */
    private fun isBatteryOptimized(): Boolean {
        return serviceOptimizer.isOptimizationEnabled()
    }
    
    /**
     * Extension function to get CPU state info for processes
     * This is a workaround for the missing API in some Android versions
     */
    private fun ActivityManager.getProcessCpuStateInfo(pids: IntArray): Array<ActivityManager.ProcessCpuStateInfo>? {
        try {
            val method = ActivityManager::class.java.getMethod("getProcessCpuStateInfo", IntArray::class.java)
            return method.invoke(this, pids) as? Array<ActivityManager.ProcessCpuStateInfo>
        } catch (e: Exception) {
            Timber.e(e, "Error getting process CPU state info")
            return null
        }
    }
    
    /**
     * Extension property to get CPU usage percentage from ProcessCpuStateInfo
     */
    private val ActivityManager.ProcessCpuStateInfo.cpuPercent: Float
        get() {
            try {
                val field = ActivityManager.ProcessCpuStateInfo::class.java.getField("cpuPercent")
                return field.get(this) as Float
            } catch (e: Exception) {
                Timber.e(e, "Error getting CPU percent")
                return 0f
            }
        }
}
