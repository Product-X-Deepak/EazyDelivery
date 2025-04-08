package com.eazydelivery.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.eazydelivery.app.data.local.entity.OrderNotificationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for order notifications
 */
@Dao
interface OrderNotificationDao {
    @Insert(onConflictStrategy = OnConflictStrategy.REPLACE)
    suspend fun insertOrderNotification(notification: OrderNotificationEntity)
    
    @Query("SELECT * FROM order_notifications WHERE id = :id")
    suspend fun getOrderNotificationById(id: String): OrderNotificationEntity?
    
    @Query("SELECT * FROM order_notifications ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentOrderNotifications(limit: Int): List<OrderNotificationEntity>
    
    @Query("SELECT * FROM order_notifications ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentOrderNotificationsFlow(limit: Int): Flow<List<OrderNotificationEntity>>
    
    @Query("SELECT * FROM order_notifications WHERE platformName = :platformName ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentOrderNotificationsByPlatform(platformName: String, limit: Int): List<OrderNotificationEntity>
    
    @Query("SELECT * FROM order_notifications WHERE priority = :priority ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentOrderNotificationsByPriority(priority: String, limit: Int): List<OrderNotificationEntity>
    
    @Query("DELETE FROM order_notifications WHERE timestamp < :cutoffTime")
    suspend fun deleteOldNotifications(cutoffTime: Long): Int
    
    @Query("SELECT COUNT(*) FROM order_notifications")
    suspend fun getNotificationCount(): Int
    
    @Query("SELECT AVG(amount) FROM order_notifications WHERE platformName = :platformName")
    suspend fun getAverageAmountByPlatform(platformName: String): Double?
    
    @Query("SELECT AVG(estimatedDistance) FROM order_notifications WHERE platformName = :platformName AND estimatedDistance IS NOT NULL")
    suspend fun getAverageDistanceByPlatform(platformName: String): Double?
}
