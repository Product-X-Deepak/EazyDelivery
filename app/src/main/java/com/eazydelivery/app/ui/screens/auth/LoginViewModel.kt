package com.eazydelivery.app.ui.screens.auth

import androidx.lifecycle.viewModelScope
import com.eazydelivery.app.domain.repository.AuthRepository
import com.eazydelivery.app.ui.base.BaseViewModel
import com.eazydelivery.app.util.ErrorHandler
import com.eazydelivery.app.util.Validator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val validator: Validator,
    errorHandler: ErrorHandler
) : BaseViewModel(errorHandler) {
    
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    fun updateEmail(email: String) {
        _uiState.update { it.copy(email = email, emailError = null) }
    }
    
    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null) }
    }
    
    fun login() {
        if (!validateInputs()) return
        
        _uiState.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            try {
                val result = authRepository.login(
                    email = uiState.value.email,
                    password = uiState.value.password
                )
                
                result.fold(
                    onSuccess = { userInfo ->
                        Timber.d("Login successful for user: ${userInfo.email}")
                        _uiState.update { it.copy(loginSuccess = true, isLoading = false) }
                        emitSuccess("Login successful")
                    },
                    onFailure = { exception ->
                        Timber.e(exception, "Login failed")
                        _uiState.update { 
                            it.copy(
                                errorMessage = exception.message ?: "Login failed",
                                isLoading = false
                            ) 
                        }
                        emitError(exception.message ?: "Login failed")
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Login failed with exception")
                _uiState.update { 
                    it.copy(
                        errorMessage = e.message ?: "Login failed",
                        isLoading = false
                    ) 
                }
                emitError(e.message ?: "Login failed")
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    private fun validateInputs(): Boolean {
        var isValid = true
        
        // Validate email
        val emailValidation = validator.validateEmail(uiState.value.email)
        if (!emailValidation.isValid) {
            _uiState.update { it.copy(emailError = emailValidation.errorMessage) }
            isValid = false
        }
        
        // Validate password
        val passwordValidation = validator.validatePassword(uiState.value.password)
        if (!passwordValidation.isValid) {
            _uiState.update { it.copy(passwordError = passwordValidation.errorMessage) }
            isValid = false
        }
        
        return isValid
    }
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val loginSuccess: Boolean = false,
    val errorMessage: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null
)
