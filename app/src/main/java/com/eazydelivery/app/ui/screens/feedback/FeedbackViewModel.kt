package com.eazydelivery.app.ui.screens.feedback

import androidx.lifecycle.viewModelScope
import com.eazydelivery.app.domain.repository.FeedbackRepository
import com.eazydelivery.app.ui.base.BaseViewModel
import com.eazydelivery.app.util.ErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val feedbackRepository: FeedbackRepository,
    errorHandler: ErrorHandler
) : BaseViewModel(errorHandler) {
    
    private val _uiState = MutableStateFlow(FeedbackUiState())
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()
    
    fun updateRating(rating: Int) {
        _uiState.update { it.copy(rating = rating) }
    }
    
    fun updateFeedbackType(type: String) {
        _uiState.update { it.copy(feedbackType = type) }
    }
    
    fun updateComments(comments: String) {
        _uiState.update { it.copy(comments = comments) }
    }
    
    fun submitFeedback() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                val feedback = com.eazydelivery.app.domain.model.Feedback(
                    id = java.util.UUID.randomUUID().toString(),
                    rating = uiState.value.rating,
                    feedbackType = uiState.value.feedbackType,
                    comments = uiState.value.comments,
                    timestamp = System.currentTimeMillis()
                )
                
                val result = feedbackRepository.submitFeedback(feedback)
                
                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(
                            feedbackSubmitted = true,
                            isLoading = false
                        ) }
                        Timber.d("Feedback submitted successfully")
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(
                            error = "Failed to submit feedback: ${e.message}",
                            isLoading = false
                        ) }
                        Timber.e(e, "Failed to submit feedback")
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    error = "An unexpected error occurred: ${e.message}",
                    isLoading = false
                ) }
                Timber.e(e, "Error submitting feedback")
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class FeedbackUiState(
    val rating: Int = 0,
    val feedbackType: String = "",
    val comments: String = "",
    val isLoading: Boolean = false,
    val feedbackSubmitted: Boolean = false,
    val error: String? = null
)
