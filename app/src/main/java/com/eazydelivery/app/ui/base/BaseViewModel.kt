package com.eazydelivery.app.ui.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eazydelivery.app.util.ErrorHandler
import com.eazydelivery.app.util.error.AppError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Base ViewModel class with common functionality for all ViewModels
 */
abstract class BaseViewModel(
    protected val errorHandler: ErrorHandler
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    // LiveData for AppError objects
    private val _appError = MutableLiveData<Event<AppError>>()
    val appError: LiveData<Event<AppError>> = _appError

    private val _success = MutableSharedFlow<String>()
    val success: SharedFlow<String> = _success.asSharedFlow()

    /**
     * Executes a suspending function safely within the ViewModel scope
     */
    protected fun launchSafe(
        tag: String,
        showLoading: Boolean = true,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        block: suspend () -> Unit
    ) {
        viewModelScope.launch(dispatcher) {
            try {
                if (showLoading) {
                    setLoading(true)
                }
                block()
            } catch (e: Exception) {
                val appError = errorHandler.handleException(tag, e)
                _appError.postValue(Event(appError))
                emitError(appError.message)
            } finally {
                if (showLoading) {
                    setLoading(false)
                }
            }
        }
    }

    /**
     * Executes a suspending function with retry capability
     */
    protected fun launchWithRetry(
        tag: String,
        maxRetries: Int = 3,
        showLoading: Boolean = true,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        block: suspend () -> Unit
    ) {
        viewModelScope.launch(dispatcher) {
            try {
                if (showLoading) {
                    setLoading(true)
                }

                var retryCount = 0
                var success = false

                while (!success && retryCount < maxRetries) {
                    try {
                        block()
                        success = true
                    } catch (e: Exception) {
                        retryCount++
                        if (retryCount >= maxRetries) {
                            throw e
                        }
                        Timber.w("Retry attempt $retryCount for $tag")
                        kotlinx.coroutines.delay(1000L * retryCount) // Exponential backoff
                    }
                }
            } catch (e: Exception) {
                val appError = errorHandler.handleException(tag, e)
                _appError.postValue(Event(appError))
                emitError(appError.message)
            } finally {
                if (showLoading) {
                    setLoading(false)
                }
            }
        }
    }

    /**
     * Handles a Result object and emits appropriate loading/error states
     */
    protected suspend fun <T> handleResult(
        result: Result<T>,
        onSuccess: suspend (T) -> Unit
    ) {
        result.fold(
            onSuccess = { onSuccess(it) },
            onFailure = { e ->
                emitError(e.message ?: "An unexpected error occurred")
            }
        )
    }

    /**
     * Emits an error message
     */
    protected suspend fun emitError(message: String) {
        Timber.e("Error: $message")
        _error.emit(message)
    }

    /**
     * Emits a success message
     */
    protected suspend fun emitSuccess(message: String) {
        Timber.d("Success: $message")
        _success.emit(message)
    }

    /**
     * Sets the loading state
     */
    protected fun setLoading(isLoading: Boolean) {
        _isLoading.update { isLoading }
    }

    /**
     * Event wrapper for LiveData
     * Used to prevent multiple observations of the same event
     */
    class Event<out T>(private val content: T) {
        private var hasBeenHandled = false

        /**
         * Returns the content and prevents its use again
         */
        fun getContentIfNotHandled(): T? {
            return if (hasBeenHandled) {
                null
            } else {
                hasBeenHandled = true
                content
            }
        }

        /**
         * Returns the content, even if it's already been handled
         */
        fun peekContent(): T = content
    }
}
