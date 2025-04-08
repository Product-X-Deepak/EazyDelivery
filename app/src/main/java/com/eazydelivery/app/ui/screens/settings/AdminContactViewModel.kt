package com.eazydelivery.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.eazydelivery.app.ui.base.BaseViewModel
import com.eazydelivery.app.util.AdminContactSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * ViewModel for the AdminContactScreen
 */
@HiltViewModel
class AdminContactViewModel @Inject constructor(
    private val adminContactSettings: AdminContactSettings
) : BaseViewModel() {

    // UI state for the admin contact screen
    private val _uiState = MutableStateFlow(AdminContactUiState())
    val uiState: StateFlow<AdminContactUiState> = _uiState.asStateFlow()

    init {
        // Load admin contact information
        loadAdminContactInfo()
    }

    /**
     * Load admin contact information from AdminContactSettings
     */
    private fun loadAdminContactInfo() {
        _uiState.update { currentState ->
            currentState.copy(
                adminEmail = adminContactSettings.getAdminEmail(),
                adminPhone = adminContactSettings.getAdminPhone()
            )
        }
    }
}

/**
 * UI state for the admin contact screen
 */
data class AdminContactUiState(
    val adminEmail: String = "",
    val adminPhone: String = ""
)
