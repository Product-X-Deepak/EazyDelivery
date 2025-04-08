package com.eazydelivery.app.domain.model

data class Platform(
    val name: String,
    val isEnabled: Boolean,
    val minAmount: Int,
    val maxAmount: Int = 500,
    val autoAccept: Boolean = true,
    val notificationSound: String? = null,
    val priority: Int = 0,
    val acceptMediumPriority: Boolean = false,
    val packageName: String = "",
    val shouldRemove: Boolean = false
)
