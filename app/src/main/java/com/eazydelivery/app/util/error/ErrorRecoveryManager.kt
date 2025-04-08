package com.eazydelivery.app.util.error

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.eazydelivery.app.util.ConnectivityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.pow

/**
 * Manages error recovery strategies
 */
@Singleton
class ErrorRecoveryManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val connectivityManager: ConnectivityManager
) {
    companion object {
        private const val MAX_RETRY_COUNT = 3
        private const val BASE_DELAY_MS = 1000L
        private const val MAX_DELAY_MS = 10000L
    }
    
    /**
     * Executes a suspending function with retry logic
     * Uses exponential backoff for retries
     * 
     * @param maxRetries Maximum number of retry attempts
     * @param initialDelayMs Initial delay in milliseconds
     * @param maxDelayMs Maximum delay in milliseconds
     * @param shouldRetry Function to determine if a retry should be attempted for a given exception
     * @param onRetry Callback invoked before each retry attempt
     * @param block The suspending function to execute
     * @return The result of the function execution
     * @throws Exception if all retry attempts fail
     */
    suspend fun <T> withRetry(
        maxRetries: Int = MAX_RETRY_COUNT,
        initialDelayMs: Long = BASE_DELAY_MS,
        maxDelayMs: Long = MAX_DELAY_MS,
        shouldRetry: (Exception) -> Boolean = { true },
        onRetry: (Exception, Int, Long) -> Unit = { _, _, _ -> },
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMs
        var lastException: Exception? = null
        
        repeat(maxRetries + 1) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                
                // Check if we should retry
                if (attempt >= maxRetries || !shouldRetry(e)) {
                    Timber.d("Not retrying: attempt=$attempt, maxRetries=$maxRetries, shouldRetry=${shouldRetry(e)}")
                    throw e
                }
                
                // Calculate next delay with exponential backoff
                val nextAttempt = attempt + 1
                val nextDelay = min(currentDelay * 2.0.pow(attempt).toLong(), maxDelayMs)
                
                // Invoke retry callback
                onRetry(e, nextAttempt, nextDelay)
                
                // Wait before retrying
                Timber.d("Retrying after $nextDelay ms (attempt ${nextAttempt}/${maxRetries})")
                delay(nextDelay)
                currentDelay = nextDelay
            }
        }
        
        // This should never be reached, but just in case
        throw lastException ?: IllegalStateException("Unknown error during retry")
    }
    
    /**
     * Executes a suspending function with network-aware retry logic
     * Only retries if network is available
     * 
     * @param maxRetries Maximum number of retry attempts
     * @param block The suspending function to execute
     * @return The result of the function execution
     * @throws Exception if all retry attempts fail
     */
    suspend fun <T> withNetworkRetry(
        maxRetries: Int = MAX_RETRY_COUNT,
        block: suspend () -> T
    ): T {
        return withRetry(
            maxRetries = maxRetries,
            shouldRetry = { e ->
                // Only retry network-related errors if network is available
                val isNetworkError = e is AppError.Network || AppError.from(e) is AppError.Network
                isNetworkError && connectivityManager.isNetworkAvailable()
            },
            onRetry = { e, attempt, delay ->
                Timber.d("Network retry attempt $attempt after $delay ms due to: ${e.message}")
            },
            block = block
        )
    }
    
    /**
     * Opens the app settings screen
     * Useful for permission-related errors
     * 
     * @return true if the settings screen was opened, false otherwise
     */
    fun openAppSettings(): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to open app settings")
            false
        }
    }
    
    /**
     * Opens the device's network settings
     * Useful for network-related errors
     * 
     * @return true if the network settings screen was opened, false otherwise
     */
    fun openNetworkSettings(): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to open network settings")
            false
        }
    }
    
    /**
     * Determines if an error is recoverable
     * 
     * @param error The error to check
     * @return true if the error is recoverable, false otherwise
     */
    fun isRecoverable(error: AppError): Boolean {
        return when (error) {
            // Network errors are generally recoverable
            is AppError.Network -> true
            
            // Some API errors are recoverable
            is AppError.Api.ServerError -> true
            is AppError.Api.ClientError -> false
            is AppError.Api.AuthError -> true
            is AppError.Api.Generic -> true
            
            // Database errors may be recoverable
            is AppError.Database.ReadError -> true
            is AppError.Database.WriteError -> true
            is AppError.Database.MigrationError -> false
            is AppError.Database.Generic -> true
            
            // Permission errors are recoverable through settings
            is AppError.Permission -> true
            
            // Feature errors may be recoverable
            is AppError.Feature.NotAvailable -> false
            is AppError.Feature.RequiresUpgrade -> true
            is AppError.Feature.Generic -> true
            
            // Unexpected errors are not generally recoverable
            is AppError.Unexpected -> false
        }
    }
    
    /**
     * Gets a recovery action for the given error
     * 
     * @param error The error to get a recovery action for
     * @return A recovery action or null if no recovery action is available
     */
    fun getRecoveryAction(error: AppError): RecoveryAction? {
        return when (error) {
            // Network errors
            is AppError.Network.NoConnection -> RecoveryAction.OpenNetworkSettings
            is AppError.Network.Timeout -> RecoveryAction.Retry
            is AppError.Network.ServerUnreachable -> RecoveryAction.Retry
            is AppError.Network.Generic -> RecoveryAction.Retry
            
            // API errors
            is AppError.Api.ServerError -> RecoveryAction.Retry
            is AppError.Api.ClientError -> null
            is AppError.Api.AuthError -> RecoveryAction.Relogin
            is AppError.Api.Generic -> RecoveryAction.Retry
            
            // Database errors
            is AppError.Database.ReadError -> RecoveryAction.RestartApp
            is AppError.Database.WriteError -> RecoveryAction.RestartApp
            is AppError.Database.MigrationError -> RecoveryAction.UpdateApp
            is AppError.Database.Generic -> RecoveryAction.RestartApp
            
            // Permission errors
            is AppError.Permission.Denied -> RecoveryAction.RequestPermission
            is AppError.Permission.PermanentlyDenied -> RecoveryAction.OpenAppSettings
            is AppError.Permission.Generic -> RecoveryAction.RequestPermission
            
            // Feature errors
            is AppError.Feature.NotAvailable -> null
            is AppError.Feature.RequiresUpgrade -> RecoveryAction.Upgrade
            is AppError.Feature.Generic -> RecoveryAction.Retry
            
            // Unexpected errors
            is AppError.Unexpected -> null
        }
    }
    
    /**
     * Recovery actions that can be taken to recover from errors
     */
    sealed class RecoveryAction {
        object Retry : RecoveryAction()
        object RestartApp : RecoveryAction()
        object UpdateApp : RecoveryAction()
        object OpenAppSettings : RecoveryAction()
        object OpenNetworkSettings : RecoveryAction()
        object RequestPermission : RecoveryAction()
        object Relogin : RecoveryAction()
        object Upgrade : RecoveryAction()
    }
}
