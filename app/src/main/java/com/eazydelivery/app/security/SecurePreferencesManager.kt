package com.eazydelivery.app.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import com.eazydelivery.app.util.error.AppError
import com.eazydelivery.app.util.LogUtils
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
    private val securityManager: SecurityManager,
    private val securityConfig: SecurityConfig
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

        // Sensitive keys that require additional encryption
        private val SENSITIVE_KEYS = setOf(
            KEY_AUTH_TOKEN,
            KEY_REFRESH_TOKEN,
            KEY_PIN_HASH
        )
    }

    // Encrypted shared preferences instance
    private lateinit var encryptedPrefs: SharedPreferences

    /**
     * Initialize the secure preferences manager
     */
    fun initialize() {
        try {
            // Ensure the security manager is initialized
            securityManager.initialize()

            // Initialize encrypted shared preferences
            encryptedPrefs = securityConfig.createEncryptedSharedPreferences()

            // Generate a device ID if not already present
            if (getString(KEY_DEVICE_ID) == null) {
                val deviceId = securityManager.generateSecureToken()
                putString(KEY_DEVICE_ID, deviceId)
                Timber.d("Generated new device ID: $deviceId")
            }

            // Check if biometric authentication is available
            val biometricAvailable = securityConfig.isBiometricAuthAvailable()
            LogUtils.d("SecurePreferencesManager", "Biometric authentication available: $biometricAvailable")

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
            // For sensitive keys, use additional encryption layer
            val finalValue = if (SENSITIVE_KEYS.contains(key)) {
                securityManager.encryptString(value)
            } else {
                value
            }

            // Store in encrypted shared preferences
            encryptedPrefs.edit().putString(key, finalValue).apply()

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
            // Retrieve from encrypted shared preferences
            val encryptedValue = encryptedPrefs.getString(key, null) ?: return null

            // For sensitive keys, use additional decryption layer
            val value = if (SENSITIVE_KEYS.contains(key)) {
                securityManager.decryptString(encryptedValue)
            } else {
                encryptedValue
            }

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
            // Store in encrypted shared preferences
            encryptedPrefs.edit().putBoolean(key, value).apply()

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
            // Retrieve from encrypted shared preferences
            val value = encryptedPrefs.getBoolean(key, defaultValue)

            Timber.d("Retrieved boolean value for key: $key")
            value
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
            // Store in encrypted shared preferences
            encryptedPrefs.edit().putLong(key, value).apply()

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
            // Retrieve from encrypted shared preferences
            val value = encryptedPrefs.getLong(key, defaultValue)

            Timber.d("Retrieved long value for key: $key")
            value
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
            // Store in encrypted shared preferences
            encryptedPrefs.edit().putInt(key, value).apply()

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
            // Retrieve from encrypted shared preferences
            val value = encryptedPrefs.getInt(key, defaultValue)

            Timber.d("Retrieved int value for key: $key")
            value
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
            // Remove from encrypted shared preferences
            encryptedPrefs.edit().remove(key).apply()

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
            // Clear all encrypted shared preferences
            encryptedPrefs.edit().clear().apply()

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
        return encryptedPrefs.contains(key)
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
