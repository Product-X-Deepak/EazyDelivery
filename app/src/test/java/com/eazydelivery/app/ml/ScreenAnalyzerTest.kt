package com.eazydelivery.app.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.LruCache
import android.view.accessibility.AccessibilityNodeInfo
import com.eazydelivery.app.util.ErrorHandler
import com.eazydelivery.app.util.PerformanceTestUtils
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(MockitoJUnitRunner::class)
class ScreenAnalyzerTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockErrorHandler: ErrorHandler
    
    @Mock
    private lateinit var mockBitmap: Bitmap
    
    @Mock
    private lateinit var mockRootNode: AccessibilityNodeInfo
    
    private lateinit var screenAnalyzer: ScreenAnalyzer
    
    @Before
    fun setup() {
        // Setup mock bitmap
        `when`(mockBitmap.width).thenReturn(1080)
        `when`(mockBitmap.height).thenReturn(1920)
        `when`(mockBitmap.isMutable).thenReturn(true)
        
        // Setup mock context
        val mockFile = mock(File::class.java)
        val mockFileInputStream = mock(FileInputStream::class.java)
        val mockFileChannel = mock(FileChannel::class.java)
        val mockByteBuffer = mock(ByteBuffer::class.java)
        
        `when`(mockContext.assets).thenReturn(mock(android.content.res.AssetManager::class.java))
        `when`(mockContext.assets.open(any())).thenReturn(ByteArrayInputStream(ByteArray(100)))
        `when`(mockFile.exists()).thenReturn(true)
        `when`(mockFileInputStream.channel).thenReturn(mockFileChannel)
        `when`(mockFileChannel.map(any(), eq(0L), any())).thenReturn(mockByteBuffer)
        
        // Create a spy of ScreenAnalyzer to avoid actual TensorFlow initialization
        screenAnalyzer = spy(ScreenAnalyzer(mockContext, mockErrorHandler))
    }
    
    @Test
    fun `analyzeScreen should use cached result for repeated calls`() {
        // Given
        val packageName = "com.test.package"
        val mockResult = ScreenAnalyzer.AnalysisResult(
            acceptButtonFound = true,
            acceptButtonNode = null,
            confidence = 0.9f,
            needsConfirmation = false
        )
        
        // Mock the internal methods to return a known result
        `when`(screenAnalyzer.analyzeScreen(mockBitmap, packageName, mockRootNode)).thenCallRealMethod()
        
        // Use reflection to set the cache
        val cacheField = ScreenAnalyzer::class.java.getDeclaredField("analysisCache")
        cacheField.isAccessible = true
        val cache = LruCache<String, ScreenAnalyzer.AnalysisResult>(10)
        cache.put(packageName, mockResult)
        cacheField.set(screenAnalyzer, cache)
        
        // Set the last analysis timestamp
        val timestampField = ScreenAnalyzer::class.java.getDeclaredField("lastAnalysisTimestamp")
        timestampField.isAccessible = true
        val timestamps = mutableMapOf<String, Long>()
        timestamps[packageName] = System.currentTimeMillis()
        timestampField.set(screenAnalyzer, timestamps)
        
        // When
        val result = screenAnalyzer.analyzeScreen(mockBitmap, packageName, mockRootNode)
        
        // Then
        assertEquals(mockResult, result)
    }
    
    @Test
    fun `analyzeScreen should process new package`() {
        // Given
        val packageName = "com.new.package"
        
        // Mock the internal methods to avoid actual processing
        `when`(screenAnalyzer.analyzeScreen(mockBitmap, packageName, mockRootNode)).thenCallRealMethod()
        
        // When/Then - Just verify it doesn't throw an exception
        screenAnalyzer.analyzeScreen(mockBitmap, packageName, mockRootNode)
    }
    
    @Test
    fun `findAcceptButton should use direct API first`() {
        // Given
        val packageName = "com.test.package"
        val mockNode = mock(AccessibilityNodeInfo::class.java)
        val mockNodes = listOf(mockNode)
        
        // Mock the accessibility API
        `when`(mockRootNode.findAccessibilityNodeInfosByText(any())).thenReturn(mockNodes)
        `when`(mockNode.isClickable).thenReturn(true)
        
        // Use reflection to access the private method
        val method = ScreenAnalyzer::class.java.getDeclaredMethod(
            "findAcceptButton",
            AccessibilityNodeInfo::class.java,
            String::class.java
        )
        method.isAccessible = true
        
        // When
        val result = method.invoke(screenAnalyzer, mockRootNode, packageName) as AccessibilityNodeInfo?
        
        // Then
        assertNotNull(result)
        assertEquals(mockNode, result)
        
        // Verify that the direct API was called
        verify(mockRootNode, times(1)).findAccessibilityNodeInfosByText(any())
    }
    
    @Test
    fun `performance - analyzeScreen should be faster with caching`() {
        // Given
        val packageName = "com.test.package"
        
        // Mock the internal methods to avoid actual processing
        `when`(screenAnalyzer.analyzeScreen(mockBitmap, packageName, mockRootNode)).thenCallRealMethod()
        
        // First call to populate the cache
        screenAnalyzer.analyzeScreen(mockBitmap, packageName, mockRootNode)
        
        // When/Then
        PerformanceTestUtils.assertExecutionTime(50) {
            // Second call should use the cache and be very fast
            screenAnalyzer.analyzeScreen(mockBitmap, packageName, mockRootNode)
        }
    }
    
    @Test
    fun `performance - findAcceptButton should be faster with direct API`() {
        // Given
        val packageName = "com.test.package"
        val mockNode = mock(AccessibilityNodeInfo::class.java)
        val mockNodes = listOf(mockNode)
        
        // Mock the accessibility API
        `when`(mockRootNode.findAccessibilityNodeInfosByText(any())).thenReturn(mockNodes)
        `when`(mockNode.isClickable).thenReturn(true)
        
        // Use reflection to access the private method
        val method = ScreenAnalyzer::class.java.getDeclaredMethod(
            "findAcceptButton",
            AccessibilityNodeInfo::class.java,
            String::class.java
        )
        method.isAccessible = true
        
        // When/Then
        PerformanceTestUtils.assertExecutionTime(50) {
            method.invoke(screenAnalyzer, mockRootNode, packageName)
        }
    }
}
