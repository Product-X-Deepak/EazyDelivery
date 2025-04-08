package com.eazydelivery.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "platforms",
    indices = [
        Index("isEnabled"),
        Index("priority")
    ]
)
data class PlatformEntity(
    @PrimaryKey
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
