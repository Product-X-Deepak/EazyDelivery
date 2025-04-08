package com.eazydelivery.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.eazydelivery.app.R
import com.eazydelivery.app.ui.navigation.Screen
import com.lucide.compose.icons.LucideIcons
import com.lucide.compose.icons.lucide.ArrowLeft
import com.lucide.compose.icons.lucide.Battery
import com.lucide.compose.icons.lucide.Bell
import com.lucide.compose.icons.lucide.ChevronRight
import com.lucide.compose.icons.lucide.Filter
import com.lucide.compose.icons.lucide.Fingerprint
import com.lucide.compose.icons.lucide.Globe
import com.lucide.compose.icons.lucide.HelpCircle
import com.lucide.compose.icons.lucide.Lock
import com.lucide.compose.icons.lucide.MessageSquare
import com.lucide.compose.icons.lucide.Moon
import com.lucide.compose.icons.lucide.Power
import com.lucide.compose.icons.lucide.Smartphone

@Composable
fun SettingsScreen(
   navController: NavController,
   viewModel: SettingsViewModel = hiltViewModel()
) {
   val uiState by viewModel.uiState.collectAsState()

   Scaffold(
       topBar = {
           TopAppBar(
               title = { Text(stringResource(R.string.settings)) },
               navigationIcon = {
                   androidx.compose.material3.IconButton(onClick = { navController.navigateUp() }) {
                       Icon(
                           imageVector = LucideIcons.ArrowLeft,
                           contentDescription = "Back"
                       )
                   }
               }
           )
       }
   ) { padding ->
       Column(
           modifier = Modifier
               .fillMaxSize()
               .padding(padding)
               .verticalScroll(rememberScrollState())
       ) {
           Card(
               modifier = Modifier
                   .fillMaxWidth()
                   .padding(16.dp),
               elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
           ) {
               Column(
                   modifier = Modifier.padding(16.dp)
               ) {
                   Text(
                       text = stringResource(R.string.language),
                       style = MaterialTheme.typography.titleMedium,
                       fontWeight = FontWeight.Bold
                   )

                   Spacer(modifier = Modifier.height(16.dp))

                   SettingsNavigationItem(
                       icon = LucideIcons.Globe,
                       title = stringResource(R.string.language),
                       subtitle = if (uiState.languageCode == "en") "English" else "हिंदी",
                       onClick = { navController.navigate(Screen.Language.route) }
                   )

                   Divider(modifier = Modifier.padding(vertical = 8.dp))

                   SettingsNavigationItem(
                       icon = LucideIcons.Settings,
                       title = stringResource(R.string.language_settings),
                       subtitle = "Configure additional languages",
                       onClick = { navController.navigate(Screen.LanguageSettings.route) }
                   )
               }
           }

           Card(
               modifier = Modifier
                   .fillMaxWidth()
                   .padding(16.dp),
               elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
           ) {
               Column(
                   modifier = Modifier.padding(16.dp)
               ) {
                   Text(
                       text = "Appearance",
                       style = MaterialTheme.typography.titleMedium,
                       fontWeight = FontWeight.Bold
                   )

                   Spacer(modifier = Modifier.height(16.dp))

                   SettingsSwitchItem(
                       icon = LucideIcons.Moon,
                       title = stringResource(R.string.dark_mode),
                       subtitle = "Use dark theme",
                       checked = uiState.darkModeEnabled,
                       onCheckedChange = { viewModel.toggleDarkMode() }
                   )

                   Divider(modifier = Modifier.padding(vertical = 8.dp))

                   SettingsNavigationItem(
                       icon = LucideIcons.Fingerprint,
                       title = stringResource(R.string.biometric_authentication),
                       subtitle = stringResource(R.string.biometric_subtitle),
                       onClick = { navController.navigate(Screen.BiometricSettings.route) }
                   )
               }
           }

           Card(
               modifier = Modifier
                   .fillMaxWidth()
                   .padding(16.dp),
               elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
           ) {
               Column(
                   modifier = Modifier.padding(16.dp)
               ) {
                   Text(
                       text = stringResource(R.string.notifications),
                       style = MaterialTheme.typography.titleMedium,
                       fontWeight = FontWeight.Bold
                   )

                   Spacer(modifier = Modifier.height(16.dp))

                   SettingsSwitchItem(
                       icon = LucideIcons.Bell,
                       title = "Push Notifications",
                       subtitle = "Receive notifications for new orders",
                       checked = uiState.notificationsEnabled,
                       onCheckedChange = { viewModel.toggleNotifications() }
                   )
               }
           }

           Card(
               modifier = Modifier
                   .fillMaxWidth()
                   .padding(16.dp),
               elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
           ) {
               Column(
                   modifier = Modifier.padding(16.dp)
               ) {
                   Text(
                       text = "Order Prioritization",
                       style = MaterialTheme.typography.titleMedium,
                       fontWeight = FontWeight.Bold
                   )

                   Spacer(modifier = Modifier.height(16.dp))

                   SettingsNavigationItem(
                       icon = LucideIcons.Filter,
                       title = "Prioritization Settings",
                       subtitle = "Customize how orders are prioritized",
                       onClick = { navController.navigate(Screen.PrioritizationSettings.route) }
                   )
               }
           }

           Card(
               modifier = Modifier
                   .fillMaxWidth()
                   .padding(16.dp),
               elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
           ) {
               Column(
                   modifier = Modifier.padding(16.dp)
               ) {
                   Text(
                       text = "Service Settings",
                       style = MaterialTheme.typography.titleMedium,
                       fontWeight = FontWeight.Bold
                   )

                   Spacer(modifier = Modifier.height(16.dp))

                   SettingsSwitchItem(
                       icon = LucideIcons.Power,
                       title = "Auto-Start Service",
                       subtitle = "Start service automatically on device boot",
                       checked = uiState.autoStartEnabled,
                       onCheckedChange = { viewModel.toggleAutoStart() }
                   )

                   Divider(modifier = Modifier.padding(vertical = 8.dp))

                   SettingsSwitchItem(
                       icon = LucideIcons.Battery,
                       title = "Ignore Battery Optimization",
                       subtitle = "Allow app to run in background",
                       checked = uiState.batteryOptimizationDisabled,
                       onCheckedChange = { viewModel.toggleBatteryOptimization() }
                   )

                   Divider(modifier = Modifier.padding(vertical = 8.dp))

                   SettingsSwitchItem(
                       icon = LucideIcons.Smartphone,
                       title = "Accessibility Service",
                       subtitle = "Required for auto-accepting orders",
                       checked = uiState.accessibilityServiceEnabled,
                       onCheckedChange = { viewModel.openAccessibilitySettings() }
                   )
               }
           }

           Card(
               modifier = Modifier
                   .fillMaxWidth()
                   .padding(16.dp),
               elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
           ) {
               Column(
                   modifier = Modifier.padding(16.dp)
               ) {
                   Text(
                       text = "Feedback & Support",
                       style = MaterialTheme.typography.titleMedium,
                       fontWeight = FontWeight.Bold
                   )

                   Spacer(modifier = Modifier.height(16.dp))

                   SettingsNavigationItem(
                       icon = LucideIcons.MessageSquare,
                       title = "Send Feedback",
                       subtitle = "Help us improve EazyDelivery",
                       onClick = { navController.navigate(Screen.Feedback.route) }
                   )

                   Divider(modifier = Modifier.padding(vertical = 8.dp))

                   SettingsNavigationItem(
                       icon = LucideIcons.HelpCircle,
                       title = stringResource(R.string.help_and_support),
                       subtitle = "Get help with the app",
                       onClick = { navController.navigate(Screen.Help.route) }
                   )
               }
           }

           Card(
               modifier = Modifier
                   .fillMaxWidth()
                   .padding(16.dp),
               elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
           ) {
               Column(
                   modifier = Modifier.padding(16.dp)
               ) {
                   Text(
                       text = "Legal",
                       style = MaterialTheme.typography.titleMedium,
                       fontWeight = FontWeight.Bold
                   )

                   Spacer(modifier = Modifier.height(16.dp))

                   SettingsNavigationItem(
                       icon = LucideIcons.Lock,
                       title = stringResource(R.string.terms_and_conditions),
                       subtitle = "View our terms and conditions",
                       onClick = { navController.navigate(Screen.TermsAndConditions.route) }
                   )

                   Divider(modifier = Modifier.padding(vertical = 8.dp))

                   SettingsNavigationItem(
                       icon = LucideIcons.Lock,
                       title = stringResource(R.string.privacy_policy),
                       subtitle = "View our privacy policy",
                       onClick = { navController.navigate(Screen.PrivacyPolicy.route) }
                   )
               }
           }

           Text(
               text = "App Version: 1.0.0",
               style = MaterialTheme.typography.bodyMedium,
               color = MaterialTheme.colorScheme.onSurfaceVariant,
               modifier = Modifier
                   .fillMaxWidth()
                   .padding(16.dp)
                   .align(Alignment.CenterHorizontally)
           )
       }
   }
}

