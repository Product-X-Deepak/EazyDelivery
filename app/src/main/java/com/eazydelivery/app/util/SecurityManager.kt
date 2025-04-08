package com.eazydelivery.app.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced security manager for handling encryption/decryption operations
 * using Android Keystore and EncryptedSharedPreferences for secure operations.
 */
@Singleton
class SecurityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val MASTER_KEY_ALIAS = "eazy_delivery_master_key"
        private const val ENCRYPTION_KEY_ALIAS = "eazy_delivery_encryption_key"
        private const val ENCRYPTION_KEY_ALIAS_V2 = "eazy_delivery_encryption_key_v2"
        private const val GCM_TAG_LENGTH = 128
        private const val ENCRYPTED_PREFS_FILE = "eazy_delivery_secure_prefs"
        private const val IV_SIZE = 12
        private const val SALT_SIZE = 16

        // Key for storing the current key version
        private const val KEY_VERSION_PREF = "encryption_key_version"

        // Default key version
        private const val DEFAULT_KEY_VERSION = 1
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context, MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setKeyGenParameterSpec(
                KeyGenParameterSpec.Builder(
                    MASTER_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .build()
            )
            .build()
    }

    private val encryptedSharedPreferences by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to create EncryptedSharedPreferences")
            null
        }
    }

    /**
     * Securely stores a string value using EncryptedSharedPreferences
     * @param key The key to store the value under
     * @param value The value to store
     * @return true if successful, false otherwise
     */
    fun secureStore(key: String, value: String): Boolean {
        return try {
            encryptedSharedPreferences?.edit()?.putString(key, value)?.apply()
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to securely store value")
            false
        }
    }

    /**
     * Retrieves a securely stored string value from EncryptedSharedPreferences
     * @param key The key to retrieve
     * @param defaultValue The default value if key not found
     * @return The stored value or defaultValue
     */
    fun secureRetrieve(key: String, defaultValue: String = ""): String {
        return try {
            encryptedSharedPreferences?.getString(key, defaultValue) ?: defaultValue
        } catch (e: Exception) {
            Timber.e(e, "Failed to retrieve secure value")
            defaultValue
        }
    }

    /**
     * Removes a securely stored value from EncryptedSharedPreferences
     * @param key The key to remove
     * @return true if successful, false otherwise
     */
    fun secureRemove(key: String): Boolean {
        return try {
            encryptedSharedPreferences?.edit()?.remove(key)?.apply()
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove secure value")
            false
        }
    }

    /**
     * Creates an encrypted file using EncryptedFile API
     * @param fileName The name of the file
     * @return The EncryptedFile object
     */
    fun createEncryptedFile(fileName: String): EncryptedFile {
        val file = File(context.filesDir, fileName)
        return EncryptedFile.Builder(
            context,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
    }

    /**
     * Writes data to an encrypted file
     * @param fileName The name of the file
     * @param data The data to write
     * @return true if successful, false otherwise
     */
    fun writeToEncryptedFile(fileName: String, data: ByteArray): Boolean {
        return try {
            val encryptedFile = createEncryptedFile(fileName)
            encryptedFile.openFileOutput().use { outputStream ->
                outputStream.write(data)
                outputStream.flush()
            }
            true
        } catch (e: Exception) {
            Timber.e(e, "Error writing to encrypted file")
            false
        }
    }

    /**
     * Reads data from an encrypted file
     * @param fileName The name of the file
     * @return The file contents as a ByteArray, or null if reading fails
     */
    fun readFromEncryptedFile(fileName: String): ByteArray? {
        return try {
            val file = File(context.filesDir, fileName)
            if (!file.exists()) return null

            val encryptedFile = createEncryptedFile(fileName)
            val outputStream = ByteArrayOutputStream()
            encryptedFile.openFileInput().use { inputStream ->
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
            }
            outputStream.toByteArray()
        } catch (e: Exception) {
            Timber.e(e, "Error reading from encrypted file")
            null
        }
    }

    /**
     * Encrypts a string value using AES/GCM with a random IV and salt
     * @param plainText The text to encrypt
     * @return The encrypted data as a Base64 encoded string, or null if encryption fails
     */
    fun encryptString(plainText: String): String? {
        if (plainText.isEmpty()) return plainText

        return try {
            val secretKey = getOrCreateSecretKey()

            // Generate random salt and IV
            val random = SecureRandom()
            val salt = ByteArray(SALT_SIZE)
            val iv = ByteArray(IV_SIZE)
            random.nextBytes(salt)
            random.nextBytes(iv)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
            cipher.updateAAD(salt)

            val encryptedBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))

            // Combine salt, IV, and encrypted data
            val combined = ByteArray(salt.size + iv.size + encryptedBytes.size)
            System.arraycopy(salt, 0, combined, 0, salt.size)
            System.arraycopy(iv, 0, combined, salt.size, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, salt.size + iv.size, encryptedBytes.size)

            android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            Timber.e(e, "Error encrypting string")
            null
        }
    }

    /**
     * Decrypts a string value using AES/GCM
     * @param encryptedText The Base64 encoded encrypted data
     * @return The decrypted string, or null if decryption fails
     */
    fun decryptString(encryptedText: String?): String? {
        if (encryptedText.isNullOrEmpty()) return encryptedText

        return try {
            val secretKey = getOrCreateSecretKey()
            val encryptedBytes = android.util.Base64.decode(encryptedText, android.util.Base64.NO_WRAP)

            // Extract salt, IV, and encrypted content
            val salt = encryptedBytes.copyOfRange(0, SALT_SIZE)
            val iv = encryptedBytes.copyOfRange(SALT_SIZE, SALT_SIZE + IV_SIZE)
            val encrypted = encryptedBytes.copyOfRange(SALT_SIZE + IV_SIZE, encryptedBytes.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            cipher.updateAAD(salt)

            String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            Timber.e(e, "Error decrypting string")
            null
        }
    }

    /**
     * Gets or creates a secret key for encryption/decryption
     * Uses the current key version to determine which key to use
     * @return The SecretKey
     */
    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
        keyStore.load(null)

        // Get the current key version
        val keyVersion = getCurrentKeyVersion()

        // Determine which key alias to use based on version
        val keyAlias = when (keyVersion) {
            1 -> ENCRYPTION_KEY_ALIAS
            2 -> ENCRYPTION_KEY_ALIAS_V2
            else -> ENCRYPTION_KEY_ALIAS // Default to v1 if unknown
        }

        return if (keyStore.containsAlias(keyAlias)) {
            keyStore.getKey(keyAlias, null) as SecretKey
        } else {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
            )

            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }

    /**
     * Gets the current key version from preferences
     * @return The current key version (defaults to 1)
     */
    private fun getCurrentKeyVersion(): Int {
        return encryptedSharedPreferences?.getInt(KEY_VERSION_PREF, DEFAULT_KEY_VERSION) ?: DEFAULT_KEY_VERSION
    }

    /**
     * Rotates the encryption key to a new version
     * This will create a new key and update the key version
     * @return true if successful, false otherwise
     */
    fun rotateEncryptionKey(): Boolean {
        try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)

            // Get current key version
            val currentVersion = getCurrentKeyVersion()

            // Determine the new key alias
            val newKeyAlias = when (currentVersion) {
                1 -> ENCRYPTION_KEY_ALIAS_V2
                2 -> ENCRYPTION_KEY_ALIAS
                else -> ENCRYPTION_KEY_ALIAS
            }

            // Create a new key
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
            )

            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                newKeyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()

            // Update the key version
            val newVersion = if (currentVersion == 1) 2 else 1
            encryptedSharedPreferences?.edit()?.putInt(KEY_VERSION_PREF, newVersion)?.apply()

            Timber.d("Rotated encryption key to version $newVersion")
            return true
        } catch (e: Exception) {
            Timber.e(e, "Failed to rotate encryption key")
            return false
        }
    }
}
