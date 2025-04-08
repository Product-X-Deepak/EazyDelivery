package com.eazydelivery.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.eazydelivery.app.data.local.entity.FeedbackEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for user feedback
 */
@Dao
interface FeedbackDao {
    @Insert(onConflictStrategy = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: FeedbackEntity)
    
    @Query("SELECT * FROM feedback WHERE id = :id")
    suspend fun getFeedbackById(id: String): FeedbackEntity?
    
    @Query("SELECT * FROM feedback ORDER BY timestamp DESC")
    suspend fun getAllFeedback(): List<FeedbackEntity>
    
    @Query("SELECT * FROM feedback ORDER BY timestamp DESC")
    fun getAllFeedbackFlow(): Flow<List<FeedbackEntity>>
    
    @Query("SELECT * FROM feedback ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentFeedback(limit: Int): List<FeedbackEntity>
    
    @Query("SELECT AVG(rating) FROM feedback")
    suspend fun getAverageRating(): Float?
    
    @Query("SELECT COUNT(*) FROM feedback WHERE rating >= 4")
    suspend fun getPositiveFeedbackCount(): Int
    
    @Query("SELECT COUNT(*) FROM feedback WHERE rating <= 2")
    suspend fun getNegativeFeedbackCount(): Int
    
    @Query("SELECT feedbackType, COUNT(*) as count FROM feedback GROUP BY feedbackType ORDER BY count DESC")
    suspend fun getFeedbackTypeDistribution(): Map<String, Int>
}
