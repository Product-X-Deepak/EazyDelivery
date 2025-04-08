package com.eazydelivery.app.security

import com.eazydelivery.app.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interceptor for adding security headers and validating requests
 */
@Singleton
class SecureNetworkInterceptor @Inject constructor(
    private val securityManager: SecurityManager
) : Interceptor {
    
    companion object {
        private const val HEADER_API_KEY = "X-API-Key"
        private const val HEADER_REQUEST_ID = "X-Request-ID"
        private const val HEADER_APP_VERSION = "X-App-Version"
        private const val HEADER_DEVICE_ID = "X-Device-ID"
        private const val HEADER_AUTHORIZATION = "Authorization"
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Skip security headers for certain endpoints
        if (shouldSkipSecurityHeaders(originalRequest.url.toString())) {
            return chain.proceed(originalRequest)
        }
        
        try {
            // Create a new request with security headers
            val requestWithHeaders = originalRequest.newBuilder()
                .addHeader(HEADER_API_KEY, BuildConfig.API_KEY)
                .addHeader(HEADER_REQUEST_ID, UUID.randomUUID().toString())
                .addHeader(HEADER_APP_VERSION, BuildConfig.VERSION_NAME)
                .addHeader(HEADER_DEVICE_ID, getDeviceId())
                
            // Add authorization header if available
            getAuthToken()?.let { token ->
                requestWithHeaders.addHeader(HEADER_AUTHORIZATION, "Bearer $token")
            }
            
            // Proceed with the modified request
            val response = chain.proceed(requestWithHeaders.build())
            
            // Validate the response
            validateResponse(response)
            
            return response
        } catch (e: Exception) {
            Timber.e(e, "Error in secure network interceptor")
            throw e
        }
    }
    
    /**
     * Check if security headers should be skipped for a URL
     * 
     * @param url The URL
     * @return true if security headers should be skipped, false otherwise
     */
    private fun shouldSkipSecurityHeaders(url: String): Boolean {
        // Skip security headers for public endpoints
        return url.contains("/public/") || url.contains("/auth/login")
    }
    
    /**
     * Get the device ID
     * 
     * @return The device ID
     */
    private fun getDeviceId(): String {
        // Try to get the device ID from secure storage
        return securityManager.retrieveSecurely("device_id") ?: run {
            // Generate a new device ID if not found
            val newDeviceId = securityManager.generateSecureToken()
            securityManager.storeSecurely("device_id", newDeviceId)
            newDeviceId
        }
    }
    
    /**
     * Get the authentication token
     * 
     * @return The authentication token, or null if not available
     */
    private fun getAuthToken(): String? {
        return securityManager.retrieveSecurely("auth_token")
    }
    
    /**
     * Validate the response
     * 
     * @param response The response
     */
    private fun validateResponse(response: Response) {
        // Check for security headers in the response
        val securityHeaders = listOf(
            "X-Content-Type-Options",
            "X-Frame-Options",
            "X-XSS-Protection",
            "Strict-Transport-Security"
        )
        
        // Log missing security headers in debug builds
        if (BuildConfig.DEBUG) {
            securityHeaders.forEach { header ->
                if (response.header(header) == null) {
                    Timber.w("Missing security header: $header")
                }
            }
        }
    }
}
