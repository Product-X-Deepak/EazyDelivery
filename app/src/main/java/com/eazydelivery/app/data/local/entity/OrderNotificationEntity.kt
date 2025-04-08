package com.eazydelivery.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity class for storing order notifications in the database
 */
@Entity(tableName = "order_notifications")
data class OrderNotificationEntity(
    @PrimaryKey
    val id: String,
    val packageName: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val amount: Double,
    val platformName: String,
    val estimatedDistance: Double? = null,
    val estimatedTime: Int? = null,
    val priority: String,
    val highEarningScore: Float,
    val lowDistanceScore: Float,
    val busyTimeScore: Float,
    val lowPriorityScore: Float
)
