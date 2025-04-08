package com.eazydelivery.app.ui.screens.auth

import androidx.lifecycle.viewModelScope
import com.eazydelivery.app.domain.repository.AuthRepository
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
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    errorHandler: ErrorHandler
) : BaseViewModel(errorHandler) {
    
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()
    
    fun updateName(name: String) {
        _uiState.update { it.copy(name = name, nameError = null) }
    }
    
    fun updateEmail(email: String) {
        _uiState.update { it.copy(email = email, emailError = null) }
    }
    
    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null) }
    }
    
    fun updateConfirmPassword(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword, confirmPasswordError = null) }
    }
    
    fun register() {
        if (!validateInputs()) return
        
        _uiState.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            try {
                val result = authRepository.register(
                    email = uiState.value.email,
                    password = uiState.value.password,
                    name = uiState.value.name
                )
                
                result.fold(
                    onSuccess = { userInfo ->
                        Timber.d("Registration successful for user: ${userInfo.email}")
                        _uiState.update { it.copy(registrationSuccess = true, isLoading = false) }
                    },
                    onFailure = { exception ->
                        Timber.e(exception, "Registration failed")
                        _uiState.update { 
                            it.copy(
                                errorMessage = exception.message ?: "Registration failed",
                                isLoading = false
                            ) 
                        }
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Registration failed with exception")
                _uiState.update { 
                    it.copy(
                        errorMessage = e.message ?: "Registration failed",
                        isLoading = false
                    ) 
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    private fun validateInputs(): Boolean {
        var isValid = true
        
        if (uiState.value.name.isBlank()) {
            _uiState.update { it.copy(nameError = "Name cannot be empty") }
            isValid = false
        }
        
        if (uiState.value.email.isBlank()) {
            _uiState.update { it.copy(emailError = "Email cannot be empty") }
            isValid = false
        } else if (!isValidEmail(uiState.value.email)) {
            _uiState.update { it.copy(emailError = "Please enter a valid email") }
            isValid = false
        }
        
        if (uiState.value.password.isBlank()) {
            _uiState.update { it.copy(passwordError = "Password cannot be empty") }
            isValid = false
        } else if (uiState.value.password.length < 6) {
            _uiState.update { it.copy(passwordError = "Password must be at least 6 characters") }
            isValid = false
        }
        
        if (uiState.value.confirmPassword.isBlank()) {
            _uiState.update { it.copy(confirmPasswordError = "Please confirm your password") }
            isValid = false
        } else if (uiState.value.password != uiState.value.confirmPassword) {
            _uiState.update { it.copy(confirmPasswordError = "Passwords do not match") }
            isValid = false
        }
        
        return isValid
    }
    
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val registrationSuccess: Boolean = false,
    val errorMessage: String? = null,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null
)
