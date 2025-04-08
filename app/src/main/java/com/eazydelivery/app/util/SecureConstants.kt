package com.eazydelivery.app.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.eazydelivery.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure storage for sensitive constants
 * This class provides a secure way to store and retrieve sensitive constants
 */
@Singleton
class SecureConstants @Inject constructor(
    @ApplicationContext private val context: Context,
    private val errorHandler: ErrorHandler
) {
    companion object {
        private const val SECURE_PREFS_FILE = "secure_constants"
        private const val KEY_ADMIN_PHONE = "admin_phone"
        private const val KEY_ADMIN_EMAIL = "admin_email"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_API_KEY_SALT = "api_key_salt"
        private const val KEY_API_KEY_LAST_ROTATED = "api_key_last_rotated"

        // Rotation period in milliseconds (30 days)
        private const val API_KEY_ROTATION_PERIOD_MS = 30L * 24 * 60 * 60 * 1000
    }

    private val masterKey: MasterKey by lazy {
        try {
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .setKeyGenParameterSpec(
                    KeyGenParameterSpec.Builder(
                        "_secure_constants_master_key_",
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                )
                .build()
        } catch (e: Exception) {
            errorHandler.handleException("SecureConstants.masterKey", e)
            throw e // Rethrow as this is critical
        }
    }

    private val encryptedSharedPreferences by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            errorHandler.handleException("SecureConstants.encryptedSharedPreferences", e)
            // Fallback to regular shared preferences in case of error
            context.getSharedPreferences(SECURE_PREFS_FILE, Context.MODE_PRIVATE)
        }
    }

    /**
     * Initialize secure constants with values from BuildConfig
     * This should be called once during app startup
     */
    fun initialize() {
        try {
            // Only initialize if values are not already set
            if (!encryptedSharedPreferences.contains(KEY_ADMIN_PHONE)) {
                setAdminPhone(BuildConfig.ADMIN_PHONE)
            }

            if (!encryptedSharedPreferences.contains(KEY_ADMIN_EMAIL)) {
                setAdminEmail(BuildConfig.ADMIN_EMAIL)
            }

            if (!encryptedSharedPreferences.contains(KEY_API_KEY)) {
                setApiKey(BuildConfig.API_KEY)
            }
        } catch (e: Exception) {
            errorHandler.handleException("SecureConstants.initialize", e)
        }
    }

    /**
     * Get the admin phone number
     */
    fun getAdminPhone(): String {
        return try {
            encryptedSharedPreferences.getString(KEY_ADMIN_PHONE, BuildConfig.ADMIN_PHONE) ?: BuildConfig.ADMIN_PHONE
        } catch (e: Exception) {
            errorHandler.handleException("SecureConstants.getAdminPhone", e)
            BuildConfig.ADMIN_PHONE
        }
    }

    /**
     * Set the admin phone number
     */
    fun setAdminPhone(phone: String) {
        try {
            encryptedSharedPreferences.edit().putString(KEY_ADMIN_PHONE, phone).apply()
        } catch (e: Exception) {
            errorHandler.handleException("SecureConstants.setAdminPhone", e)
        }
    }

    /**
     * Get the admin email
     */
    fun getAdminEmail(): String {
        return try {
            encryptedSharedPreferences.getString(KEY_ADMIN_EMAIL, BuildConfig.ADMIN_EMAIL) ?: BuildConfig.ADMIN_EMAIL
        } catch (e: Exception) {
            errorHandler.handleException("SecureConstants.getAdminEmail", e)
            BuildConfig.ADMIN_EMAIL
        }
    }

    /**
     * Set the admin email
     */
    fun setAdminEmail(email: String) {
        try {
            encryptedSharedPreferences.edit().putString(KEY_ADMIN_EMAIL, email).apply()
        } catch (e: Exception) {
            errorHandler.handleException("SecureConstants.setAdminEmail", e)
        }
    }

    /**
     * Get the API key
     * Applies additional obfuscation with a salt
     */
    fun getApiKey(): String {
        return try {
            // Check if API key rotation is needed
            checkAndRotateApiKey()

            // Get the stored API key and salt
            val storedKey = encryptedSharedPreferences.getString(KEY_API_KEY, "") ?: ""
            val salt = encryptedSharedPreferences.getString(KEY_API_KEY_SALT, "") ?: ""

            if (storedKey.isEmpty()) {
                // If no stored key, use the one from BuildConfig
                BuildConfig.API_KEY
            } else {
                // Deobfuscate the key using the salt
                deobfuscateApiKey(storedKey, salt)
            }
        } catch (e: Exception) {
            errorHandler.handleException("SecureConstants.getApiKey", e)
            BuildConfig.API_KEY
        }
    }

    /**
     * Set the API key with additional obfuscation
     */
    fun setApiKey(apiKey: String) {
        try {
            // Generate a random salt for obfuscation
            val salt = generateSalt()

            // Obfuscate the API key with the salt
            val obfuscatedKey = obfuscateApiKey(apiKey, salt)

            // Store both the obfuscated key and salt
            encryptedSharedPreferences.edit()
                .putString(KEY_API_KEY, obfuscatedKey)
                .putString(KEY_API_KEY_SALT, salt)
                .putLong(KEY_API_KEY_LAST_ROTATED, System.currentTimeMillis())
                .apply()

            Timber.d("API key stored securely with salt")
        } catch (e: Exception) {
            errorHandler.handleException("SecureConstants.setApiKey", e)
        }
    }

    /**
     * Checks if the API key needs rotation and rotates it if necessary
     */
    private fun checkAndRotateApiKey() {
        try {
            // Get the last rotation time
            val lastRotated = encryptedSharedPreferences.getLong(KEY_API_KEY_LAST_ROTATED, 0L)

            // If never rotated or rotation period has passed, rotate the key
            if (lastRotated == 0L || System.currentTimeMillis() - lastRotated > API_KEY_ROTATION_PERIOD_MS) {
                // Get the current API key
                val currentKey = encryptedSharedPreferences.getString(KEY_API_KEY, "") ?: ""
                val currentSalt = encryptedSharedPreferences.getString(KEY_API_KEY_SALT, "") ?: ""

                if (currentKey.isNotEmpty() && currentSalt.isNotEmpty()) {
                    // Deobfuscate the current key
                    val apiKey = deobfuscateApiKey(currentKey, currentSalt)

                    // Re-encrypt with a new salt
                    setApiKey(apiKey)
                    Timber.d("API key rotated successfully")
                }
            }
        } catch (e: Exception) {
            errorHandler.handleException("SecureConstants.checkAndRotateApiKey", e)
        }
    }

    /**
     * Generates a random salt for API key obfuscation
     */
    private fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt.joinToString("") { "%02x".format(it) }
    }

    /**
     * Obfuscates the API key using XOR with the salt
     */
    private fun obfuscateApiKey(apiKey: String, salt: String): String {
        val apiKeyBytes = apiKey.toByteArray()
        val saltBytes = salt.toByteArray()

        // XOR the API key with the salt (repeating the salt if needed)
        val obfuscatedBytes = ByteArray(apiKeyBytes.size)
        for (i in apiKeyBytes.indices) {
            obfuscatedBytes[i] = (apiKeyBytes[i].toInt() xor saltBytes[i % saltBytes.size].toInt()).toByte()
        }

        return obfuscatedBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Deobfuscates the API key using XOR with the salt
     */
    private fun deobfuscateApiKey(obfuscatedKey: String, salt: String): String {
        // Convert hex string to byte array
        val obfuscatedBytes = ByteArray(obfuscatedKey.length / 2)
        for (i in obfuscatedBytes.indices) {
            val index = i * 2
            obfuscatedBytes[i] = obfuscatedKey.substring(index, index + 2).toInt(16).toByte()
        }

        val saltBytes = salt.toByteArray()

        // XOR the obfuscated bytes with the salt to get the original API key
        val apiKeyBytes = ByteArray(obfuscatedBytes.size)
        for (i in obfuscatedBytes.indices) {
            apiKeyBytes[i] = (obfuscatedBytes[i].toInt() xor saltBytes[i % saltBytes.size].toInt()).toByte()
        }

        return String(apiKeyBytes)
    }
}
