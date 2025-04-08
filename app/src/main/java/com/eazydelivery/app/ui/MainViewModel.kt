package com.eazydelivery.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eazydelivery.app.domain.repository.AuthRepository
import com.eazydelivery.app.domain.repository.SubscriptionRepository
import com.eazydelivery.app.domain.repository.UserPreferencesRepository
import com.eazydelivery.app.ui.navigation.Screen
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val firebaseAnalytics: FirebaseAnalytics
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                val isLoggedIn = authRepository.isLoggedIn()
                val isOnboardingCompleted = userPreferencesRepository.isOnboardingCompleted()
                
                // Check if trial should be started for new users
                if (isLoggedIn && !subscriptionRepository.isTrialActive() && 
                    !subscriptionRepository.getSubscriptionStatus().isSubscribed) {
                    subscriptionRepository.startTrial()
                    
                    // Log event
                    firebaseAnalytics.logEvent("trial_started") {
                        param("user_id", authRepository.getUserInfo().id)
                    }
                }
                
                val initialRoute = when {
                    !isOnboardingCompleted -> Screen.Onboarding.route
                    !isLoggedIn -> Screen.Login.route
                    else -> Screen.Home.route
                }
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        initialRoute = initialRoute
                    )
                }
                
                // Log app open event
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN) {
                    param("initial_route", initialRoute)
                    param("is_logged_in", isLoggedIn.toString())
                }
            } catch (e: Exception) {
                Timber.e(e, "Error in MainViewModel initialization")
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to initialize app: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isShowingDialog = true, dialogMessage = "Logging out...") }
                
                // Log event before logout to capture user ID
                val userId = authRepository.getUserInfo().id
                firebaseAnalytics.logEvent("user_logout") {
                    param("user_id", userId)
                }
                
                authRepository.logout()
                
                _uiState.update { it.copy(isShowingDialog = false) }
                
                Timber.d("User logged out successfully")
            } catch (e: Exception) {
                Timber.e(e, "Error logging out")
                _uiState.update { 
                    it.copy(
                        isShowingDialog = false,
                        error = "Failed to log out: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun showDialog(message: String) {
        _uiState.update { it.copy(isShowingDialog = true, dialogMessage = message) }
    }
    
    fun hideDialog() {
        _uiState.update { it.copy(isShowingDialog = false, dialogMessage = null) }
    }
}

data class MainUiState(
    val isLoading: Boolean = false,
    val initialRoute: String? = null,
    val error: String? = null,
    val isShowingDialog: Boolean = false,
    val dialogMessage: String? = null
)

