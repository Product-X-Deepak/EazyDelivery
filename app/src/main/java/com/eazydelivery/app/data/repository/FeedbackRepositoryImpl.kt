package com.eazydelivery.app.data.repository

import com.eazydelivery.app.data.local.dao.FeedbackDao
import com.eazydelivery.app.data.local.entity.FeedbackEntity
import com.eazydelivery.app.domain.model.Feedback
import com.eazydelivery.app.domain.repository.BaseRepository
import com.eazydelivery.app.domain.repository.FeedbackRepository
import com.eazydelivery.app.util.ErrorHandler
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackRepositoryImpl @Inject constructor(
    private val feedbackDao: FeedbackDao,
    private val firebaseAnalytics: FirebaseAnalytics,
    override val errorHandler: ErrorHandler
) : FeedbackRepository, BaseRepository {
    
    override suspend fun submitFeedback(feedback: Feedback): Result<Unit> = safeDbCall("FeedbackRepository.submitFeedback") {
        // Convert to entity and save to database
        val entity = FeedbackEntity(
            id = feedback.id,
            rating = feedback.rating,
            feedbackType = feedback.feedbackType,
            comments = feedback.comments,
            timestamp = feedback.timestamp
        )
        
        feedbackDao.insertFeedback(entity)
        
        // Log feedback event to analytics
        firebaseAnalytics.logEvent("feedback_submitted") {
            param("rating", feedback.rating.toDouble())
            param("feedback_type", feedback.feedbackType)
            param("has_comments", feedback.comments.isNotEmpty())
        }
    }
    
    override suspend fun getFeedbackById(id: String): Result<Feedback?> = safeDbCall("FeedbackRepository.getFeedbackById") {
        val entity = feedbackDao.getFeedbackById(id)
        entity?.toDomain()
    }
    
    override suspend fun getAllFeedback(): Result<List<Feedback>> = safeDbCall("FeedbackRepository.getAllFeedback") {
        feedbackDao.getAllFeedback().map { it.toDomain() }
    }
    
    override suspend fun getRecentFeedback(limit: Int): Result<List<Feedback>> = safeDbCall("FeedbackRepository.getRecentFeedback") {
        feedbackDao.getRecentFeedback(limit).map { it.toDomain() }
    }
    
    override suspend fun getAverageRating(): Result<Float> = safeDbCall("FeedbackRepository.getAverageRating") {
        feedbackDao.getAverageRating() ?: 0f
    }
    
    private fun FeedbackEntity.toDomain(): Feedback {
        return Feedback(
            id = id,
            rating = rating,
            feedbackType = feedbackType,
            comments = comments,
            timestamp = timestamp
        )
    }
}
