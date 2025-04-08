package com.eazydelivery.app.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.viewModelScope
import com.eazydelivery.app.accessibility.AccessibilityManager
import com.eazydelivery.app.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for accessibility settings
 */
@HiltViewModel
class AccessibilitySettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accessibilityManager: AccessibilityManager
) : BaseViewModel() {
    
    companion object {
        private const val PREFS_NAME = "accessibility_preferences"
        private const val KEY_HIGH_CONTRAST = "high_contrast_enabled"
        private const val KEY_REDUCE_MOTION = "reduce_motion_enabled"
        private const val KEY_LARGE_TOUCH_TARGETS = "large_touch_targets_enabled"
        private const val KEY_SIMPLIFIED_UI = "simplified_ui_enabled"
    }
    
    // Shared preferences for storing accessibility settings
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    // Accessibility settings
    private val _accessibilitySettings = MutableStateFlow(AccessibilitySettings())
    val accessibilitySettings: StateFlow<AccessibilitySettings> = _accessibilitySettings
    
    /**
     * Load accessibility settings
     */
    fun loadAccessibilitySettings() {
        viewModelScope.launch {
            try {
                setLoading(true)
                
                // Load settings from shared preferences
                val settings = AccessibilitySettings(
                    highContrastEnabled = sharedPreferences.getBoolean(KEY_HIGH_CONTRAST, false),
                    reduceMotionEnabled = sharedPreferences.getBoolean(KEY_REDUCE_MOTION, accessibilityManager.isReduceMotionEnabled()),
                    largeTouchTargetsEnabled = sharedPreferences.getBoolean(KEY_LARGE_TOUCH_TARGETS, false),
                    simplifiedUIEnabled = sharedPreferences.getBoolean(KEY_SIMPLIFIED_UI, false)
                )
                
                _accessibilitySettings.value = settings
                
                Timber.d("Accessibility settings loaded: $settings")
            } catch (e: Exception) {
                errorHandler.handleException("AccessibilitySettingsViewModel.loadAccessibilitySettings", e)
            } finally {
                setLoading(false)
            }
        }
    }
    
    /**
     * Set high contrast mode
     * 
     * @param enabled Whether high contrast mode is enabled
     */
    fun setHighContrastEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                // Update shared preferences
                sharedPreferences.edit().putBoolean(KEY_HIGH_CONTRAST, enabled).apply()
                
                // Update state
                _accessibilitySettings.value = _accessibilitySettings.value.copy(
                    highContrastEnabled = enabled
                )
                
                Timber.d("High contrast mode ${if (enabled) "enabled" else "disabled"}")
            } catch (e: Exception) {
                errorHandler.handleException("AccessibilitySettingsViewModel.setHighContrastEnabled", e)
            }
        }
    }
    
    /**
     * Set reduce motion mode
     * 
     * @param enabled Whether reduce motion mode is enabled
     */
    fun setReduceMotionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                // Update shared preferences
                sharedPreferences.edit().putBoolean(KEY_REDUCE_MOTION, enabled).apply()
                
                // Update state
                _accessibilitySettings.value = _accessibilitySettings.value.copy(
                    reduceMotionEnabled = enabled
                )
                
                Timber.d("Reduce motion mode ${if (enabled) "enabled" else "disabled"}")
            } catch (e: Exception) {
                errorHandler.handleException("AccessibilitySettingsViewModel.setReduceMotionEnabled", e)
            }
        }
    }
    
    /**
     * Set large touch targets mode
     * 
     * @param enabled Whether large touch targets mode is enabled
     */
    fun setLargeTouchTargetsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                // Update shared preferences
                sharedPreferences.edit().putBoolean(KEY_LARGE_TOUCH_TARGETS, enabled).apply()
                
                // Update state
                _accessibilitySettings.value = _accessibilitySettings.value.copy(
                    largeTouchTargetsEnabled = enabled
                )
                
                Timber.d("Large touch targets mode ${if (enabled) "enabled" else "disabled"}")
            } catch (e: Exception) {
                errorHandler.handleException("AccessibilitySettingsViewModel.setLargeTouchTargetsEnabled", e)
            }
        }
    }
    
    /**
     * Set simplified UI mode
     * 
     * @param enabled Whether simplified UI mode is enabled
     */
    fun setSimplifiedUIEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                // Update shared preferences
                sharedPreferences.edit().putBoolean(KEY_SIMPLIFIED_UI, enabled).apply()
                
                // Update state
                _accessibilitySettings.value = _accessibilitySettings.value.copy(
                    simplifiedUIEnabled = enabled
                )
                
                Timber.d("Simplified UI mode ${if (enabled) "enabled" else "disabled"}")
            } catch (e: Exception) {
                errorHandler.handleException("AccessibilitySettingsViewModel.setSimplifiedUIEnabled", e)
            }
        }
    }
    
    /**
     * Reset all accessibility settings to defaults
     */
    fun resetAccessibilitySettings() {
        viewModelScope.launch {
            try {
                setLoading(true)
                
                // Clear shared preferences
                sharedPreferences.edit().clear().apply()
                
                // Reset to defaults
                val settings = AccessibilitySettings(
                    highContrastEnabled = false,
                    reduceMotionEnabled = accessibilityManager.isReduceMotionEnabled(),
                    largeTouchTargetsEnabled = false,
                    simplifiedUIEnabled = false
                )
                
                _accessibilitySettings.value = settings
                
                Timber.d("Accessibility settings reset to defaults")
            } catch (e: Exception) {
                errorHandler.handleException("AccessibilitySettingsViewModel.resetAccessibilitySettings", e)
            } finally {
                setLoading(false)
            }
        }
    }
    
    /**
     * Data class for accessibility settings
     */
    data class AccessibilitySettings(
        val highContrastEnabled: Boolean = false,
        val reduceMotionEnabled: Boolean = false,
        val largeTouchTargetsEnabled: Boolean = false,
        val simplifiedUIEnabled: Boolean = false
    )
}
