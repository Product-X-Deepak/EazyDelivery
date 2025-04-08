package com.eazydelivery.app.security

import com.eazydelivery.app.BuildConfig
import com.eazydelivery.app.util.error.AppError
import okhttp3.CertificatePinner
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Certificate pinner for network security
 */
@Singleton
class CertificatePinner @Inject constructor() {
    
    // Certificate hashes for API domains
    private val certificateHashes = mapOf(
        "api.eazydelivery.com" to listOf(
            "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", // Replace with actual certificate hash
            "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=" // Backup certificate hash
        ),
        "dev-api.eazydelivery.com" to listOf(
            "sha256/CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC=", // Replace with actual certificate hash
            "sha256/DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD=" // Backup certificate hash
        )
    )
    
    /**
     * Create an OkHttp certificate pinner
     * 
     * @return The certificate pinner
     */
    fun createCertificatePinner(): CertificatePinner {
        return try {
            val builder = CertificatePinner.Builder()
            
            // Add certificate pins for each domain
            certificateHashes.forEach { (domain, hashes) ->
                hashes.forEach { hash ->
                    builder.add(domain, hash)
                }
            }
            
            // In debug builds, we can disable certificate pinning
            if (BuildConfig.DEBUG) {
                Timber.w("Certificate pinning is disabled in debug builds")
                return CertificatePinner.DEFAULT
            }
            
            builder.build()
        } catch (e: Exception) {
            Timber.e(e, "Error creating certificate pinner")
            throw AppError.Security.CertificatePinningError("Failed to create certificate pinner", e)
        }
    }
    
    /**
     * Get the certificate hashes for a domain
     * 
     * @param domain The domain
     * @return The certificate hashes
     */
    fun getCertificateHashes(domain: String): List<String> {
        return certificateHashes[domain] ?: emptyList()
    }
    
    /**
     * Check if certificate pinning is enabled
     * 
     * @return true if certificate pinning is enabled, false otherwise
     */
    fun isCertificatePinningEnabled(): Boolean {
        return !BuildConfig.DEBUG
    }
}
