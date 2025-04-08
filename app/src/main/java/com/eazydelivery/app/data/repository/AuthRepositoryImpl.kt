package com.eazydelivery.app.data.repository

import com.eazydelivery.app.data.model.OtpRequest
import com.eazydelivery.app.data.model.OtpVerificationRequest
import com.eazydelivery.app.data.model.ProfileUpdateRequest
import com.eazydelivery.app.data.remote.ApiService
import com.eazydelivery.app.domain.model.UserInfo
import com.eazydelivery.app.domain.repository.AuthRepository
import com.eazydelivery.app.domain.repository.BaseRepository
import com.eazydelivery.app.util.ErrorHandler
import com.eazydelivery.app.util.SecureStorage
import com.google.firebase.auth.FirebaseAuth
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val secureStorage: SecureStorage,
    private val apiService: ApiService,
    override val errorHandler: ErrorHandler
) : AuthRepository, BaseRepository {

    override suspend fun sendOtp(phoneNumber: String): Result<Unit> = safeApiCall {
        // In a real app, we would use Firebase Phone Auth here
        // For simplicity, we'll just simulate OTP sending

        // Call our API to send OTP
        val otpRequest = OtpRequest(phoneNumber)
        val apiResponse = apiService.sendOtp(otpRequest)

        if (!apiResponse.success) {
            throw Exception(apiResponse.error ?: "Failed to send OTP")
        }

        // Save phone number to secure storage for later use
        secureStorage.saveString(SecureStorage.KEY_USER_PHONE, phoneNumber)

        Timber.d("OTP sent to $phoneNumber")
    }

    override suspend fun verifyOtp(phoneNumber: String, otp: String): Result<UserInfo> = safeApiCall {
        // In a real app, we would verify the OTP with Firebase Phone Auth
        // For simplicity, we'll just simulate OTP verification

        // Call our API to verify OTP
        val verificationRequest = OtpVerificationRequest(phoneNumber, otp)
        val apiResponse = apiService.verifyOtp(verificationRequest)

        if (!apiResponse.success || apiResponse.data == null) {
            throw Exception(apiResponse.error ?: "OTP verification failed")
        }

        val userData = apiResponse.data

        // Save user info to secure storage for persistent login
        secureStorage.saveString(SecureStorage.KEY_USER_ID, userData.id)
        secureStorage.saveString(SecureStorage.KEY_USER_PHONE, userData.phone)
        userData.name?.let { secureStorage.saveString(SecureStorage.KEY_USER_NAME, it) }
        userData.email?.let { secureStorage.saveString(SecureStorage.KEY_USER_EMAIL, it) }
        userData.token?.let { secureStorage.saveString(SecureStorage.KEY_AUTH_TOKEN, it) }

        UserInfo(
            id = userData.id,
            phone = userData.phone,
            name = userData.name,
            email = userData.email,
            isNewUser = userData.isNewUser ?: false
        )
    }

    override suspend fun updateProfile(name: String, email: String?): Result<UserInfo> = safeApiCall {
        // Call our API to update profile
        val updateRequest = ProfileUpdateRequest(name, email)
        val apiResponse = apiService.updateProfile(updateRequest)

        if (!apiResponse.success || apiResponse.data == null) {
            throw Exception(apiResponse.error ?: "Profile update failed")
        }

        val userData = apiResponse.data

        // Update user info in secure storage
        secureStorage.saveString(SecureStorage.KEY_USER_NAME, name)
        email?.let { secureStorage.saveString(SecureStorage.KEY_USER_EMAIL, it) }

        UserInfo(
            id = userData.id,
            phone = userData.phone,
            name = userData.name,
            email = userData.email
        )
    }

    override suspend fun logout(): Result<Unit> = safeApiCall {
        firebaseAuth.signOut()

        // Clear user info from secure storage
        secureStorage.remove(SecureStorage.KEY_USER_ID)
        secureStorage.remove(SecureStorage.KEY_USER_PHONE)
        secureStorage.remove(SecureStorage.KEY_USER_NAME)
        secureStorage.remove(SecureStorage.KEY_USER_EMAIL)
        secureStorage.remove(SecureStorage.KEY_AUTH_TOKEN)

        Timber.d("User logged out")
    }

    override suspend fun getUserInfo(): Result<UserInfo> = safeApiCall {
        val userId = secureStorage.getString(SecureStorage.KEY_USER_ID)
        val userPhone = secureStorage.getString(SecureStorage.KEY_USER_PHONE)

        if (userId.isNotEmpty() && userPhone.isNotEmpty()) {
            // Try to get additional user data from API
            try {
                val apiResponse = apiService.getUserProfile(userId)

                if (apiResponse.success && apiResponse.data != null) {
                    val userData = apiResponse.data
                    return@safeApiCall UserInfo(
                        id = userData.id,
                        phone = userData.phone,
                        name = userData.name,
                        email = userData.email
                    )
                }
            } catch (e: Exception) {
                errorHandler.handleException("AuthRepository.getUserInfo", e, "API call failed, falling back to local data")
                // Fall back to local data
            }

            // Return user info from secure storage
            UserInfo(
                id = userId,
                phone = userPhone,
                name = secureStorage.getString(SecureStorage.KEY_USER_NAME),
                email = secureStorage.getString(SecureStorage.KEY_USER_EMAIL)
            )
        } else {
            throw Exception("User not logged in")
        }
    }

    override suspend fun isLoggedIn(): Result<Boolean> = safeApiCall {
        secureStorage.getString(SecureStorage.KEY_USER_ID).isNotEmpty() &&
                secureStorage.getString(SecureStorage.KEY_USER_PHONE).isNotEmpty()
    }
}
