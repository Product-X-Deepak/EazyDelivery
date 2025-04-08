package com.eazydelivery.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eazydelivery.app.data.local.dao.OptimizedQueries
import com.eazydelivery.app.data.local.entity.OrderEntity
import com.eazydelivery.app.data.local.entity.OrderNotificationEntity
import com.eazydelivery.app.data.local.entity.PlatformStatEntity
import com.eazydelivery.app.util.PerformanceMonitor
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.UUID
import kotlin.system.measureTimeMillis
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class OptimizedQueriesTest {
    
    private lateinit var db: AppDatabase
    private lateinit var optimizedQueries: OptimizedQueries
    private lateinit var performanceMonitor: PerformanceMonitor
    
    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        optimizedQueries = db.optimizedQueries()
        performanceMonitor = PerformanceMonitor(context, ErrorHandler())
        
        // Populate the database with test data
        populateTestData()
    }
    
    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }
    
    @Test
    fun testPlatformStatsForPeriod() = runBlocking {
        // Given
        val startDate = "2023-01-01"
        val endDate = "2023-12-31"
        
        // When
        val stats = optimizedQueries.getPlatformStatsForPeriod(startDate, endDate)
        
        // Then
        assertTrue(stats.isNotEmpty())
        stats.forEach { stat ->
            assertTrue(stat.orderCount > 0)
            assertTrue(stat.totalEarnings > 0)
        }
    }
    
    @Test
    fun testDailyStatsForPeriod() = runBlocking {
        // Given
        val startDate = "2023-01-01"
        val endDate = "2023-12-31"
        
        // When
        val stats = optimizedQueries.getDailyStatsForPeriod(startDate, endDate)
        
        // Then
        assertTrue(stats.isNotEmpty())
        stats.forEach { stat ->
            assertTrue(stat.orderCount > 0)
            assertTrue(stat.totalEarnings > 0)
        }
    }
    
    @Test
    fun testRecentOrdersOptimized() = runBlocking {
        // Given
        val startDate = "2023-01-01"
        val endDate = "2023-12-31"
        val limit = 10
        
        // When
        val orders = optimizedQueries.getRecentOrdersOptimized(startDate, endDate, limit)
        
        // Then
        assertTrue(orders.isNotEmpty())
        assertTrue(orders.size <= limit)
    }
    
    @Test
    fun testNotificationStatsForPeriod() = runBlocking {
        // Given
        val startTime = 0L
        val endTime = System.currentTimeMillis()
        
        // When
        val stats = optimizedQueries.getNotificationStatsForPeriod(startTime, endTime)
        
        // Then
        assertTrue(stats.isNotEmpty())
        stats.forEach { stat ->
            assertTrue(stat.notificationCount > 0)
        }
    }
    
    @Test
    fun testCleanupOldNotifications() = runBlocking {
        // Given
        val cutoffTime = System.currentTimeMillis() - 86400000 // 1 day ago
        val keepCount = 5
        
        // When
        val deletedCount = optimizedQueries.cleanupOldNotifications(cutoffTime, keepCount)
        
        // Then
        assertTrue(deletedCount >= 0)
    }
    
    @Test
    fun testRawQuery() = runBlocking {
        // Given
        val query = SimpleSQLiteQuery("SELECT COUNT(*) FROM orders")
        
        // When
        val result = optimizedQueries.executeRawQuery(query)
        
        // Then
        assertTrue(result.isNotEmpty())
    }
    
    @Test
    fun testPerformanceComparison() = runBlocking {
        // Given
        val startDate = "2023-01-01"
        val endDate = "2023-12-31"
        
        // When - Measure time for optimized query
        val optimizedTime = measureTimeMillis {
            optimizedQueries.getPlatformStatsForPeriod(startDate, endDate)
        }
        
        // When - Measure time for equivalent non-optimized query
        val nonOptimizedTime = measureTimeMillis {
            val rawQuery = SimpleSQLiteQuery("""
                SELECT 
                    platformName, 
                    COUNT(*) as orderCount, 
                    SUM(amount) as totalEarnings,
                    AVG(amount) as averageEarning,
                    MAX(amount) as highestEarning,
                    MIN(amount) as lowestEarning
                FROM orders 
                WHERE timestamp BETWEEN '$startDate' AND '$endDate'
                GROUP BY platformName
            """.trimIndent())
            optimizedQueries.executeRawQuery(rawQuery)
        }
        
        // Then
        println("Optimized query time: $optimizedTime ms")
        println("Non-optimized query time: $nonOptimizedTime ms")
        
        // The optimized query should be at least as fast as the non-optimized query
        assertTrue(optimizedTime <= nonOptimizedTime * 1.1) // Allow 10% margin
    }
    
    private fun populateTestData() = runBlocking {
        // Insert test orders
        val platforms = listOf("in.swiggy.deliveryapp", "com.zomato.delivery", "com.ubercab.driver", "com.bigbasket.delivery")
        val statuses = listOf("COMPLETED", "CANCELLED", "IN_PROGRESS")
        
        val orders = mutableListOf<OrderEntity>()
        repeat(100) { i ->
            val platform = platforms.random()
            val status = statuses.random()
            val timestamp = "2023-${(1..12).random().toString().padStart(2, '0')}-${(1..28).random().toString().padStart(2, '0')}"
            val amount = (100..1000).random().toDouble()
            
            orders.add(
                OrderEntity(
                    id = i.toString(),
                    orderId = "ORD-$i",
                    platformName = platform,
                    timestamp = timestamp,
                    amount = amount,
                    deliveryStatus = status,
                    customerName = "Customer $i",
                    deliveryAddress = "Address $i",
                    items = "Item $i",
                    notes = "Note $i"
                )
            )
        }
        
        db.orderDao().insertAll(orders)
        
        // Insert test notifications
        val priorities = listOf("HIGH", "MEDIUM", "LOW")
        val notifications = mutableListOf<OrderNotificationEntity>()
        
        repeat(100) { i ->
            val platform = platforms.random()
            val priority = priorities.random()
            val timestamp = System.currentTimeMillis() - (i * 3600000) // Spread over time
            val amount = (100..1000).random().toDouble()
            
            notifications.add(
                OrderNotificationEntity(
                    id = UUID.randomUUID().toString(),
                    platformName = platform,
                    timestamp = timestamp,
                    amount = amount,
                    estimatedDistance = (1..10).random().toDouble(),
                    priority = priority,
                    isAccepted = i % 2 == 0,
                    notificationText = "Notification $i"
                )
            )
        }
        
        db.orderNotificationDao().insertAll(notifications)
    }
}
