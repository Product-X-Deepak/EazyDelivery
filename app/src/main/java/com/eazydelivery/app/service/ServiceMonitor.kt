package com.eazydelivery.app.service

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.eazydelivery.app.domain.repository.ServiceRepository
import com.eazydelivery.app.util.ErrorHandler
import com.eazydelivery.app.worker.ServiceMonitorWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors and manages the accessibility and notification listener services
 * Ensures they are running when they should be and restarts them if necessary
 */
@Singleton
class ServiceMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serviceRepository: ServiceRepository,
    private val errorHandler: ErrorHandler
) {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    companion object {
        private const val SERVICE_MONITOR_WORK_NAME = "service_monitor_work"
        private const val SERVICE_MONITOR_INTERVAL_MINUTES = 15L
    }
    
    /**
     * Initialize the service monitor
     */
    fun initialize() {
        try {
            Timber.d("Initializing service monitor")
            
            // Schedule periodic work to check service status
            scheduleServiceMonitorWork()
            
            // Perform initial check
            serviceScope.launch {
                checkServices()
            }
        } catch (e: Exception) {
            errorHandler.handleException("ServiceMonitor.initialize", e)
        }
    }
    
    /**
     * Schedule periodic work to check service status
     */
    private fun scheduleServiceMonitorWork() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(false)
                .build()
            
            val workRequest = PeriodicWorkRequestBuilder<ServiceMonitorWorker>(
                SERVICE_MONITOR_INTERVAL_MINUTES, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                SERVICE_MONITOR_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
            
            Timber.d("Scheduled service monitor work")
        } catch (e: Exception) {
            errorHandler.handleException("ServiceMonitor.scheduleServiceMonitorWork", e)
        }
    }
    
    /**
     * Check if services are running and restart them if necessary
     */
    suspend fun checkServices() {
        try {
            // Check if services should be active
            val shouldBeActive = serviceRepository.isServiceActive().getOrDefault(false)
            
            if (shouldBeActive) {
                // Check if accessibility service is enabled
                val accessibilityEnabled = isAccessibilityServiceEnabled()
                
                // Check if notification listener service is enabled
                val notificationListenerEnabled = isNotificationListenerEnabled()
                
                Timber.d("Service check: Accessibility: $accessibilityEnabled, Notification: $notificationListenerEnabled")
                
                // If either service is not enabled, prompt the user to enable them
                if (!accessibilityEnabled || !notificationListenerEnabled) {
                    Timber.w("Services not enabled: Accessibility: $accessibilityEnabled, Notification: $notificationListenerEnabled")
                    
                    // Update service repository with current status
                    serviceRepository.updateServiceStatus(
                        accessibilityEnabled = accessibilityEnabled,
                        notificationListenerEnabled = notificationListenerEnabled
                    )
                }
            }
        } catch (e: Exception) {
            errorHandler.handleException("ServiceMonitor.checkServices", e)
        }
    }
    
    /**
     * Check if the accessibility service is enabled
     */
    fun isAccessibilityServiceEnabled(): Boolean {
        try {
            val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            
            val componentName = ComponentName(context, DeliveryAccessibilityService::class.java)
            
            return enabledServices.any { 
                it.resolveInfo.serviceInfo.packageName == componentName.packageName && 
                it.resolveInfo.serviceInfo.name == componentName.className 
            }
        } catch (e: Exception) {
            errorHandler.handleException("ServiceMonitor.isAccessibilityServiceEnabled", e)
            return false
        }
    }
    
    /**
     * Check if the notification listener service is enabled
     */
    fun isNotificationListenerEnabled(): Boolean {
        try {
            val componentName = ComponentName(context, DeliveryNotificationListenerService::class.java)
            val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
            
            return enabledListeners.contains(componentName.packageName)
        } catch (e: Exception) {
            errorHandler.handleException("ServiceMonitor.isNotificationListenerEnabled", e)
            return false
        }
    }
    
    /**
     * Open accessibility settings
     */
    fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            errorHandler.handleException("ServiceMonitor.openAccessibilitySettings", e)
        }
    }
    
    /**
     * Open notification listener settings
     */
    fun openNotificationListenerSettings() {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            } else {
                Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            }
            
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            errorHandler.handleException("ServiceMonitor.openNotificationListenerSettings", e)
        }
    }
    
    /**
     * Open battery optimization settings
     */
    fun openBatteryOptimizationSettings() {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            errorHandler.handleException("ServiceMonitor.openBatteryOptimizationSettings", e)
        }
    }
}
