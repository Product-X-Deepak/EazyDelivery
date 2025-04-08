package com.eazydelivery.app.ui.screens.profile

import androidx.lifecycle.viewModelScope
import com.eazydelivery.app.domain.repository.AuthRepository
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
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val subscriptionRepository: SubscriptionRepository,
    errorHandler: ErrorHandler
) : BaseViewModel(errorHandler) {
    
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    init {
        loadUserProfile()
        loadSubscriptionStatus()
    }
    
    private fun loadUserProfile() {
        _uiState.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            try {
                val userInfo = authRepository.getUserInfo().getOrThrow()
                
                _uiState.update {
                    it.copy(
                        name = userInfo.name,
                        email = userInfo.email,
                        phone = userInfo.phone,
                        profilePicUrl = userInfo.profilePicUrl,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading user profile")
                _uiState.update { 
                    it.copy(
                        errorMessage = "Failed to load profile: ${e.message}",
                        isLoading = false
                    ) 
                }
            }
        }
    }
    
    private fun loadSubscriptionStatus() {
        viewModelScope.launch {
            try {
                val subscriptionStatus = subscriptionRepository.getSubscriptionStatus().getOrThrow()
                
                _uiState.update {
                    it.copy(
                        isSubscribed = subscriptionStatus.isSubscribed,
                        trialDaysLeft = subscriptionStatus.trialDaysLeft
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading subscription status")
            }
        }
    }
    
    fun logout() {
        _uiState.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            try {
                authRepository.logout().getOrThrow()
                _uiState.update { it.copy(logoutSuccess = true, isLoading = false) }
            } catch (e: Exception) {
                Timber.e(e, "Error during logout")
                _uiState.update { 
                    it.copy(
                        errorMessage = "Failed to logout: ${e.message}",
                        isLoading = false
                    ) 
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val phone: String? = null,
    val profilePicUrl: String? = null,
    val isSubscribed: Boolean = false,
    val trialDaysLeft: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val logoutSuccess: Boolean = false
)
