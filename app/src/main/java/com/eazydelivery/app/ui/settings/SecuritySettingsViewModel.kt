package com.eazydelivery.app.ui.settings

import androidx.lifecycle.viewModelScope
import com.eazydelivery.app.security.BiometricAuthManager
import com.eazydelivery.app.security.PinAuthManager
import com.eazydelivery.app.security.SecurePreferencesManager
import com.eazydelivery.app.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for security settings
 */
@HiltViewModel
class SecuritySettingsViewModel @Inject constructor(
    private val biometricAuthManager: BiometricAuthManager,
    private val pinAuthManager: PinAuthManager,
    private val securePreferencesManager: SecurePreferencesManager
) : BaseViewModel() {
    
    // Biometric availability
    private val _biometricAvailability = MutableStateFlow<BiometricAuthManager.BiometricAvailability>(BiometricAuthManager.BiometricAvailability.UNKNOWN)
    val biometricAvailability: StateFlow<BiometricAuthManager.BiometricAvailability> = _biometricAvailability
    
    // Biometric enabled state
    private val _isBiometricEnabled = MutableStateFlow(false)
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled
    
    // PIN enabled state
    private val _isPinEnabled = MutableStateFlow(false)
    val isPinEnabled: StateFlow<Boolean> = _isPinEnabled
    
    /**
     * Load security settings
     */
    fun loadSecuritySettings() {
        viewModelScope.launch {
            try {
                setLoading(true)
                
                // Check biometric availability
                val availability = biometricAuthManager.checkBiometricAvailability()
                _biometricAvailability.value = availability
                
                // Check if biometric is enabled
                val biometricEnabled = biometricAuthManager.isBiometricEnabled()
                _isBiometricEnabled.value = biometricEnabled
                
                // Check if PIN is enabled
                val pinEnabled = pinAuthManager.isPinEnabled()
                _isPinEnabled.value = pinEnabled
                
                Timber.d("Security settings loaded: biometric=$biometricEnabled, pin=$pinEnabled, availability=$availability")
            } catch (e: Exception) {
                errorHandler.handleException("SecuritySettingsViewModel.loadSecuritySettings", e)
            } finally {
                setLoading(false)
            }
        }
    }
    
    /**
     * Enable or disable biometric authentication
     * 
     * @param enabled true to enable biometric authentication, false to disable
     */
    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                setLoading(true)
                
                // Enable or disable biometric authentication
                biometricAuthManager.setBiometricEnabled(enabled)
                
                // Update state
                _isBiometricEnabled.value = enabled
                
                Timber.d("Biometric authentication ${if (enabled) "enabled" else "disabled"}")
            } catch (e: Exception) {
                errorHandler.handleException("SecuritySettingsViewModel.setBiometricEnabled", e)
            } finally {
                setLoading(false)
            }
        }
    }
    
    /**
     * Set a PIN
     * 
     * @param pin The PIN to set
     */
    fun setPin(pin: String) {
        viewModelScope.launch {
            try {
                setLoading(true)
                
                // Set the PIN
                pinAuthManager.setPin(pin)
                
                // Update state
                _isPinEnabled.value = true
                
                Timber.d("PIN set successfully")
            } catch (e: Exception) {
                errorHandler.handleException("SecuritySettingsViewModel.setPin", e)
            } finally {
                setLoading(false)
            }
        }
    }
    
    /**
     * Clear the PIN
     */
    fun clearPin() {
        viewModelScope.launch {
            try {
                setLoading(true)
                
                // Clear the PIN
                pinAuthManager.clearPin()
                
                // Update state
                _isPinEnabled.value = false
                
                Timber.d("PIN cleared successfully")
            } catch (e: Exception) {
                errorHandler.handleException("SecuritySettingsViewModel.clearPin", e)
            } finally {
                setLoading(false)
            }
        }
    }
    
    /**
     * Clear all secure data
     */
    fun clearAllSecureData() {
        viewModelScope.launch {
            try {
                setLoading(true)
                
                // Clear all secure preferences
                securePreferencesManager.clearAll()
                
                // Disable biometric authentication
                biometricAuthManager.setBiometricEnabled(false)
                
                // Clear PIN
                pinAuthManager.clearPin()
                
                // Update state
                _isBiometricEnabled.value = false
                _isPinEnabled.value = false
                
                Timber.d("All secure data cleared successfully")
            } catch (e: Exception) {
                errorHandler.handleException("SecuritySettingsViewModel.clearAllSecureData", e)
            } finally {
                setLoading(false)
            }
        }
    }
}
