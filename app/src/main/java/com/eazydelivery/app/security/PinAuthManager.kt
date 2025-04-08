package com.eazydelivery.app.security

import android.content.Context
import com.eazydelivery.app.util.error.AppError
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for PIN-based authentication
 */
@Singleton
class PinAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securityManager: SecurityManager,
    private val securePreferencesManager: SecurePreferencesManager
) {
    companion object {
        private const val MAX_PIN_ATTEMPTS = 5
        private const val PIN_ATTEMPT_TIMEOUT_MS = 30 * 60 * 1000L // 30 minutes
        private const val KEY_PIN_ATTEMPTS = "pin_attempts"
        private const val KEY_PIN_LOCKOUT_TIME = "pin_lockout_time"
    }
    
    /**
     * Check if PIN authentication is enabled
     * 
     * @return true if PIN authentication is enabled, false otherwise
     */
    fun isPinEnabled(): Boolean {
        return securePreferencesManager.getBoolean(SecurePreferencesManager.KEY_PIN_ENABLED, false)
    }
    
    /**
     * Enable or disable PIN authentication
     * 
     * @param enabled true to enable PIN authentication, false to disable
     */
    fun setPinEnabled(enabled: Boolean) {
        securePreferencesManager.putBoolean(SecurePreferencesManager.KEY_PIN_ENABLED, enabled)
        Timber.d("PIN authentication ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Set a PIN
     * 
     * @param pin The PIN to set
     */
    fun setPin(pin: String) {
        try {
            // Validate PIN
            validatePin(pin)
            
            // Hash the PIN
            val pinHash = securityManager.hashSha256(pin)
            
            // Store the PIN hash
            securePreferencesManager.putString(SecurePreferencesManager.KEY_PIN_HASH, pinHash)
            
            // Enable PIN authentication
            setPinEnabled(true)
            
            // Reset PIN attempts
            resetPinAttempts()
            
            Timber.d("PIN set successfully")
        } catch (e: Exception) {
            Timber.e(e, "Error setting PIN")
            throw AppError.Security.OperationError("Failed to set PIN", e)
        }
    }
    
    /**
     * Verify a PIN
     * 
     * @param pin The PIN to verify
     * @return true if the PIN is correct, false otherwise
     */
    fun verifyPin(pin: String): Boolean {
        try {
            // Check if PIN authentication is enabled
            if (!isPinEnabled()) {
                Timber.e("PIN authentication is not enabled")
                return false
            }
            
            // Check if PIN is locked out
            if (isPinLockedOut()) {
                Timber.e("PIN is locked out")
                throw AppError.Security.OperationError("PIN is locked out")
            }
            
            // Get the stored PIN hash
            val storedPinHash = securePreferencesManager.getString(SecurePreferencesManager.KEY_PIN_HASH)
            if (storedPinHash == null) {
                Timber.e("No PIN hash found")
                return false
            }
            
            // Hash the provided PIN
            val pinHash = securityManager.hashSha256(pin)
            
            // Compare the hashes
            val isCorrect = pinHash == storedPinHash
            
            if (isCorrect) {
                // Reset PIN attempts on successful verification
                resetPinAttempts()
                Timber.d("PIN verified successfully")
            } else {
                // Increment PIN attempts on failed verification
                incrementPinAttempts()
                Timber.e("PIN verification failed")
            }
            
            return isCorrect
        } catch (e: Exception) {
            Timber.e(e, "Error verifying PIN")
            throw AppError.Security.OperationError("Failed to verify PIN", e)
        }
    }
    
    /**
     * Clear the PIN
     */
    fun clearPin() {
        try {
            // Remove the PIN hash
            securePreferencesManager.remove(SecurePreferencesManager.KEY_PIN_HASH)
            
            // Disable PIN authentication
            setPinEnabled(false)
            
            // Reset PIN attempts
            resetPinAttempts()
            
            Timber.d("PIN cleared successfully")
        } catch (e: Exception) {
            Timber.e(e, "Error clearing PIN")
            throw AppError.Security.OperationError("Failed to clear PIN", e)
        }
    }
    
    /**
     * Change the PIN
     * 
     * @param currentPin The current PIN
     * @param newPin The new PIN
     * @return true if the PIN was changed successfully, false otherwise
     */
    fun changePin(currentPin: String, newPin: String): Boolean {
        try {
            // Verify the current PIN
            if (!verifyPin(currentPin)) {
                Timber.e("Current PIN is incorrect")
                return false
            }
            
            // Set the new PIN
            setPin(newPin)
            
            Timber.d("PIN changed successfully")
            return true
        } catch (e: Exception) {
            Timber.e(e, "Error changing PIN")
            throw AppError.Security.OperationError("Failed to change PIN", e)
        }
    }
    
    /**
     * Check if the PIN is locked out
     * 
     * @return true if the PIN is locked out, false otherwise
     */
    fun isPinLockedOut(): Boolean {
        try {
            // Get PIN attempts
            val attempts = securePreferencesManager.getInt(KEY_PIN_ATTEMPTS, 0)
            
            // Check if attempts exceed the maximum
            if (attempts >= MAX_PIN_ATTEMPTS) {
                // Get lockout time
                val lockoutTime = securePreferencesManager.getLong(KEY_PIN_LOCKOUT_TIME, 0L)
                
                // Check if lockout has expired
                val currentTime = System.currentTimeMillis()
                if (currentTime - lockoutTime < PIN_ATTEMPT_TIMEOUT_MS) {
                    // Lockout is still active
                    return true
                } else {
                    // Lockout has expired, reset attempts
                    resetPinAttempts()
                    return false
                }
            }
            
            return false
        } catch (e: Exception) {
            Timber.e(e, "Error checking PIN lockout")
            throw AppError.Security.OperationError("Failed to check PIN lockout", e)
        }
    }
    
    /**
     * Get the remaining PIN attempts
     * 
     * @return The number of remaining PIN attempts
     */
    fun getRemainingPinAttempts(): Int {
        try {
            // Get PIN attempts
            val attempts = securePreferencesManager.getInt(KEY_PIN_ATTEMPTS, 0)
            
            // Calculate remaining attempts
            return MAX_PIN_ATTEMPTS - attempts
        } catch (e: Exception) {
            Timber.e(e, "Error getting remaining PIN attempts")
            throw AppError.Security.OperationError("Failed to get remaining PIN attempts", e)
        }
    }
    
    /**
     * Get the PIN lockout time remaining in milliseconds
     * 
     * @return The PIN lockout time remaining in milliseconds, or 0 if not locked out
     */
    fun getPinLockoutTimeRemaining(): Long {
        try {
            // Check if PIN is locked out
            if (!isPinLockedOut()) {
                return 0L
            }
            
            // Get lockout time
            val lockoutTime = securePreferencesManager.getLong(KEY_PIN_LOCKOUT_TIME, 0L)
            
            // Calculate remaining time
            val currentTime = System.currentTimeMillis()
            val elapsedTime = currentTime - lockoutTime
            val remainingTime = PIN_ATTEMPT_TIMEOUT_MS - elapsedTime
            
            return if (remainingTime > 0) remainingTime else 0L
        } catch (e: Exception) {
            Timber.e(e, "Error getting PIN lockout time remaining")
            throw AppError.Security.OperationError("Failed to get PIN lockout time remaining", e)
        }
    }
    
    /**
     * Reset PIN attempts
     */
    private fun resetPinAttempts() {
        securePreferencesManager.putInt(KEY_PIN_ATTEMPTS, 0)
        securePreferencesManager.remove(KEY_PIN_LOCKOUT_TIME)
        Timber.d("PIN attempts reset")
    }
    
    /**
     * Increment PIN attempts
     */
    private fun incrementPinAttempts() {
        try {
            // Get current attempts
            val attempts = securePreferencesManager.getInt(KEY_PIN_ATTEMPTS, 0)
            
            // Increment attempts
            val newAttempts = attempts + 1
            securePreferencesManager.putInt(KEY_PIN_ATTEMPTS, newAttempts)
            
            // Check if attempts exceed the maximum
            if (newAttempts >= MAX_PIN_ATTEMPTS) {
                // Set lockout time
                securePreferencesManager.putLong(KEY_PIN_LOCKOUT_TIME, System.currentTimeMillis())
                Timber.d("PIN locked out")
            }
            
            Timber.d("PIN attempts incremented to $newAttempts")
        } catch (e: Exception) {
            Timber.e(e, "Error incrementing PIN attempts")
            throw AppError.Security.OperationError("Failed to increment PIN attempts", e)
        }
    }
    
    /**
     * Validate a PIN
     * 
     * @param pin The PIN to validate
     * @throws IllegalArgumentException if the PIN is invalid
     */
    private fun validatePin(pin: String) {
        // Check PIN length
        if (pin.length < 4 || pin.length > 8) {
            throw IllegalArgumentException("PIN must be between 4 and 8 digits")
        }
        
        // Check if PIN contains only digits
        if (!pin.all { it.isDigit() }) {
            throw IllegalArgumentException("PIN must contain only digits")
        }
        
        // Check for sequential digits
        for (i in 0 until pin.length - 2) {
            val a = pin[i].digitToInt()
            val b = pin[i + 1].digitToInt()
            val c = pin[i + 2].digitToInt()
            
            if ((a + 1 == b && b + 1 == c) || (a - 1 == b && b - 1 == c)) {
                throw IllegalArgumentException("PIN must not contain sequential digits")
            }
        }
        
        // Check for repeated digits
        if (pin.groupBy { it }.any { it.value.size >= 3 }) {
            throw IllegalArgumentException("PIN must not contain more than 2 repeated digits")
        }
    }
}
