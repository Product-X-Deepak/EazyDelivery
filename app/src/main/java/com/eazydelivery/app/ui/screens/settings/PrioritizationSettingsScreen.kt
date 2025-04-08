package com.eazydelivery.app.ui.screens.settings

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
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.lucide.compose.icons.LucideIcons
import com.lucide.compose.icons.lucide.ArrowLeft
import com.lucide.compose.icons.lucide.Clock
import com.lucide.compose.icons.lucide.IndianRupee
import com.lucide.compose.icons.lucide.MapPin

@Composable
fun PrioritizationSettingsScreen(
    navController: NavController,
    viewModel: PrioritizationSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order Prioritization Settings") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Customize Order Prioritization",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Adjust how EazyDelivery prioritizes incoming orders",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Weighting factors
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Weighting Factors",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Earnings weight
                    WeightSlider(
                        icon = LucideIcons.IndianRupee,
                        title = "Earnings",
                        description = "How important is the order amount",
                        value = uiState.earningsWeight,
                        onValueChange = { viewModel.updateEarningsWeight(it) }
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                    
                    // Distance weight
                    WeightSlider(
                        icon = LucideIcons.MapPin,
                        title = "Distance",
                        description = "How important is the delivery distance",
                        value = uiState.distanceWeight,
                        onValueChange = { viewModel.updateDistanceWeight(it) }
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                    
                    // Time weight
                    WeightSlider(
                        icon = LucideIcons.Clock,
                        title = "Time of Day",
                        description = "How important is the time of day",
                        value = uiState.timeWeight,
                        onValueChange = { viewModel.updateTimeWeight(it) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Auto-accept settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Auto-Accept Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Accept Medium Priority Orders",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            
                            Text(
                                text = "Automatically accept orders with medium priority",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Switch(
                            checked = uiState.acceptMediumPriority,
                            onCheckedChange = { viewModel.updateAcceptMediumPriority(it) }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Explanation
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "How Prioritization Works",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "EazyDelivery uses machine learning to analyze incoming orders and assign them a priority level (High, Medium, or Low) based on the weighting factors you set above.\n\n" +
                               "High priority orders are always auto-accepted if they meet your minimum amount criteria. Medium priority orders are only auto-accepted if you enable the option above.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun WeightSlider(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    var sliderPosition by remember { mutableStateOf(value) }
    
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Low",
                style = MaterialTheme.typography.bodySmall
            )
            
            Slider(
                value = sliderPosition,
                onValueChange = { 
                    sliderPosition = it
                    onValueChange(it)
                },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f)
            )
            
            Text(
                text = "High",
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Text(
            text = "${(sliderPosition * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.align(Alignment.End)
        )
    }
}
