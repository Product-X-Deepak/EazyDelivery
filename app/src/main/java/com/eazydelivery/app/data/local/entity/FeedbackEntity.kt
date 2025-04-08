package com.eazydelivery.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity class for storing user feedback in the database
 */
@Entity(tableName = "feedback")
data class FeedbackEntity(
    @PrimaryKey
    val id: String,
    val rating: Int,
    val feedbackType: String,
    val comments: String,
    val timestamp: Long
)
