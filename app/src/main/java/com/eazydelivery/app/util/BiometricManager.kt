package com.eazydelivery.app.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility for handling biometric authentication
 */
@Singleton
class BiometricManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureStorage: SecureStorage,
    private val errorHandler: ErrorHandler
) {

    companion object {
        private const val BIOMETRIC_ENABLED_KEY = "biometric_auth_enabled"

        // Biometric authentication options
        private const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }

    /**
     * Checks if biometric authentication is available on the device
     * @return true if biometric authentication is available, false otherwise
     */
    fun canAuthenticate(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Timber.d("No biometric hardware available")
                false
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Timber.d("Biometric hardware unavailable")
                false
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Timber.d("No biometrics enrolled")
                false
            }
            else -> {
                Timber.d("Biometric status unknown")
                false
            }
        }
    }

    /**
     * Checks if biometric authentication is enabled by the user
     * @return true if biometric authentication is enabled, false otherwise
     */
    fun isBiometricEnabled(): Boolean {
        return secureStorage.getBoolean(BIOMETRIC_ENABLED_KEY, false)
    }

    /**
     * Enables or disables biometric authentication
     * @param enabled true to enable biometric authentication, false to disable
     */
    fun setBiometricEnabled(enabled: Boolean) {
        secureStorage.saveBoolean(BIOMETRIC_ENABLED_KEY, enabled)
    }

    /**
     * Shows biometric prompt for authentication
     * @param activity The activity to show the prompt in
     * @param title The title of the prompt
     * @param subtitle The subtitle of the prompt
     * @param description The description of the prompt
     * @param negativeButtonText The text for the negative button
     * @param onSuccess Callback for successful authentication
     * @param onError Callback for authentication error
     * @param onFailed Callback for authentication failure
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        description: String,
        negativeButtonText: String = "Cancel",
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit = { _, _ -> },
        onFailed: () -> Unit = {}
    ) {
        try {
            val executor = ContextCompat.getMainExecutor(activity)

            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errorCode, errString)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            }

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDescription(description)
                .setNegativeButtonText(negativeButtonText)
                .setConfirmationRequired(false)
                .setAllowedAuthenticators(AUTHENTICATORS)
                .build()

            val biometricPrompt = BiometricPrompt(activity, executor, callback)
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            errorHandler.handleException("BiometricManager.showBiometricPrompt", e)
        }
    }
}

