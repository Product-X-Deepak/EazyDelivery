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
class OtpVerificationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    errorHandler: ErrorHandler
) : BaseViewModel(errorHandler) {
    
    private val _uiState = MutableStateFlow(OtpVerificationUiState())
    val uiState: StateFlow<OtpVerificationUiState> = _uiState.asStateFlow()
    
    fun setPhoneNumber(phoneNumber: String) {
        _uiState.update { it.copy(phoneNumber = phoneNumber) }
    }
    
    fun updateOtp(otp: String) {
        if (otp.length <= 6 && otp.all { it.isDigit() }) {
            _uiState.update { it.copy(otp = otp) }
        }
    }
    
    fun verifyOtp() {
        if (uiState.value.otp.length != 6) return
        
        _uiState.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            try {
                val result = authRepository.verifyOtp(
                    phoneNumber = uiState.value.phoneNumber,
                    otp = uiState.value.otp
                )
                
                result.fold(
                    onSuccess = { userInfo ->
                        Timber.d("OTP verification successful for user: ${userInfo.phone}")
                        _uiState.update { 
                            it.copy(
                                verificationSuccess = true, 
                                isNewUser = userInfo.isNewUser,
                                isLoading = false
                            ) 
                        }
                    },
                    onFailure = { exception ->
                        Timber.e(exception, "OTP verification failed")
                        _uiState.update { 
                            it.copy(
                                errorMessage = exception.message ?: "Verification failed",
                                isLoading = false
                            ) 
                        }
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error during OTP verification")
                _uiState.update { 
                    it.copy(
                        errorMessage = e.message ?: "An error occurred",
                        isLoading = false
                    ) 
                }
            }
        }
    }
    
    fun resendOtp() {
        _uiState.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            try {
                val result = authRepository.sendOtp(uiState.value.phoneNumber)
                
                result.fold(
                    onSuccess = {
                        Timber.d("OTP resent successfully to ${uiState.value.phoneNumber}")
                        _uiState.update { 
                            it.copy(
                                otp = "",
                                isLoading = false
                            ) 
                        }
                    },
                    onFailure = { exception ->
                        Timber.e(exception, "Failed to resend OTP")
                        _uiState.update { 
                            it.copy(
                                errorMessage = exception.message ?: "Failed to resend OTP",
                                isLoading = false
                            ) 
                        }
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error resending OTP")
                _uiState.update { 
                    it.copy(
                        errorMessage = e.message ?: "An error occurred",
                        isLoading = false
                    ) 
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

data class OtpVerificationUiState(
    val phoneNumber: String = "",
    val otp: String = "",
    val isLoading: Boolean = false,
    val verificationSuccess: Boolean = false,
    val isNewUser: Boolean = false,
    val errorMessage: String? = null
)
