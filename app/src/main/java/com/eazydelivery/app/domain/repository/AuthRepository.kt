package com.eazydelivery.app.domain.repository

import com.eazydelivery.app.domain.model.UserInfo

/**
 * Repository interface for authentication operations
 */
public interface AuthRepository {
    /**
     * Send OTP to the provided phone number
     *
     * @param phoneNumber User's phone number with country code
     * @return Result containing Unit on success or Exception on failure
     */
    suspend fun sendOtp(phoneNumber: String): Result<Unit>

    /**
     * Verify OTP for the provided phone number
     *
     * @param phoneNumber User's phone number with country code
     * @param otp OTP received by the user
     * @return Result containing UserInfo on success or Exception on failure
     */
    suspend fun verifyOtp(phoneNumber: String, otp: String): Result<UserInfo>

    /**
     * Update user profile information
     *
     * @param name User's name
     * @param email User's email (optional)
     * @return Result containing UserInfo on success or Exception on failure
     */
    suspend fun updateProfile(name: String, email: String? = null): Result<UserInfo>

    /**
     * Logout the current user
     *
     * @return Result containing Unit on success or Exception on failure
     */
    suspend fun logout(): Result<Unit>

    /**
     * Get the current user's information
     *
     * @return Result containing UserInfo on success or Exception on failure
     */
    suspend fun getUserInfo(): Result<UserInfo>

    /**
     * Check if a user is currently logged in
     *
     * @return Result containing Boolean on success or Exception on failure
     */
    suspend fun isLoggedIn(): Result<Boolean>
}
