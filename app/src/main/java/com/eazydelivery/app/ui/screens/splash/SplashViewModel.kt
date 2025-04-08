package com.eazydelivery.app.ui.screens.splash

import androidx.lifecycle.ViewModel
import com.eazydelivery.app.domain.repository.AuthRepository
import com.eazydelivery.app.domain.repository.UserPreferencesRepository
import com.eazydelivery.app.ui.navigation.Screen
import com.eazydelivery.app.util.ErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val errorHandler: ErrorHandler
) : ViewModel() {
    
    suspend fun getStartDestination(): String {
        return errorHandler.executeSafely("SplashViewModel.getStartDestination", Screen.PhoneLogin.route) {
            val isLoggedIn = authRepository.isLoggedIn().getOrNull() ?: false
            
            if (!isLoggedIn) {
                return@executeSafely Screen.PhoneLogin.route
            }
            
            val onboardingCompleted = userPreferencesRepository.isOnboardingCompleted().getOrNull() ?: false
            if (!onboardingCompleted) {
                return@executeSafely Screen.UserOnboarding.route
            }
            
            return@executeSafely Screen.Home.route
        }
    }
}
