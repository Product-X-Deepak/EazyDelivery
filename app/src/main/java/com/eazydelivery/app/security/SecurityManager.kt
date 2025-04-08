package com.eazydelivery.app.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.eazydelivery.app.util.error.AppError
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class for handling security-related operations
 */
@Singleton
class SecurityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "eazydelivery_master_key"
        private const val ENCRYPTION_KEY_ALIAS = "eazydelivery_encryption_key"
        private const val ENCRYPTED_PREFS_FILE = "eazydelivery_secure_prefs"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val IV_LENGTH_BYTES = 12
    }
    
    // Master key for EncryptedSharedPreferences and EncryptedFile
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context, MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    // Encrypted shared preferences
    private val encryptedSharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    /**
     * Initialize the security manager
     */
    fun initialize() {
        try {
            // Ensure the encryption key exists
            if (!doesKeyExist(ENCRYPTION_KEY_ALIAS)) {
                generateEncryptionKey()
            }
            
            Timber.d("Security manager initialized")
        } catch (e: Exception) {
            Timber.e(e, "Error initializing security manager")
            throw AppError.Security.InitializationError("Failed to initialize security manager", e)
        }
    }
    
    /**
     * Store a value securely in encrypted shared preferences
     * 
     * @param key The key
     * @param value The value
     */
    fun storeSecurely(key: String, value: String) {
        try {
            encryptedSharedPreferences.edit().putString(key, value).apply()
            Timber.d("Value stored securely for key: $key")
        } catch (e: Exception) {
            Timber.e(e, "Error storing value securely for key: $key")
            throw AppError.Security.EncryptionError("Failed to store value securely", e)
        }
    }
    
    /**
     * Retrieve a value securely from encrypted shared preferences
     * 
     * @param key The key
     * @return The value, or null if not found
     */
    fun retrieveSecurely(key: String): String? {
        return try {
            val value = encryptedSharedPreferences.getString(key, null)
            Timber.d("Value retrieved securely for key: $key")
            value
        } catch (e: Exception) {
            Timber.e(e, "Error retrieving value securely for key: $key")
            throw AppError.Security.DecryptionError("Failed to retrieve value securely", e)
        }
    }
    
    /**
     * Remove a value securely from encrypted shared preferences
     * 
     * @param key The key
     */
    fun removeSecurely(key: String) {
        try {
            encryptedSharedPreferences.edit().remove(key).apply()
            Timber.d("Value removed securely for key: $key")
        } catch (e: Exception) {
            Timber.e(e, "Error removing value securely for key: $key")
            throw AppError.Security.OperationError("Failed to remove value securely", e)
        }
    }
    
    /**
     * Clear all values securely from encrypted shared preferences
     */
    fun clearAllSecurely() {
        try {
            encryptedSharedPreferences.edit().clear().apply()
            Timber.d("All values cleared securely")
        } catch (e: Exception) {
            Timber.e(e, "Error clearing all values securely")
            throw AppError.Security.OperationError("Failed to clear all values securely", e)
        }
    }
    
    /**
     * Encrypt a string using AES/GCM
     * 
     * @param plaintext The plaintext to encrypt
     * @return The encrypted data as a Base64-encoded string
     */
    fun encrypt(plaintext: String): String {
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val secretKey = getOrCreateSecretKey()
            
            // Generate a random IV
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            
            // Encrypt the plaintext
            val encryptedBytes = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
            
            // Combine IV and encrypted data
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
            
            // Encode as Base64
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Base64.getEncoder().encodeToString(combined)
            } else {
                android.util.Base64.encodeToString(combined, android.util.Base64.DEFAULT)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error encrypting data")
            throw AppError.Security.EncryptionError("Failed to encrypt data", e)
        }
    }
    
    /**
     * Decrypt a string using AES/GCM
     * 
     * @param encryptedData The encrypted data as a Base64-encoded string
     * @return The decrypted plaintext
     */
    fun decrypt(encryptedData: String): String {
        try {
            // Decode from Base64
            val combined = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Base64.getDecoder().decode(encryptedData)
            } else {
                android.util.Base64.decode(encryptedData, android.util.Base64.DEFAULT)
            }
            
            // Extract IV and encrypted data
            val iv = ByteArray(IV_LENGTH_BYTES)
            val encryptedBytes = ByteArray(combined.size - IV_LENGTH_BYTES)
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTES)
            System.arraycopy(combined, IV_LENGTH_BYTES, encryptedBytes, 0, encryptedBytes.size)
            
            // Initialize cipher for decryption
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val secretKey = getOrCreateSecretKey()
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            
            // Decrypt the data
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            Timber.e(e, "Error decrypting data")
            throw AppError.Security.DecryptionError("Failed to decrypt data", e)
        }
    }
    
    /**
     * Create an encrypted file
     * 
     * @param file The file to encrypt
     * @return The encrypted file
     */
    fun createEncryptedFile(file: File): EncryptedFile {
        return try {
            EncryptedFile.Builder(
                context,
                file,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()
        } catch (e: IOException) {
            Timber.e(e, "Error creating encrypted file: ${file.name}")
            throw AppError.Security.EncryptionError("Failed to create encrypted file", e)
        }
    }
    
    /**
     * Hash a string using SHA-256
     * 
     * @param input The input string
     * @return The hashed string as a hexadecimal string
     */
    fun hashSha256(input: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Timber.e(e, "Error hashing data")
            throw AppError.Security.OperationError("Failed to hash data", e)
        }
    }
    
    /**
     * Generate a secure random token
     * 
     * @param length The length of the token in bytes
     * @return The token as a hexadecimal string
     */
    fun generateSecureToken(length: Int = 32): String {
        return try {
            val bytes = ByteArray(length)
            java.security.SecureRandom().nextBytes(bytes)
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Timber.e(e, "Error generating secure token")
            throw AppError.Security.OperationError("Failed to generate secure token", e)
        }
    }
    
    /**
     * Check if a key exists in the Android Keystore
     * 
     * @param alias The key alias
     * @return true if the key exists, false otherwise
     */
    private fun doesKeyExist(alias: String): Boolean {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            keyStore.containsAlias(alias)
        } catch (e: Exception) {
            Timber.e(e, "Error checking if key exists: $alias")
            false
        }
    }
    
    /**
     * Generate an encryption key in the Android Keystore
     */
    private fun generateEncryptionKey() {
        try {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                ENCRYPTION_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
            
            Timber.d("Encryption key generated")
        } catch (e: Exception) {
            Timber.e(e, "Error generating encryption key")
            throw AppError.Security.KeyGenerationError("Failed to generate encryption key", e)
        }
    }
    
    /**
     * Get or create a secret key from the Android Keystore
     * 
     * @return The secret key
     */
    private fun getOrCreateSecretKey(): SecretKey {
        return try {
            if (!doesKeyExist(ENCRYPTION_KEY_ALIAS)) {
                generateEncryptionKey()
            }
            
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            keyStore.getKey(ENCRYPTION_KEY_ALIAS, null) as SecretKey
        } catch (e: Exception) {
            Timber.e(e, "Error getting or creating secret key")
            throw AppError.Security.KeyRetrievalError("Failed to get or create secret key", e)
        }
    }
}
