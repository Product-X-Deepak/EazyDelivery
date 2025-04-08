package com.eazydelivery.app.util

/**
 * Constants used throughout the application
 */
object Constants {
    // Admin credentials are now managed by AdminContactSettings class
    // This provides better security and flexibility

    // Notification channels
    const val CHANNEL_ID_FOREGROUND = "eazydelivery_service_channel"
    const val CHANNEL_ID_NOTIFICATIONS = "eazydelivery_notifications_channel"

    // Shared preferences keys
    const val PREF_KEY_FIRST_LAUNCH = "first_launch"
    const val PREF_KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    const val PREF_KEY_THEME_MODE = "theme_mode"
    const val PREF_KEY_LANGUAGE = "language"

    // API endpoints
    const val ENDPOINT_LOGIN = "auth/login"
    const val ENDPOINT_REGISTER = "auth/register"
    const val ENDPOINT_PROFILE = "user/profile"
    const val ENDPOINT_ORDERS = "orders"

    // Error messages
    const val ERROR_NETWORK_UNAVAILABLE = "Network unavailable. Please check your internet connection."
    const val ERROR_SERVER_UNREACHABLE = "Server unreachable. Please try again later."
    const val ERROR_REQUEST_TIMEOUT = "Request timed out. Please try again."
    const val ERROR_UNKNOWN = "An unknown error occurred. Please try again."

    // Feature flags
    const val FEATURE_MULTI_PLATFORM_SUPPORT = true
    const val FEATURE_ANALYTICS_DASHBOARD = true
    const val FEATURE_AUTO_ACCEPT = true
    const val FEATURE_PRIORITY_SUPPORT = true

    // Service Constants
    const val MIN_ANALYSIS_INTERVAL_MS = 2000L // 2 seconds
    const val WAKE_LOCK_TIMEOUT_MS = 10000L // 10 seconds

    // Delivery App Package Names
    const val PACKAGE_SWIGGY = "in.swiggy.deliveryapp"
    const val PACKAGE_ZOMATO = "com.zomato.delivery"
    const val PACKAGE_ZEPTO = "com.zepto.rider"
    const val PACKAGE_BLINKIT = "app.blinkit.onboarding"
    const val PACKAGE_UBER_EATS = "com.ubercab.driver"
    const val PACKAGE_BIGBASKET = "com.bigbasket.delivery"

    // Platform Names
    const val PLATFORM_SWIGGY = "swiggy"
    const val PLATFORM_INSTAMART = "instamart"
    const val PLATFORM_ZOMATO = "zomato"
    const val PLATFORM_ZEPTO = "zepto"
    const val PLATFORM_BLINKIT = "blinkit"
    const val PLATFORM_UBER_EATS = "ubereats"
    const val PLATFORM_BIGBASKET = "bigbasket"

    // Supported Delivery Apps
    val SUPPORTED_DELIVERY_PACKAGES = setOf(
        PACKAGE_SWIGGY,      // Swiggy and Instamart (same package)
        PACKAGE_ZOMATO,      // Zomato
        PACKAGE_ZEPTO,       // Zepto
        PACKAGE_BLINKIT,     // Blinkit
        PACKAGE_UBER_EATS,   // Uber Eats
        PACKAGE_BIGBASKET    // BigBasket
    )

    // Country Codes
    const val INDIA_COUNTRY_CODE = "+91"

    // Validation Constants
    const val PHONE_NUMBER_LENGTH = 10
    const val MIN_VALID_INDIAN_PREFIX = 6
}
