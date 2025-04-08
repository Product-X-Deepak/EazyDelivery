package com.eazydelivery.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null
)

@Serializable
data class OtpRequest(
    val phoneNumber: String
)

@Serializable
data class OtpVerificationRequest(
    val phoneNumber: String,
    val otp: String
)

@Serializable
data class ProfileUpdateRequest(
    val name: String,
    val email: String? = null
)

@Serializable
data class UserData(
    val id: String,
    val phone: String,
    val name: String? = null,
    val email: String? = null,
    val profilePicUrl: String? = null,
    val token: String? = null,
    val isNewUser: Boolean? = null
)

@Serializable
data class SubscriptionData(
    val isSubscribed: Boolean,
    val endDate: String,
    val plan: String? = null,
    val price: Double? = null
)

@Serializable
data class OrderData(
    val id: String,
    val platformName: String,
    val amount: Double,
    val timestamp: String,
    val isAccepted: Boolean,
    val deliveryStatus: String = "PENDING",
    val notes: String? = null
)
