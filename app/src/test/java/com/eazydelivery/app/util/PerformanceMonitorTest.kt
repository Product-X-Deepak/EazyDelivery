package com.eazydelivery.app.util

import android.app.ActivityManager
import android.content.Context
import com.eazydelivery.app.util.error.AppError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PerformanceMonitorTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockErrorHandler: ErrorHandler
    
    @Mock
    private lateinit var mockActivityManager: ActivityManager
    
    @Mock
    private lateinit var mockMemoryInfo: ActivityManager.MemoryInfo
    
    @Mock
    private lateinit var mockCacheDir: File
    
    private lateinit var performanceMonitor: PerformanceMonitor
    
    @Before
    fun setup() {
        // Setup mock activity manager
        `when`(mockContext.getSystemService(Context.ACTIVITY_SERVICE)).thenReturn(mockActivityManager)
        `when`(mockActivityManager.getMemoryInfo(any())).thenAnswer {
            val memoryInfo = it.getArgument<ActivityManager.MemoryInfo>(0)
            memoryInfo.availMem = 1024L * 1024L * 100L // 100 MB
            memoryInfo.totalMem = 1024L * 1024L * 1024L // 1 GB
            memoryInfo.lowMemory = false
            null
        }
        
        // Setup mock cache directory
        `when`(mockContext.cacheDir).thenReturn(mockCacheDir)
        `when`(mockCacheDir.exists()).thenReturn(true)
        `when`(mockCacheDir.isDirectory).thenReturn(true)
        `when`(mockCacheDir.listFiles()).thenReturn(arrayOf())
        
        // Create performance monitor
        performanceMonitor = PerformanceMonitor(mockContext, mockErrorHandler)
    }
    
    @Test
    fun `startMonitoring should not throw exceptions`() = runTest {
        // When/Then - Just verify it doesn't throw an exception
        performanceMonitor.startMonitoring()
    }
    
    @Test
    fun `stopMonitoring should update monitoring flag`() {
        // Given
        performanceMonitor.startMonitoring()
        
        // When
        performanceMonitor.stopMonitoring()
        
        // Then - Use reflection to check the private field
        val field = PerformanceMonitor::class.java.getDeclaredField("monitoringEnabled")
        field.isAccessible = true
        val monitoringEnabled = field.getBoolean(performanceMonitor)
        
        assertEquals(false, monitoringEnabled)
    }
    
    @Test
    fun `measureOperation should record operation duration`() {
        // Given
        val operationName = "TestOperation"
        
        // When
        performanceMonitor.measureOperation(operationName) {
            // Simulate some work
            Thread.sleep(10)
        }
        
        // Then
        val stats = performanceMonitor.getOperationStats(operationName)
        
        assertEquals(operationName, stats.operationName)
        assertEquals(1, stats.count)
        assertTrue(stats.averageDuration > 0)
    }
    
    @Test
    fun `getOperationStats should return stats for an operation`() {
        // Given
        val operationName = "TestOperation"
        
        // Perform the operation multiple times
        repeat(5) {
            performanceMonitor.measureOperation(operationName) {
                // Simulate some work
                Thread.sleep(10)
            }
        }
        
        // When
        val stats = performanceMonitor.getOperationStats(operationName)
        
        // Then
        assertEquals(operationName, stats.operationName)
        assertEquals(5, stats.count)
        assertTrue(stats.averageDuration > 0)
        assertTrue(stats.minDuration > 0)
        assertTrue(stats.maxDuration > 0)
        assertTrue(stats.p90Duration > 0)
    }
    
    @Test
    fun `getAllOperationStats should return stats for all operations`() {
        // Given
        val operations = listOf("Operation1", "Operation2", "Operation3")
        
        // Perform multiple operations
        operations.forEach { operationName ->
            repeat(3) {
                performanceMonitor.measureOperation(operationName) {
                    // Simulate some work
                    Thread.sleep(10)
                }
            }
        }
        
        // When
        val allStats = performanceMonitor.getAllOperationStats()
        
        // Then
        assertEquals(operations.size, allStats.size)
        operations.forEach { operationName ->
            val stats = allStats.find { it.operationName == operationName }
            assertNotNull(stats)
            assertEquals(3, stats.count)
        }
    }
    
    @Test
    fun `getMemoryUsage should return memory usage information`() {
        // When
        val memoryUsage = performanceMonitor.getMemoryUsage()
        
        // Then
        assertNotNull(memoryUsage)
        assertTrue(memoryUsage.usedMemory >= 0)
        assertTrue(memoryUsage.maxMemory > 0)
        assertTrue(memoryUsage.percentage >= 0)
        assertNotNull(memoryUsage.formattedUsed)
        assertNotNull(memoryUsage.formattedMax)
    }
    
    @Test
    fun `formatSize should format bytes correctly`() {
        // Use reflection to access the private method
        val method = PerformanceMonitor::class.java.getDeclaredMethod("formatSize", Long::class.java)
        method.isAccessible = true
        
        // Test various sizes
        assertEquals("10.0 B", method.invoke(performanceMonitor, 10L))
        assertEquals("1.0 KB", method.invoke(performanceMonitor, 1024L))
        assertEquals("1.0 MB", method.invoke(performanceMonitor, 1024L * 1024L))
        assertEquals("1.0 GB", method.invoke(performanceMonitor, 1024L * 1024L * 1024L))
        assertEquals("1.0 TB", method.invoke(performanceMonitor, 1024L * 1024L * 1024L * 1024L))
    }
    
    @Test
    fun `calculatePercentile should calculate percentiles correctly`() {
        // Use reflection to access the private method
        val method = PerformanceMonitor::class.java.getDeclaredMethod(
            "calculatePercentile",
            List::class.java,
            Int::class.java
        )
        method.isAccessible = true
        
        // Test with a sorted list
        val sortedList = listOf(10L, 20L, 30L, 40L, 50L, 60L, 70L, 80L, 90L, 100L)
        
        // 50th percentile should be the median
        assertEquals(50L, method.invoke(performanceMonitor, sortedList, 50))
        
        // 90th percentile should be the 9th element (0-based index 8)
        assertEquals(90L, method.invoke(performanceMonitor, sortedList, 90))
        
        // 100th percentile should be the maximum
        assertEquals(100L, method.invoke(performanceMonitor, sortedList, 100))
        
        // 0th percentile should be the minimum
        assertEquals(10L, method.invoke(performanceMonitor, sortedList, 0))
    }
    
    @Test
    fun `startTrace and stopTrace should handle Firebase traces`() {
        // Given
        val traceName = "TestTrace"
        
        // When
        performanceMonitor.startTrace(traceName)
        performanceMonitor.stopTrace(traceName)
        
        // Then - No exceptions should be thrown
    }
    
    @Test
    fun `incrementMetric should handle Firebase trace metrics`() {
        // Given
        val traceName = "TestTrace"
        val metricName = "TestMetric"
        
        // When
        performanceMonitor.startTrace(traceName)
        performanceMonitor.incrementMetric(traceName, metricName)
        performanceMonitor.stopTrace(traceName)
        
        // Then - No exceptions should be thrown
    }
}
