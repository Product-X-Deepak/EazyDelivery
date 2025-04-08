# Performance Optimization Guide

This guide documents the performance optimizations implemented in the EazyDelivery app and provides guidelines for maintaining and improving performance.

## Table of Contents

1. [Overview](#overview)
2. [Screen Analysis Optimizations](#screen-analysis-optimizations)
3. [Database Optimizations](#database-optimizations)
4. [Background Service Optimizations](#background-service-optimizations)
5. [Memory Management](#memory-management)
6. [Performance Monitoring](#performance-monitoring)
7. [Testing and Validation](#testing-and-validation)
8. [Best Practices](#best-practices)

## Overview

Performance is critical for the EazyDelivery app, as it needs to run continuously in the background while consuming minimal resources. The app needs to quickly analyze delivery app screens, process notifications, and respond to user interactions without draining the battery or using excessive memory.

The optimizations implemented in the app focus on:

- Reducing CPU usage
- Minimizing memory consumption
- Optimizing battery usage
- Improving response times
- Enhancing overall app stability

## Screen Analysis Optimizations

The `ScreenAnalyzer` class has been optimized to improve performance when analyzing delivery app screens:

### Caching

```kotlin
// Cache for recent analysis results to avoid redundant processing
private val analysisCache = LruCache<String, AnalysisResult>(10)

// Timestamp of last analysis for each package
private val lastAnalysisTimestamp = mutableMapOf<String, Long>()

// Minimum time between analyses for the same package (in milliseconds)
private val minAnalysisInterval = 500L
```

- **Result Caching**: Analysis results are cached to avoid redundant processing of similar screens.
- **Throttling**: A minimum interval between analyses for the same package prevents excessive processing.

### Image Processing

```kotlin
private fun preprocessImage(bitmap: Bitmap): Bitmap {
    // Use a more efficient scaling method for better performance
    val matrix = Matrix()
    val scaleWidth = inputWidth.toFloat() / bitmap.width
    val scaleHeight = inputHeight.toFloat() / bitmap.height
    matrix.postScale(scaleWidth, scaleHeight)
    
    return Bitmap.createBitmap(
        bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
    ).also {
        // If the input bitmap is not the same as the output, recycle it to free memory
        if (it != bitmap && !bitmap.isRecycled) {
            // Only recycle if it's not a shared bitmap
            if (bitmap.isMutable) {
                bitmap.recycle()
            }
        }
    }
}
```

- **Efficient Scaling**: Uses more efficient scaling methods for image preprocessing.
- **Memory Management**: Recycles bitmaps when they're no longer needed to free memory.
- **Optimized Pixel Extraction**: Uses more efficient methods for extracting pixel data.

### Accessibility Tree Traversal

```kotlin
// First, try to find direct matches using accessibility APIs (faster)
for (buttonText in buttonTexts) {
    val nodes = rootNode.findAccessibilityNodeInfosByText(buttonText)
    for (node in nodes) {
        if (node.isClickable) {
            return node
        }
    }
}
```

- **Direct API Usage**: Uses the accessibility API's built-in search functionality first.
- **Early Termination**: Returns as soon as a match is found to avoid unnecessary processing.
- **Depth Limiting**: Limits the depth of tree traversal to avoid excessive processing.

## Database Optimizations

The database has been optimized for better performance:

### Indexes

```kotlin
// Add index on orders.timestamp for faster date-based queries
database.execSQL(
    "CREATE INDEX IF NOT EXISTS index_orders_timestamp ON orders(timestamp)"
)

// Add index on orders.platformName for faster platform-based queries
database.execSQL(
    "CREATE INDEX IF NOT EXISTS index_orders_platformName ON orders(platformName)"
)
```

- **Indexed Fields**: Critical fields used in WHERE clauses and JOIN conditions are indexed.
- **Composite Indexes**: Fields that are frequently queried together have composite indexes.

### Optimized Queries

```kotlin
@Query("""
    SELECT 
        platformName, 
        COUNT(*) as orderCount, 
        SUM(amount) as totalEarnings,
        AVG(amount) as averageEarning,
        MAX(amount) as highestEarning,
        MIN(amount) as lowestEarning
    FROM orders 
    WHERE timestamp BETWEEN :startDate AND :endDate
    GROUP BY platformName
""")
suspend fun getPlatformStatsForPeriod(startDate: String, endDate: String): List<PlatformStatEntity>
```

- **Single Query Optimization**: Uses single queries instead of multiple separate queries.
- **Projection Optimization**: Only selects the columns that are actually needed.
- **LIMIT Usage**: Uses LIMIT clauses to restrict the number of results when appropriate.

### Write-Ahead Logging

```kotlin
// Set journal mode to WAL for better performance
.setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
```

- **Write-Ahead Logging**: Uses WAL mode for better write performance and concurrency.
- **Transaction Batching**: Batches multiple write operations into single transactions.

### Database Maintenance

```kotlin
// Use the DatabaseIndexes utility to optimize the database
DatabaseIndexes.optimizeDatabase(db.openHelper.writableDatabase)
```

- **Regular ANALYZE**: Periodically runs ANALYZE to update statistics.
- **Index Rebuilding**: Rebuilds indexes to maintain performance.
- **Integrity Checks**: Runs integrity checks to ensure database health.
- **Old Data Cleanup**: Periodically cleans up old data to prevent database bloat.

## Background Service Optimizations

The background services have been optimized for better battery life:

### Battery-Aware Processing

```kotlin
// Adjust sampling interval based on battery level and charging state
currentSamplingIntervalMs = when {
    isCharging -> highBatterySamplingIntervalMs
    batteryLevel <= lowBatteryThreshold -> lowBatterySamplingIntervalMs
    batteryLevel <= mediumBatteryThreshold -> mediumBatterySamplingIntervalMs
    else -> highBatterySamplingIntervalMs
}
```

- **Adaptive Sampling**: Adjusts sampling intervals based on battery level and charging state.
- **Deferred Processing**: Defers non-critical operations when battery is low.
- **Charging Detection**: Performs more intensive operations when the device is charging.

### Wake Lock Management

```kotlin
// Acquire wake lock for critical operations
wakeLock = serviceOptimizer.acquireWakeLock("AccessibilityEvent", 10000) // 10 seconds timeout

try {
    // Critical operations
} finally {
    // Release wake lock
    serviceOptimizer.releaseWakeLock(wakeLock, "AccessibilityEvent")
}
```

- **Targeted Wake Locks**: Uses wake locks only for critical operations.
- **Timeout Limits**: Sets timeouts on wake locks to prevent battery drain.
- **Proper Release**: Ensures wake locks are always released, even in error cases.

### Memory Monitoring

```kotlin
// Check if we're in a low memory situation
val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
val memoryInfo = ActivityManager.MemoryInfo()
activityManager.getMemoryInfo(memoryInfo)

if (memoryInfo.lowMemory) {
    Timber.w("Device is in a low memory situation")
    
    // Take action to reduce memory usage
    clearCaches()
}
```

- **Memory Monitoring**: Monitors memory usage and responds to low memory conditions.
- **Cache Clearing**: Clears caches when memory is low to free up resources.
- **Adaptive Behavior**: Adjusts behavior based on available memory.

## Memory Management

The app implements several memory management optimizations:

### Bitmap Recycling

```kotlin
// Recycle the bitmap if it's no longer needed
if (!bitmap.isRecycled && bitmap.isMutable) {
    bitmap.recycle()
}
```

- **Bitmap Recycling**: Recycles bitmaps when they're no longer needed.
- **Efficient Bitmap Creation**: Uses efficient methods for creating and manipulating bitmaps.
- **Size Limiting**: Limits the size of bitmaps to reduce memory usage.

### Cache Management

```kotlin
// Clear operation durations cache
operationDurations.clear()

// Clear operation counts cache
operationCounts.clear()

// Clear app cache directory
val cacheDir = context.cacheDir
clearDirectory(cacheDir)
```

- **LRU Caches**: Uses LRU (Least Recently Used) caches to limit memory usage.
- **Cache Clearing**: Periodically clears caches to free up memory.
- **Size Limiting**: Limits the size of caches to prevent excessive memory usage.

### Memory Monitoring

```kotlin
// Get memory info
val runtime = Runtime.getRuntime()
val usedMemory = runtime.totalMemory() - runtime.freeMemory()
val maxMemory = runtime.maxMemory()
val memoryPercentage = (usedMemory.toFloat() / maxMemory.toFloat()) * 100

// Log memory usage
Timber.d("Memory usage: ${formatSize(usedMemory)}/${formatSize(maxMemory)} (${memoryPercentage.toInt()}%)")
```

- **Memory Usage Tracking**: Monitors memory usage and logs warnings if memory usage is high.
- **Garbage Collection Suggestions**: Suggests garbage collection when memory usage is very high.
- **Low Memory Handling**: Takes action to reduce memory usage when the device is in a low memory situation.

## Performance Monitoring

The app includes a comprehensive performance monitoring system:

### Operation Timing

```kotlin
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
```

- **Operation Timing**: Measures the execution time of operations.
- **Slow Operation Detection**: Logs warnings for operations that take longer than expected.
- **Statistics Collection**: Collects statistics on operation durations for analysis.

### Firebase Performance Monitoring

```kotlin
fun startTrace(traceName: String) {
    try {
        val trace = FirebasePerformance.getInstance().newTrace(traceName)
        trace.start()
        traces[traceName] = trace
        startTimes[traceName] = SystemClock.elapsedRealtime()
    } catch (e: Exception) {
        Timber.e(e, "Error starting trace: $traceName")
    }
}
```

- **Custom Traces**: Creates custom traces for critical operations.
- **Metric Collection**: Collects metrics for performance analysis.
- **Remote Monitoring**: Sends performance data to Firebase for remote monitoring.

### Performance Statistics

```kotlin
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
```

- **Statistical Analysis**: Calculates statistics on operation durations.
- **Percentile Calculation**: Calculates percentiles for more accurate performance analysis.
- **Reporting**: Generates reports on performance statistics.

## Testing and Validation

The app includes tools for testing and validating performance:

### Performance Test Utilities

```kotlin
inline fun measureExecutionTime(block: () -> Unit): Long {
    return measureTimeMillis { block() }
}

inline fun assertExecutionTime(timeLimit: Long, block: () -> Unit) {
    val executionTime = measureExecutionTime(block)
    Assert.assertTrue(
        "Execution time ($executionTime ms) exceeded time limit ($timeLimit ms)",
        executionTime <= timeLimit
    )
}
```

- **Execution Time Measurement**: Measures the execution time of code blocks.
- **Execution Time Assertions**: Asserts that code executes within a specified time limit.
- **Benchmarking**: Runs code multiple times and calculates average execution time.

### Performance Validation

```kotlin
fun validatePerformance(onComplete: (Map<String, ValidationResult>) -> Unit) {
    if (validationInProgress) {
        showToast("Performance validation already in progress")
        return
    }
    
    validationInProgress = true
    showToast("Starting performance validation...")
    
    validationScope.launch {
        try {
            // Start performance monitoring
            performanceMonitor.startMonitoring()
            
            // Run database performance tests
            validateDatabasePerformance()
            
            // Run screen analyzer performance tests
            validateScreenAnalyzerPerformance()
            
            // Run memory usage tests
            validateMemoryUsage()
            
            // Run battery usage tests
            validateBatteryUsage()
            
            // Generate validation report
            val report = generateValidationReport()
            
            // Save report to file
            saveReportToFile(report)
            
            // Stop performance monitoring
            performanceMonitor.stopMonitoring()
            
            // Notify completion
            withContext(Dispatchers.Main) {
                onComplete(validationResults)
            }
            
            showToast("Performance validation complete")
        } catch (e: Exception) {
            val appError = errorHandler.handleException("PerformanceValidator.validatePerformance", e)
            showToast("Performance validation failed: ${appError.message}")
        } finally {
            validationInProgress = false
        }
    }
}
```

- **Comprehensive Validation**: Validates multiple aspects of app performance.
- **Report Generation**: Generates detailed reports on validation results.
- **User Feedback**: Provides feedback to users on validation progress and results.

## Best Practices

To maintain and improve performance in the EazyDelivery app, follow these best practices:

### General

- **Profile Before Optimizing**: Always profile the app to identify performance bottlenecks before making optimizations.
- **Measure Impact**: Measure the impact of optimizations to ensure they actually improve performance.
- **Test on Real Devices**: Test performance on real devices, especially lower-end devices.
- **Monitor in Production**: Use Firebase Performance Monitoring to monitor performance in production.

### CPU Usage

- **Avoid Blocking the Main Thread**: Perform heavy operations on background threads.
- **Use Coroutines**: Use Kotlin coroutines for asynchronous operations.
- **Optimize Algorithms**: Use efficient algorithms and data structures.
- **Avoid Excessive Object Creation**: Reuse objects when possible to reduce garbage collection.

### Memory Usage

- **Recycle Bitmaps**: Recycle bitmaps when they're no longer needed.
- **Use WeakReferences**: Use WeakReferences for objects that can be recreated.
- **Limit Cache Sizes**: Use LRU caches with appropriate size limits.
- **Monitor Memory Usage**: Monitor memory usage and respond to low memory conditions.

### Battery Usage

- **Minimize Wake Locks**: Use wake locks only when necessary and release them promptly.
- **Batch Operations**: Batch operations to reduce the number of times the device wakes up.
- **Adapt to Battery Level**: Adjust behavior based on battery level and charging state.
- **Optimize Network Usage**: Minimize network requests and use efficient data formats.

### Database Usage

- **Use Indexes**: Add indexes for fields used in WHERE clauses and JOIN conditions.
- **Optimize Queries**: Use efficient queries and avoid unnecessary joins.
- **Batch Transactions**: Batch multiple write operations into single transactions.
- **Clean Up Old Data**: Periodically clean up old data to prevent database bloat.

By following these best practices, you can maintain and improve the performance of the EazyDelivery app.
