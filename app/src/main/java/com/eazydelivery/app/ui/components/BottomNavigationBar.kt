package com.eazydelivery.app.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.eazydelivery.app.R
import com.eazydelivery.app.ui.navigation.Screen
import com.eazydelivery.app.ui.theme.EazyDeliveryTheme
import com.lucide.compose.icons.LucideIcons
import com.lucide.compose.icons.lucide.BarChart2
import com.lucide.compose.icons.lucide.Home
import com.lucide.compose.icons.lucide.Settings
import com.lucide.compose.icons.lucide.User

@Composable
fun BottomNavigationBar(
    navController: NavController,
    currentRoute: String?
) {
    val items = listOf(
        NavigationItem(
            title = stringResource(R.string.home),
            icon = LucideIcons.Home,
            route = Screen.Home.route
        ),
        NavigationItem(
            title = stringResource(R.string.analytics),
            icon = LucideIcons.BarChart2,
            route = Screen.Analytics.route
        ),
        NavigationItem(
            title = stringResource(R.string.profile),
            icon = LucideIcons.User,
            route = Screen.Profile.route
        ),
        NavigationItem(
            title = stringResource(R.string.settings),
            icon = LucideIcons.Settings,
            route = Screen.Settings.route
        )
    )

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

data class NavigationItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: String
)
