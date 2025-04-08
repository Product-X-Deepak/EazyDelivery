package com.eazydelivery.app.util

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.os.Process
import android.os.SystemClock
import com.eazydelivery.app.util.error.AppError
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility for monitoring app performance
 */
@Singleton
class PerformanceMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val errorHandler: ErrorHandler
) {

    // Firebase Performance traces
    private val traces = mutableMapOf<String, Trace>()
    private val startTimes = mutableMapOf<String, Long>()

    // Coroutine scope for background operations
    private val monitorScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Flag to indicate if monitoring is enabled
    private var monitoringEnabled = false

    // Map to store operation durations for performance tracking
    private val operationDurations = ConcurrentHashMap<String, MutableList<Long>>()

    // Map to store operation counts for performance tracking
    private val operationCounts = ConcurrentHashMap<String, Int>()

    // Threshold for slow operations (in milliseconds)
    private val slowOperationThreshold = 100L

    // Threshold for memory warnings (in percentage of max memory)
    private val memoryWarningThreshold = 80

    /**
     * Start a performance trace
     */
    fun startTrace(traceName: String) {
        try {
            val trace = FirebasePerformance.getInstance().newTrace(traceName)
            trace.start()
            traces[traceName] = trace
            startTimes[traceName] = SystemClock.elapsedRealtime()
            Timber.d("Started trace: $traceName")
        } catch (e: Exception) {
            Timber.e(e, "Error starting trace: $traceName")
        }
    }

    /**
     * Stop a performance trace
     */
    fun stopTrace(traceName: String) {
        try {
            val trace = traces.remove(traceName)
            val startTime = startTimes.remove(traceName)

            if (trace != null) {
                trace.stop()

                if (startTime != null) {
                    val duration = SystemClock.elapsedRealtime() - startTime
                    Timber.d("Stopped trace: $traceName, duration: $duration ms")
                } else {
                    Timber.d("Stopped trace: $traceName")
                }
            } else {
                Timber.w("Attempted to stop non-existent trace: $traceName")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error stopping trace: $traceName")
        }
    }

    /**
     * Add a metric to a trace
     */
    fun putMetric(traceName: String, metricName: String, value: Long) {
        try {
            val trace = traces[traceName]
            if (trace != null) {
                trace.putMetric(metricName, value)
                Timber.d("Added metric to trace: $traceName, metric: $metricName, value: $value")
            } else {
                Timber.w("Attempted to add metric to non-existent trace: $traceName")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error adding metric to trace: $traceName")
        }
    }

    /**
     * Add an attribute to a trace
     */
    fun putAttribute(traceName: String, attributeName: String, value: String) {
        try {
            val trace = traces[traceName]
            if (trace != null) {
                trace.putAttribute(attributeName, value)
                Timber.d("Added attribute to trace: $traceName, attribute: $attributeName, value: $value")
            } else {
                Timber.w("Attempted to add attribute to non-existent trace: $traceName")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error adding attribute to trace: $traceName")
        }
    }

    /**
     * Increment a counter in a trace
     */
    fun incrementMetric(traceName: String, metricName: String, incrementBy: Long = 1) {
        try {
            val trace = traces[traceName]
            if (trace != null) {
                trace.incrementMetric(metricName, incrementBy)
                Timber.d("Incremented metric in trace: $traceName, metric: $metricName, by: $incrementBy")
            } else {
                Timber.w("Attempted to increment metric in non-existent trace: $traceName")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error incrementing metric in trace: $traceName")
        }
    }

    /**
     * Starts performance monitoring
     */
    fun startMonitoring() {
        if (monitoringEnabled) return

        monitoringEnabled = true
        Timber.d("Starting performance monitoring")

        // Start periodic monitoring
        monitorScope.launch {
            try {
                while (monitoringEnabled) {
                    monitorMemoryUsage()
                    delay(TimeUnit.MINUTES.toMillis(5)) // Check every 5 minutes
                }
            } catch (e: Exception) {
                val appError = errorHandler.handleException("PerformanceMonitor.startMonitoring", e)
                if (appError !is AppError.Unexpected) {
                    // Restart monitoring if it was a recoverable error
                    delay(TimeUnit.SECONDS.toMillis(30))
                    startMonitoring()
                }
            }
        }
    }

    /**
     * Stops performance monitoring
     */
    fun stopMonitoring() {
        monitoringEnabled = false
        Timber.d("Stopping performance monitoring")
    }

    /**
     * Measures the execution time of a block of code
     *
     * @param operationName The name of the operation being measured
     * @param block The block of code to measure
     * @return The result of the block
     */
    inline fun <T> measureOperation(operationName: String, block: () -> T): T {
        val startTime = SystemClock.elapsedRealtime()

        try {
            return block()
        } finally {
            val duration = SystemClock.elapsedRealtime() - startTime

            // Record the duration
            operationDurations.getOrPut(operationName) { mutableListOf() }.add(duration)

            // Increment the count
            operationCounts[operationName] = (operationCounts[operationName] ?: 0) + 1

            // Log slow operations
            if (duration > slowOperationThreshold) {
                Timber.w("Slow operation: $operationName took $duration ms")
            }
        }
    }

    /**
     * Monitors memory usage and logs warnings if memory usage is high
     */
    private fun monitorMemoryUsage() {
        try {
            // Get memory info
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val maxMemory = runtime.maxMemory()
            val memoryPercentage = (usedMemory.toFloat() / maxMemory.toFloat()) * 100

            // Log memory usage
            Timber.d("Memory usage: ${formatSize(usedMemory)}/${formatSize(maxMemory)} (${memoryPercentage.toInt()}%)")

            // Check if memory usage is high
            if (memoryPercentage > memoryWarningThreshold) {
                Timber.w("High memory usage: ${memoryPercentage.toInt()}% of max memory")

                // Suggest garbage collection if memory usage is very high
                if (memoryPercentage > 90) {
                    Timber.w("Suggesting garbage collection due to high memory usage")
                    System.gc()
                }
            }

            // Check if we're in a low memory situation
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)

            if (memoryInfo.lowMemory) {
                Timber.w("Device is in a low memory situation")

                // Take action to reduce memory usage
                clearCaches()
            }
        } catch (e: Exception) {
            errorHandler.handleException("PerformanceMonitor.monitorMemoryUsage", e)
        }
    }

    /**
     * Clears caches to free up memory
     */
    private fun clearCaches() {
        try {
            // Clear operation durations cache
            operationDurations.clear()

            // Clear operation counts cache
            operationCounts.clear()

            // Clear app cache directory
            val cacheDir = context.cacheDir
            clearDirectory(cacheDir)

            Timber.d("Cleared caches to free up memory")
        } catch (e: Exception) {
            errorHandler.handleException("PerformanceMonitor.clearCaches", e)
        }
    }

    /**
     * Clears a directory recursively
     *
     * @param directory The directory to clear
     */
    private fun clearDirectory(directory: File) {
        try {
            if (directory.exists() && directory.isDirectory) {
                val files = directory.listFiles()
                if (files != null) {
                    for (file in files) {
                        if (file.isDirectory) {
                            clearDirectory(file)
                        } else {
                            file.delete()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            errorHandler.handleException("PerformanceMonitor.clearDirectory", e)
        }
    }

    /**
     * Gets performance statistics for an operation
     *
     * @param operationName The name of the operation
     * @return The performance statistics
     */
    fun getOperationStats(operationName: String): OperationStats {
        val durations = operationDurations[operationName] ?: emptyList()
        val count = operationCounts[operationName] ?: 0

        return if (durations.isNotEmpty()) {
            OperationStats(
                operationName = operationName,
                count = count,
                averageDuration = durations.average().toLong(),
                minDuration = durations.minOrNull() ?: 0,
                maxDuration = durations.maxOrNull() ?: 0,
                p90Duration = calculatePercentile(durations, 90)
            )
        } else {
            OperationStats(
                operationName = operationName,
                count = count,
                averageDuration = 0,
                minDuration = 0,
                maxDuration = 0,
                p90Duration = 0
            )
        }
    }

    /**
     * Gets all operation statistics
     *
     * @return A list of operation statistics
     */
    fun getAllOperationStats(): List<OperationStats> {
        return operationDurations.keys.map { getOperationStats(it) }
    }

    /**
     * Calculates a percentile value from a list of durations
     *
     * @param durations The list of durations
     * @param percentile The percentile to calculate (0-100)
     * @return The percentile value
     */
    private fun calculatePercentile(durations: List<Long>, percentile: Int): Long {
        if (durations.isEmpty()) return 0

        val sorted = durations.sorted()
        val index = (percentile / 100.0 * sorted.size).toInt()
        return sorted[index.coerceAtMost(sorted.size - 1)]
    }

    /**
     * Formats a size in bytes to a human-readable string
     *
     * @param bytes The size in bytes
     * @return A human-readable string
     */
    private fun formatSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }

        return String.format("%.1f %s", size, units[unitIndex])
    }

    /**
     * Gets the current memory usage
     *
     * @return The memory usage information
     */
    fun getMemoryUsage(): MemoryUsage {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val memoryPercentage = (usedMemory.toFloat() / maxMemory.toFloat()) * 100

        return MemoryUsage(
            usedMemory = usedMemory,
            maxMemory = maxMemory,
            percentage = memoryPercentage.toInt(),
            formattedUsed = formatSize(usedMemory),
            formattedMax = formatSize(maxMemory)
        )
    }

    /**
     * Data class for operation statistics
     */
    data class OperationStats(
        val operationName: String,
        val count: Int,
        val averageDuration: Long,
        val minDuration: Long,
        val maxDuration: Long,
        val p90Duration: Long
    )

    /**
     * Data class for memory usage information
     */
    data class MemoryUsage(
        val usedMemory: Long,
        val maxMemory: Long,
        val percentage: Int,
        val formattedUsed: String,
        val formattedMax: String
    )
}

