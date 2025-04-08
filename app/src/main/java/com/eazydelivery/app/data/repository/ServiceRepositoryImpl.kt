package com.eazydelivery.app.data.repository

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.eazydelivery.app.domain.repository.BaseRepository
import com.eazydelivery.app.domain.repository.ServiceRepository
import com.eazydelivery.app.domain.repository.ServiceStatistics
import com.eazydelivery.app.service.DeliveryAccessibilityService
import com.eazydelivery.app.service.DeliveryNotificationListenerService
import com.eazydelivery.app.util.ErrorHandler
import com.eazydelivery.app.util.SecureStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureStorage: SecureStorage,
    override val errorHandler: ErrorHandler
) : ServiceRepository, BaseRepository {

    companion object {
        private const val KEY_SERVICE_ACTIVE = "service_active"
        private const val KEY_SERVICE_START_TIME = "service_start_time"
        private const val KEY_ORDERS_PROCESSED = "orders_processed"
        private const val KEY_ORDERS_ACCEPTED = "orders_accepted"
        private const val KEY_ORDERS_REJECTED = "orders_rejected"
        private const val KEY_ACCESSIBILITY_ENABLED = "accessibility_enabled"
        private const val KEY_NOTIFICATION_LISTENER_ENABLED = "notification_listener_enabled"
        private const val KEY_LAST_SERVICE_CHECK = "last_service_check"
    }

    override suspend fun isServiceActive(): Result<Boolean> = safeDbCall("ServiceRepository.isServiceActive") {
        secureStorage.getBoolean(KEY_SERVICE_ACTIVE, false)
    }

    override suspend fun setServiceActive(active: Boolean): Result<Unit> = safeDbCall("ServiceRepository.setServiceActive") {
        secureStorage.saveBoolean(KEY_SERVICE_ACTIVE, active)

        if (active) {
            // Record service start time if not already set
            if (secureStorage.getLong(KEY_SERVICE_START_TIME) == 0L) {
                secureStorage.saveLong(KEY_SERVICE_START_TIME, System.currentTimeMillis())
            }

            // Start services if needed
            startServices()
        } else {
            // Reset service start time
            secureStorage.saveLong(KEY_SERVICE_START_TIME, 0L)

            // Stop services if needed
            stopServices()
        }
    }

    override suspend fun isNotificationListenerEnabled(): Result<Boolean> = safeDbCall("ServiceRepository.isNotificationListenerEnabled") {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )

        enabledListeners?.contains(context.packageName + "/" + DeliveryNotificationListenerService::class.java.name) ?: false
    }

    override suspend fun isAccessibilityServiceEnabled(): Result<Boolean> = safeDbCall("ServiceRepository.isAccessibilityServiceEnabled") {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )

        enabledServices?.contains(context.packageName + "/" + DeliveryAccessibilityService::class.java.name) ?: false
    }

    override suspend fun getBatteryOptimizationStatus(): Result<Boolean> = safeDbCall("ServiceRepository.getBatteryOptimizationStatus") {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    override suspend fun getServiceStatistics(): Result<ServiceStatistics> = safeDbCall("ServiceRepository.getServiceStatistics") {
        val startTime = secureStorage.getLong(KEY_SERVICE_START_TIME, 0L)
        val uptime = if (startTime > 0) System.currentTimeMillis() - startTime else 0L

        ServiceStatistics(
            totalOrdersProcessed = secureStorage.getInt(KEY_ORDERS_PROCESSED, 0),
            totalOrdersAccepted = secureStorage.getInt(KEY_ORDERS_ACCEPTED, 0),
            totalOrdersRejected = secureStorage.getInt(KEY_ORDERS_REJECTED, 0),
            uptime = uptime
        )
    }

    private fun startServices() {
        // Start notification listener service if enabled
        if (isNotificationListenerEnabled().getOrNull() == true) {
            val notificationListenerIntent = Intent(context, DeliveryNotificationListenerService::class.java)
            ContextCompat.startForegroundService(context, notificationListenerIntent)
        }

        // Start accessibility service if enabled
        if (isAccessibilityServiceEnabled().getOrNull() == true) {
            val accessibilityServiceIntent = Intent(context, DeliveryAccessibilityService::class.java)
            ContextCompat.startForegroundService(context, accessibilityServiceIntent)
        }
    }

    private fun stopServices() {
        // Stop notification listener service
        context.stopService(Intent(context, DeliveryNotificationListenerService::class.java))

        // Stop accessibility service
        context.stopService(Intent(context, DeliveryAccessibilityService::class.java))
    }

    override suspend fun updateServiceStatus(accessibilityEnabled: Boolean, notificationListenerEnabled: Boolean): Result<Unit> =
        safeDbCall("ServiceRepository.updateServiceStatus") {
            // Save the current status
            secureStorage.saveBoolean(KEY_ACCESSIBILITY_ENABLED, accessibilityEnabled)
            secureStorage.saveBoolean(KEY_NOTIFICATION_LISTENER_ENABLED, notificationListenerEnabled)
            secureStorage.saveLong(KEY_LAST_SERVICE_CHECK, System.currentTimeMillis())

            // If both services are enabled, make sure they're running
            if (accessibilityEnabled && notificationListenerEnabled) {
                startServices()
            }
        }
}
