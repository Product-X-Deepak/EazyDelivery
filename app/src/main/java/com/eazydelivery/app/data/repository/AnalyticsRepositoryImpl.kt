package com.eazydelivery.app.data.repository

import com.eazydelivery.app.data.local.dao.OrderDao
import com.eazydelivery.app.data.local.entity.OrderEntity
import com.eazydelivery.app.domain.model.DailyStats
import com.eazydelivery.app.domain.model.Order
import com.eazydelivery.app.domain.model.PlatformStat
import com.eazydelivery.app.domain.repository.AnalyticsRepository
import com.eazydelivery.app.domain.repository.BaseRepositoryImpl
import com.eazydelivery.app.util.ErrorHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepositoryImpl @Inject constructor(
    private val orderDao: OrderDao,
    override val errorHandler: ErrorHandler
) : AnalyticsRepository, BaseRepositoryImpl() {

    override suspend fun getTodayStats(): Result<DailyStats> = safeDatabaseOperation("getTodayStats") {
        val calendar = Calendar.getInstance()
        val endDate = calendar.time
        
        // Set start time to beginning of today
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val startDateStr = dateFormat.format(startDate)
        val endDateStr = dateFormat.format(endDate)
        
        // Get orders for today
        val orders = orderDao.getOrdersBetweenDates(startDateStr, endDateStr)
        
        // Calculate total earnings
        val totalEarnings = orders.sumOf { it.amount }
        
        // Group by platform
        val platformBreakdown = orders.groupBy { it.platformName }
            .mapValues { (_, platformOrders) ->
                PlatformStat(
                    platformName = platformOrders.first().platformName,
                    orderCount = platformOrders.size,
                    totalEarnings = platformOrders.sumOf { it.amount }
                )
            }
        
        DailyStats(
            date = startDate,
            totalOrders = orders.size,
            totalEarnings = totalEarnings,
            platformBreakdown = platformBreakdown
        )
    }

    override suspend fun getEarningsForDateRange(startDate: Date, endDate: Date): Result<Double> = 
        safeDatabaseOperation("getEarningsForDateRange") {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val startDateStr = dateFormat.format(startDate)
            val endDateStr = dateFormat.format(endDate)
            
            orderDao.getTotalEarningsBetweenDates(startDateStr, endDateStr)
        }

    override suspend fun getOrderCountForDateRange(startDate: Date, endDate: Date): Result<Int> = 
        safeDatabaseOperation("getOrderCountForDateRange") {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val startDateStr = dateFormat.format(startDate)
            val endDateStr = dateFormat.format(endDate)
            
            orderDao.getOrderCountBetweenDates(startDateStr, endDateStr)
        }

    override suspend fun getPlatformStats(): Result<List<PlatformStat>> = safeDatabaseOperation("getPlatformStats") {
        orderDao.getPlatformStats().map { stat ->
            PlatformStat(
                platformName = stat.platformName,
                orderCount = stat.orderCount,
                totalEarnings = stat.totalEarnings
            )
        }
    }

    override suspend fun getRecentOrders(limit: Int): Result<List<Order>> = safeDatabaseOperation("getRecentOrders") {
        orderDao.getRecentOrders(limit).map { it.toDomainModel() }
    }

    override suspend fun addOrder(order: Order): Result<Unit> = safeDatabaseOperation("addOrder") {
        val orderEntity = OrderEntity(
            id = order.id,
            platformName = order.platformName,
            amount = order.amount,
            timestamp = order.timestamp,
            isAccepted = order.isAccepted,
            deliveryStatus = order.deliveryStatus,
            priority = order.priority,
            estimatedDistance = order.estimatedDistance,
            estimatedTime = order.estimatedTime
        )
        orderDao.insertOrder(orderEntity)
    }

    override suspend fun updateOrderStatus(orderId: String, status: String): Result<Unit> = 
        safeDatabaseOperation("updateOrderStatus") {
            orderDao.updateOrderStatus(orderId, status)
        }

    private fun OrderEntity.toDomainModel(): Order {
        return Order(
            id = id,
            platformName = platformName,
            amount = amount,
            timestamp = timestamp,
            isAccepted = isAccepted,
            deliveryStatus = deliveryStatus,
            priority = priority,
            estimatedDistance = estimatedDistance,
            estimatedTime = estimatedTime
        )
    }
}
