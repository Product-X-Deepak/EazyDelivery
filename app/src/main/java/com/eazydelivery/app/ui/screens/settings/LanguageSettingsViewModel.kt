package com.eazydelivery.app.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import com.eazydelivery.app.ui.base.BaseViewModel
import com.eazydelivery.app.util.LocalizationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * ViewModel for the language settings screen
 */
@HiltViewModel
class LanguageSettingsViewModel @Inject constructor(
    private val localizationHelper: LocalizationHelper
) : BaseViewModel() {

    // UI state for the language settings screen
    private val _uiState = MutableStateFlow(LanguageSettingsUiState())
    val uiState: StateFlow<LanguageSettingsUiState> = _uiState.asStateFlow()

    init {
        // Load language settings
        loadLanguageSettings()
    }

    /**
     * Load language settings from LocalizationHelper
     */
    private fun loadLanguageSettings() {
        val currentLanguage = localizationHelper.getCurrentLanguage()
        val supportedLanguages = localizationHelper.getSupportedLanguages()
        
        _uiState.update { currentState ->
            currentState.copy(
                selectedLanguage = currentLanguage,
                languages = supportedLanguages
            )
        }
    }
    
    /**
     * Set the app language
     * @param languageCode The language code to set
     * @param context The context to update
     */
    fun setLanguage(languageCode: String, context: Context) {
        // Update the app language
        localizationHelper.setAppLanguage(languageCode)
        
        // Update the UI state
        _uiState.update { currentState ->
            currentState.copy(
                selectedLanguage = languageCode
            )
        }
    }
}

/**
 * UI state for the language settings screen
 */
data class LanguageSettingsUiState(
    val selectedLanguage: String = "",
    val languages: Map<String, String> = emptyMap()
)
