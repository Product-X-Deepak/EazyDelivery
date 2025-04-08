package com.eazydelivery.app.ui.screens.settings

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.eazydelivery.app.domain.repository.UserPreferencesRepository
import com.eazydelivery.app.ui.base.BaseViewModel
import com.eazydelivery.app.util.ErrorHandler
import com.eazydelivery.app.util.LocalizationHelper
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
class LanguageViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val localizationHelper: LocalizationHelper,
    errorHandler: ErrorHandler
) : BaseViewModel(errorHandler) {

    private val _uiState = MutableStateFlow(LanguageUiState())
    val uiState: StateFlow<LanguageUiState> = _uiState.asStateFlow()

    init {
        loadLanguagePreference()
    }

    private fun loadLanguagePreference() {
        viewModelScope.launch {
            try {
                val languageCode = localizationHelper.getCurrentLanguage()
                _uiState.update { it.copy(selectedLanguage = languageCode, showAllLanguages = false) }
            } catch (e: Exception) {
                Timber.e(e, "Error loading language preference")
            }
        }
    }

    fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            try {
                // Save language preference using LocalizationHelper
                localizationHelper.setAppLanguage(languageCode)

                // Update UI state
                _uiState.update { it.copy(selectedLanguage = languageCode) }

                // Also save to user preferences repository for backward compatibility
                userPreferencesRepository.setLanguage(languageCode)
            } catch (e: Exception) {
                Timber.e(e, "Error setting language")
            }
        }
    }

    /**
     * Toggle showing all languages
     */
    fun toggleShowAllLanguages() {
        _uiState.update { it.copy(showAllLanguages = !it.showAllLanguages) }
    }
}

data class LanguageUiState(
    val selectedLanguage: String = "en",
    val showAllLanguages: Boolean = false
)
