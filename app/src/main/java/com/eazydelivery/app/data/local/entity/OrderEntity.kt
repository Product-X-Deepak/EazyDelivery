package com.eazydelivery.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "orders",
    indices = [
        Index("platformName"),
        Index("timestamp"),
        Index("deliveryStatus"),
        Index("amount"),
        Index("priority")
    ],
    foreignKeys = [
        ForeignKey(
            entity = PlatformEntity::class,
            parentColumns = ["name"],
            childColumns = ["platformName"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class OrderEntity(
    @PrimaryKey
    val id: String,
    val platformName: String,
    val amount: Double,
    val timestamp: String,
    val isAccepted: Boolean,
    val deliveryStatus: String = "PENDING",
    val priority: String = "MEDIUM",
    val estimatedDistance: Double? = null,
    val estimatedTime: Int? = null
)
