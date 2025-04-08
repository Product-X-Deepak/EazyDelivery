package com.eazydelivery.app.util

import com.eazydelivery.app.util.error.AppError
import com.eazydelivery.app.util.error.ErrorMessageProvider
import com.eazydelivery.app.util.error.ErrorRecoveryManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized error handling utility
 */
@Singleton
class ErrorHandler @Inject constructor(
    private val errorMessageProvider: ErrorMessageProvider,
    private val errorRecoveryManager: ErrorRecoveryManager
) {

    /**
     * Creates a CoroutineExceptionHandler that logs errors and reports to Crashlytics
     */
    fun createCoroutineExceptionHandler(
        tag: String,
        onError: ((Throwable) -> Unit)? = null
    ): CoroutineExceptionHandler {
        return CoroutineExceptionHandler { _, throwable ->
            handleException(tag, throwable)
            onError?.invoke(throwable)
        }
    }

    /**
     * Handles exceptions by logging and reporting to Crashlytics
     * Also classifies the error and provides recovery options
     */
    fun handleException(tag: String, throwable: Throwable, customMessage: String? = null): AppError {
        // Convert to AppError for better classification
        val appError = if (throwable is AppError) throwable else AppError.from(throwable)

        // Log the error with appropriate level based on type
        when (appError) {
            is AppError.Network -> LogUtils.w(tag, customMessage ?: appError.message, appError.cause)
            else -> {
                if (customMessage != null) {
                    LogUtils.e(tag, customMessage, appError.cause)
                } else {
                    LogUtils.e(tag, appError.message, appError.cause)
                }
            }
        }

        // Report to Crashlytics with additional context
        try {
            // Don't report certain types of errors to reduce noise
            if (appError !is AppError.Network.NoConnection &&
                appError !is AppError.Network.Timeout) {
                val crashlytics = FirebaseCrashlytics.getInstance()

                // Add more context to the error report
                crashlytics.setCustomKey("error_tag", tag)
                crashlytics.setCustomKey("error_type", appError.javaClass.simpleName)
                crashlytics.setCustomKey("error_recoverable", errorRecoveryManager.isRecoverable(appError).toString())
                crashlytics.setCustomKey("error_timestamp", System.currentTimeMillis().toString())
                crashlytics.setCustomKey("error_thread", Thread.currentThread().name)

                // Add custom message if provided
                customMessage?.let { crashlytics.setCustomKey("error_message", it) }

                // Add stack trace as a custom key for better searchability
                val stackTrace = throwable.stackTraceToString().take(1000) // Limit length
                crashlytics.setCustomKey("error_stack_trace", stackTrace)

                // Add user-friendly message that would be shown to the user
                val userMessage = getUserFriendlyErrorMessage(throwable)
                crashlytics.setCustomKey("error_user_message", userMessage)

                // Only record non-network exceptions to reduce noise
                if (appError !is AppError.Network) {
                    crashlytics.recordException(appError.cause ?: Exception(appError.message))
                }
            }
        } catch (e: Exception) {
            LogUtils.e("ErrorHandler", "Failed to report error to Crashlytics", e)
        }

        return appError
    }

    // Shared coroutine scope for all error handling operations
    private val sharedErrorScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Creates a safe coroutine scope with error handling
     * Uses a shared dispatcher to prevent resource exhaustion
     */
    fun createSafeCoroutineScope(tag: String): CoroutineScope {
        return CoroutineScope(
            SupervisorJob() +
            Dispatchers.Default +
            createCoroutineExceptionHandler(tag)
        )
    }

    /**
     * Executes a block of code safely, catching and handling any exceptions
     */
    inline fun <T> executeSafely(
        tag: String,
        defaultValue: T,
        block: () -> T
    ): T {
        return try {
            val result = block()
            // Log success at debug level
            LogUtils.d(tag, "Operation completed successfully")
            result
        } catch (e: Exception) {
            handleException(tag, e)
            defaultValue
        }
    }

    /**
     * Executes a suspending block of code safely in a coroutine
     */
    fun <T> executeCoroutineSafely(
        tag: String,
        defaultValue: T,
        block: suspend () -> T,
        onSuccess: ((T) -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null
    ) {
        val scope = createSafeCoroutineScope(tag)
        scope.launch {
            try {
                LogUtils.d(tag, "Starting coroutine operation")
                val startTime = System.currentTimeMillis()

                val result = block()

                val duration = System.currentTimeMillis() - startTime
                LogUtils.d(tag, "Coroutine operation completed in ${duration}ms")

                onSuccess?.invoke(result)
            } catch (e: Exception) {
                handleException(tag, e)
                onError?.invoke(e)
            }
        }
    }

    /**
     * Wraps a result in a Result class for better error handling
     */
    suspend fun <T> wrapWithResult(
        tag: String,
        block: suspend () -> T
    ): Result<T> {
        return try {
            val startTime = System.currentTimeMillis()

            val result = block()

            val duration = System.currentTimeMillis() - startTime
            LogUtils.d(tag, "Operation completed successfully in ${duration}ms")

            Result.success(result)
        } catch (e: Exception) {
            handleException(tag, e)
            Result.failure(e)
        }
    }

    /**
     * Gets a user-friendly error message based on the exception type
     */
    fun getUserFriendlyErrorMessage(throwable: Throwable): String {
        return errorMessageProvider.getMessage(throwable)
    }

    /**
     * Gets a user-friendly error message with a suggestion based on the exception type
     */
    fun getUserFriendlyErrorMessageWithSuggestion(throwable: Throwable): String {
        return errorMessageProvider.getMessageWithSuggestion(throwable)
    }

    /**
     * Handles an error with recovery options
     *
     * @param tag The tag for logging
     * @param throwable The throwable to handle
     * @param onRecoveryAction Callback for when a recovery action is available
     * @return The AppError that was handled
     */
    fun handleErrorWithRecovery(
        tag: String,
        throwable: Throwable,
        onRecoveryAction: (RecoveryAction) -> Unit
    ): AppError {
        val appError = handleException(tag, throwable)

        // Check if there's a recovery action available
        val recoveryAction = errorRecoveryManager.getRecoveryAction(appError)

        // If there's a recovery action, invoke the callback
        recoveryAction?.let { action ->
            onRecoveryAction(action)
        }

        return appError
    }

    /**
     * Gets a user-friendly error message with a suggestion based on the exception type
     */
    fun getUserFriendlyErrorMessageWithSuggestion(throwable: Throwable): String {
        return errorMessageProvider.getMessageWithSuggestion(throwable)
    }

    /**
     * Gets a recovery action for the given throwable
     */
    fun getRecoveryAction(throwable: Throwable): ErrorRecoveryManager.RecoveryAction? {
        val appError = if (throwable is AppError) throwable else AppError.from(throwable)
        return errorRecoveryManager.getRecoveryAction(appError)
    }

    /**
     * Executes a suspending function with retry logic
     */
    suspend fun <T> withRetry(
        tag: String,
        block: suspend () -> T
    ): T {
        return try {
            errorRecoveryManager.withRetry(
                onRetry = { e, attempt, delay ->
                    Timber.d("[$tag] Retry attempt $attempt after $delay ms due to: ${e.message}")
                },
                block = block
            )
        } catch (e: Exception) {
            val appError = handleException(tag, e)
            throw appError
        }
    }

    /**
     * Executes a suspending function with network-aware retry logic
     */
    suspend fun <T> withNetworkRetry(
        tag: String,
        block: suspend () -> T
    ): T {
        return try {
            errorRecoveryManager.withNetworkRetry(block = block)
        } catch (e: Exception) {
            val appError = handleException(tag, e)
            throw appError
        }
    }

    /**
     * Standardized error handling for repository operations
     *
     * @param tag The tag for logging
     * @param operation A description of the operation being performed
     * @param block The suspending function to execute
     * @return Result containing the operation result or an error
     */
    suspend fun <T> safeRepositoryCall(
        tag: String,
        operation: String,
        block: suspend () -> T
    ): Result<T> {
        return try {
            val startTime = System.currentTimeMillis()

            // Execute the operation with retry logic for network operations
            val result = withNetworkRetry(tag) { block() }

            val duration = System.currentTimeMillis() - startTime
            LogUtils.d(tag, "$operation completed successfully in ${duration}ms")

            Result.success(result)
        } catch (e: Exception) {
            // Handle the exception and get a user-friendly error message
            val appError = handleException(tag, e, "Error in $operation")
            val userMessage = getUserFriendlyErrorMessage(appError)

            // Create a new exception with the user-friendly message
            val wrappedError = Exception(userMessage, e)

            Result.failure(wrappedError)
        }
    }
}

