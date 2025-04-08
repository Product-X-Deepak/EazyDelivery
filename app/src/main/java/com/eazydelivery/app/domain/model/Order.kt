package com.eazydelivery.app.domain.model

data class Order(
    val id: String,
    val platformName: String,
    val amount: Double,
    val timestamp: String,
    val isAccepted: Boolean,
    val deliveryStatus: String = "PENDING",
    val notes: String? = null,
    val priority: String = "MEDIUM",
    val estimatedDistance: Double? = null,
    val estimatedTime: Int? = null
)
