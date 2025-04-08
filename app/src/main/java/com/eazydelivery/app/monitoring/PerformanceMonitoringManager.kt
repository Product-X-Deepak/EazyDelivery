package com.eazydelivery.app.monitoring

import android.content.Context
import android.os.SystemClock
import com.eazydelivery.app.analytics.AnalyticsEvents
import com.eazydelivery.app.analytics.AnalyticsManager
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class for monitoring app performance
 */
@Singleton
class PerformanceMonitoringManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyticsManager: AnalyticsManager
) {
    // Map of active traces
    private val activeTraces = ConcurrentHashMap<String, Trace>()
    
    // Map of trace start times
    private val traceStartTimes = ConcurrentHashMap<String, Long>()
    
    // Map of custom metrics
    private val customMetrics = ConcurrentHashMap<String, MutableList<Long>>()
    
    /**
     * Start a performance trace
     * 
     * @param traceName The name of the trace
     * @param attributes Optional attributes to add to the trace
     */
    fun startTrace(traceName: String, attributes: Map<String, String>? = null) {
        try {
            // Create a new trace
            val trace = FirebasePerformance.getInstance().newTrace(traceName)
            
            // Add attributes
            attributes?.forEach { (key, value) ->
                trace.putAttribute(key, value)
            }
            
            // Start the trace
            trace.start()
            
            // Store the trace and start time
            activeTraces[traceName] = trace
            traceStartTimes[traceName] = SystemClock.elapsedRealtime()
            
            Timber.d("Started trace: $traceName")
        } catch (e: Exception) {
            Timber.e(e, "Error starting trace: $traceName")
        }
    }
    
    /**
     * Stop a performance trace
     * 
     * @param traceName The name of the trace
     * @param trackInAnalytics Whether to track the trace in analytics
     */
    fun stopTrace(traceName: String, trackInAnalytics: Boolean = true) {
        try {
            // Get the trace
            val trace = activeTraces.remove(traceName)
            val startTime = traceStartTimes.remove(traceName)
            
            if (trace != null && startTime != null) {
                // Stop the trace
                trace.stop()
                
                // Calculate duration
                val duration = SystemClock.elapsedRealtime() - startTime
                
                // Track in analytics if requested
                if (trackInAnalytics) {
                    val analyticsEventName = when (traceName) {
                        TRACE_SCREEN_ANALYSIS -> AnalyticsEvents.PERF_SCREEN_ANALYSIS
                        TRACE_DATABASE_QUERY -> AnalyticsEvents.PERF_DATABASE_QUERY
                        TRACE_NOTIFICATION_PROCESSING -> AnalyticsEvents.PERF_NOTIFICATION_PROCESSING
                        TRACE_APP_STARTUP -> AnalyticsEvents.PERF_APP_STARTUP
                        TRACE_SCREEN_LOAD -> AnalyticsEvents.PERF_SCREEN_LOAD
                        else -> "performance_$traceName"
                    }
                    
                    analyticsManager.trackPerformanceEvent(
                        eventName = analyticsEventName,
                        durationMs = duration,
                        additionalParams = mapOf(
                            "trace_name" to traceName
                        )
                    )
                }
                
                Timber.d("Stopped trace: $traceName, duration: $duration ms")
            } else {
                Timber.w("Trace not found: $traceName")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error stopping trace: $traceName")
        }
    }
    
    /**
     * Add a metric to a trace
     * 
     * @param traceName The name of the trace
     * @param metricName The name of the metric
     * @param value The value of the metric
     */
    fun addTraceMetric(traceName: String, metricName: String, value: Long) {
        try {
            // Get the trace
            val trace = activeTraces[traceName]
            
            if (trace != null) {
                // Add the metric
                trace.putMetric(metricName, value)
                Timber.d("Added metric to trace: $traceName, metric: $metricName, value: $value")
            } else {
                Timber.w("Trace not found: $traceName")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error adding metric to trace: $traceName, metric: $metricName")
        }
    }
    
    /**
     * Add an attribute to a trace
     * 
     * @param traceName The name of the trace
     * @param attributeName The name of the attribute
     * @param value The value of the attribute
     */
    fun addTraceAttribute(traceName: String, attributeName: String, value: String) {
        try {
            // Get the trace
            val trace = activeTraces[traceName]
            
            if (trace != null) {
                // Add the attribute
                trace.putAttribute(attributeName, value)
                Timber.d("Added attribute to trace: $traceName, attribute: $attributeName, value: $value")
            } else {
                Timber.w("Trace not found: $traceName")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error adding attribute to trace: $traceName, attribute: $attributeName")
        }
    }
    
    /**
     * Record a custom metric
     * 
     * @param metricName The name of the metric
     * @param value The value of the metric
     */
    fun recordCustomMetric(metricName: String, value: Long) {
        try {
            // Get or create the metric list
            val metricList = customMetrics.getOrPut(metricName) { mutableListOf() }
            
            // Add the value
            metricList.add(value)
            
            Timber.d("Recorded custom metric: $metricName, value: $value")
        } catch (e: Exception) {
            Timber.e(e, "Error recording custom metric: $metricName")
        }
    }
    
    /**
     * Get statistics for a custom metric
     * 
     * @param metricName The name of the metric
     * @return The metric statistics
     */
    fun getMetricStatistics(metricName: String): MetricStatistics? {
        try {
            // Get the metric list
            val metricList = customMetrics[metricName]
            
            if (metricList != null && metricList.isNotEmpty()) {
                // Calculate statistics
                val count = metricList.size
                val sum = metricList.sum()
                val average = sum.toDouble() / count
                val min = metricList.minOrNull() ?: 0
                val max = metricList.maxOrNull() ?: 0
                
                // Calculate percentiles
                val sortedList = metricList.sorted()
                val p50Index = (count * 0.5).toInt()
                val p90Index = (count * 0.9).toInt()
                val p95Index = (count * 0.95).toInt()
                val p99Index = (count * 0.99).toInt()
                
                val p50 = sortedList.getOrNull(p50Index) ?: 0
                val p90 = sortedList.getOrNull(p90Index) ?: 0
                val p95 = sortedList.getOrNull(p95Index) ?: 0
                val p99 = sortedList.getOrNull(p99Index) ?: 0
                
                return MetricStatistics(
                    name = metricName,
                    count = count,
                    sum = sum,
                    average = average,
                    min = min,
                    max = max,
                    p50 = p50,
                    p90 = p90,
                    p95 = p95,
                    p99 = p99
                )
            }
            
            return null
        } catch (e: Exception) {
            Timber.e(e, "Error getting metric statistics: $metricName")
            return null
        }
    }
    
    /**
     * Get all metric statistics
     * 
     * @return Map of metric name to statistics
     */
    fun getAllMetricStatistics(): Map<String, MetricStatistics> {
        val result = mutableMapOf<String, MetricStatistics>()
        
        customMetrics.keys.forEach { metricName ->
            getMetricStatistics(metricName)?.let { statistics ->
                result[metricName] = statistics
            }
        }
        
        return result
    }
    
    /**
     * Clear all custom metrics
     */
    fun clearCustomMetrics() {
        customMetrics.clear()
        Timber.d("Cleared all custom metrics")
    }
    
    /**
     * Clear a specific custom metric
     * 
     * @param metricName The name of the metric to clear
     */
    fun clearCustomMetric(metricName: String) {
        customMetrics.remove(metricName)
        Timber.d("Cleared custom metric: $metricName")
    }
    
    /**
     * Measure the execution time of a block of code
     * 
     * @param operationName The name of the operation
     * @param trackInAnalytics Whether to track the operation in analytics
     * @param block The block of code to measure
     * @return The result of the block
     */
    inline fun <T> measureOperation(
        operationName: String,
        trackInAnalytics: Boolean = true,
        block: () -> T
    ): T {
        val startTime = SystemClock.elapsedRealtime()
        
        try {
            return block()
        } finally {
            val duration = SystemClock.elapsedRealtime() - startTime
            
            // Record custom metric
            recordCustomMetric(operationName, duration)
            
            // Track in analytics if requested
            if (trackInAnalytics) {
                analyticsManager.trackPerformanceEvent(
                    eventName = "operation_$operationName",
                    durationMs = duration
                )
            }
            
            Timber.d("Operation: $operationName, duration: $duration ms")
        }
    }
    
    /**
     * Data class for metric statistics
     */
    data class MetricStatistics(
        val name: String,
        val count: Int,
        val sum: Long,
        val average: Double,
        val min: Long,
        val max: Long,
        val p50: Long,
        val p90: Long,
        val p95: Long,
        val p99: Long
    )
    
    companion object {
        // Trace names
        const val TRACE_SCREEN_ANALYSIS = "screen_analysis"
        const val TRACE_DATABASE_QUERY = "database_query"
        const val TRACE_NOTIFICATION_PROCESSING = "notification_processing"
        const val TRACE_APP_STARTUP = "app_startup"
        const val TRACE_SCREEN_LOAD = "screen_load"
        
        // Metric names
        const val METRIC_MEMORY_USAGE = "memory_usage"
        const val METRIC_CPU_USAGE = "cpu_usage"
        const val METRIC_BATTERY_USAGE = "battery_usage"
        const val METRIC_NETWORK_REQUESTS = "network_requests"
        const val METRIC_DATABASE_OPERATIONS = "database_operations"
    }
}
