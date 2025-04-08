# Error Handling Guide

This guide documents the error handling system implemented in the EazyDelivery app and provides guidelines for handling errors effectively.

## Table of Contents

1. [Overview](#overview)
2. [Error Classification](#error-classification)
3. [Error Handling Components](#error-handling-components)
4. [Error Recovery](#error-recovery)
5. [User-Facing Error Messages](#user-facing-error-messages)
6. [Error Reporting](#error-reporting)
7. [Testing Error Handling](#testing-error-handling)
8. [Best Practices](#best-practices)

## Overview

Effective error handling is critical for the EazyDelivery app to ensure reliability and a good user experience. The app implements a comprehensive error handling system that:

- Classifies errors into specific categories
- Provides user-friendly error messages
- Implements recovery mechanisms for recoverable errors
- Reports errors to Firebase Crashlytics for monitoring
- Logs errors for debugging

## Error Classification

The app uses a sealed class hierarchy to classify errors:

```kotlin
sealed class AppError(
    open val message: String,
    open val cause: Throwable? = null
) {
    sealed class Network(...) : AppError(...)
    sealed class Api(...) : AppError(...)
    sealed class Database(...) : AppError(...)
    sealed class Permission(...) : AppError(...)
    sealed class Feature(...) : AppError(...)
    class Unexpected(...) : AppError(...)
}
```

### Network Errors

```kotlin
sealed class Network(
    override val message: String,
    override val cause: Throwable? = null
) : AppError(message, cause) {
    
    class NoConnection(...) : Network(...)
    class Timeout(...) : Network(...)
    class ServerUnreachable(...) : Network(...)
    class Generic(...) : Network(...)
}
```

Network errors occur when the app cannot communicate with the server:

- **NoConnection**: No internet connection is available
- **Timeout**: The connection timed out
- **ServerUnreachable**: The server is unreachable
- **Generic**: A generic network error occurred

### API Errors

```kotlin
sealed class Api(
    override val message: String,
    override val cause: Throwable? = null,
    open val errorCode: Int? = null
) : AppError(message, cause) {
    
    class ServerError(...) : Api(...)
    class ClientError(...) : Api(...)
    class AuthError(...) : Api(...)
    class Generic(...) : Api(...)
}
```

API errors occur when the app receives an error response from the server:

- **ServerError**: The server returned a 5xx error
- **ClientError**: The server returned a 4xx error
- **AuthError**: The user is not authenticated or the authentication token is invalid
- **Generic**: A generic API error occurred

### Database Errors

```kotlin
sealed class Database(
    override val message: String,
    override val cause: Throwable? = null
) : AppError(message, cause) {
    
    class ReadError(...) : Database(...)
    class WriteError(...) : Database(...)
    class MigrationError(...) : Database(...)
    class Generic(...) : Database(...)
}
```

Database errors occur when the app cannot read from or write to the database:

- **ReadError**: Error reading from the database
- **WriteError**: Error writing to the database
- **MigrationError**: Error migrating the database
- **Generic**: A generic database error occurred

### Permission Errors

```kotlin
sealed class Permission(
    override val message: String,
    override val cause: Throwable? = null
) : AppError(message, cause) {
    
    class Denied(...) : Permission(...)
    class PermanentlyDenied(...) : Permission(...)
    class Generic(...) : Permission(...)
}
```

Permission errors occur when the app does not have the required permissions:

- **Denied**: The user denied a permission
- **PermanentlyDenied**: The user permanently denied a permission
- **Generic**: A generic permission error occurred

### Feature Errors

```kotlin
sealed class Feature(
    override val message: String,
    override val cause: Throwable? = null,
    open val featureId: String? = null
) : AppError(message, cause) {
    
    class NotAvailable(...) : Feature(...)
    class RequiresUpgrade(...) : Feature(...)
    class Generic(...) : Feature(...)
}
```

Feature errors occur when a feature is not available or requires an upgrade:

- **NotAvailable**: The feature is not available
- **RequiresUpgrade**: The feature requires an upgrade
- **Generic**: A generic feature error occurred

### Unexpected Errors

```kotlin
class Unexpected(
    override val message: String = "An unexpected error occurred",
    override val cause: Throwable? = null
) : AppError(message, cause)
```

Unexpected errors are errors that do not fit into any of the above categories.

## Error Handling Components

The app includes several components for handling errors:

### ErrorHandler

```kotlin
class ErrorHandler @Inject constructor(
    private val errorMessageProvider: ErrorMessageProvider,
    private val errorRecoveryManager: ErrorRecoveryManager
) {
    fun handleException(tag: String, throwable: Throwable, customMessage: String? = null): AppError {
        // Convert to AppError for better classification
        val appError = if (throwable is AppError) throwable else AppError.from(throwable)
        
        // Log the error
        // Report to Crashlytics
        // Return the AppError
    }
    
    fun getUserFriendlyErrorMessage(throwable: Throwable): String {
        return errorMessageProvider.getMessage(throwable)
    }
    
    fun getUserFriendlyErrorMessageWithSuggestion(throwable: Throwable): String {
        return errorMessageProvider.getMessageWithSuggestion(throwable)
    }
    
    fun getRecoveryAction(throwable: Throwable): ErrorRecoveryManager.RecoveryAction? {
        val appError = if (throwable is AppError) throwable else AppError.from(throwable)
        return errorRecoveryManager.getRecoveryAction(appError)
    }
    
    suspend fun <T> withRetry(tag: String, block: suspend () -> T): T {
        // Execute with retry logic
    }
    
    suspend fun <T> withNetworkRetry(tag: String, block: suspend () -> T): T {
        // Execute with network-aware retry logic
    }
}
```

The `ErrorHandler` is the central component for handling errors:

- **handleException**: Logs and reports exceptions, and converts them to `AppError`
- **getUserFriendlyErrorMessage**: Gets a user-friendly error message
- **getUserFriendlyErrorMessageWithSuggestion**: Gets a user-friendly error message with a suggestion
- **getRecoveryAction**: Gets a recovery action for an error
- **withRetry**: Executes a block with retry logic
- **withNetworkRetry**: Executes a block with network-aware retry logic

### ErrorMessageProvider

```kotlin
class ErrorMessageProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getMessage(error: AppError): String {
        // Return a user-friendly error message based on the error type
    }
    
    fun getMessageWithSuggestion(error: AppError): String {
        // Return a user-friendly error message with a suggestion
    }
    
    fun getSuggestion(error: AppError): String {
        // Return a suggestion for resolving the error
    }
    
    fun getMessage(throwable: Throwable): String {
        // Convert to AppError and get message
    }
    
    fun getMessageWithSuggestion(throwable: Throwable): String {
        // Convert to AppError and get message with suggestion
    }
}
```

The `ErrorMessageProvider` provides user-friendly error messages:

- **getMessage**: Gets a user-friendly error message
- **getMessageWithSuggestion**: Gets a user-friendly error message with a suggestion
- **getSuggestion**: Gets a suggestion for resolving the error

### ErrorRecoveryManager

```kotlin
class ErrorRecoveryManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val connectivityManager: ConnectivityManager
) {
    suspend fun <T> withRetry(
        maxRetries: Int = MAX_RETRY_COUNT,
        initialDelayMs: Long = BASE_DELAY_MS,
        maxDelayMs: Long = MAX_DELAY_MS,
        shouldRetry: (Exception) -> Boolean = { true },
        onRetry: (Exception, Int, Long) -> Unit = { _, _, _ -> },
        block: suspend () -> T
    ): T {
        // Execute with retry logic
    }
    
    suspend fun <T> withNetworkRetry(
        maxRetries: Int = MAX_RETRY_COUNT,
        block: suspend () -> T
    ): T {
        // Execute with network-aware retry logic
    }
    
    fun openAppSettings(): Boolean {
        // Open app settings
    }
    
    fun openNetworkSettings(): Boolean {
        // Open network settings
    }
    
    fun isRecoverable(error: AppError): Boolean {
        // Determine if an error is recoverable
    }
    
    fun getRecoveryAction(error: AppError): RecoveryAction? {
        // Get a recovery action for an error
    }
    
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
```

The `ErrorRecoveryManager` provides recovery mechanisms for errors:

- **withRetry**: Executes a block with retry logic
- **withNetworkRetry**: Executes a block with network-aware retry logic
- **openAppSettings**: Opens the app settings screen
- **openNetworkSettings**: Opens the network settings screen
- **isRecoverable**: Determines if an error is recoverable
- **getRecoveryAction**: Gets a recovery action for an error

## Error Recovery

The app implements several recovery mechanisms for errors:

### Retry Logic

```kotlin
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
                throw e
            }
            
            // Calculate next delay with exponential backoff
            val nextAttempt = attempt + 1
            val nextDelay = min(currentDelay * 2.0.pow(attempt).toLong(), maxDelayMs)
            
            // Invoke retry callback
            onRetry(e, nextAttempt, nextDelay)
            
            // Wait before retrying
            delay(nextDelay)
            currentDelay = nextDelay
        }
    }
    
    // This should never be reached, but just in case
    throw lastException ?: IllegalStateException("Unknown error during retry")
}
```

The retry logic uses exponential backoff to retry operations:

- **Exponential Backoff**: Increases the delay between retries exponentially
- **Maximum Retries**: Limits the number of retry attempts
- **Conditional Retries**: Only retries if the error is retryable
- **Retry Callbacks**: Invokes callbacks before each retry attempt

### Network-Aware Retry Logic

```kotlin
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
```

The network-aware retry logic only retries network-related errors if the network is available:

- **Network Availability Check**: Only retries if the network is available
- **Network Error Detection**: Only retries network-related errors
- **Logging**: Logs retry attempts for debugging

### Recovery Actions

```kotlin
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
```

The app provides recovery actions for different types of errors:

- **Retry**: Retry the operation
- **RestartApp**: Restart the app
- **UpdateApp**: Update the app
- **OpenAppSettings**: Open the app settings screen
- **OpenNetworkSettings**: Open the network settings screen
- **RequestPermission**: Request the required permission
- **Relogin**: Log in again
- **Upgrade**: Upgrade to access the feature

## User-Facing Error Messages

The app provides user-friendly error messages for different types of errors:

### Error Messages

```kotlin
fun getMessage(error: AppError): String {
    return when (error) {
        // Network errors
        is AppError.Network.NoConnection -> context.getString(R.string.error_no_internet_connection)
        is AppError.Network.Timeout -> context.getString(R.string.error_connection_timeout)
        is AppError.Network.ServerUnreachable -> context.getString(R.string.error_server_unreachable)
        is AppError.Network.Generic -> context.getString(R.string.error_network_generic)
        
        // API errors
        is AppError.Api.ServerError -> context.getString(R.string.error_server)
        is AppError.Api.ClientError -> context.getString(R.string.error_client)
        is AppError.Api.AuthError -> context.getString(R.string.error_authentication)
        is AppError.Api.Generic -> context.getString(R.string.error_api_generic)
        
        // Database errors
        is AppError.Database.ReadError -> context.getString(R.string.error_database_read)
        is AppError.Database.WriteError -> context.getString(R.string.error_database_write)
        is AppError.Database.MigrationError -> context.getString(R.string.error_database_migration)
        is AppError.Database.Generic -> context.getString(R.string.error_database_generic)
        
        // Permission errors
        is AppError.Permission.Denied -> context.getString(R.string.error_permission_denied)
        is AppError.Permission.PermanentlyDenied -> context.getString(R.string.error_permission_permanently_denied)
        is AppError.Permission.Generic -> context.getString(R.string.error_permission_generic)
        
        // Feature errors
        is AppError.Feature.NotAvailable -> context.getString(R.string.error_feature_not_available)
        is AppError.Feature.RequiresUpgrade -> context.getString(R.string.error_feature_requires_upgrade)
        is AppError.Feature.Generic -> context.getString(R.string.error_feature_generic)
        
        // Unexpected errors
        is AppError.Unexpected -> error.message.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.error_unexpected)
    }
}
```

The app provides localized error messages for different types of errors.

### Error Suggestions

```kotlin
fun getSuggestion(error: AppError): String {
    return when (error) {
        // Network errors
        is AppError.Network.NoConnection -> context.getString(R.string.suggestion_check_internet)
        is AppError.Network.Timeout -> context.getString(R.string.suggestion_try_again_later)
        is AppError.Network.ServerUnreachable -> context.getString(R.string.suggestion_check_internet)
        is AppError.Network.Generic -> context.getString(R.string.suggestion_check_internet)
        
        // API errors
        is AppError.Api.ServerError -> context.getString(R.string.suggestion_try_again_later)
        is AppError.Api.ClientError -> context.getString(R.string.suggestion_contact_support)
        is AppError.Api.AuthError -> context.getString(R.string.suggestion_login_again)
        is AppError.Api.Generic -> context.getString(R.string.suggestion_try_again_later)
        
        // Database errors
        is AppError.Database.ReadError -> context.getString(R.string.suggestion_restart_app)
        is AppError.Database.WriteError -> context.getString(R.string.suggestion_restart_app)
        is AppError.Database.MigrationError -> context.getString(R.string.suggestion_update_app)
        is AppError.Database.Generic -> context.getString(R.string.suggestion_restart_app)
        
        // Permission errors
        is AppError.Permission.Denied -> context.getString(R.string.suggestion_grant_permission)
        is AppError.Permission.PermanentlyDenied -> context.getString(R.string.suggestion_app_settings)
        is AppError.Permission.Generic -> context.getString(R.string.suggestion_grant_permission)
        
        // Feature errors
        is AppError.Feature.NotAvailable -> context.getString(R.string.suggestion_update_app)
        is AppError.Feature.RequiresUpgrade -> context.getString(R.string.suggestion_upgrade)
        is AppError.Feature.Generic -> context.getString(R.string.suggestion_try_again_later)
        
        // Unexpected errors
        is AppError.Unexpected -> context.getString(R.string.suggestion_try_again_later)
    }
}
```

The app provides suggestions for resolving different types of errors.

## Error Reporting

The app reports errors to Firebase Crashlytics for monitoring:

```kotlin
// Report to Crashlytics with additional context
try {
    // Don't report certain types of errors to reduce noise
    if (appError !is AppError.Network.NoConnection &&
        appError !is AppError.Network.Timeout) {
        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.setCustomKey("error_tag", tag)
        crashlytics.setCustomKey("error_type", appError.javaClass.simpleName)
        crashlytics.setCustomKey("error_recoverable", errorRecoveryManager.isRecoverable(appError).toString())
        customMessage?.let { crashlytics.setCustomKey("error_message", it) }

        // Only record non-network exceptions to reduce noise
        if (appError !is AppError.Network) {
            crashlytics.recordException(appError.cause ?: Exception(appError.message))
        }
    }
} catch (e: Exception) {
    LogUtils.e("ErrorHandler", "Failed to report error to Crashlytics", e)
}
```

The app reports errors to Firebase Crashlytics with additional context:

- **Error Tag**: The tag associated with the error
- **Error Type**: The type of error
- **Error Recoverable**: Whether the error is recoverable
- **Error Message**: A custom message associated with the error

The app does not report certain types of errors to reduce noise:

- **Network Errors**: Network errors are common and often not actionable
- **Timeout Errors**: Timeout errors are common and often not actionable

## Testing Error Handling

The app includes tests for the error handling system:

### AppErrorTest

```kotlin
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
    
    // Other tests...
}
```

Tests for the `AppError` class ensure that exceptions are correctly mapped to `AppError` types.

### ErrorMessageProviderTest

```kotlin
class ErrorMessageProviderTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    private lateinit var errorMessageProvider: ErrorMessageProvider
    
    @Before
    fun setup() {
        // Setup mock string resources
        `when`(mockContext.getString(com.eazydelivery.app.R.string.error_no_internet_connection))
            .thenReturn("No internet connection available.")
        // Other mock setup...
        
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
    
    // Other tests...
}
```

Tests for the `ErrorMessageProvider` class ensure that error messages are correctly provided for different types of errors.

### ErrorRecoveryManagerTest

```kotlin
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
    
    // Other tests...
}
```

Tests for the `ErrorRecoveryManager` class ensure that retry logic and recovery actions work correctly.

## Best Practices

To handle errors effectively in the EazyDelivery app, follow these best practices:

### Error Classification

- **Use AppError**: Convert exceptions to `AppError` for better classification
- **Be Specific**: Use the most specific error type possible
- **Include Cause**: Include the original exception as the cause
- **Include Context**: Include relevant context in error messages

### Error Handling

- **Handle All Errors**: Handle all possible errors, even unexpected ones
- **Log Errors**: Log errors with appropriate level and context
- **Report Errors**: Report errors to Firebase Crashlytics for monitoring
- **Provide User Feedback**: Show user-friendly error messages
- **Implement Recovery**: Implement recovery mechanisms for recoverable errors

### Error Recovery

- **Use Retry Logic**: Use retry logic for transient errors
- **Use Exponential Backoff**: Use exponential backoff for retries
- **Check Network Availability**: Only retry network operations if the network is available
- **Limit Retries**: Limit the number of retry attempts
- **Provide Recovery Actions**: Provide recovery actions for different types of errors

### User Experience

- **Show User-Friendly Messages**: Show user-friendly error messages
- **Provide Suggestions**: Provide suggestions for resolving errors
- **Be Honest**: Be honest about what went wrong
- **Be Helpful**: Provide helpful information for resolving errors
- **Be Consistent**: Use consistent error messages and styling

### Testing

- **Test Error Paths**: Test error paths as well as happy paths
- **Test Recovery Mechanisms**: Test recovery mechanisms
- **Test User-Facing Messages**: Test that user-facing messages are correct
- **Test Error Reporting**: Test that errors are correctly reported to Firebase Crashlytics

By following these best practices, you can handle errors effectively in the EazyDelivery app and provide a better user experience.
