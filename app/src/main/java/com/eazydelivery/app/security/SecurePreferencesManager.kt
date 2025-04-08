package com.eazydelivery.app.security

import android.content.Context
import com.eazydelivery.app.util.error.AppError
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for securely storing and retrieving user preferences
 */
@Singleton
class SecurePreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securityManager: SecurityManager
) {
    companion object {
        // Preference keys
        const val KEY_USER_ID = "user_id"
        const val KEY_AUTH_TOKEN = "auth_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_USER_NAME = "user_name"
        const val KEY_USER_EMAIL = "user_email"
        const val KEY_USER_PHONE = "user_phone"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_LAST_LOGIN = "last_login"
        const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        const val KEY_PIN_ENABLED = "pin_enabled"
        const val KEY_PIN_HASH = "pin_hash"
        const val KEY_NOTIFICATION_TOKEN = "notification_token"
    }
    
    /**
     * Initialize the secure preferences manager
     */
    fun initialize() {
        try {
            // Ensure the security manager is initialized
            securityManager.initialize()
            
            // Generate a device ID if not already present
            if (getString(KEY_DEVICE_ID) == null) {
                val deviceId = securityManager.generateSecureToken()
                putString(KEY_DEVICE_ID, deviceId)
                Timber.d("Generated new device ID: $deviceId")
            }
            
            Timber.d("Secure preferences manager initialized")
        } catch (e: Exception) {
            Timber.e(e, "Error initializing secure preferences manager")
            throw AppError.Security.InitializationError("Failed to initialize secure preferences manager", e)
        }
    }
    
    /**
     * Store a string value securely
     * 
     * @param key The preference key
     * @param value The value to store
     */
    fun putString(key: String, value: String) {
        try {
            securityManager.storeSecurely(key, value)
            Timber.d("Stored string value for key: $key")
        } catch (e: Exception) {
            Timber.e(e, "Error storing string value for key: $key")
            throw AppError.Security.EncryptionError("Failed to store string value securely", e)
        }
    }
    
    /**
     * Retrieve a string value securely
     * 
     * @param key The preference key
     * @return The stored value, or null if not found
     */
    fun getString(key: String): String? {
        return try {
            val value = securityManager.retrieveSecurely(key)
            Timber.d("Retrieved string value for key: $key")
            value
        } catch (e: Exception) {
            Timber.e(e, "Error retrieving string value for key: $key")
            throw AppError.Security.DecryptionError("Failed to retrieve string value securely", e)
        }
    }
    
    /**
     * Store a boolean value securely
     * 
     * @param key The preference key
     * @param value The value to store
     */
    fun putBoolean(key: String, value: Boolean) {
        try {
            securityManager.storeSecurely(key, value.toString())
            Timber.d("Stored boolean value for key: $key")
        } catch (e: Exception) {
            Timber.e(e, "Error storing boolean value for key: $key")
            throw AppError.Security.EncryptionError("Failed to store boolean value securely", e)
        }
    }
    
    /**
     * Retrieve a boolean value securely
     * 
     * @param key The preference key
     * @param defaultValue The default value to return if the key is not found
     * @return The stored value, or the default value if not found
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return try {
            val value = securityManager.retrieveSecurely(key)
            Timber.d("Retrieved boolean value for key: $key")
            value?.toBoolean() ?: defaultValue
        } catch (e: Exception) {
            Timber.e(e, "Error retrieving boolean value for key: $key")
            throw AppError.Security.DecryptionError("Failed to retrieve boolean value securely", e)
        }
    }
    
    /**
     * Store a long value securely
     * 
     * @param key The preference key
     * @param value The value to store
     */
    fun putLong(key: String, value: Long) {
        try {
            securityManager.storeSecurely(key, value.toString())
            Timber.d("Stored long value for key: $key")
        } catch (e: Exception) {
            Timber.e(e, "Error storing long value for key: $key")
            throw AppError.Security.EncryptionError("Failed to store long value securely", e)
        }
    }
    
    /**
     * Retrieve a long value securely
     * 
     * @param key The preference key
     * @param defaultValue The default value to return if the key is not found
     * @return The stored value, or the default value if not found
     */
    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return try {
            val value = securityManager.retrieveSecurely(key)
            Timber.d("Retrieved long value for key: $key")
            value?.toLongOrNull() ?: defaultValue
        } catch (e: Exception) {
            Timber.e(e, "Error retrieving long value for key: $key")
            throw AppError.Security.DecryptionError("Failed to retrieve long value securely", e)
        }
    }
    
    /**
     * Store an int value securely
     * 
     * @param key The preference key
     * @param value The value to store
     */
    fun putInt(key: String, value: Int) {
        try {
            securityManager.storeSecurely(key, value.toString())
            Timber.d("Stored int value for key: $key")
        } catch (e: Exception) {
            Timber.e(e, "Error storing int value for key: $key")
            throw AppError.Security.EncryptionError("Failed to store int value securely", e)
        }
    }
    
    /**
     * Retrieve an int value securely
     * 
     * @param key The preference key
     * @param defaultValue The default value to return if the key is not found
     * @return The stored value, or the default value if not found
     */
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return try {
            val value = securityManager.retrieveSecurely(key)
            Timber.d("Retrieved int value for key: $key")
            value?.toIntOrNull() ?: defaultValue
        } catch (e: Exception) {
            Timber.e(e, "Error retrieving int value for key: $key")
            throw AppError.Security.DecryptionError("Failed to retrieve int value securely", e)
        }
    }
    
    /**
     * Remove a value securely
     * 
     * @param key The preference key
     */
    fun remove(key: String) {
        try {
            securityManager.removeSecurely(key)
            Timber.d("Removed value for key: $key")
        } catch (e: Exception) {
            Timber.e(e, "Error removing value for key: $key")
            throw AppError.Security.OperationError("Failed to remove value securely", e)
        }
    }
    
    /**
     * Clear all secure preferences
     */
    fun clearAll() {
        try {
            securityManager.clearAllSecurely()
            Timber.d("Cleared all secure preferences")
        } catch (e: Exception) {
            Timber.e(e, "Error clearing all secure preferences")
            throw AppError.Security.OperationError("Failed to clear all secure preferences", e)
        }
    }
    
    /**
     * Check if a key exists
     * 
     * @param key The preference key
     * @return true if the key exists, false otherwise
     */
    fun contains(key: String): Boolean {
        return securityManager.retrieveSecurely(key) != null
    }
    
    /**
     * Store authentication data securely
     * 
     * @param userId The user ID
     * @param authToken The authentication token
     * @param refreshToken The refresh token
     */
    fun storeAuthData(userId: String, authToken: String, refreshToken: String) {
        try {
            putString(KEY_USER_ID, userId)
            putString(KEY_AUTH_TOKEN, authToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putLong(KEY_LAST_LOGIN, System.currentTimeMillis())
            Timber.d("Stored authentication data for user: $userId")
        } catch (e: Exception) {
            Timber.e(e, "Error storing authentication data")
            throw AppError.Security.EncryptionError("Failed to store authentication data securely", e)
        }
    }
    
    /**
     * Clear authentication data
     */
    fun clearAuthData() {
        try {
            remove(KEY_USER_ID)
            remove(KEY_AUTH_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_USER_NAME)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_PHONE)
            remove(KEY_LAST_LOGIN)
            Timber.d("Cleared authentication data")
        } catch (e: Exception) {
            Timber.e(e, "Error clearing authentication data")
            throw AppError.Security.OperationError("Failed to clear authentication data", e)
        }
    }
    
    /**
     * Check if the user is authenticated
     * 
     * @return true if the user is authenticated, false otherwise
     */
    fun isAuthenticated(): Boolean {
        return getString(KEY_AUTH_TOKEN) != null
    }
    
    /**
     * Get the authentication token
     * 
     * @return The authentication token, or null if not authenticated
     */
    fun getAuthToken(): String? {
        return getString(KEY_AUTH_TOKEN)
    }
    
    /**
     * Get the refresh token
     * 
     * @return The refresh token, or null if not authenticated
     */
    fun getRefreshToken(): String? {
        return getString(KEY_REFRESH_TOKEN)
    }
    
    /**
     * Get the user ID
     * 
     * @return The user ID, or null if not authenticated
     */
    fun getUserId(): String? {
        return getString(KEY_USER_ID)
    }
    
    /**
     * Get the device ID
     * 
     * @return The device ID
     */
    fun getDeviceId(): String {
        return getString(KEY_DEVICE_ID) ?: run {
            val deviceId = securityManager.generateSecureToken()
            putString(KEY_DEVICE_ID, deviceId)
            deviceId
        }
    }
}
