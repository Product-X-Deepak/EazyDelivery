package com.eazydelivery.app.domain.model

/**
 * Model class for user feedback
 */
data class Feedback(
    val id: String,
    val rating: Int,
    val feedbackType: String,
    val comments: String,
    val timestamp: Long
)
