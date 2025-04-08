package com.eazydelivery.app.domain.repository

interface ServiceRepository {
    suspend fun isServiceActive(): Result<Boolean>
    suspend fun setServiceActive(active: Boolean): Result<Unit>
    suspend fun isNotificationListenerEnabled(): Result<Boolean>
    suspend fun isAccessibilityServiceEnabled(): Result<Boolean>
    suspend fun getBatteryOptimizationStatus(): Result<Boolean>
    suspend fun getServiceStatistics(): Result<ServiceStatistics>
    suspend fun updateServiceStatus(accessibilityEnabled: Boolean, notificationListenerEnabled: Boolean): Result<Unit>
}

data class ServiceStatistics(
    val totalOrdersProcessed: Int,
    val totalOrdersAccepted: Int,
    val totalOrdersRejected: Int,
    val uptime: Long // in milliseconds
)
