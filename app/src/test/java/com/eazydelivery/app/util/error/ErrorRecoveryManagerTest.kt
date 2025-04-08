package com.eazydelivery.app.util.error

import android.content.Context
import android.content.Intent
import com.eazydelivery.app.util.ConnectivityManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import java.net.SocketTimeoutException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ErrorRecoveryManagerTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockConnectivityManager: ConnectivityManager
    
    private lateinit var errorRecoveryManager: ErrorRecoveryManager
    
    @Before
    fun setup() {
        errorRecoveryManager = ErrorRecoveryManager(mockContext, mockConnectivityManager)
    }
    
    @Test
    fun `withRetry should return result when successful on first attempt`() = runTest {
        // Given
        val expected = "Success"
        
        // When
        val result = errorRecoveryManager.withRetry {
            expected
        }
        
        // Then
        assertEquals(expected, result)
    }
    
    @Test
    fun `withRetry should retry on exception and succeed`() = runTest {
        // Given
        val expected = "Success"
        var attempts = 0
        
        // When
        val result = errorRecoveryManager.withRetry {
            attempts++
            if (attempts == 1) {
                throw RuntimeException("First attempt failed")
            }
            expected
        }
        
        // Then
        assertEquals(expected, result)
        assertEquals(2, attempts)
    }
    
    @Test
    fun `withRetry should throw exception after max retries`() = runTest {
        // Given
        var attempts = 0
        
        // When/Then
        try {
            errorRecoveryManager.withRetry(maxRetries = 2) {
                attempts++
                throw RuntimeException("Attempt $attempts failed")
            }
            assert(false) { "Should have thrown an exception" }
        } catch (e: Exception) {
            assertEquals("Attempt 3 failed", e.message)
            assertEquals(3, attempts) // Initial attempt + 2 retries
        }
    }
    
    @Test
    fun `withNetworkRetry should only retry if network is available`() = runTest {
        // Given
        var attempts = 0
        val networkError = SocketTimeoutException("Network error")
        
        // Set up connectivity manager to report network available
        `when`(mockConnectivityManager.isNetworkAvailable()).thenReturn(true)
        
        // When/Then
        try {
            errorRecoveryManager.withNetworkRetry(maxRetries = 2) {
                attempts++
                throw networkError
            }
            assert(false) { "Should have thrown an exception" }
        } catch (e: Exception) {
            assertEquals(3, attempts) // Initial attempt + 2 retries
        }
    }
    
    @Test
    fun `withNetworkRetry should not retry if network is unavailable`() = runTest {
        // Given
        var attempts = 0
        val networkError = SocketTimeoutException("Network error")
        
        // Set up connectivity manager to report network unavailable
        `when`(mockConnectivityManager.isNetworkAvailable()).thenReturn(false)
        
        // When/Then
        try {
            errorRecoveryManager.withNetworkRetry(maxRetries = 2) {
                attempts++
                throw networkError
            }
            assert(false) { "Should have thrown an exception" }
        } catch (e: Exception) {
            assertEquals(1, attempts) // Only initial attempt, no retries
        }
    }
    
    @Test
    fun `openAppSettings should start activity with correct intent`() {
        // Given
        `when`(mockContext.packageName).thenReturn("com.eazydelivery.app")
        
        // When
        val result = errorRecoveryManager.openAppSettings()
        
        // Then
        assertTrue(result)
        verify(mockContext).startActivity(org.mockito.kotlin.any())
    }
    
    @Test
    fun `openNetworkSettings should start activity with correct intent`() {
        // Given
        // No specific setup needed
        
        // When
        val result = errorRecoveryManager.openNetworkSettings()
        
        // Then
        assertTrue(result)
        verify(mockContext).startActivity(org.mockito.kotlin.any())
    }
    
    @Test
    fun `isRecoverable should return correct value for different error types`() {
        // Network errors are recoverable
        assertTrue(errorRecoveryManager.isRecoverable(AppError.Network.NoConnection()))
        assertTrue(errorRecoveryManager.isRecoverable(AppError.Network.Timeout()))
        
        // Auth errors are recoverable
        assertTrue(errorRecoveryManager.isRecoverable(AppError.Api.AuthError()))
        
        // Some errors are not recoverable
        assertFalse(errorRecoveryManager.isRecoverable(AppError.Database.MigrationError()))
        assertFalse(errorRecoveryManager.isRecoverable(AppError.Feature.NotAvailable()))
        assertFalse(errorRecoveryManager.isRecoverable(AppError.Unexpected()))
    }
    
    @Test
    fun `getRecoveryAction should return correct action for different error types`() {
        // Network errors
        assertEquals(
            ErrorRecoveryManager.RecoveryAction.OpenNetworkSettings,
            errorRecoveryManager.getRecoveryAction(AppError.Network.NoConnection())
        )
        
        assertEquals(
            ErrorRecoveryManager.RecoveryAction.Retry,
            errorRecoveryManager.getRecoveryAction(AppError.Network.Timeout())
        )
        
        // Auth errors
        assertEquals(
            ErrorRecoveryManager.RecoveryAction.Relogin,
            errorRecoveryManager.getRecoveryAction(AppError.Api.AuthError())
        )
        
        // Permission errors
        assertEquals(
            ErrorRecoveryManager.RecoveryAction.OpenAppSettings,
            errorRecoveryManager.getRecoveryAction(AppError.Permission.PermanentlyDenied())
        )
        
        // Some errors have no recovery action
        assertEquals(
            null,
            errorRecoveryManager.getRecoveryAction(AppError.Feature.NotAvailable())
        )
    }
}
