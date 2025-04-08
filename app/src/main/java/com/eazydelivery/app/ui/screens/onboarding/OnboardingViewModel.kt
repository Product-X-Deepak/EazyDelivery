package com.eazydelivery.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eazydelivery.app.domain.repository.UserPreferencesRepository
import com.eazydelivery.app.util.ErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val errorHandler: ErrorHandler
) : ViewModel() {
    
    fun completeOnboarding() {
        viewModelScope.launch {
            errorHandler.executeSafely("OnboardingViewModel.completeOnboarding", Unit) {
                userPreferencesRepository.setOnboardingCompleted(true)
            }
        }
    }
}
