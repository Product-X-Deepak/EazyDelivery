package com.eazydelivery.app.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.eazydelivery.app.util.LogUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Security configuration for the app
 * Provides methods for creating encrypted files and shared preferences
 */
@Singleton
class SecurityConfig @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "SecurityConfig"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "eazydelivery_master_key"
        private const val ENCRYPTED_PREFS_FILE_NAME = "eazydelivery_secure_prefs"
        private const val KEY_SIZE = 256
    }
    
    /**
     * Create or get the master key for encryption
     */
    fun getMasterKey(): MasterKey {
        return MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setKeyGenParameterSpec(
                KeyGenParameterSpec.Builder(
                    MASTER_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE)
                .setUserAuthenticationRequired(false) // Set to true for biometric protection
                .build()
            )
            .build()
    }
    
    /**
     * Create encrypted shared preferences
     */
    fun createEncryptedSharedPreferences(): EncryptedSharedPreferences {
        return EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_FILE_NAME,
            getMasterKey(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences
    }
    
    /**
     * Create an encrypted file
     */
    fun createEncryptedFile(file: File): EncryptedFile {
        return EncryptedFile.Builder(
            context,
            file,
            getMasterKey(),
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
    }
    
    /**
     * Generate a new secret key for encryption
     */
    fun generateSecretKey(keyAlias: String): SecretKey {
        try {
            // First check if the key already exists
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            
            if (keyStore.containsAlias(keyAlias)) {
                return keyStore.getKey(keyAlias, null) as SecretKey
            }
            
            // Generate a new key if it doesn't exist
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE)
                .setUserAuthenticationRequired(false) // Set to true for biometric protection
                .build()
            
            keyGenerator.init(keyGenParameterSpec)
            return keyGenerator.generateKey()
        } catch (e: Exception) {
            LogUtils.e(TAG, "Error generating secret key", e)
            throw e
        }
    }
    
    /**
     * Check if biometric authentication is available
     */
    fun isBiometricAuthAvailable(): Boolean {
        return try {
            val biometricManager = androidx.biometric.BiometricManager.from(context)
            biometricManager.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG) == 
                androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
        } catch (e: Exception) {
            LogUtils.e(TAG, "Error checking biometric availability", e)
            false
        }
    }
}
