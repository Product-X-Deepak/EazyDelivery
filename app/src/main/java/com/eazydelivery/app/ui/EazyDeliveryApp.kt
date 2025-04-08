package com.eazydelivery.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.eazydelivery.app.ui.components.BottomNavigationBar
import com.eazydelivery.app.ui.navigation.Screen
import com.eazydelivery.app.ui.screens.analytics.AnalyticsScreen
import com.eazydelivery.app.ui.screens.auth.OtpVerificationScreen
import com.eazydelivery.app.ui.screens.auth.PhoneLoginScreen
import com.eazydelivery.app.ui.screens.feedback.FeedbackScreen
import com.eazydelivery.app.ui.screens.help.HelpScreen
import com.eazydelivery.app.ui.screens.home.HomeScreen
import com.eazydelivery.app.ui.screens.onboarding.UserOnboardingScreen
import com.eazydelivery.app.ui.screens.order.OrderDetailScreen
import com.eazydelivery.app.ui.screens.profile.ProfileScreen
import com.eazydelivery.app.ui.screens.settings.AboutScreen
import com.eazydelivery.app.ui.screens.settings.AdminContactScreen
import com.eazydelivery.app.ui.screens.settings.BiometricSettingsScreen
import com.eazydelivery.app.ui.screens.settings.LanguageScreen
import com.eazydelivery.app.ui.screens.settings.LanguageSettingsScreen
import com.eazydelivery.app.ui.screens.settings.PrioritizationSettingsScreen
import com.eazydelivery.app.ui.screens.settings.SettingsScreen
import com.eazydelivery.app.ui.screens.splash.SplashScreen
import com.eazydelivery.app.ui.screens.subscription.SubscriptionScreen
import com.eazydelivery.app.ui.screens.terms.PrivacyPolicyScreen
import com.eazydelivery.app.ui.screens.terms.TermsAndConditionsScreen

@Composable
fun EazyDeliveryApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar(currentRoute)) {
                BottomNavigationBar(navController = navController, currentRoute = currentRoute)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(navController = navController)
            }

            composable(Screen.PhoneLogin.route) {
                PhoneLoginScreen(navController = navController)
            }

            composable(
                route = Screen.OtpVerification.route,
                arguments = listOf(
                    navArgument("phoneNumber") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
                OtpVerificationScreen(navController = navController, phoneNumber = phoneNumber)
            }

            composable(Screen.UserOnboarding.route) {
                UserOnboardingScreen(navController = navController)
            }

            composable(Screen.Home.route) {
                HomeScreen(navController = navController)
            }

            composable(Screen.Analytics.route) {
                AnalyticsScreen(navController = navController)
            }

            composable(Screen.Profile.route) {
                ProfileScreen(navController = navController)
            }

            composable(Screen.Settings.route) {
                SettingsScreen(navController = navController)
            }

            composable(Screen.Language.route) {
                LanguageScreen(navController = navController)
            }

            composable(Screen.LanguageSettings.route) {
                LanguageSettingsScreen(navController = navController)
            }

            composable(Screen.BiometricSettings.route) {
                BiometricSettingsScreen(navController = navController)
            }

            composable(Screen.TermsAndConditions.route) {
                TermsAndConditionsScreen(navController = navController)
            }

            composable(Screen.PrivacyPolicy.route) {
                PrivacyPolicyScreen(navController = navController)
            }

            composable(Screen.Help.route) {
                HelpScreen(navController = navController)
            }

            composable(Screen.Subscription.route) {
                SubscriptionScreen(navController = navController)
            }

            composable(Screen.Feedback.route) {
                FeedbackScreen(navController = navController)
            }

            composable(Screen.PrioritizationSettings.route) {
                PrioritizationSettingsScreen(navController = navController)
            }

            composable(Screen.About.route) {
                AboutScreen(navController = navController)
            }

            composable(Screen.AdminContact.route) {
                AdminContactScreen(navController = navController)
            }

            // Add order detail screen
            composable(
                route = Screen.OrderDetail.route,
                arguments = listOf(
                    navArgument("orderId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                OrderDetailScreen(navController = navController, orderId = orderId)
            }
        }
    }
}

private fun shouldShowBottomBar(currentRoute: String?): Boolean {
    return currentRoute in listOf(
        Screen.Home.route,
        Screen.Analytics.route,
        Screen.Profile.route,
        Screen.Settings.route
    )
}
