package com.eazydelivery.app.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.eazydelivery.app.service.ServiceOptimizer
import com.eazydelivery.app.ui.component.PerformanceStatusView
import com.eazydelivery.app.util.error.AppError
import com.eazydelivery.app.util.error.ErrorHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * Unit test for the PerformanceStatusManager
 */
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class PerformanceStatusManagerTest {
    
    @Mock
    private lateinit var context: Context
    
    @Mock
    private lateinit var errorHandler: ErrorHandler
    
    @Mock
    private lateinit var serviceOptimizer: ServiceOptimizer
    
    @Mock
    private lateinit var performanceStatusView: PerformanceStatusView
    
    @Mock
    private lateinit var handler: Handler
    
    private lateinit var performanceStatusManager: PerformanceStatusManager
    
    private val testDispatcher = TestCoroutineDispatcher()
    
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        // Create the performance status manager with mocked dependencies
        performanceStatusManager = PerformanceStatusManager(
            context = context,
            errorHandler = errorHandler,
            serviceOptimizer = serviceOptimizer
        )
        
        // Mock the main handler
        val field = PerformanceStatusManager::class.java.getDeclaredField("mainHandler")
        field.isAccessible = true
        field.set(performanceStatusManager, handler)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }
    
    @Test
    fun `startMonitoring should set monitoringEnabled to true`() = testDispatcher.runBlockingTest {
        // Given
        val field = PerformanceStatusManager::class.java.getDeclaredField("monitoringEnabled")
        field.isAccessible = true
        
        // When
        performanceStatusManager.startMonitoring(performanceStatusView)
        
        // Then
        assert(field.getBoolean(performanceStatusManager))
    }
    
    @Test
    fun `stopMonitoring should set monitoringEnabled to false`() = testDispatcher.runBlockingTest {
        // Given
        val field = PerformanceStatusManager::class.java.getDeclaredField("monitoringEnabled")
        field.isAccessible = true
        field.setBoolean(performanceStatusManager, true)
        
        // When
        performanceStatusManager.stopMonitoring()
        
        // Then
        assert(!field.getBoolean(performanceStatusManager))
    }
    
    @Test
    fun `updatePerformanceStatus should update the view`() = testDispatcher.runBlockingTest {
        // Given
        val cpuUsage = 30f
        val memoryUsage = 50f
        val batteryOptimized = true
        
        // Mock the methods that get performance metrics
        val cpuMethod = PerformanceStatusManager::class.java.getDeclaredMethod("getCpuUsage")
        cpuMethod.isAccessible = true
        val memoryMethod = PerformanceStatusManager::class.java.getDeclaredMethod("getMemoryUsage")
        memoryMethod.isAccessible = true
        val batteryMethod = PerformanceStatusManager::class.java.getDeclaredMethod("isBatteryOptimized")
        batteryMethod.isAccessible = true
        
        // Use reflection to replace the methods with mocked versions
        val cpuField = PerformanceStatusManager::class.java.getDeclaredField("getCpuUsage")
        cpuField.isAccessible = true
        cpuField.set(performanceStatusManager) { cpuUsage }
        
        val memoryField = PerformanceStatusManager::class.java.getDeclaredField("getMemoryUsage")
        memoryField.isAccessible = true
        memoryField.set(performanceStatusManager) { memoryUsage }
        
        val batteryField = PerformanceStatusManager::class.java.getDeclaredField("isBatteryOptimized")
        batteryField.isAccessible = true
        batteryField.set(performanceStatusManager) { batteryOptimized }
        
        // Mock the service optimizer
        `when`(serviceOptimizer.isOptimizationEnabled()).thenReturn(batteryOptimized)
        
        // When
        val updateMethod = PerformanceStatusManager::class.java.getDeclaredMethod("updatePerformanceStatus")
        updateMethod.isAccessible = true
        updateMethod.invoke(performanceStatusManager)
        
        // Then
        verify(handler).post(org.mockito.ArgumentMatchers.any())
    }
    
    @Test
    fun `updatePerformanceStatus should handle exceptions`() = testDispatcher.runBlockingTest {
        // Given
        val exception = RuntimeException("Test exception")
        
        // Mock the methods that get performance metrics to throw an exception
        val cpuMethod = PerformanceStatusManager::class.java.getDeclaredMethod("getCpuUsage")
        cpuMethod.isAccessible = true
        val cpuField = PerformanceStatusManager::class.java.getDeclaredField("getCpuUsage")
        cpuField.isAccessible = true
        cpuField.set(performanceStatusManager) { throw exception }
        
        // Mock the error handler
        `when`(errorHandler.handleException(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any()
        )).thenReturn(AppError.Unexpected("Test exception"))
        
        // When
        val updateMethod = PerformanceStatusManager::class.java.getDeclaredMethod("updatePerformanceStatus")
        updateMethod.isAccessible = true
        updateMethod.invoke(performanceStatusManager)
        
        // Then
        verify(errorHandler).handleException(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any()
        )
    }
}
