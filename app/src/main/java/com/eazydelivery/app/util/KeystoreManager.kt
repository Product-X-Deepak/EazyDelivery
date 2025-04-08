package com.eazydelivery.app.util

import android.content.Context
import com.eazydelivery.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages keystore operations securely
 * Provides methods to access keystore information without hardcoding passwords
 */
@Singleton
class KeystoreManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureStorage: SecureStorage
) {
    companion object {
        private const val KEYSTORE_PASSWORD_KEY = "keystore_password"
        private const val KEY_PASSWORD_KEY = "key_password"
        private const val KEYSTORE_PATH = "../keystore/eazydelivery.jks"
        private const val KEYSTORE_TYPE = "JKS"
        private const val KEY_ALIAS = "eazydelivery"
    }

    /**
     * Gets the keystore password from secure storage or environment
     * @return The keystore password or null if not available
     */
    fun getKeystorePassword(): String? {
        // First try to get from secure storage
        val storedPassword = secureStorage.getString(KEYSTORE_PASSWORD_KEY, "")
        if (storedPassword.isNotEmpty()) {
            return storedPassword
        }

        // Then try to get from BuildConfig
        val buildConfigPassword = BuildConfig.KEYSTORE_PASSWORD
        if (buildConfigPassword.isNotEmpty()) {
            // Store for future use
            secureStorage.saveString(KEYSTORE_PASSWORD_KEY, buildConfigPassword)
            return buildConfigPassword
        }

        // If not available, log a warning
        Timber.w("Keystore password not available")
        return null
    }

    /**
     * Gets the key password from secure storage or environment
     * @return The key password or null if not available
     */
    fun getKeyPassword(): String? {
        // First try to get from secure storage
        val storedPassword = secureStorage.getString(KEY_PASSWORD_KEY, "")
        if (storedPassword.isNotEmpty()) {
            return storedPassword
        }

        // Then try to get from BuildConfig
        val buildConfigPassword = BuildConfig.KEY_PASSWORD
        if (buildConfigPassword.isNotEmpty()) {
            // Store for future use
            secureStorage.saveString(KEY_PASSWORD_KEY, buildConfigPassword)
            return buildConfigPassword
        }

        // If not available, log a warning
        Timber.w("Key password not available")
        return null
    }

    /**
     * Validates the keystore and key passwords
     * @return true if valid, false otherwise
     */
    fun validateKeystoreCredentials(): Boolean {
        val keystorePassword = getKeystorePassword() ?: return false
        val keyPassword = getKeyPassword() ?: return false

        try {
            val keystore = KeyStore.getInstance(KEYSTORE_TYPE)
            val keystoreFile = File(context.filesDir, KEYSTORE_PATH)
            
            if (!keystoreFile.exists()) {
                Timber.e("Keystore file not found at ${keystoreFile.absolutePath}")
                return false
            }
            
            FileInputStream(keystoreFile).use { fis ->
                keystore.load(fis, keystorePassword.toCharArray())
            }
            
            // Check if the key alias exists and can be accessed
            val key = keystore.getKey(KEY_ALIAS, keyPassword.toCharArray())
            return key != null
        } catch (e: Exception) {
            Timber.e(e, "Error validating keystore credentials")
            return false
        }
    }

    /**
     * Gets the certificate from the keystore
     * @return The X509Certificate or null if not available
     */
    fun getCertificate(): X509Certificate? {
        val keystorePassword = getKeystorePassword() ?: return null

        try {
            val keystore = KeyStore.getInstance(KEYSTORE_TYPE)
            val keystoreFile = File(context.filesDir, KEYSTORE_PATH)
            
            if (!keystoreFile.exists()) {
                Timber.e("Keystore file not found at ${keystoreFile.absolutePath}")
                return null
            }
            
            FileInputStream(keystoreFile).use { fis ->
                keystore.load(fis, keystorePassword.toCharArray())
            }
            
            val certificateEntry = keystore.getCertificate(KEY_ALIAS)
            if (certificateEntry != null) {
                val certFactory = CertificateFactory.getInstance("X.509")
                return certificateEntry as X509Certificate
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting certificate from keystore")
        }
        
        return null
    }
}
