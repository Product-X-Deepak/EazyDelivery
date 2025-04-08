package com.eazydelivery.app.ui.screens.subscription

import androidx.lifecycle.viewModelScope
import com.eazydelivery.app.domain.repository.SubscriptionRepository
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
class SubscriptionViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    errorHandler: ErrorHandler
) : BaseViewModel(errorHandler) {
    
    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()
    
    init {
        loadSubscriptionStatus()
    }
    
    private fun loadSubscriptionStatus() {
        viewModelScope.launch {
            try {
                val subscriptionStatus = subscriptionRepository.getSubscriptionStatus().getOrThrow()
                
                _uiState.update {
                    it.copy(
                        isSubscribed = subscriptionStatus.isSubscribed,
                        trialDaysLeft = subscriptionStatus.trialDaysLeft,
                        endDate = subscriptionStatus.endDate
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading subscription status")
            }
        }
    }
    
    fun subscribe(plan: String = "monthly") {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                // In a real app, this would initiate a payment flow
                // For now, we'll just simulate a successful subscription
                val subscriptionId = if (plan == "yearly") "yearly_plan" else "monthly_plan"
                
                val result = subscriptionRepository.activateSubscription(subscriptionId)
                
                result.fold(
                    onSuccess = { status ->
                        _uiState.update {
                            it.copy(
                                isSubscribed = true,
                                endDate = status.endDate,
                                isLoading = false
                            )
                        }
                    },
                    onFailure = { e ->
                        Timber.e(e, "Error subscribing")
                        _uiState.update { it.copy(isLoading = false) }
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error subscribing")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    fun cancelSubscription() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                val result = subscriptionRepository.cancelSubscription()
                
                result.fold(
                    onSuccess = { success ->
                        if (success) {
                            _uiState.update {
                                it.copy(
                                    isSubscribed = false,
                                    isLoading = false
                                )
                            }
                        }
                    },
                    onFailure = { e ->
                        Timber.e(e, "Error cancelling subscription")
                        _uiState.update { it.copy(isLoading = false) }
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error cancelling subscription")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}

data class SubscriptionUiState(
    val isSubscribed: Boolean = false,
    val trialDaysLeft: Int = 0,
    val endDate: String = "",
    val isLoading: Boolean = false
)
