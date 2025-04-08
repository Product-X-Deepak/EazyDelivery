package com.eazydelivery.app.data.repository

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.eazydelivery.app.domain.repository.BaseRepository
import com.eazydelivery.app.domain.repository.PermissionRepository
import com.eazydelivery.app.service.DeliveryAccessibilityService
import com.eazydelivery.app.util.ErrorHandler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionRepositoryImpl @Inject constructor(
    override val errorHandler: ErrorHandler
) : PermissionRepository, BaseRepository {
    
    override suspend fun requestNotificationPermission(context: Context): Result<Boolean> = safeApiCall {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                // In a real app, we would use ActivityResultLauncher to request permission
                // For simplicity, we'll just open the app settings
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", context.packageName, null)
                intent.data = uri
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                
                Timber.d("Redirecting to app settings for notification permission")
                return@safeApiCall false
            }
        }
        
        // For notification listener service
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        
        Timber.d("Redirecting to notification listener settings")
        true
    }
    
    override suspend fun requestAccessibilityPermission(context: Context): Result<Boolean> = safeApiCall {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        
        Timber.d("Redirecting to accessibility settings")
        true
    }
    
    override suspend fun requestBatteryOptimizationPermission(context: Context): Result<Boolean> = safeApiCall {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:${context.packageName}")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                
                Timber.d("Redirecting to battery optimization settings")
                return@safeApiCall false
            }
        }
        
        true
    }
    
    override suspend fun checkNotificationPermission(context: Context): Result<Boolean> = safeApiCall {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            return@safeApiCall ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
        
        // For older Android versions, check if notification listener service is enabled
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        
        enabledListeners?.contains(context.packageName) ?: false
    }
    
    override suspend fun checkAccessibilityPermission(context: Context): Result<Boolean> = safeApiCall {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        
        enabledServices?.contains(context.packageName + "/" + DeliveryAccessibilityService::class.java.name) ?: false
    }
    
    override suspend fun checkBatteryOptimizationPermission(context: Context): Result<Boolean> = safeApiCall {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return@safeApiCall powerManager.isIgnoringBatteryOptimizations(context.packageName)
        }
        
        // For older Android versions, battery optimization is not available
        true
    }
}
