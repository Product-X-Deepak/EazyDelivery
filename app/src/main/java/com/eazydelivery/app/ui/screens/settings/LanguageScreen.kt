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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import com.lucide.compose.icons.lucide.ArrowDown
import com.lucide.compose.icons.lucide.ArrowLeft
import com.lucide.compose.icons.lucide.ArrowUp
import com.lucide.compose.icons.lucide.Check
import com.lucide.compose.icons.lucide.Settings

@Composable
fun LanguageScreen(
    navController: NavController,
    viewModel: LanguageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.language)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = LucideIcons.ArrowLeft,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.LanguageSettings.route) }) {
                        Icon(
                            imageVector = LucideIcons.Settings,
                            contentDescription = stringResource(R.string.language_settings)
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
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.select_language),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.language_restart_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    LanguageItem(
                        name = "English",
                        isSelected = uiState.selectedLanguage == "en",
                        onClick = { viewModel.setLanguage("en") }
                    )

                    Divider()

                    LanguageItem(
                        name = "हिंदी (Hindi)",
                        isSelected = uiState.selectedLanguage == "hi",
                        onClick = { viewModel.setLanguage("hi") }
                    )

                    // Add more languages as needed
                    if (uiState.showAllLanguages) {
                        Divider()

                        LanguageItem(
                            name = "தமிழ் (Tamil)",
                            isSelected = uiState.selectedLanguage == "ta",
                            onClick = { viewModel.setLanguage("ta") }
                        )

                        Divider()

                        LanguageItem(
                            name = "తెలుగు (Telugu)",
                            isSelected = uiState.selectedLanguage == "te",
                            onClick = { viewModel.setLanguage("te") }
                        )
                    }

                    Divider()

                    // Show/Hide more languages button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleShowAllLanguages() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (uiState.showAllLanguages) "Show Less" else "Show More Languages",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )

                        Icon(
                            imageVector = if (uiState.showAllLanguages)
                                LucideIcons.ArrowUp else LucideIcons.ArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "For more language options, tap the settings icon in the top-right corner.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LanguageItem(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.weight(1f))

        if (isSelected) {
            Icon(
                imageVector = LucideIcons.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
