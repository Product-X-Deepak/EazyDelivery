package com.eazydelivery.app.util

import com.eazydelivery.app.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class to manage admin contact information
 * This allows for centralized management of admin contact details
 * and provides a way to update them without hardcoding values
 */
@Singleton
class AdminContactSettings @Inject constructor(
    private val secureStorage: SecureStorage
) {
    companion object {
        private const val KEY_ADMIN_PHONE = "admin_phone_number"
        private const val KEY_ADMIN_EMAIL = "admin_email"
        
        // Default values from BuildConfig (should be empty in production)
        private val DEFAULT_ADMIN_PHONE = BuildConfig.ADMIN_PHONE
        private val DEFAULT_ADMIN_EMAIL = BuildConfig.ADMIN_EMAIL
    }
    
    /**
     * Get the admin phone number
     * First tries to get from secure storage, then falls back to BuildConfig
     */
    fun getAdminPhone(): String {
        val storedPhone = secureStorage.getString(KEY_ADMIN_PHONE, "")
        return if (storedPhone.isNotEmpty()) {
            storedPhone
        } else {
            DEFAULT_ADMIN_PHONE
        }
    }
    
    /**
     * Get the admin email
     * First tries to get from secure storage, then falls back to BuildConfig
     */
    fun getAdminEmail(): String {
        val storedEmail = secureStorage.getString(KEY_ADMIN_EMAIL, "")
        return if (storedEmail.isNotEmpty()) {
            storedEmail
        } else {
            DEFAULT_ADMIN_EMAIL
        }
    }
    
    /**
     * Set the admin phone number in secure storage
     */
    fun setAdminPhone(phone: String) {
        secureStorage.saveString(KEY_ADMIN_PHONE, phone)
    }
    
    /**
     * Set the admin email in secure storage
     */
    fun setAdminEmail(email: String) {
        secureStorage.saveString(KEY_ADMIN_EMAIL, email)
    }
}
