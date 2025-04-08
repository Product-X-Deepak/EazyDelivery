package com.eazydelivery.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.eazydelivery.app.service.DeliveryAccessibilityService
import com.eazydelivery.app.service.DeliveryNotificationListenerService
import com.eazydelivery.app.util.ErrorHandler
import com.eazydelivery.app.util.SecureStorage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Receiver for device boot events to start services automatically
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var secureStorage: SecureStorage
    
    @Inject
    lateinit var errorHandler: ErrorHandler
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Timber.d("Boot completed, checking if services should be started")
            
            scope.launch {
                try {
                    // Check if auto-start is enabled
                    val autoStartEnabled = secureStorage.getBoolean("auto_start_enabled", true)
                    
                    if (autoStartEnabled) {
                        // Check if service was active before device reboot
                        val serviceActive = secureStorage.getBoolean("service_active", false)
                        
                        if (serviceActive) {
                            Timber.d("Starting services after boot")
                            startServices(context)
                        }
                    }
                } catch (e: Exception) {
                    errorHandler.handleException("BootReceiver.onReceive", e)
                }
            }
        }
    }
    
    private fun startServices(context: Context) {
        // Start notification listener service
        val notificationListenerIntent = Intent(context, DeliveryNotificationListenerService::class.java)
        ContextCompat.startForegroundService(context, notificationListenerIntent)
        
        // Start accessibility service
        val accessibilityServiceIntent = Intent(context, DeliveryAccessibilityService::class.java)
        ContextCompat.startForegroundService(context, accessibilityServiceIntent)
    }
}
