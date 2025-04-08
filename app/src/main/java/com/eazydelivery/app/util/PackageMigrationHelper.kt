package com.eazydelivery.app.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import com.eazydelivery.app.data.model.Platform
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class to assist with migration from old package structure
 * This helps maintain compatibility with both old and new package structures
 */
@Singleton
class PackageMigrationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val errorHandler: ErrorHandler
) {
    companion object {
        // Old package prefix
        private const val OLD_PACKAGE_PREFIX = "com.application.eazydelivery"

        // New package prefix
        private const val NEW_PACKAGE_PREFIX = "com.eazydelivery.app"

        // Delivery app package mappings
        private val deliveryAppPackageMappings = mapOf(
            "com.dunzo.delivery" to "", // Removed as per requirements
            "com.ubercab.eats" to "com.ubercab.driver", // Changed Uber Eats package as per requirements
            "in.swiggy.deliveryapp" to "in.swiggy.deliveryapp", // Swiggy and Instamart share the same package
            "com.zomato.delivery" to "com.zomato.delivery",
            "com.zepto.rider" to "com.zepto.rider",
            "app.blinkit.onboarding" to "app.blinkit.onboarding",
            "com.bigbasket.delivery" to "com.bigbasket.delivery"
        )

        // Map of old to new activity names
        private val activityMappings = mapOf(
            "$OLD_PACKAGE_PREFIX.MainActivity" to "$NEW_PACKAGE_PREFIX.MainActivity",
            "$OLD_PACKAGE_PREFIX.ui.onboarding.OnboardingActivity" to "$NEW_PACKAGE_PREFIX.ui.onboarding.OnboardingActivity",
            "$OLD_PACKAGE_PREFIX.ui.settings.SettingsActivity" to "$NEW_PACKAGE_PREFIX.ui.settings.SettingsActivity",
            "$OLD_PACKAGE_PREFIX.ui.auth.BiometricAuthActivity" to "$NEW_PACKAGE_PREFIX.ui.auth.BiometricAuthActivity",
            "$OLD_PACKAGE_PREFIX.ui.auth.PhoneLoginActivity" to "$NEW_PACKAGE_PREFIX.ui.auth.PhoneLoginActivity"
        )

        // Map of old to new service names
        private val serviceMappings = mapOf(
            "$OLD_PACKAGE_PREFIX.service.DeliveryNotificationListenerService" to "$NEW_PACKAGE_PREFIX.service.DeliveryNotificationListenerService",
            "$OLD_PACKAGE_PREFIX.service.DeliveryAccessibilityService" to "$NEW_PACKAGE_PREFIX.service.DeliveryAccessibilityService",
            "$OLD_PACKAGE_PREFIX.service.AutoAcceptService" to "$NEW_PACKAGE_PREFIX.service.AutoAcceptService"
        )

        // Map of old to new receiver names
        private val receiverMappings = mapOf(
            "$OLD_PACKAGE_PREFIX.receiver.BootReceiver" to "$NEW_PACKAGE_PREFIX.receiver.BootReceiver",
            "$OLD_PACKAGE_PREFIX.receiver.NotificationActionReceiver" to "$NEW_PACKAGE_PREFIX.receiver.NotificationActionReceiver"
        )
    }

    /**
     * Converts an intent from old package structure to new package structure
     * @param intent The intent to convert
     * @return The converted intent
     */
    fun migrateIntent(intent: Intent): Intent {
        try {
            // Get the component name
            val componentName = intent.component ?: return intent

            // Get the class name
            val className = componentName.className

            // Check if this is an old package class
            if (className.startsWith(OLD_PACKAGE_PREFIX)) {
                // Try to find the new class name
                val newClassName = activityMappings[className]
                    ?: serviceMappings[className]
                    ?: receiverMappings[className]

                if (newClassName != null) {
                    // Create a new intent with the new class name
                    val newIntent = Intent(intent)
                    newIntent.setClassName(context, newClassName)
                    Timber.d("Migrated intent from $className to $newClassName")
                    return newIntent
                } else {
                    // If we don't have a specific mapping, try a generic replacement
                    val genericNewClassName = className.replace(OLD_PACKAGE_PREFIX, NEW_PACKAGE_PREFIX)
                    if (classExists(genericNewClassName)) {
                        val newIntent = Intent(intent)
                        newIntent.setClassName(context, genericNewClassName)
                        Timber.d("Migrated intent using generic replacement from $className to $genericNewClassName")
                        return newIntent
                    }
                }
            }

            // Also check if the package name in the intent data needs migration
            intent.data?.let { uri ->
                val scheme = uri.scheme
                val host = uri.host

                if (scheme == "package" && host != null && host.startsWith(OLD_PACKAGE_PREFIX)) {
                    val newHost = host.replace(OLD_PACKAGE_PREFIX, NEW_PACKAGE_PREFIX)
                    val newUri = uri.buildUpon().authority(newHost).build()
                    return Intent(intent).setData(newUri)
                }
            }
        } catch (e: Exception) {
            errorHandler.handleException("PackageMigrationHelper.migrateIntent", e)
        }

        return intent
    }

    /**
     * Checks if a class exists in the app
     * @param className The class name to check
     * @return true if the class exists, false otherwise
     */
    fun classExists(className: String): Boolean {
        return try {
            Class.forName(className)
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    /**
     * Migrates platform data from old package names to new package names
     * @param platform The platform to migrate
     * @return The migrated platform
     */
    fun migratePlatform(platform: Platform): Platform {
        try {
            // Check if the package name is using the old structure
            if (platform.packageName.startsWith(OLD_PACKAGE_PREFIX)) {
                // Create a new platform with the new package name
                return platform.copy(
                    packageName = platform.packageName.replace(OLD_PACKAGE_PREFIX, NEW_PACKAGE_PREFIX)
                )
            }

            // Check if this is a delivery app that needs package migration
            deliveryAppPackageMappings[platform.packageName]?.let { newPackage ->
                // If the new package is empty, this platform should be removed
                if (newPackage.isEmpty()) {
                    Timber.d("Platform ${platform.name} uses removed package ${platform.packageName}")
                    // Return the original platform but mark it for removal
                    return platform.copy(isEnabled = false, shouldRemove = true)
                }

                // Otherwise, update to the new package
                return platform.copy(packageName = newPackage)
            }
        } catch (e: Exception) {
            errorHandler.handleException("PackageMigrationHelper.migratePlatform", e)
        }

        return platform
    }

    /**
     * Checks if an app is installed by package name
     * @param packageName The package name to check
     * @return true if the app is installed, false otherwise
     */
    fun isAppInstalled(packageName: String): Boolean {
        return try {
            // Check if this is a removed package
            if (deliveryAppPackageMappings[packageName] == "") {
                return false
            }

            // Check if the app is installed
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        } catch (e: Exception) {
            errorHandler.handleException("PackageMigrationHelper.isAppInstalled", e)
            false
        }
    }

    /**
     * Migrates a package name from old to new format
     * @param packageName The package name to migrate
     * @return The migrated package name, or the original if no migration is needed
     */
    fun migratePackageName(packageName: String): String {
        // Check if this is using the old package structure
        if (packageName.startsWith(OLD_PACKAGE_PREFIX)) {
            return packageName.replace(OLD_PACKAGE_PREFIX, NEW_PACKAGE_PREFIX)
        }

        // Check if this is a delivery app that needs package migration
        deliveryAppPackageMappings[packageName]?.let { newPackage ->
            if (newPackage.isNotEmpty()) {
                return newPackage
            }
        }

        return packageName
    }
}
