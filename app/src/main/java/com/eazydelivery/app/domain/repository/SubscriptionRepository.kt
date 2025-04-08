package com.eazydelivery.app.domain.repository

import com.eazydelivery.app.domain.model.SubscriptionStatus

interface SubscriptionRepository {
    suspend fun getSubscriptionStatus(): Result<SubscriptionStatus>
    suspend fun activateSubscription(subscriptionId: String): Result<SubscriptionStatus>
    suspend fun cancelSubscription(): Result<Boolean>
    suspend fun extendTrial(days: Int): Result<SubscriptionStatus>
}
