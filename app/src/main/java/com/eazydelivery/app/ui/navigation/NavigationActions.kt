package com.eazydelivery.app.ui

import androidx.navigation.NavController
import com.eazydelivery.app.ui.navigation.Screen

class NavigationActions(private val navController: NavController) {
    
    val navigateToHome: () -> Unit = {
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Onboarding.route) { inclusive = true }
        }
    }
    
    val navigateToAnalytics: () -> Unit = {
        navController.navigate(Screen.Analytics.route)
    }
    
    val navigateToSettings: () -> Unit = {
        navController.navigate(Screen.Settings.route)
    }
    
    val navigateToSubscription: () -> Unit = {
        navController.navigate(Screen.Subscription.route)
    }
    
    val navigateUp: () -> Unit = {
        navController.navigateUp()
    }
}

