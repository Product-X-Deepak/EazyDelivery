package com.eazydelivery.app.ui.screens.onboarding

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.eazydelivery.app.domain.repository.PermissionRepository
import com.eazydelivery.app.domain.repository.UserPreferencesRepository
import com.eazydelivery.app.ui.base.BaseViewModel
import com.eazydelivery.app.util.ErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class UserOnboardingViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val permissionRepository: PermissionRepository,
    @ApplicationContext private val context: Context,
    errorHandler: ErrorHandler
) : BaseViewModel(errorHandler) {
    
    private val _uiState = MutableStateFlow(UserOnboardingUiState())
    val uiState: StateFlow<UserOnboardingUiState> = _uiState.asStateFlow()
    
    fun completeOnboarding() {
        viewModelScope.launch {
            try {
                // Mark onboarding as completed
                userPreferencesRepository.setOnboardingCompleted(true).getOrThrow()
                
                // Request necessary permissions
                permissionRepository.requestNotificationPermission(context)
                
                _uiState.update { it.copy(onboardingCompleted = true) }
            } catch (e: Exception) {
                Timber.e(e, "Error completing onboarding")
                _uiState.update { 
                    it.copy(
                        errorMessage = e.message ?: "An error occurred"
                    ) 
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

data class UserOnboardingUiState(
    val onboardingCompleted: Boolean = false,
    val errorMessage: String? = null
)
