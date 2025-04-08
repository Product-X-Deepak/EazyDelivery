package com.eazydelivery.app.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.Assert
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

/**
 * Utility class for performance testing
 */
object PerformanceTestUtils {
    
    /**
     * Measures the execution time of a block of code
     * 
     * @param block The block of code to measure
     * @return The execution time in milliseconds
     */
    inline fun measureExecutionTime(block: () -> Unit): Long {
        return measureTimeMillis { block() }
    }
    
    /**
     * Asserts that a block of code executes within a specified time limit
     * 
     * @param timeLimit The time limit in milliseconds
     * @param block The block of code to measure
     */
    inline fun assertExecutionTime(timeLimit: Long, block: () -> Unit) {
        val executionTime = measureExecutionTime(block)
        Assert.assertTrue(
            "Execution time ($executionTime ms) exceeded time limit ($timeLimit ms)",
            executionTime <= timeLimit
        )
    }
    
    /**
     * Runs a block of code multiple times and returns the average execution time
     * 
     * @param iterations The number of iterations to run
     * @param warmupIterations The number of warmup iterations to run (not included in the average)
     * @param block The block of code to measure
     * @return The average execution time in milliseconds
     */
    inline fun benchmarkExecutionTime(
        iterations: Int = 10,
        warmupIterations: Int = 3,
        block: () -> Unit
    ): Double {
        // Run warmup iterations
        repeat(warmupIterations) {
            block()
        }
        
        // Run measured iterations
        val executionTimes = mutableListOf<Long>()
        repeat(iterations) {
            val executionTime = measureExecutionTime(block)
            executionTimes.add(executionTime)
        }
        
        // Calculate average execution time
        return executionTimes.average()
    }
    
    /**
     * Compares the execution time of two blocks of code
     * 
     * @param iterations The number of iterations to run
     * @param warmupIterations The number of warmup iterations to run (not included in the average)
     * @param block1 The first block of code to measure
     * @param block2 The second block of code to measure
     * @return The ratio of the average execution time of block1 to block2
     */
    inline fun compareExecutionTime(
        iterations: Int = 10,
        warmupIterations: Int = 3,
        block1: () -> Unit,
        block2: () -> Unit
    ): Double {
        val executionTime1 = benchmarkExecutionTime(iterations, warmupIterations, block1)
        val executionTime2 = benchmarkExecutionTime(iterations, warmupIterations, block2)
        
        return executionTime1 / executionTime2
    }
    
    /**
     * Asserts that a block of code is faster than another block of code by a specified factor
     * 
     * @param factor The factor by which block2 should be faster than block1
     * @param iterations The number of iterations to run
     * @param warmupIterations The number of warmup iterations to run (not included in the average)
     * @param block1 The first block of code to measure
     * @param block2 The second block of code to measure
     */
    inline fun assertFaster(
        factor: Double,
        iterations: Int = 10,
        warmupIterations: Int = 3,
        block1: () -> Unit,
        block2: () -> Unit
    ) {
        val ratio = compareExecutionTime(iterations, warmupIterations, block1, block2)
        Assert.assertTrue(
            "Block2 is not faster than Block1 by a factor of $factor (actual ratio: $ratio)",
            ratio >= factor
        )
    }
    
    /**
     * Measures memory usage before and after a block of code
     * 
     * @param block The block of code to measure
     * @return The memory usage in bytes (positive value means memory was allocated, negative means it was freed)
     */
    inline fun measureMemoryUsage(block: () -> Unit): Long {
        System.gc() // Request garbage collection before measurement
        
        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        block()
        System.gc() // Request garbage collection after measurement
        
        val finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        return finalMemory - initialMemory
    }
    
    /**
     * Asserts that a block of code uses less than a specified amount of memory
     * 
     * @param memoryLimit The memory limit in bytes
     * @param block The block of code to measure
     */
    inline fun assertMemoryUsage(memoryLimit: Long, block: () -> Unit) {
        val memoryUsage = measureMemoryUsage(block)
        Assert.assertTrue(
            "Memory usage ($memoryUsage bytes) exceeded memory limit ($memoryLimit bytes)",
            memoryUsage <= memoryLimit
        )
    }
    
    /**
     * Simulates a high CPU load for a specified duration
     * 
     * @param durationMs The duration in milliseconds
     */
    suspend fun simulateHighCpuLoad(durationMs: Long) {
        withContext(Dispatchers.Default) {
            val endTime = System.currentTimeMillis() + durationMs
            while (System.currentTimeMillis() < endTime) {
                // Perform CPU-intensive operations
                var result = 0.0
                for (i in 1..1000) {
                    result += Math.sin(i.toDouble()) * Math.cos(i.toDouble())
                }
            }
        }
    }
    
    /**
     * Simulates a high memory load by allocating a specified amount of memory
     * 
     * @param sizeInMb The amount of memory to allocate in megabytes
     * @return The allocated memory (to prevent it from being garbage collected)
     */
    fun simulateHighMemoryLoad(sizeInMb: Int): List<ByteArray> {
        val result = mutableListOf<ByteArray>()
        val sizeInBytes = sizeInMb * 1024 * 1024
        
        // Allocate memory in chunks to avoid OutOfMemoryError
        val chunkSize = 1024 * 1024 // 1 MB
        val chunks = sizeInBytes / chunkSize
        
        repeat(chunks) {
            result.add(ByteArray(chunkSize))
        }
        
        return result
    }
    
    /**
     * Formats a size in bytes to a human-readable string
     * 
     * @param bytes The size in bytes
     * @return A human-readable string
     */
    fun formatSize(bytes: Long): String {
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
     * Formats a duration in milliseconds to a human-readable string
     * 
     * @param millis The duration in milliseconds
     * @return A human-readable string
     */
    fun formatDuration(millis: Long): String {
        return when {
            millis < 1 -> "< 1 ms"
            millis < 1000 -> "$millis ms"
            millis < 60000 -> String.format("%.2f s", millis / 1000.0)
            else -> String.format(
                "%d min %d s",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) % 60
            )
        }
    }
}
