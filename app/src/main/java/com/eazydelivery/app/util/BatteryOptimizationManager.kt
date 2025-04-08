package com.eazydelivery.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class to manage battery optimization settings
 */
@Singleton
class BatteryOptimizationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    /**
     * Checks if the app is ignoring battery optimizations
     */
    fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }
    
    /**
     * Opens the battery optimization settings for the app
     */
    fun openBatteryOptimizationSettings() {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            } else {
                Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "Failed to open battery optimization settings")
        }
    }
    
    /**
     * Opens the device-specific battery optimization settings
     */
    fun openDeviceSpecificBatterySettings() {
        try {
            val manufacturer = Build.MANUFACTURER.lowercase()
            val intent = when {
                manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> {
                    Intent().apply {
                        component = android.content.ComponentName(
                            "com.miui.powerkeeper",
                            "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
                        )
                    }
                }
                manufacturer.contains("oppo") -> {
                    Intent().apply {
                        component = android.content.ComponentName(
                            "com.coloros.oppoguardelf",
                            "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity"
                        )
                    }
                }
                manufacturer.contains("vivo") -> {
                    Intent().apply {
                        component = android.content.ComponentName(
                            "com.vivo.abe",
                            "com.vivo.applicationbehaviorengine.ui.ExcessivePowerManagerActivity"
                        )
                    }
                }
                manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                    Intent().apply {
                        component = android.content.ComponentName(
                            "com.huawei.systemmanager",
                            "com.huawei.systemmanager.optimize.process.ProtectActivity"
                        )
                    }
                }
                manufacturer.contains("samsung") -> {
                    Intent().apply {
                        component = android.content.ComponentName(
                            "com.samsung.android.lool",
                            "com.samsung.android.sm.ui.battery.BatteryActivity"
                        )
                    }
                }
                else -> null
            }
            
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } else {
                // Fallback to standard battery settings
                openBatteryOptimizationSettings()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to open device-specific battery settings")
            // Fallback to standard battery settings
            openBatteryOptimizationSettings()
        }
    }
}

