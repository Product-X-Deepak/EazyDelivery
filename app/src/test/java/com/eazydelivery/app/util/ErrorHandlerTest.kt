package com.eazydelivery.app.util

import com.eazydelivery.app.util.error.AppError
import com.eazydelivery.app.util.error.ErrorMessageProvider
import com.eazydelivery.app.util.error.ErrorRecoveryManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import java.net.SocketTimeoutException

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ErrorHandlerTest {

    @Mock
    private lateinit var mockErrorMessageProvider: ErrorMessageProvider

    @Mock
    private lateinit var mockErrorRecoveryManager: ErrorRecoveryManager

    private lateinit var errorHandler: ErrorHandler

    @Before
    fun setup() {
        // Setup mock responses
        `when`(mockErrorMessageProvider.getMessage(any<Throwable>())).thenReturn("Error message")
        `when`(mockErrorMessageProvider.getMessageWithSuggestion(any<Throwable>())).thenReturn("Error message with suggestion")

        errorHandler = ErrorHandler(mockErrorMessageProvider, mockErrorRecoveryManager)
    }

    @Test
    fun `executeSafely returns result when no exception occurs`() {
        // Given
        val expected = "Success"

        // When
        val result = errorHandler.executeSafely("test", "Default") {
            expected
        }

        // Then
        assert(result == expected)
    }

    @Test
    fun `executeSafely returns default value when exception occurs`() {
        // Given
        val defaultValue = "Default"

        // When
        val result = errorHandler.executeSafely("test", defaultValue) {
            throw RuntimeException("Test exception")
        }

        // Then
        assert(result == defaultValue)
    }

    @Test
    fun `executeCoroutineSafely calls onSuccess when no exception occurs`() = runTest {
        // Given
        val expected = "Success"
        val onSuccess: (String) -> Unit = mock()
        val onError: (Throwable) -> Unit = mock()

        // When
        errorHandler.executeCoroutineSafely(
            "test",
            "Default",
            { expected },
            onSuccess,
            onError
        )

        // Then
        verify(onSuccess).invoke(expected)
        verifyNoInteractions(onError)
    }

    @Test
    fun `executeCoroutineSafely calls onError when exception occurs`() = runTest {
        // Given
        val exception = RuntimeException("Test exception")
        val onSuccess: (String) -> Unit = mock()
        val onError: (Throwable) -> Unit = mock()

        // When
        errorHandler.executeCoroutineSafely(
            "test",
            "Default",
            { throw exception },
            onSuccess,
            onError
        )

        // Then
        verify(onError).invoke(exception)
        verifyNoInteractions(onSuccess)
    }

    @Test
    fun `handleException should convert throwable to AppError`() {
        // Given
        val exception = SocketTimeoutException("Connection timed out")

        // When
        val result = errorHandler.handleException("test", exception)

        // Then
        assert(result is AppError.Network.Timeout)
    }

    @Test
    fun `getUserFriendlyErrorMessage should delegate to ErrorMessageProvider`() {
        // Given
        val exception = RuntimeException("Test exception")

        // When
        val result = errorHandler.getUserFriendlyErrorMessage(exception)

        // Then
        assert(result == "Error message")
        verify(mockErrorMessageProvider).getMessage(any<Throwable>())
    }

    @Test
    fun `getUserFriendlyErrorMessageWithSuggestion should delegate to ErrorMessageProvider`() {
        // Given
        val exception = RuntimeException("Test exception")

        // When
        val result = errorHandler.getUserFriendlyErrorMessageWithSuggestion(exception)

        // Then
        assert(result == "Error message with suggestion")
        verify(mockErrorMessageProvider).getMessageWithSuggestion(any<Throwable>())
    }

    @Test
    fun `getRecoveryAction should delegate to ErrorRecoveryManager`() {
        // Given
        val exception = RuntimeException("Test exception")
        val recoveryAction = ErrorRecoveryManager.RecoveryAction.Retry
        `when`(mockErrorRecoveryManager.getRecoveryAction(any())).thenReturn(recoveryAction)

        // When
        val result = errorHandler.getRecoveryAction(exception)

        // Then
        assert(result == recoveryAction)
        verify(mockErrorRecoveryManager).getRecoveryAction(any())
    }
}
