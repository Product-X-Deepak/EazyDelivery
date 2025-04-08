package com.eazydelivery.app.data.remote

import android.content.Context
import com.eazydelivery.app.BuildConfig
import com.eazydelivery.app.util.SecureStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.CertificatePinner
import timber.log.Timber
import java.net.URL
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced certificate pinner with backup pins for SSL pinning
 * This helps prevent man-in-the-middle attacks and supports key rotation
 */
@Singleton
class CertificatePinner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureStorage: SecureStorage
) {

    companion object {
        // Primary pins - SHA-256 hashes of the public key certificates
        private const val PRIMARY_PIN_1 = "sha256/Vjs8r4z+80wjNcr1YKepWQboSIRi63WsWXhIMN+eWys="
        private const val PRIMARY_PIN_2 = "sha256/YLh1dUR9y6Kja30RrAn7JKnbQG/uEtLMkBgFF2Fuihg="

        // Backup pins - used if the primary pins are rotated
        private const val BACKUP_PIN_1 = "sha256/FEzVOUp4dF3gI0ZVPRJhFbSJVXR+uQmMH65xhs1glH4="
        private const val BACKUP_PIN_2 = "sha256/Y9mvm0exBk1JoQ57f9Vm28jKo5lFm/woKcVxrYxu80o="

        // Keys for secure storage
        private const val KEY_CERT_EXPIRATION = "cert_expiration_date"
        private const val KEY_CUSTOM_PIN_1 = "custom_pin_1"
        private const val KEY_CUSTOM_PIN_2 = "custom_pin_2"

        // Default expiration (1 year from now in milliseconds)
        private const val DEFAULT_EXPIRATION_MS = 365L * 24 * 60 * 60 * 1000
    }

    /**
     * Get the certificate pinner with primary and backup pins
     * Includes any custom pins that have been set via remote configuration
     */
    fun getPinner(): CertificatePinner {
        val hostname = getHostname()
        val builder = CertificatePinner.Builder()

        // Add primary pins
        builder.add(hostname, PRIMARY_PIN_1)
        builder.add(hostname, PRIMARY_PIN_2)

        // Add backup pins
        builder.add(hostname, BACKUP_PIN_1)
        builder.add(hostname, BACKUP_PIN_2)

        // Add any custom pins from secure storage
        getCustomPin1()?.let { pin ->
            if (pin.isNotEmpty() && pin.startsWith("sha256/")) {
                builder.add(hostname, pin)
                Timber.d("Added custom pin 1")
            }
        }

        getCustomPin2()?.let { pin ->
            if (pin.isNotEmpty() && pin.startsWith("sha256/")) {
                builder.add(hostname, pin)
                Timber.d("Added custom pin 2")
            }
        }

        return builder.build()
    }

    /**
     * Check if certificate pins need to be rotated based on expiration date
     * @return true if pins need to be rotated, false otherwise
     */
    fun needsRotation(): Boolean {
        val expirationTime = getExpirationTime()
        val currentTime = System.currentTimeMillis()

        // If expiration time is in the past or within 30 days, rotation is needed
        val thirtyDaysInMs = 30L * 24 * 60 * 60 * 1000
        return currentTime > (expirationTime - thirtyDaysInMs)
    }

    /**
     * Set custom certificate pins for key rotation
     * @param pin1 First custom pin (sha256/...)
     * @param pin2 Second custom pin (sha256/...)
     * @param expirationTimeMs Expiration time in milliseconds since epoch
     */
    fun setCustomPins(pin1: String, pin2: String, expirationTimeMs: Long = System.currentTimeMillis() + DEFAULT_EXPIRATION_MS) {
        if (pin1.isNotEmpty() && pin1.startsWith("sha256/")) {
            secureStorage.saveString(KEY_CUSTOM_PIN_1, pin1)
        }

        if (pin2.isNotEmpty() && pin2.startsWith("sha256/")) {
            secureStorage.saveString(KEY_CUSTOM_PIN_2, pin2)
        }

        // Set new expiration time
        secureStorage.saveLong(KEY_CERT_EXPIRATION, expirationTimeMs)

        Timber.d("Set custom pins with expiration: ${Date(expirationTimeMs)}")
    }

    /**
     * Get the first custom pin from secure storage
     */
    private fun getCustomPin1(): String? {
        return secureStorage.getString(KEY_CUSTOM_PIN_1, "")
    }

    /**
     * Get the second custom pin from secure storage
     */
    private fun getCustomPin2(): String? {
        return secureStorage.getString(KEY_CUSTOM_PIN_2, "")
    }

    /**
     * Get the expiration time for certificate pins
     * @return Expiration time in milliseconds since epoch
     */
    private fun getExpirationTime(): Long {
        val storedExpiration = secureStorage.getLong(KEY_CERT_EXPIRATION, 0L)

        // If no expiration is stored, set a default expiration (1 year from now)
        if (storedExpiration == 0L) {
            val defaultExpiration = System.currentTimeMillis() + DEFAULT_EXPIRATION_MS
            secureStorage.saveLong(KEY_CERT_EXPIRATION, defaultExpiration)
            return defaultExpiration
        }

        return storedExpiration
    }

    /**
     * Extract hostname from the API base URL
     */
    private fun getHostname(): String {
        return try {
            val url = URL(BuildConfig.API_BASE_URL)
            url.host
        } catch (e: Exception) {
            Timber.e(e, "Error extracting hostname from URL: ${BuildConfig.API_BASE_URL}")
            "api.eazydelivery.com" // Fallback hostname
        }
    }
}