@Composable
fun SettingsSwitchItem(
   icon: androidx.compose.ui.graphics.vector.ImageVector,
   title: String,
   subtitle: String,
   checked: Boolean,
   onCheckedChange: (Boolean) -> Unit
) {
   Row(
       modifier = Modifier
           .fillMaxWidth()
           .padding(vertical = 8.dp),
       verticalAlignment = Alignment.CenterVertically
   ) {
       Icon(
           imageVector = icon,
           contentDescription = null,
           tint = MaterialTheme.colorScheme.primary
       )

       Spacer(modifier = Modifier.width(16.dp))

       Column(
           modifier = Modifier.weight(1f)
       ) {
           Text(
               text = title,
               style = MaterialTheme.typography.titleMedium
           )

           Text(
               text = subtitle,
               style = MaterialTheme.typography.bodyMedium,
               color = MaterialTheme.colorScheme.onSurfaceVariant
           )
       }

       Switch(
           checked = checked,
           onCheckedChange = onCheckedChange
       )
   }
}

@Composable
fun SettingsNavigationItem(
   icon: androidx.compose.ui.graphics.vector.ImageVector,
   title: String,
   subtitle: String,
   onClick: () -> Unit
) {
   Row(
       modifier = Modifier
           .fillMaxWidth()
           .clickable(onClick = onClick)
           .padding(vertical = 8.dp),
       verticalAlignment = Alignment.CenterVertically
   ) {
       Icon(
           imageVector = icon,
           contentDescription = null,
           tint = MaterialTheme.colorScheme.primary
       )

       Spacer(modifier = Modifier.width(16.dp))

       Column(
           modifier = Modifier.weight(1f)
       ) {
           Text(
               text = title,
               style = MaterialTheme.typography.titleMedium
           )

           Text(
               text = subtitle,
               style = MaterialTheme.typography.bodyMedium,
               color = MaterialTheme.colorScheme.onSurfaceVariant
           )
       }

       Icon(
           imageVector = LucideIcons.ChevronRight,
           contentDescription = null,
           modifier = Modifier.padding(8.dp)
       )
   }
}
