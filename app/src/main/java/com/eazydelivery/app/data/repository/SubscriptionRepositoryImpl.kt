package com.eazydelivery.app.data.repository

import com.eazydelivery.app.data.remote.ApiService
import com.eazydelivery.app.domain.model.SubscriptionStatus
import com.eazydelivery.app.domain.repository.BaseRepository
import com.eazydelivery.app.domain.repository.SubscriptionRepository
import com.eazydelivery.app.util.ErrorHandler
import com.eazydelivery.app.util.SecureStorage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val secureStorage: SecureStorage,
    override val errorHandler: ErrorHandler
) : SubscriptionRepository, BaseRepository {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    override suspend fun getSubscriptionStatus(): Result<SubscriptionStatus> = safeApiCall("SubscriptionRepository.getSubscriptionStatus") {
        try {
            // Try to get subscription status from API
            val response = apiService.getSubscriptionStatus()
            
            if (response.success && response.data != null) {
                val data = response.data
                
                // Save subscription data to secure storage
                secureStorage.saveBoolean(SecureStorage.KEY_SUBSCRIPTION_STATUS, data.isSubscribed)
                secureStorage.saveString(SecureStorage.KEY_SUBSCRIPTION_EXPIRY, data.endDate)
                
                // Calculate trial days left
                val trialDaysLeft = calculateTrialDaysLeft(data.endDate)
                
                return@safeApiCall SubscriptionStatus(
                    isSubscribed = data.isSubscribed,
                    trialDaysLeft = trialDaysLeft,
                    endDate = data.endDate
                )
            }
        } catch (e: Exception) {
            errorHandler.handleException("SubscriptionRepository.getSubscriptionStatus", e, "API call failed, falling back to local data")
            // Fall back to local data
        }
        
        // Get subscription data from secure storage
        val isSubscribed = secureStorage.getBoolean(SecureStorage.KEY_SUBSCRIPTION_STATUS, false)
        val endDate = secureStorage.getString(SecureStorage.KEY_SUBSCRIPTION_EXPIRY, "")
        
        // If no subscription data is available, create a trial
        if (endDate.isEmpty()) {
            val trialEndDate = createTrialPeriod()
            return@safeApiCall SubscriptionStatus(
                isSubscribed = false,
                trialDaysLeft = 7, // Default trial period
                endDate = trialEndDate
            )
        }
        
        // Calculate trial days left
        val trialDaysLeft = calculateTrialDaysLeft(endDate)
        
        SubscriptionStatus(
            isSubscribed = isSubscribed,
            trialDaysLeft = trialDaysLeft,
            endDate = endDate
        )
    }
    
    override suspend fun activateSubscription(subscriptionId: String): Result<SubscriptionStatus> = safeApiCall("SubscriptionRepository.activateSubscription") {
        val response = apiService.activateSubscription(subscriptionId)
        
        if (response.success && response.data != null) {
            val data = response.data
            
            // Save subscription data to secure storage
            secureStorage.saveBoolean(SecureStorage.KEY_SUBSCRIPTION_STATUS, true)
            secureStorage.saveString(SecureStorage.KEY_SUBSCRIPTION_EXPIRY, data.endDate)
            
            SubscriptionStatus(
                isSubscribed = true,
                trialDaysLeft = 0,
                endDate = data.endDate
            )
        } else {
            throw Exception(response.error ?: "Failed to activate subscription")
        }
    }
    
    override suspend fun cancelSubscription(): Result<Boolean> = safeApiCall("SubscriptionRepository.cancelSubscription") {
        val response = apiService.cancelSubscription()
        
        if (response.success) {
            // Update subscription data in secure storage
            secureStorage.saveBoolean(SecureStorage.KEY_SUBSCRIPTION_STATUS, false)
            
            true
        } else {
            throw Exception(response.error ?: "Failed to cancel subscription")
        }
    }
    
    override suspend fun extendTrial(days: Int): Result<SubscriptionStatus> = safeApiCall("SubscriptionRepository.extendTrial") {
        val currentEndDate = secureStorage.getString(SecureStorage.KEY_SUBSCRIPTION_EXPIRY, "")
        
        val calendar = Calendar.getInstance()
        if (currentEndDate.isNotEmpty()) {
            try {
                val date = dateFormat.parse(currentEndDate)
                if (date != null) {
                    calendar.time = date
                }
            } catch (e: Exception) {
                // If parsing fails, use current date
            }
        }
        
        // Add the specified number of days
        calendar.add(Calendar.DAY_OF_MONTH, days)
        val newEndDate = dateFormat.format(calendar.time)
        
        // Save the new end date
        secureStorage.saveString(SecureStorage.KEY_SUBSCRIPTION_EXPIRY, newEndDate)
        
        // Calculate trial days left
        val trialDaysLeft = calculateTrialDaysLeft(newEndDate)
        
        SubscriptionStatus(
            isSubscribed = false,
            trialDaysLeft = trialDaysLeft,
            endDate = newEndDate
        )
    }
    
    private fun createTrialPeriod(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 7) // 7-day trial
        val trialEndDate = dateFormat.format(calendar.time)
        
        // Save trial end date
        secureStorage.saveString(SecureStorage.KEY_SUBSCRIPTION_EXPIRY, trialEndDate)
        secureStorage.saveString(SecureStorage.KEY_TRIAL_END_DATE, trialEndDate)
        
        return trialEndDate
    }
    
    private fun calculateTrialDaysLeft(endDateStr: String): Int {
        try {
            val endDate = dateFormat.parse(endDateStr) ?: return 0
            val currentDate = Date()
            
            if (endDate.before(currentDate)) {
                return 0
            }
            
            val diffInMillis = endDate.time - currentDate.time
            return TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()
        } catch (e: Exception) {
            errorHandler.handleException("SubscriptionRepository.calculateTrialDaysLeft", e)
            return 0
        }
    }
}
