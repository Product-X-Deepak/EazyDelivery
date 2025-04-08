package com.eazydelivery.app.ui.screens.settings

import androidx.lifecycle.viewModelScope
import com.eazydelivery.app.domain.repository.UserPreferencesRepository
import com.eazydelivery.app.ui.base.BaseViewModel
import com.eazydelivery.app.util.ErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PrioritizationSettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    errorHandler: ErrorHandler
) : BaseViewModel(errorHandler) {
    
    private val _uiState = MutableStateFlow(PrioritizationSettingsUiState())
    val uiState: StateFlow<PrioritizationSettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                // Load earnings weight
                val earningsWeight = userPreferencesRepository.getEarningsWeight().getOrDefault(0.5f)
                
                // Load distance weight
                val distanceWeight = userPreferencesRepository.getDistanceWeight().getOrDefault(0.3f)
                
                // Load time weight
                val timeWeight = userPreferencesRepository.getTimeWeight().getOrDefault(0.2f)
                
                // Load accept medium priority setting
                val acceptMediumPriority = userPreferencesRepository.getAcceptMediumPriority().getOrDefault(false)
                
                _uiState.update {
                    it.copy(
                        earningsWeight = earningsWeight,
                        distanceWeight = distanceWeight,
                        timeWeight = timeWeight,
                        acceptMediumPriority = acceptMediumPriority
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading prioritization settings")
            }
        }
    }
    
    fun updateEarningsWeight(weight: Float) {
        viewModelScope.launch {
            try {
                userPreferencesRepository.setEarningsWeight(weight)
                _uiState.update { it.copy(earningsWeight = weight) }
            } catch (e: Exception) {
                Timber.e(e, "Error updating earnings weight")
            }
        }
    }
    
    fun updateDistanceWeight(weight: Float) {
        viewModelScope.launch {
            try {
                userPreferencesRepository.setDistanceWeight(weight)
                _uiState.update { it.copy(distanceWeight = weight) }
            } catch (e: Exception) {
                Timber.e(e, "Error updating distance weight")
            }
        }
    }
    
    fun updateTimeWeight(weight: Float) {
        viewModelScope.launch {
            try {
                userPreferencesRepository.setTimeWeight(weight)
                _uiState.update { it.copy(timeWeight = weight) }
            } catch (e: Exception) {
                Timber.e(e, "Error updating time weight")
            }
        }
    }
    
    fun updateAcceptMediumPriority(accept: Boolean) {
        viewModelScope.launch {
            try {
                userPreferencesRepository.setAcceptMediumPriority(accept)
                _uiState.update { it.copy(acceptMediumPriority = accept) }
            } catch (e: Exception) {
                Timber.e(e, "Error updating accept medium priority setting")
            }
        }
    }
}

data class PrioritizationSettingsUiState(
    val earningsWeight: Float = 0.5f,
    val distanceWeight: Float = 0.3f,
    val timeWeight: Float = 0.2f,
    val acceptMediumPriority: Boolean = false
)
