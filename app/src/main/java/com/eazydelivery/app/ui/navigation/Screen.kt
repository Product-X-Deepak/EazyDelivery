package com.eazydelivery.app.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object PhoneLogin : Screen("phone_login")
    object OtpVerification : Screen("otp_verification/{phoneNumber}") {
        fun createRoute(phoneNumber: String) = "otp_verification/$phoneNumber"
    }
    object UserOnboarding : Screen("user_onboarding")
    object Home : Screen("home")
    object Analytics : Screen("analytics")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    object Language : Screen("language")
    object LanguageSettings : Screen("language_settings")
    object BiometricSettings : Screen("biometric_settings")
    object TermsAndConditions : Screen("terms_and_conditions")
    object PrivacyPolicy : Screen("privacy_policy")
    object Help : Screen("help")
    object Feedback : Screen("feedback")
    object PrioritizationSettings : Screen("prioritization_settings")
    object About : Screen("about")
    object AdminContact : Screen("admin_contact")

    // Detail screens
    object OrderDetail : Screen("order_detail/{orderId}") {
        fun createRoute(orderId: String) = "order_detail/$orderId"
    }

    object PlatformSettings : Screen("platform_settings/{platformName}") {
        fun createRoute(platformName: String) = "platform_settings/$platformName"
    }

    object Subscription : Screen("subscription")
    object Notifications : Screen("notifications")
}
