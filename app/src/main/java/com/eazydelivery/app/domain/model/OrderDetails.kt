package com.eazydelivery.app.domain.model

data class OrderDetails(
    val platform: String,
    val amount: Double,
    val orderId: String,
    val timestamp: Long
)

