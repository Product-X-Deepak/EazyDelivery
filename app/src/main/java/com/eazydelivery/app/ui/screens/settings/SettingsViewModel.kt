package com.eazydelivery.app.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.viewModelScope
import com.eazydelivery.app.domain.repository.PermissionRepository
import com.eazydelivery.app.domain.repository.ServiceRepository
import com.eazydelivery.app.domain.repository.UserPreferencesRepository
import com.eazydelivery.app.ui.base.BaseViewModel
import com.eazydelivery.app.util.ErrorHandler
import com.eazydelivery.app.util.LocaleHelper
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
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val serviceRepository: ServiceRepository,
    private val permissionRepository: PermissionRepository,
    @ApplicationContext private val context: Context,
    errorHandler: ErrorHandler
) : BaseViewModel(errorHandler) {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                // Load language
                val languageCode = userPreferencesRepository.getLanguage().getOrDefault("en")
                
                // Load theme mode
                val themeMode = userPreferencesRepository.getThemeMode().getOrDefault(0)
                val darkModeEnabled = themeMode == 2 // 0 = System default, 1 = Light, 2 = Dark
                
                // Load notification settings
                val notificationsEnabled = userPreferencesRepository.getNotificationsEnabled().getOrDefault(true)
                
                // Load auto-start settings
                val autoStartEnabled = userPreferencesRepository.getAutoStartEnabled().getOrDefault(true)
                
                // Load battery optimization status
                val batteryOptimizationDisabled = serviceRepository.getBatteryOptimizationStatus().getOrDefault(false)
                
                // Load accessibility service status
                val accessibilityServiceEnabled = serviceRepository.isAccessibilityServiceEnabled().getOrDefault(false)
                
                _uiState.update {
                    it.copy(
                        languageCode = languageCode,
                        darkModeEnabled = darkModeEnabled,
                        notificationsEnabled = notificationsEnabled,
                        autoStartEnabled = autoStartEnabled,
                        batteryOptimizationDisabled = batteryOptimizationDisabled,
                        accessibilityServiceEnabled = accessibilityServiceEnabled
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading settings")
            }
        }
    }
    
    fun toggleDarkMode() {
        viewModelScope.launch {
            try {
                val newThemeMode = if (uiState.value.darkModeEnabled) 1 else 2 // 1 = Light, 2 = Dark
                userPreferencesRepository.setThemeMode(newThemeMode).getOrThrow()
                _uiState.update { it.copy(darkModeEnabled = !it.darkModeEnabled) }
            } catch (e: Exception) {
                Timber.e(e, "Error toggling dark mode")
            }
        }
    }
    
    fun toggleNotifications() {
        viewModelScope.launch {
            try {
                val newValue = !uiState.value.notificationsEnabled
                userPreferencesRepository.setNotificationsEnabled(newValue).getOrThrow()
                _uiState.update { it.copy(notificationsEnabled = newValue) }
                
                // Request notification permission if enabling
                if (newValue) {
                    permissionRepository.requestNotificationPermission(context)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error toggling notifications")
            }
        }
    }
    
    fun toggleAutoStart() {
        viewModelScope.launch {
            try {
                val newValue = !uiState.value.autoStartEnabled
                userPreferencesRepository.setAutoStartEnabled(newValue).getOrThrow()
                _uiState.update { it.copy(autoStartEnabled = newValue) }
            } catch (e: Exception) {
                Timber.e(e, "Error toggling auto-start")
            }
        }
    }
    
    fun toggleBatteryOptimization() {
        viewModelScope.launch {
            try {
                permissionRepository.requestBatteryOptimizationPermission(context)
                // The actual status will be updated when the settings screen is resumed
            } catch (e: Exception) {
                Timber.e(e, "Error toggling battery optimization")
            }
        }
    }
    
    fun openAccessibilitySettings() {
        viewModelScope.launch {
            try {
                permissionRepository.requestAccessibilityPermission(context)
                // The actual status will be updated when the settings screen is resumed
            } catch (e: Exception) {
                Timber.e(e, "Error opening accessibility settings")
            }
        }
    }
}

data class SettingsUiState(
    val languageCode: String = "en",
    val darkModeEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val autoStartEnabled: Boolean = true,
    val batteryOptimizationDisabled: Boolean = false,
    val accessibilityServiceEnabled: Boolean = false
)
