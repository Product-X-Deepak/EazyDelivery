package com.eazydelivery.app.service

import android.content.Context
import android.content.Intent
import com.eazydelivery.app.util.ErrorHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class to migrate from SimpleNotificationListenerService to DeliveryNotificationListenerService
 * This class handles the transition between the old and new service implementations
 */
@Singleton
class SimpleNotificationListenerServiceMigration @Inject constructor(
    @ApplicationContext private val context: Context,
    private val errorHandler: ErrorHandler
) {
    /**
     * Migrate any existing SimpleNotificationListenerService settings to DeliveryNotificationListenerService
     * This should be called during app startup to ensure a smooth transition
     */
    fun migrateServiceSettings() {
        try {
            Timber.d("Migrating notification listener service settings")
            
            // Stop the old service if it's running
            stopOldService()
            
            // Start the new service
            startNewService()
            
            Timber.d("Notification listener service migration completed")
        } catch (e: Exception) {
            errorHandler.handleException("SimpleNotificationListenerServiceMigration.migrateServiceSettings", e)
        }
    }
    
    /**
     * Stop the old SimpleNotificationListenerService if it's running
     */
    private fun stopOldService() {
        try {
            // Create an intent to stop the old service
            val intent = Intent(context, SimpleNotificationListenerService::class.java)
            context.stopService(intent)
            
            Timber.d("Old notification listener service stopped")
        } catch (e: Exception) {
            // Just log the error, don't throw
            Timber.e(e, "Error stopping old notification listener service")
        }
    }
    
    /**
     * Start the new DeliveryNotificationListenerService
     */
    private fun startNewService() {
        try {
            // Create an intent to start the new service
            val intent = Intent(context, DeliveryNotificationListenerService::class.java)
            
            // Start the service
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            
            Timber.d("New notification listener service started")
        } catch (e: Exception) {
            errorHandler.handleException("SimpleNotificationListenerServiceMigration.startNewService", e)
        }
    }
}
