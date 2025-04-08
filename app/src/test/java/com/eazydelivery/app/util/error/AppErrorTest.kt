package com.eazydelivery.app.util.error

import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppErrorTest {
    
    @Test
    fun `from should map UnknownHostException to Network NoConnection`() {
        // Given
        val exception = UnknownHostException("No internet")
        
        // When
        val appError = AppError.from(exception)
        
        // Then
        assertTrue(appError is AppError.Network.NoConnection)
        assertEquals(exception, appError.cause)
    }
    
    @Test
    fun `from should map SocketTimeoutException to Network Timeout`() {
        // Given
        val exception = SocketTimeoutException("Connection timed out")
        
        // When
        val appError = AppError.from(exception)
        
        // Then
        assertTrue(appError is AppError.Network.Timeout)
        assertEquals(exception, appError.cause)
    }
    
    @Test
    fun `from should map ConnectException to Network ServerUnreachable`() {
        // Given
        val exception = ConnectException("Failed to connect")
        
        // When
        val appError = AppError.from(exception)
        
        // Then
        assertTrue(appError is AppError.Network.ServerUnreachable)
        assertEquals(exception, appError.cause)
    }
    
    @Test
    fun `from should map IOException to Network Generic`() {
        // Given
        val exception = IOException("IO error")
        
        // When
        val appError = AppError.from(exception)
        
        // Then
        assertTrue(appError is AppError.Network.Generic)
        assertEquals(exception, appError.cause)
    }
    
    @Test
    fun `from should return the same AppError if already an AppError`() {
        // Given
        val originalError = AppError.Api.AuthError("Auth failed")
        
        // When
        val appError = AppError.from(originalError)
        
        // Then
        assertEquals(originalError, appError)
    }
    
    @Test
    fun `from should map unknown exceptions to Unexpected`() {
        // Given
        val exception = IllegalArgumentException("Invalid argument")
        
        // When
        val appError = AppError.from(exception)
        
        // Then
        assertTrue(appError is AppError.Unexpected)
        assertEquals(exception, appError.cause)
    }
}
