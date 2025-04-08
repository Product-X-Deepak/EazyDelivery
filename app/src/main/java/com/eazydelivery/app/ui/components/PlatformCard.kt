package com.eazydelivery.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.eazydelivery.app.R
import com.eazydelivery.app.util.PlatformResources

@Composable
fun PlatformCard(
    name: String,
    isEnabled: Boolean,
    minAmount: Int,
    onToggle: () -> Unit,
    onMinAmountChange: (Int) -> Unit
) {
    var sliderPosition by remember { mutableStateOf(minAmount.toFloat()) }
    var textFieldValue by remember { mutableStateOf(minAmount.toString()) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlatformLogo(platformName = name)
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Text(
                        text = PlatformResources.getPlatformDisplayName(name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { onToggle() }
                )
            }
            
            if (isEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(R.string.minimum_order_amount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Slider(
                        value = sliderPosition,
                        onValueChange = { 
                            sliderPosition = it
                            textFieldValue = it.toInt().toString()
                            onMinAmountChange(it.toInt())
                        },
                        valueRange = 0f..500f,
                        steps = 50,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    OutlinedTextField(
                        value = textFieldValue,
                        onValueChange = { 
                            textFieldValue = it
                            it.toIntOrNull()?.let { value ->
                                if (value in 0..500) {
                                    sliderPosition = value.toFloat()
                                    onMinAmountChange(value)
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        prefix = { Text("₹") },
                        modifier = Modifier.width(100.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PlatformLogo(platformName: String) {
    // In a real app, you would load the platform logo from resources
    // For now, we'll use a colored circle as a placeholder
    val color = when (platformName.lowercase()) {
        "zomato" -> MaterialTheme.colorScheme.error
        "swiggy" -> MaterialTheme.colorScheme.tertiary
        "blinkit" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }
    
    androidx.compose.foundation.Canvas(
        modifier = Modifier.size(32.dp)
    ) {
        drawCircle(color = color)
    }
}
