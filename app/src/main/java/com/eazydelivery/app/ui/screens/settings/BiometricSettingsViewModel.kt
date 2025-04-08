package com.eazydelivery.app.ui.screens.settings

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import com.eazydelivery.app.ui.base.BaseViewModel
import com.eazydelivery.app.util.BiometricManager
import com.eazydelivery.app.util.ErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the biometric settings screen
 */
@HiltViewModel
class BiometricSettingsViewModel @Inject constructor(
    private val biometricManager: BiometricManager,
    errorHandler: ErrorHandler
) : BaseViewModel(errorHandler) {

    // UI state for the biometric settings screen
    private val _uiState = MutableStateFlow(BiometricSettingsUiState())
    val uiState: StateFlow<BiometricSettingsUiState> = _uiState.asStateFlow()

    init {
        // Load biometric settings
        loadBiometricSettings()
    }

    /**
     * Load biometric settings from BiometricManager
     */
    private fun loadBiometricSettings() {
        val isBiometricAvailable = biometricManager.canAuthenticate()
        val isBiometricEnabled = biometricManager.isBiometricEnabled()
        
        _uiState.update { currentState ->
            currentState.copy(
                isBiometricAvailable = isBiometricAvailable,
                isBiometricEnabled = isBiometricEnabled
            )
        }
    }
    
    /**
     * Enable or disable biometric authentication
     * @param enabled Whether to enable biometric authentication
     */
    fun setBiometricEnabled(enabled: Boolean) {
        biometricManager.setBiometricEnabled(enabled)
        
        _uiState.update { currentState ->
            currentState.copy(
                isBiometricEnabled = enabled
            )
        }
    }
    
    /**
     * Verify biometric authentication and enable it if successful
     * @param activity The activity to show the biometric prompt in
     */
    fun verifyAndEnableBiometric(activity: FragmentActivity) {
        try {
            biometricManager.showBiometricPrompt(
                activity = activity,
                title = "Verify Biometric",
                subtitle = "Verify your identity to enable biometric authentication",
                description = "This helps ensure that only you can enable biometric authentication",
                onSuccess = {
                    // Enable biometric authentication
                    setBiometricEnabled(true)
                    Timber.d("Biometric authentication enabled")
                },
                onError = { errorCode, errString ->
                    Timber.e("Biometric authentication error: $errorCode - $errString")
                },
                onFailed = {
                    Timber.e("Biometric authentication failed")
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Error showing biometric prompt")
        }
    }
}

/**
 * UI state for the biometric settings screen
 */
data class BiometricSettingsUiState(
    val isBiometricAvailable: Boolean = false,
    val isBiometricEnabled: Boolean = false
)
