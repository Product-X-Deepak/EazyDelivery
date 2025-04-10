package com.eazydelivery.app.util

import com.eazydelivery.app.util.error.AppError
import com.eazydelivery.app.util.error.RecoveryAction
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Mock implementation of ErrorHandler for testing
 */
class MockErrorHandler : ErrorHandler() {

    /**
     * Creates a CoroutineExceptionHandler that logs errors
     */
    override fun createCoroutineExceptionHandler(
        tag: String,
        onError: ((Throwable) -> Unit)?
    ): CoroutineExceptionHandler {
        return CoroutineExceptionHandler { _, throwable ->
            println("[$tag] Error: ${throwable.message}")
            onError?.invoke(throwable)
        }
    }

    /**
     * Handles exceptions by logging
     */
    override fun handleException(tag: String, throwable: Throwable, customMessage: String?): AppError {
        val message = customMessage ?: throwable.message ?: "Unknown error"
        println("[$tag] Error: $message")
        return AppError.from(throwable)
    }

    /**
     * Creates a safe coroutine scope with error handling
     */
    override fun createSafeCoroutineScope(tag: String): CoroutineScope {
        return CoroutineScope(
            SupervisorJob() +
            Dispatchers.Default +
            createCoroutineExceptionHandler(tag)
        )
    }

    /**
     * Executes a block of code safely, catching and handling any exceptions
     */
    override fun <T> executeSafely(
        tag: String,
        defaultValue: T,
        block: () -> T
    ): T {
        return try {
            block()
        } catch (e: Exception) {
            handleException(tag, e)
            defaultValue
        }
    }

    /**
     * Gets a user-friendly error message based on the exception type
     */
    override fun getUserFriendlyErrorMessage(throwable: Throwable): String {
        return throwable.message ?: "An error occurred"
    }

    /**
     * Gets a recovery action for the given throwable
     */
    override fun getRecoveryAction(throwable: Throwable): RecoveryAction? {
        return null
    }

    /**
     * Handles an error with recovery options
     */
    override fun handleErrorWithRecovery(
        tag: String,
        throwable: Throwable,
        onRecoveryAction: (RecoveryAction) -> Unit
    ): AppError {
        return handleException(tag, throwable)
    }
}
