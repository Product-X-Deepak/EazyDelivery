package com.eazydelivery.app.domain.model

data class SubscriptionStatus(
    val isSubscribed: Boolean,
    val trialDaysLeft: Int,
    val endDate: String
)
