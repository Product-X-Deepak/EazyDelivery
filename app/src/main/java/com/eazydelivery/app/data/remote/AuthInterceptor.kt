package com.eazydelivery.app.data.remote

import com.eazydelivery.app.BuildConfig
import com.eazydelivery.app.util.SecureStorage
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val secureStorage: SecureStorage
) : Interceptor {
    
    companion object {
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val HEADER_API_KEY = "X-API-Key"
        private const val HEADER_DEVICE_ID = "X-Device-ID"
        private const val HEADER_APP_VERSION = "X-App-Version"
        private const val HEADER_PLATFORM = "X-Platform"
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Get the auth token from secure storage
        val authToken = secureStorage.getString(SecureStorage.KEY_AUTH_TOKEN)
        
        // Get or generate device ID
        val deviceId = getOrGenerateDeviceId()
        
        // Build the request with security headers
        val requestBuilder = originalRequest.newBuilder()
            .header(HEADER_API_KEY, BuildConfig.API_KEY)
            .header(HEADER_DEVICE_ID, deviceId)
            .header(HEADER_APP_VERSION, BuildConfig.VERSION_NAME)
            .header(HEADER_PLATFORM, "android")
        
        // Add auth token if available
        if (authToken.isNotEmpty()) {
            requestBuilder.header(HEADER_AUTHORIZATION, "Bearer $authToken")
        }
        
        return chain.proceed(requestBuilder.build())
    }
    
    private fun getOrGenerateDeviceId(): String {
        val deviceId = secureStorage.getString(SecureStorage.KEY_DEVICE_ID)
        
        if (deviceId.isNotEmpty()) {
            return deviceId
        }
        
        // Generate a new device ID
        val newDeviceId = UUID.randomUUID().toString()
        secureStorage.saveString(SecureStorage.KEY_DEVICE_ID, newDeviceId)
        Timber.d("Generated new device ID: $newDeviceId")
        
        return newDeviceId
    }
}
