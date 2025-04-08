package com.eazydelivery.app.data.remote

import com.eazydelivery.app.data.model.ApiResponse
import com.eazydelivery.app.data.model.LoginRequest
import com.eazydelivery.app.data.model.RegisterRequest
import com.eazydelivery.app.data.model.SubscriptionData
import com.eazydelivery.app.data.model.UserData
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<UserData>
    
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<UserData>
    
    @POST("auth/reset-password")
    suspend fun resetPassword(@Query("email") email: String): ApiResponse<Unit>
    
    @GET("user/profile/{userId}")
    suspend fun getUserProfile(@Path("userId") userId: String): ApiResponse<UserData>
    
    @GET("subscription/status")
    suspend fun getSubscriptionStatus(): ApiResponse<SubscriptionData>
    
    @POST("subscription/activate/{subscriptionId}")
    suspend fun activateSubscription(@Path("subscriptionId") subscriptionId: String): ApiResponse<SubscriptionData>
    
    @POST("subscription/cancel")
    suspend fun cancelSubscription(): ApiResponse<Boolean>
    
    @POST("orders/sync")
    suspend fun syncOrders(): ApiResponse<List<com.eazydelivery.app.data.model.OrderData>>
}
