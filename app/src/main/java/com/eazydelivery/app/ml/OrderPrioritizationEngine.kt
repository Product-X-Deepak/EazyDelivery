package com.eazydelivery.app.ml

import android.content.Context
import com.eazydelivery.app.domain.model.OrderNotification
import com.eazydelivery.app.domain.repository.UserPreferencesRepository
import com.eazydelivery.app.util.ErrorHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Engine for prioritizing and scoring incoming order notifications
 */
@Singleton
class OrderPrioritizationEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val errorHandler: ErrorHandler
) {
    // Priority levels
    enum class Priority {
        HIGH,
        MEDIUM,
        LOW
    }
    
    /**
     * Scores an order notification and determines its priority
     * 
     * @param notification The order notification to score
     * @param classification The ML classification results
     * @return The priority level as a string ("HIGH", "MEDIUM", or "LOW")
     */
    fun scoreOrder(
        notification: OrderNotification,
        classification: Map<NotificationClassifier.Label, Float>
    ): String {
        try {
            // Get user preferences for weighting factors
            val earningsWeight = userPreferencesRepository.getEarningsWeight().getOrDefault(0.5f)
            val distanceWeight = userPreferencesRepository.getDistanceWeight().getOrDefault(0.3f)
            val timeWeight = userPreferencesRepository.getTimeWeight().getOrDefault(0.2f)
            
            // Calculate weighted score
            var score = 0.0f
            
            // Add earnings score
            val earningsScore = classification[NotificationClassifier.Label.HIGH_EARNING] ?: 0.5f
            score += earningsScore * earningsWeight
            
            // Add distance score
            val distanceScore = classification[NotificationClassifier.Label.LOW_DISTANCE] ?: 0.5f
            score += distanceScore * distanceWeight
            
            // Add time score
            val timeScore = classification[NotificationClassifier.Label.BUSY_TIME] ?: 0.5f
            score += timeScore * timeWeight
            
            // Subtract low priority score
            val lowPriorityScore = classification[NotificationClassifier.Label.LOW_PRIORITY] ?: 0.0f
            score -= lowPriorityScore * 0.5f
            
            // Determine priority based on score
            val priority = when {
                score >= 0.7f -> Priority.HIGH
                score >= 0.4f -> Priority.MEDIUM
                else -> Priority.LOW
            }
            
            Timber.d("Order scored with ${score}: ${priority.name} priority")
            
            return priority.name
            
        } catch (e: Exception) {
            errorHandler.handleException("OrderPrioritizationEngine.scoreOrder", e)
            // Return medium priority as default on error
            return Priority.MEDIUM.name
        }
    }
    
    /**
     * Updates the scoring model based on user feedback
     * 
     * @param notificationId The ID of the notification
     * @param userPriority The priority assigned by the user
     */
    fun updateModelWithFeedback(notificationId: String, userPriority: String) {
        try {
            // In a real implementation, this would update the model weights
            // For now, we'll just log that feedback was received
            Timber.d("Received feedback for notification $notificationId: user priority = $userPriority")
            
            // We would use this feedback to adjust the model in a real implementation
            
        } catch (e: Exception) {
            errorHandler.handleException("OrderPrioritizationEngine.updateModelWithFeedback", e)
        }
    }
}
