package com.eazydelivery.app.ui.component

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

/**
 * Instrumented test for the PerformanceStatusView
 */
@RunWith(AndroidJUnit4::class)
class PerformanceStatusViewTest {
    
    private lateinit var context: Context
    private lateinit var performanceStatusView: PerformanceStatusView
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        performanceStatusView = PerformanceStatusView(context)
    }
    
    @Test
    fun testInitialValues() {
        // Initial values should be zero or default
        assertEquals(0f, performanceStatusView.cpuUsage, 0.01f)
        assertEquals(0f, performanceStatusView.memoryUsage, 0.01f)
        assertEquals(true, performanceStatusView.batteryOptimized)
    }
    
    @Test
    fun testSetCpuUsage() {
        // Set CPU usage to 50%
        performanceStatusView.cpuUsage = 50f
        assertEquals(50f, performanceStatusView.cpuUsage, 0.01f)
        
        // Set CPU usage to a value greater than 100% (should be clamped to 100%)
        performanceStatusView.cpuUsage = 150f
        assertEquals(100f, performanceStatusView.cpuUsage, 0.01f)
        
        // Set CPU usage to a negative value (should be clamped to 0%)
        performanceStatusView.cpuUsage = -10f
        assertEquals(0f, performanceStatusView.cpuUsage, 0.01f)
    }
    
    @Test
    fun testSetMemoryUsage() {
        // Set memory usage to 75%
        performanceStatusView.memoryUsage = 75f
        assertEquals(75f, performanceStatusView.memoryUsage, 0.01f)
        
        // Set memory usage to a value greater than 100% (should be clamped to 100%)
        performanceStatusView.memoryUsage = 120f
        assertEquals(100f, performanceStatusView.memoryUsage, 0.01f)
        
        // Set memory usage to a negative value (should be clamped to 0%)
        performanceStatusView.memoryUsage = -5f
        assertEquals(0f, performanceStatusView.memoryUsage, 0.01f)
    }
    
    @Test
    fun testSetBatteryOptimized() {
        // Set battery optimized to false
        performanceStatusView.batteryOptimized = false
        assertEquals(false, performanceStatusView.batteryOptimized)
        
        // Set battery optimized to true
        performanceStatusView.batteryOptimized = true
        assertEquals(true, performanceStatusView.batteryOptimized)
    }
    
    @Test
    fun testOnDraw() {
        // Create a mock Canvas
        val canvas = mock(Canvas::class.java)
        
        // Set values
        performanceStatusView.cpuUsage = 30f
        performanceStatusView.memoryUsage = 60f
        performanceStatusView.batteryOptimized = true
        
        // Call onDraw
        performanceStatusView.draw(canvas)
        
        // Verify that canvas.drawRoundRect was called (can't verify exact parameters easily)
        verify(canvas).drawRoundRect(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyFloat(),
            org.mockito.ArgumentMatchers.anyFloat(),
            org.mockito.ArgumentMatchers.any()
        )
    }
    
    @Test
    fun testMeasurement() {
        // Set a specific size
        val width = 300
        val height = 200
        
        // Measure with exact specs
        val widthMeasureSpec = android.view.View.MeasureSpec.makeMeasureSpec(
            width, android.view.View.MeasureSpec.EXACTLY
        )
        val heightMeasureSpec = android.view.View.MeasureSpec.makeMeasureSpec(
            height, android.view.View.MeasureSpec.EXACTLY
        )
        
        performanceStatusView.measure(widthMeasureSpec, heightMeasureSpec)
        
        // Check measured dimensions
        assertEquals(width, performanceStatusView.measuredWidth)
        assertEquals(height, performanceStatusView.measuredHeight)
    }
}
