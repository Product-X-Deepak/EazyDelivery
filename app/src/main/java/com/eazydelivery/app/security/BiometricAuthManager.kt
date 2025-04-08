package com.eazydelivery.app.security

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.eazydelivery.app.R
import com.eazydelivery.app.util.error.AppError
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for biometric authentication
 */
@Singleton
class BiometricAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securePreferencesManager: SecurePreferencesManager
) {
    /**
     * Check if biometric authentication is available
     * 
     * @return The biometric availability status
     */
    fun checkBiometricAvailability(): BiometricAvailability {
        val biometricManager = BiometricManager.from(context)
        
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricAvailability.SECURITY_UPDATE_REQUIRED
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> BiometricAvailability.UNSUPPORTED
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> BiometricAvailability.UNKNOWN
            else -> BiometricAvailability.UNKNOWN
        }
    }
    
    /**
     * Check if biometric authentication is enabled
     * 
     * @return true if biometric authentication is enabled, false otherwise
     */
    fun isBiometricEnabled(): Boolean {
        return securePreferencesManager.getBoolean(SecurePreferencesManager.KEY_BIOMETRIC_ENABLED, false)
    }
    
    /**
     * Enable or disable biometric authentication
     * 
     * @param enabled true to enable biometric authentication, false to disable
     */
    fun setBiometricEnabled(enabled: Boolean) {
        securePreferencesManager.putBoolean(SecurePreferencesManager.KEY_BIOMETRIC_ENABLED, enabled)
        Timber.d("Biometric authentication ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Show biometric prompt for authentication
     * 
     * @param activity The activity
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
        title: String = context.getString(R.string.biometric_prompt_title),
        subtitle: String = context.getString(R.string.biometric_prompt_subtitle),
        description: String = context.getString(R.string.biometric_prompt_description),
        negativeButtonText: String = context.getString(R.string.biometric_prompt_negative_button),
        onSuccess: () -> Unit,
        onError: (Int, CharSequence) -> Unit,
        onFailed: () -> Unit
    ) {
        try {
            // Check if biometric authentication is available
            val availability = checkBiometricAvailability()
            if (availability != BiometricAvailability.AVAILABLE) {
                Timber.e("Biometric authentication not available: $availability")
                onError(BiometricPrompt.ERROR_HW_UNAVAILABLE, "Biometric authentication not available: $availability")
                return
            }
            
            // Create biometric prompt
            val executor = ContextCompat.getMainExecutor(context)
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Timber.d("Biometric authentication succeeded")
                    onSuccess()
                }
                
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Timber.e("Biometric authentication error: $errorCode - $errString")
                    onError(errorCode, errString)
                }
                
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Timber.e("Biometric authentication failed")
                    onFailed()
                }
            }
            
            val biometricPrompt = BiometricPrompt(activity, executor, callback)
            
            // Create prompt info
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDescription(description)
                .setNegativeButtonText(negativeButtonText)
                .setConfirmationRequired(true)
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build()
            
            // Show biometric prompt
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            Timber.e(e, "Error showing biometric prompt")
            onError(BiometricPrompt.ERROR_VENDOR, e.message ?: "Unknown error")
        }
    }
    
    /**
     * Encrypt data using biometric authentication
     * 
     * @param activity The activity
     * @param data The data to encrypt
     * @param onSuccess Callback for successful encryption
     * @param onError Callback for encryption error
     */
    fun encryptWithBiometric(
        activity: FragmentActivity,
        data: String,
        onSuccess: (String) -> Unit,
        onError: (AppError) -> Unit
    ) {
        try {
            // Show biometric prompt
            showBiometricPrompt(
                activity = activity,
                title = context.getString(R.string.biometric_encrypt_title),
                subtitle = context.getString(R.string.biometric_encrypt_subtitle),
                description = context.getString(R.string.biometric_encrypt_description),
                onSuccess = {
                    try {
                        // Encrypt data
                        val encryptedData = encryptData(data)
                        onSuccess(encryptedData)
                    } catch (e: Exception) {
                        Timber.e(e, "Error encrypting data with biometric")
                        onError(AppError.Security.EncryptionError("Failed to encrypt data with biometric", e))
                    }
                },
                onError = { errorCode, errString ->
                    Timber.e("Biometric encryption error: $errorCode - $errString")
                    onError(AppError.Security.OperationError("Biometric encryption error: $errString"))
                },
                onFailed = {
                    Timber.e("Biometric encryption failed")
                    onError(AppError.Security.OperationError("Biometric encryption failed"))
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Error encrypting with biometric")
            onError(AppError.Security.EncryptionError("Failed to encrypt with biometric", e))
        }
    }
    
    /**
     * Decrypt data using biometric authentication
     * 
     * @param activity The activity
     * @param encryptedData The encrypted data
     * @param onSuccess Callback for successful decryption
     * @param onError Callback for decryption error
     */
    fun decryptWithBiometric(
        activity: FragmentActivity,
        encryptedData: String,
        onSuccess: (String) -> Unit,
        onError: (AppError) -> Unit
    ) {
        try {
            // Show biometric prompt
            showBiometricPrompt(
                activity = activity,
                title = context.getString(R.string.biometric_decrypt_title),
                subtitle = context.getString(R.string.biometric_decrypt_subtitle),
                description = context.getString(R.string.biometric_decrypt_description),
                onSuccess = {
                    try {
                        // Decrypt data
                        val decryptedData = decryptData(encryptedData)
                        onSuccess(decryptedData)
                    } catch (e: Exception) {
                        Timber.e(e, "Error decrypting data with biometric")
                        onError(AppError.Security.DecryptionError("Failed to decrypt data with biometric", e))
                    }
                },
                onError = { errorCode, errString ->
                    Timber.e("Biometric decryption error: $errorCode - $errString")
                    onError(AppError.Security.OperationError("Biometric decryption error: $errString"))
                },
                onFailed = {
                    Timber.e("Biometric decryption failed")
                    onError(AppError.Security.OperationError("Biometric decryption failed"))
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Error decrypting with biometric")
            onError(AppError.Security.DecryptionError("Failed to decrypt with biometric", e))
        }
    }
    
    /**
     * Encrypt data
     * 
     * @param data The data to encrypt
     * @return The encrypted data
     */
    private fun encryptData(data: String): String {
        // For simplicity, we're using the SecurityManager to encrypt the data
        // In a real app, you might want to use the BiometricPrompt's CryptoObject
        return try {
            val securityManager = SecurityManager(context)
            securityManager.encrypt(data)
        } catch (e: Exception) {
            Timber.e(e, "Error encrypting data")
            throw AppError.Security.EncryptionError("Failed to encrypt data", e)
        }
    }
    
    /**
     * Decrypt data
     * 
     * @param encryptedData The encrypted data
     * @return The decrypted data
     */
    private fun decryptData(encryptedData: String): String {
        // For simplicity, we're using the SecurityManager to decrypt the data
        // In a real app, you might want to use the BiometricPrompt's CryptoObject
        return try {
            val securityManager = SecurityManager(context)
            securityManager.decrypt(encryptedData)
        } catch (e: Exception) {
            Timber.e(e, "Error decrypting data")
            throw AppError.Security.DecryptionError("Failed to decrypt data", e)
        }
    }
    
    /**
     * Biometric availability status
     */
    enum class BiometricAvailability {
        AVAILABLE,
        NO_HARDWARE,
        HARDWARE_UNAVAILABLE,
        NOT_ENROLLED,
        SECURITY_UPDATE_REQUIRED,
        UNSUPPORTED,
        UNKNOWN
    }
}
