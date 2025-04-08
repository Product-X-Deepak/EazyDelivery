package com.eazydelivery.app.util

import timber.log.Timber
import com.eazydelivery.app.util.Constants.PACKAGE_SWIGGY
import com.eazydelivery.app.util.Constants.PACKAGE_ZOMATO
import com.eazydelivery.app.util.Constants.PACKAGE_ZEPTO
import com.eazydelivery.app.util.Constants.PACKAGE_BLINKIT
import com.eazydelivery.app.util.Constants.PACKAGE_UBER_EATS
import com.eazydelivery.app.util.Constants.PACKAGE_BIGBASKET
import com.eazydelivery.app.util.Constants.PLATFORM_SWIGGY
import com.eazydelivery.app.util.Constants.PLATFORM_INSTAMART
import com.eazydelivery.app.util.Constants.PLATFORM_ZOMATO
import com.eazydelivery.app.util.Constants.PLATFORM_ZEPTO
import com.eazydelivery.app.util.Constants.PLATFORM_BLINKIT
import com.eazydelivery.app.util.Constants.PLATFORM_UBER_EATS
import com.eazydelivery.app.util.Constants.PLATFORM_BIGBASKET
import com.eazydelivery.app.util.Constants.SUPPORTED_DELIVERY_PACKAGES
import com.eazydelivery.app.util.LogUtils
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for parsing notifications from delivery apps
 */
