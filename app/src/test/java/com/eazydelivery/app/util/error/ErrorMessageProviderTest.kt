package com.eazydelivery.app.util.error

import android.content.Context
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.net.SocketTimeoutException
import kotlin.test.assertEquals

@RunWith(MockitoJUnitRunner::class)
class ErrorMessageProviderTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    private lateinit var errorMessageProvider: ErrorMessageProvider
    
    @Before
    fun setup() {
        // Setup mock string resources
        `when`(mockContext.getString(com.eazydelivery.app.R.string.error_no_internet_connection))
            .thenReturn("No internet connection available.")
        `when`(mockContext.getString(com.eazydelivery.app.R.string.error_connection_timeout))
            .thenReturn("Connection timed out.")
        `when`(mockContext.getString(com.eazydelivery.app.R.string.error_unexpected))
            .thenReturn("An unexpected error occurred.")
        `when`(mockContext.getString(com.eazydelivery.app.R.string.suggestion_check_internet))
            .thenReturn("Please check your internet connection and try again.")
        `when`(mockContext.getString(com.eazydelivery.app.R.string.suggestion_try_again_later))
            .thenReturn("Please try again later.")
        
        errorMessageProvider = ErrorMessageProvider(mockContext)
    }
    
    @Test
    fun `getMessage should return correct message for Network NoConnection`() {
        // Given
        val error = AppError.Network.NoConnection()
        
        // When
        val message = errorMessageProvider.getMessage(error)
        
        // Then
        assertEquals("No internet connection available.", message)
    }
    
    @Test
    fun `getMessage should return correct message for Network Timeout`() {
        // Given
        val error = AppError.Network.Timeout()
        
        // When
        val message = errorMessageProvider.getMessage(error)
        
        // Then
        assertEquals("Connection timed out.", message)
    }
    
    @Test
    fun `getMessageWithSuggestion should combine message and suggestion`() {
        // Given
        val error = AppError.Network.NoConnection()
        
        // When
        val message = errorMessageProvider.getMessageWithSuggestion(error)
        
        // Then
        assertEquals("No internet connection available. Please check your internet connection and try again.", message)
    }
    
    @Test
    fun `getMessage for Throwable should convert to AppError first`() {
        // Given
        val exception = SocketTimeoutException("Timeout")
        
        // When
        val message = errorMessageProvider.getMessage(exception)
        
        // Then
        assertEquals("Connection timed out.", message)
    }
    
    @Test
    fun `getMessageWithSuggestion for Throwable should convert to AppError first`() {
        // Given
        val exception = SocketTimeoutException("Timeout")
        
        // When
        val message = errorMessageProvider.getMessageWithSuggestion(exception)
        
        // Then
        assertEquals("Connection timed out. Please try again later.", message)
    }
    
    @Test
    fun `getMessage should use custom message from Unexpected error if available`() {
        // Given
        val error = AppError.Unexpected("Custom error message")
        
        // When
        val message = errorMessageProvider.getMessage(error)
        
        // Then
        assertEquals("Custom error message", message)
    }
    
    @Test
    fun `getMessage should use default message for Unexpected error if message is blank`() {
        // Given
        val error = AppError.Unexpected("")
        
        // When
        val message = errorMessageProvider.getMessage(error)
        
        // Then
        assertEquals("An unexpected error occurred.", message)
    }
}
