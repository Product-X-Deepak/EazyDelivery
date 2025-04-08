package com.eazydelivery.app.domain.repository

import com.eazydelivery.app.domain.model.DailyStats
import com.eazydelivery.app.domain.model.Order
import com.eazydelivery.app.domain.model.PlatformStat
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface AnalyticsRepository {
    suspend fun getTodayStats(): Result<DailyStats>
    suspend fun getDailyEarnings(): Result<Double>
    suspend fun getWeeklyEarnings(): Result<Double>
    suspend fun getMonthlyEarnings(): Result<Double>
    suspend fun getPlatformStats(): Result<List<PlatformStat>>
    fun getRecentOrdersFlow(limit: Int): Flow<Result<List<Order>>>
    suspend fun getRecentOrders(limit: Int): Result<List<Order>>
    suspend fun addOrder(order: Order): Result<Unit>
    suspend fun getOrderById(orderId: String): Result<Order?>
    suspend fun updateOrderStatus(orderId: String, isAccepted: Boolean): Result<Unit>
    suspend fun deleteOrder(orderId: String): Result<Unit>
    suspend fun getEarningsForDateRange(startDate: Date, endDate: Date): Result<Double>
    suspend fun getOrderCountForDateRange(startDate: Date, endDate: Date): Result<Int>
}
