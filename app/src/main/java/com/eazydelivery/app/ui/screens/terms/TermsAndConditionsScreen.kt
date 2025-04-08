package com.eazydelivery.app.ui.screens.terms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.eazydelivery.app.R
import com.lucide.compose.icons.LucideIcons
import com.lucide.compose.icons.lucide.ArrowLeft

@Composable
fun TermsAndConditionsScreen(
    navController: NavController
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.terms_and_conditions)) },
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
                text = "Terms and Conditions",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = """
                    Welcome to EazyDelivery!
                    
                    These terms and conditions outline the rules and regulations for the use of EazyDelivery's services.
                    
                    By accessing this app, we assume you accept these terms and conditions. Do not continue to use EazyDelivery if you do not agree to take all of the terms and conditions stated on this page.
                    
                    1. License to Use
                    
                    EazyDelivery grants you a limited, non-exclusive, non-transferable license to use the EazyDelivery application for your personal, non-commercial purposes.
                    
                    2. Restrictions
                    
                    You are specifically restricted from:
                    - Publishing any app material in any other media
                    - Selling, sublicensing and/or otherwise commercializing any app material
                    - Using this app in any way that is or may be damaging to this app
                    - Using this app in any way that impacts user access to this app
                    
                    3. Your Content
                    
                    In these terms and conditions, "Your Content" shall mean any audio, video, text, images or other material you choose to display on this app. By displaying Your Content, you grant EazyDelivery a non-exclusive, worldwide, irrevocable, royalty-free license to use, reproduce, adapt, publish, translate and distribute it in any and all media.
                    
                    4. No Warranties
                    
                    This app is provided "as is," with all faults, and EazyDelivery makes no express or implied representations or warranties, of any kind related to this app or the materials contained on this app.
                    
                    5. Limitation of Liability
                    
                    In no event shall EazyDelivery, nor any of its officers, directors and employees, be held liable for anything arising out of or in any way connected with your use of this app.
                    
                    6. Privacy
                    
                    Please review our Privacy Policy, which also governs your visit to our app, to understand our practices.
                    
                    7. Changes to Terms
                    
                    EazyDelivery reserves the right to modify these terms from time to time at our sole discretion. Therefore, you should review these pages periodically.
                    
                    8. Governing Law
                    
                    These terms and conditions are governed by and construed in accordance with the laws of India and you irrevocably submit to the exclusive jurisdiction of the courts in that location.
                """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