@Singleton
class NotificationParser @Inject constructor(
    private val packageMigrationHelper: PackageMigrationHelper,
    private val errorHandler: ErrorHandler
) {

    /**
     * Checks if the given package name belongs to a supported delivery app
     */
    fun isDeliveryApp(packageName: String): Boolean {
        // First check if this is a package that needs migration
        val migratedPackage = packageMigrationHelper.migratePackageName(packageName)

        // If the package was migrated and is different, check the migrated package
        if (migratedPackage != packageName) {
            return DELIVERY_APPS.contains(migratedPackage)
        }

        return DELIVERY_APPS.contains(packageName)
    }

    /**
     * Parses a notification from a delivery app to extract order details
     */
    fun parseNotification(packageName: String, title: String, text: String): OrderDetails? {
        return try {
            // Check if this is a package that needs migration
            val migratedPackage = packageMigrationHelper.migratePackageName(packageName)

            // Get platform name from package name
            var platformName = getPlatformNameFromPackage(migratedPackage)

            // If platform is not supported, return null
            if (platformName.isEmpty()) {
                Timber.d("Unsupported platform for package: $migratedPackage")
                return null
            }

            // Special handling for Swiggy/Instamart (same package)
            if (platformName == PLATFORM_SWIGGY && migratedPackage == PACKAGE_SWIGGY) {
                // Check if this is an Instamart notification
                if (isInstamartNotification(title, text)) {
                    platformName = PLATFORM_INSTAMART
                    Timber.d("Detected Instamart notification")
                }
            }

            // Check if notification contains order-related keywords
            if (!isOrderNotification(title, text, platformName)) {
                Timber.d("Not an order notification for platform: $platformName")
                return null
            }

            // Extract amount, distance, and time using generic patterns
            val amount = extractAmount(title, text)
            val estimatedDistance = extractDistance(text)
            val estimatedTime = extractTime(text)

            // Create and return order details
            if (amount > 0) {
                OrderDetails(
                    platformName = platformName,
                    amount = amount,
                    estimatedDistance = estimatedDistance,
                    estimatedTime = estimatedTime
                )
            } else {
                Timber.d("Could not extract amount from notification for platform: $platformName")
                null
            }
        } catch (e: Exception) {
            errorHandler.handleException("NotificationParser.parseNotification", e)
            null
        }
    }

    /**
     * Performs the accept action for a specific platform
     */
    fun performAcceptAction(platformName: String) {
        // This would be implemented to interact with the platform's UI
        // For now, we just log the action
        Timber.d("Performing accept action for $platformName")
    }

    /**
     * Gets the platform name from the package name
     * For Swiggy/Instamart (same package), we need to determine which one based on notification content
     */
    private fun getPlatformNameFromPackage(packageName: String): String {
        return when (packageName) {
            PACKAGE_SWIGGY -> PLATFORM_SWIGGY // Default to Swiggy, will check for Instamart in notification content
            PACKAGE_ZOMATO -> PLATFORM_ZOMATO
            PACKAGE_ZEPTO -> PLATFORM_ZEPTO
            PACKAGE_BLINKIT -> PLATFORM_BLINKIT
            PACKAGE_UBER_EATS -> PLATFORM_UBER_EATS
            PACKAGE_BIGBASKET -> PLATFORM_BIGBASKET
            else -> ""
        }
    }

    /**
     * Checks if the notification is related to a new order
     */
    private fun isOrderNotification(title: String, text: String, platformName: String): Boolean {
        // Common order-related keywords
        val orderKeywords = listOf("new order", "order", "delivery", "request")

        // Platform-specific keywords
        val platformKeywords = when (platformName) {
            PLATFORM_SWIGGY -> listOf("swiggy")
            PLATFORM_ZOMATO -> listOf("zomato")
            PLATFORM_INSTAMART -> listOf("instamart", "swiggy")
            PLATFORM_ZEPTO -> listOf("zepto")
            PLATFORM_BLINKIT -> listOf("blinkit")
            PLATFORM_UBER_EATS -> listOf("uber", "eats")
            PLATFORM_BIGBASKET -> listOf("bigbasket", "bb")
            else -> emptyList()
        }

        // Check if any order keyword is present
        val hasOrderKeyword = orderKeywords.any { keyword ->
            title.contains(keyword, ignoreCase = true) || text.contains(keyword, ignoreCase = true)
        }

        // Check if any platform keyword is present
        val hasPlatformKeyword = platformKeywords.any { keyword ->
            title.contains(keyword, ignoreCase = true) || text.contains(keyword, ignoreCase = true)
        }

        // Return true if both order and platform keywords are present
        return hasOrderKeyword && hasPlatformKeyword
    }

    /**
     * Extracts the amount from the notification
     */
    private fun extractAmount(title: String, text: String): Double {
        // Match Indian Rupee format (₹123 or ₹123.45)
        val amountRegex = "₹\\s*(\\d+(?:\\.\\d+)?)".toRegex()
        val matchResult = amountRegex.find(title) ?: amountRegex.find(text)

        return matchResult?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
    }

    /**
     * Extracts the estimated distance from the notification
     */
    private fun extractDistance(text: String): Double? {
        val distanceRegex = "(\\d+(?:\\.\\d+)?)[\\s]*km".toRegex(RegexOption.IGNORE_CASE)
        val distanceMatch = distanceRegex.find(text)

        return distanceMatch?.groupValues?.get(1)?.toDoubleOrNull()
    }

    /**
     * Extracts the estimated time from the notification
     */
    private fun extractTime(text: String): Int? {
        val timeRegex = "(\\d+)[\\s]*min".toRegex(RegexOption.IGNORE_CASE)
        val timeMatch = timeRegex.find(text)

        return timeMatch?.groupValues?.get(1)?.toIntOrNull()
    }

    data class OrderDetails(
        val platformName: String,
        val amount: Double,
        val estimatedDistance: Double? = null,
        val estimatedTime: Int? = null
    )

    /**
     * Checks if a notification is from Instamart rather than Swiggy
     */
    private fun isInstamartNotification(title: String, text: String): Boolean {
        val instamartKeywords = listOf(
            "instamart", "grocery", "groceries", "household", "essentials",
            "instant delivery", "minutes delivery"
        )

        return instamartKeywords.any { keyword ->
            title.contains(keyword, ignoreCase = true) || text.contains(keyword, ignoreCase = true)
        }
    }

    companion object {
        // Using centralized constants from Constants.kt
        private val DELIVERY_APPS = SUPPORTED_DELIVERY_PACKAGES
    }
}
