package com.eazydelivery.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.eazydelivery.app.data.local.entity.OrderEntity
import com.eazydelivery.app.data.local.entity.PlatformStatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentOrdersFlow(limit: Int): Flow<List<OrderEntity>>
    
    @Query("SELECT * FROM orders ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentOrders(limit: Int): List<OrderEntity>
    
    @Query("SELECT * FROM orders WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    suspend fun getOrdersBetweenDates(startDate: String, endDate: String): List<OrderEntity>
    
    @Query("SELECT COUNT(*) FROM orders WHERE timestamp BETWEEN :startDate AND :endDate")
    suspend fun getOrderCountBetweenDates(startDate: String, endDate: String): Int
    
    @Query("SELECT SUM(amount) FROM orders WHERE timestamp BETWEEN :startDate AND :endDate")
    suspend fun getTotalEarningsBetweenDates(startDate: String, endDate: String): Double
    
    @Query("SELECT platformName, COUNT(*) as orderCount, SUM(amount) as totalEarnings FROM orders GROUP BY platformName")
    suspend fun getPlatformStats(): List<PlatformStatEntity>
    
    @Insert(onConflictStrategy = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)
    
    @Insert(onConflictStrategy = OnConflictStrategy.REPLACE)
    suspend fun insertAllOrders(orders: List<OrderEntity>)
    
    @Query("DELETE FROM orders WHERE id = :orderId")
    suspend fun deleteOrder(orderId: String)
    
    @Query("UPDATE orders SET deliveryStatus = :status WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: String, status: String)
    
    @Transaction
    suspend fun deleteAndInsertOrders(orders: List<OrderEntity>) {
        deleteAllOrders()
        insertAllOrders(orders)
    }
    
    @Query("DELETE FROM orders")
    suspend fun deleteAllOrders()
    
    @Query("SELECT * FROM orders WHERE id = :orderId")
    suspend fun getOrderById(orderId: String): OrderEntity?
    
    @Query("SELECT * FROM orders WHERE deliveryStatus = :status ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getOrdersByStatus(status: String, limit: Int): List<OrderEntity>
    
    @Query("SELECT * FROM orders WHERE platformName = :platformName ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getOrdersByPlatform(platformName: String, limit: Int): List<OrderEntity>
}
