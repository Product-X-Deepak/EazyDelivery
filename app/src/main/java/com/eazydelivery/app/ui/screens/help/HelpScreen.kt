package com.eazydelivery.app.ui.screens.help

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.eazydelivery.app.R
import com.eazydelivery.app.ui.navigation.Screen
import com.eazydelivery.app.util.Constants
import com.lucide.compose.icons.LucideIcons
import com.lucide.compose.icons.lucide.ArrowLeft
import com.lucide.compose.icons.lucide.CheckCircle2
import com.lucide.compose.icons.lucide.ChevronDown
import com.lucide.compose.icons.lucide.ChevronUp
import com.lucide.compose.icons.lucide.HelpCircle
import com.lucide.compose.icons.lucide.Mail
import com.lucide.compose.icons.lucide.MessageSquare
import com.lucide.compose.icons.lucide.Phone

@Composable
fun HelpScreen(
    navController: NavController
) {
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.help_and_support)) },
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
                text = "Frequently Asked Questions",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            FaqItem(
                question = "How does auto-accept work?",
                answer = "The auto-accept feature automatically accepts delivery orders that meet your minimum amount criteria. You need to enable the service and set minimum amounts for each platform in the home screen."
            )
            
            FaqItem(
                question = "Why do I need to grant accessibility permissions?",
                answer = "EazyDelivery needs accessibility permissions to automatically interact with delivery apps and accept orders on your behalf. This is essential for the auto-accept feature to work properly."
            )
            
            FaqItem(
                question = "How do I change language settings?",
                answer = "You can change the language in the Settings screen. Currently, we support English and Hindi."
            )
            
            FaqItem(
                question = "Why does the app need notification access?",
                answer = "EazyDelivery needs to read notifications from delivery apps to detect new orders and their amounts. This is required for the auto-accept feature to work."
            )
            
            FaqItem(
                question = "How do I set minimum order amounts?",
                answer = "On the home screen, you can set minimum order amounts for each delivery platform by using the slider or entering a value directly."
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Contact Support",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(
                modifier = Modifier.padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Email Support",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "For any issues or questions, please email us at:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    androidx.compose.material3.Button(
                        onClick = { 
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:${Constants.ADMIN_EMAIL}")
                                putExtra(Intent.EXTRA_SUBJECT, "EazyDelivery Support Request")
                            }
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(
                            imageVector = LucideIcons.Mail,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text(Constants.ADMIN_EMAIL)
                    }
                }
            }
            
            Card(
                modifier = Modifier.padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Phone Support",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "For urgent issues, please call our support line:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    androidx.compose.material3.Button(
                        onClick = { 
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${Constants.ADMIN_PHONE}")
                            }
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(
                            imageVector = LucideIcons.Phone,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text(Constants.ADMIN_PHONE)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            androidx.compose.material3.OutlinedButton(
                onClick = { navController.navigate(Screen.Feedback.route) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = LucideIcons.MessageSquare,
                    contentDescription = null
                )
                
                Spacer(modifier = Modifier.size(8.dp))
                
                Text("Send Feedback")
            }
        }
    }
}

@Composable
fun FaqItem(
    question: String,
    answer: String
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            androidx.compose.material3.TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = question,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                Icon(
                    imageVector = if (expanded) LucideIcons.ChevronUp else LucideIcons.ChevronDown,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
            
            if (expanded) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = answer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
