package com.eazydelivery.app.ui.screens.auth

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.eazydelivery.app.R
import com.eazydelivery.app.domain.repository.AuthRepository
import com.eazydelivery.app.domain.repository.UserPreferencesRepository
import com.eazydelivery.app.ui.base.BaseViewModel
import com.eazydelivery.app.util.ErrorHandler
import com.eazydelivery.app.util.LocaleHelper
import com.eazydelivery.app.util.Validator
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
class PhoneLoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val validator: Validator,
    @ApplicationContext private val context: Context,
    errorHandler: ErrorHandler
) : BaseViewModel(errorHandler) {

    private val _uiState = MutableStateFlow(PhoneLoginUiState())
    val uiState: StateFlow<PhoneLoginUiState> = _uiState.asStateFlow()

    init {
        loadLanguagePreference()
    }

    private fun loadLanguagePreference() {
        viewModelScope.launch {
            try {
                val languageCode = userPreferencesRepository.getLanguage().getOrDefault("en")
                // Apply the language
                LocaleHelper.setLocale(context, languageCode)
            } catch (e: Exception) {
                Timber.e(e, "Error loading language preference")
            }
        }
    }

    fun updatePhoneNumber(phoneNumber: String) {
        // Accept any input initially to allow for international formats
        _uiState.update { it.copy(phoneNumber = phoneNumber, phoneNumberError = null) }

        // Clean the input for validation
        val cleanedNumber = phoneNumber.replace("\\s".toRegex(), "")
                                     .replace("-", "")
                                     .replace("(", "")
                                     .replace(")", "")

        // For Indian numbers (default), try to format if it looks like a valid number
        if (cleanedNumber.all { it.isDigit() }) {
            // For Indian numbers, we expect 10 digits
            if (cleanedNumber.length == Constants.PHONE_NUMBER_LENGTH) {
                try {
                    // Format with Indian region code
                    val formattedNumber = validator.formatPhoneNumber(cleanedNumber)
                    if (formattedNumber != null) {
                        // Update with the formatted number (includes +91 and spacing)
                        _uiState.update { it.copy(formattedPhoneNumber = formattedNumber) }
                    }
                } catch (e: Exception) {
                    // Ignore formatting errors during typing
                    Timber.d("Could not format phone number: ${e.message}")
                }
            }
            // For international numbers that might include country code
            else if (cleanedNumber.length > Constants.PHONE_NUMBER_LENGTH &&
                     (cleanedNumber.startsWith("+") || cleanedNumber.startsWith("00"))) {
                try {
                    // Try to format as an international number
                    val formattedNumber = validator.formatFullPhoneNumber(cleanedNumber)
                    if (formattedNumber != null) {
                        _uiState.update { it.copy(formattedPhoneNumber = formattedNumber) }
                    }
                } catch (e: Exception) {
                    // Ignore formatting errors during typing
                    Timber.d("Could not format international phone number: ${e.message}")
                }
            }
        }
    }

    fun sendOtp() {
        if (!validatePhoneNumber()) return

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                // Always add the Indian country code (+91) to the phone number
                val fullPhoneNumber = "+91${uiState.value.phoneNumber}"
                val result = authRepository.sendOtp(fullPhoneNumber)

                result.fold(
                    onSuccess = {
                        Timber.d("OTP sent successfully to $fullPhoneNumber")
                        _uiState.update { it.copy(otpSent = true, isLoading = false) }
                    },
                    onFailure = { exception ->
                        Timber.e(exception, "Failed to send OTP")
                        _uiState.update {
                            it.copy(
                                errorMessage = exception.message ?: "Failed to send OTP",
                                isLoading = false
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error sending OTP")
                _uiState.update {
                    it.copy(
                        errorMessage = e.message ?: "An error occurred",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            try {
                userPreferencesRepository.setLanguage(languageCode)
                LocaleHelper.setLocale(context, languageCode)
                // Force UI update by updating state
                _uiState.update { it.copy(languageCode = languageCode) }
            } catch (e: Exception) {
                Timber.e(e, "Error setting language")
            }
        }
    }

    private fun validatePhoneNumber(): Boolean {
        val phoneNumber = uiState.value.phoneNumber

        // First check if empty
        if (phoneNumber.isBlank()) {
            _uiState.update { it.copy(phoneNumberError = context.getString(R.string.phone_number_empty)) }
            return false
        }

        // Clean the input for validation
        val cleanedNumber = phoneNumber.replace("\\s".toRegex(), "")
                                     .replace("-", "")
                                     .replace("(", "")
                                     .replace(")", "")

        // Determine if this is an international number or an Indian number
        val isInternationalFormat = cleanedNumber.startsWith("+") || cleanedNumber.startsWith("00")

        // Use the appropriate validation method
        val validationResult = if (isInternationalFormat) {
            // For international format, use full phone number validation
            validator.validateFullPhoneNumber(cleanedNumber)
        } else {
            // For Indian numbers (default), validate as a national number
            validator.validatePhoneNumber(cleanedNumber)
        }

        if (!validationResult.isValid) {
            _uiState.update { it.copy(phoneNumberError = validationResult.errorMessage ?: context.getString(R.string.phone_number_invalid)) }
            return false
        }

        // Format the phone number for display in the confirmation dialog
        val formattedNumber = if (isInternationalFormat) {
            validator.formatFullPhoneNumber(cleanedNumber) ?: cleanedNumber
        } else {
            validator.formatPhoneNumber(cleanedNumber) ?: cleanedNumber
        }

        // Show confirmation dialog before proceeding
        _uiState.update { it.copy(showConfirmationDialog = true, formattedPhoneNumber = formattedNumber.toString()) }
        return false // Return false to prevent immediate OTP sending - will be triggered after confirmation
    }

    /**
     * Confirm phone number after user verification
     */
    fun confirmPhoneNumber() {
        _uiState.update { it.copy(showConfirmationDialog = false, isLoading = true) }

        viewModelScope.launch {
            try {
                val phoneNumber = uiState.value.phoneNumber
                val cleanedNumber = phoneNumber.replace("\\s".toRegex(), "")
                                             .replace("-", "")
                                             .replace("(", "")
                                             .replace(")", "")

                // Determine if this is an international number or an Indian number
                val isInternationalFormat = cleanedNumber.startsWith("+") || cleanedNumber.startsWith("00")

                // Prepare the phone number for the API call
                val fullPhoneNumber = if (isInternationalFormat) {
                    // Already has country code
                    cleanedNumber
                } else {
                    // Add Indian country code if not present
                    "+91${cleanedNumber}"
                }

                val result = authRepository.sendOtp(fullPhoneNumber)

                result.fold(
                    onSuccess = {
                        Timber.d("OTP sent successfully to $fullPhoneNumber")
                        _uiState.update { it.copy(otpSent = true, isLoading = false) }
                    },
                    onFailure = { exception ->
                        Timber.e(exception, "Failed to send OTP")
                        _uiState.update {
                            it.copy(
                                errorMessage = exception.message ?: "Failed to send OTP",
                                isLoading = false
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error sending OTP")
                _uiState.update {
                    it.copy(
                        errorMessage = e.message ?: "An error occurred",
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Cancel phone number confirmation
     */
    fun cancelConfirmation() {
        _uiState.update { it.copy(showConfirmationDialog = false) }
    }



    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

data class PhoneLoginUiState(
    val countryCode: String = "+91", // Default to India
    val phoneNumber: String = "",
    val formattedPhoneNumber: String? = null,
    val isLoading: Boolean = false,
    val otpSent: Boolean = false,
    val errorMessage: String? = null,
    val phoneNumberError: String? = null,
    val showConfirmationDialog: Boolean = false,
    val languageCode: String = "en" // Default language
)
