package com.eazydelivery.app.domain.model

/**
 * Model class for order notifications
 */
data class OrderNotification(
    val id: String,
    val packageName: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val amount: Double,
    val platformName: String,
    val estimatedDistance: Double? = null,
    val estimatedTime: Int? = null
)
