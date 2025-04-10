package com.eazydelivery.app.util.error

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Extension functions for better error handling throughout the app
 */

/**
 * Safely executes a suspend function in a coroutine scope with proper error handling
 * 
 * @param tag Tag for logging
 * @param operation Description of the operation for logging
 * @param onError Optional callback for handling errors
 * @param block The suspend function to execute
 */
fun CoroutineScope.launchSafely(
    tag: String,
    operation: String,
    onError: ((Throwable) -> Unit)? = null,
    block: suspend () -> Unit
) {
    launch {
        try {
            block()
        } catch (e: CancellationException) {
            // Don't catch cancellation exceptions, they need to propagate
            throw e
        } catch (e: Throwable) {
            Timber.e(e, "[$tag] Error in $operation")
            onError?.invoke(e)
        }
    }
}

/**
 * Safely executes a function with proper error handling
 * 
 * @param tag Tag for logging
 * @param operation Description of the operation for logging
 * @param onError Optional callback for handling errors
 * @param block The function to execute
 * @return The result of the block or null if an exception occurred
 */
inline fun <T> runSafely(
    tag: String,
    operation: String,
    onError: ((Throwable) -> Unit)? = null,
    block: () -> T
): T? {
    return try {
        block()
    } catch (e: Throwable) {
        Timber.e(e, "[$tag] Error in $operation")
        onError?.invoke(e)
        null
    }
}

/**
 * Safely executes a function with proper error handling and a default value
 * 
 * @param tag Tag for logging
 * @param operation Description of the operation for logging
 * @param defaultValue Value to return if an exception occurs
 * @param onError Optional callback for handling errors
 * @param block The function to execute
 * @return The result of the block or the default value if an exception occurred
 */
inline fun <T> runSafelyWithDefault(
    tag: String,
    operation: String,
    defaultValue: T,
    onError: ((Throwable) -> Unit)? = null,
    block: () -> T
): T {
    return try {
        block()
    } catch (e: Throwable) {
        Timber.e(e, "[$tag] Error in $operation")
        onError?.invoke(e)
        defaultValue
    }
}
