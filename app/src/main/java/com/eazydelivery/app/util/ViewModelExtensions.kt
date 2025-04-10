package com.eazydelivery.app.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eazydelivery.app.util.error.RecoveryAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Extension function for ViewModel to safely launch a coroutine with error handling
 *
 * @param tag The tag for logging
 * @param errorHandler The ErrorHandler instance to use
 * @param onError Callback for when an error occurs
 * @param onRecoveryAction Callback for when a recovery action is available
 * @param block The suspending function to execute
 */
fun ViewModel.launchSafe(
    tag: String,
    errorHandler: ErrorHandler,
    onError: ((Throwable) -> Unit)? = null,
    onRecoveryAction: ((RecoveryAction) -> Unit)? = null,
    block: suspend CoroutineScope.() -> Unit
) {
    viewModelScope.launch(errorHandler.createCoroutineExceptionHandler(tag) { throwable ->
        Timber.e(throwable, "Error in $tag")
        onError?.invoke(throwable)
    }) {
        try {
            block()
        } catch (e: Exception) {
            // If we have a recovery action callback, use handleErrorWithRecovery
            if (onRecoveryAction != null) {
                errorHandler.handleErrorWithRecovery(tag, e) { action ->
                    onRecoveryAction(action)
                }
            } else {
                // Otherwise, just handle the exception
                errorHandler.handleException(tag, e)
            }

            // Invoke the error callback if provided
            onError?.invoke(e)
        }
    }
}

/**
 * Extension function for ViewModel to safely execute a suspending function and return a Result
 *
 * @param tag The tag for logging
 * @param errorHandler The ErrorHandler instance to use
 * @param block The suspending function to execute
 * @return Result containing the operation result or an error
 */
suspend fun <T> ViewModel.wrapWithResult(
    tag: String,
    errorHandler: ErrorHandler,
    block: suspend () -> T
): Result<T> {
    return errorHandler.wrapWithResult(tag, block)
}

/**
 * Extension function for ViewModel to safely execute a repository operation
 *
 * @param tag The tag for logging
 * @param errorHandler The ErrorHandler instance to use
 * @param operation A description of the operation being performed
 * @param block The suspending function to execute
 * @return Result containing the operation result or an error
 */
suspend fun <T> ViewModel.safeRepositoryCall(
    tag: String,
    errorHandler: ErrorHandler,
    operation: String,
    block: suspend () -> T
): Result<T> {
    return errorHandler.safeRepositoryCall(tag, operation, block)
}
