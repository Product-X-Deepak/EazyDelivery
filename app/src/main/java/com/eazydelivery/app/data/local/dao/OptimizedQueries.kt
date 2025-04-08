package com.eazydelivery.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.eazydelivery.app.data.local.entity.OrderEntity
import com.eazydelivery.app.data.local.entity.OrderNotificationEntity
import com.eazydelivery.app.data.local.entity.PlatformStatEntity
import kotlinx.coroutines.flow.Flow

/**
 * Optimized queries for better performance
 * Uses indexes, raw queries, and other optimization techniques
 */
@Dao
interface OptimizedQueries {
    /**
     * Executes a raw query for maximum flexibility and performance
     * Use with caution as it bypasses Room's SQL validation
     */
    @RawQuery
    suspend fun executeRawQuery(query: SupportSQLiteQuery): List<Any>
    
    /**
     * Gets platform statistics with a single optimized query
     * This is more efficient than multiple separate queries
     */
    @Query("""
        SELECT 
            platformName, 
            COUNT(*) as orderCount, 
            SUM(amount) as totalEarnings,
            AVG(amount) as averageEarning,
            MAX(amount) as highestEarning,
            MIN(amount) as lowestEarning
        FROM orders 
        WHERE timestamp BETWEEN :startDate AND :endDate
        GROUP BY platformName
    """)
    suspend fun getPlatformStatsForPeriod(startDate: String, endDate: String): List<PlatformStatEntity>
    
    /**
     * Gets daily statistics with a single optimized query
     * Uses SQLite date functions for efficient date-based grouping
     */
    @Query("""
        SELECT 
            substr(timestamp, 1, 10) as date,
            COUNT(*) as orderCount, 
            SUM(amount) as totalEarnings
        FROM orders 
        WHERE timestamp BETWEEN :startDate AND :endDate
        GROUP BY substr(timestamp, 1, 10)
        ORDER BY date DESC
    """)
    suspend fun getDailyStatsForPeriod(startDate: String, endDate: String): List<DailyStats>
    
    /**
     * Gets recent orders with a single optimized query
     * Uses LIMIT for better performance
     */
    @Query("""
        SELECT * FROM orders 
        WHERE timestamp BETWEEN :startDate AND :endDate
        ORDER BY timestamp DESC 
        LIMIT :limit
    """)
    suspend fun getRecentOrdersOptimized(startDate: String, endDate: String, limit: Int): List<OrderEntity>
    
    /**
     * Gets notification statistics with a single optimized query
     * This is more efficient than multiple separate queries
     */
    @Query("""
        SELECT 
            platformName, 
            COUNT(*) as notificationCount, 
            AVG(amount) as averageAmount,
            AVG(estimatedDistance) as averageDistance,
            COUNT(CASE WHEN priority = 'HIGH' THEN 1 ELSE NULL END) as highPriorityCount,
            COUNT(CASE WHEN priority = 'MEDIUM' THEN 1 ELSE NULL END) as mediumPriorityCount,
            COUNT(CASE WHEN priority = 'LOW' THEN 1 ELSE NULL END) as lowPriorityCount
        FROM order_notifications 
        WHERE timestamp BETWEEN :startTime AND :endTime
        GROUP BY platformName
    """)
    suspend fun getNotificationStatsForPeriod(startTime: Long, endTime: Long): List<NotificationStats>
    
    /**
     * Gets recent notifications with optimized query
     * Uses LIMIT for better performance
     */
    @Query("""
        SELECT * FROM order_notifications 
        WHERE timestamp > :cutoffTime
        ORDER BY timestamp DESC 
        LIMIT :limit
    """)
    fun getRecentNotificationsOptimized(cutoffTime: Long, limit: Int): Flow<List<OrderNotificationEntity>>
    
    /**
     * Cleans up old data with an optimized query
     * Uses a single query for better performance
     */
    @Query("""
        DELETE FROM order_notifications 
        WHERE timestamp < :cutoffTime AND 
        id NOT IN (
            SELECT id FROM order_notifications 
            ORDER BY timestamp DESC 
            LIMIT :keepCount
        )
    """)
    suspend fun cleanupOldNotifications(cutoffTime: Long, keepCount: Int): Int
    
    /**
     * Data class for daily statistics
     */
    data class DailyStats(
        val date: String,
        val orderCount: Int,
        val totalEarnings: Double
    )
    
    /**
     * Data class for notification statistics
     */
    data class NotificationStats(
        val platformName: String,
        val notificationCount: Int,
        val averageAmount: Double?,
        val averageDistance: Double?,
        val highPriorityCount: Int,
        val mediumPriorityCount: Int,
        val lowPriorityCount: Int
    )
}
