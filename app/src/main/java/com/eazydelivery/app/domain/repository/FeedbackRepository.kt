package com.eazydelivery.app.domain.repository

import com.eazydelivery.app.domain.model.Feedback

interface FeedbackRepository {
    suspend fun submitFeedback(feedback: Feedback): Result<Unit>
    suspend fun getFeedbackById(id: String): Result<Feedback?>
    suspend fun getAllFeedback(): Result<List<Feedback>>
    suspend fun getRecentFeedback(limit: Int): Result<List<Feedback>>
    suspend fun getAverageRating(): Result<Float>
}
