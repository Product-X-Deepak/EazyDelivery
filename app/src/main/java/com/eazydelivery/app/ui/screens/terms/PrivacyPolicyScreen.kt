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
fun PrivacyPolicyScreen(
    navController: NavController
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.privacy_policy)) },
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
                text = "Privacy Policy",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = """
                    Last updated: April 2023
                    
                    EazyDelivery ("we", "our", or "us") is committed to protecting your privacy. This Privacy Policy explains how your personal information is collected, used, and disclosed by EazyDelivery.
                    
                    1. Information We Collect
                    
                    We collect information that you provide directly to us, such as when you create an account, update your profile, use our features, or communicate with us. This information may include your name, email address, phone number, and delivery preferences.
                    
                    We also collect information automatically when you use our app, including:
                    - Log information (such as app version, time and date of use, features used)
                    - Device information (such as device model, operating system, and mobile network)
                    - Location information (with your consent)
                    - Usage information (such as how you interact with our app)
                    
                    2. How We Use Your Information
                    
                    We use the information we collect to:
                    - Provide, maintain, and improve our services
                    - Process and complete transactions
                    - Send you technical notices, updates, security alerts, and support messages
                    - Respond to your comments, questions, and requests
                    - Monitor and analyze trends, usage, and activities
                    - Detect, prevent, and address technical issues
                    
                    3. Sharing of Information
                    
                    We may share the information we collect in various ways, including:
                    - With vendors, consultants, and other service providers who need access to such information to carry out work on our behalf
                    - In response to a request for information if we believe disclosure is in accordance with any applicable law, regulation, or legal process
                    - If we believe your actions are inconsistent with our user agreements or policies, or to protect the rights, property, and safety of EazyDelivery or others
                    
                    4. Data Security
                    
                    We take reasonable measures to help protect information about you from loss, theft, misuse and unauthorized access, disclosure, alteration, and destruction.
                    
                    5. Your Choices
                    
                    You can access and update certain information about you from within the app. You can also opt out of receiving promotional communications from us by following the instructions in those communications.
                    
                    6. Changes to this Policy
                    
                    We may change this Privacy Policy from time to time. If we make changes, we will notify you by revising the date at the top of the policy and, in some cases, we may provide you with additional notice.
                    
                    7. Contact Us
                    
                    If you have any questions about this Privacy Policy, please contact us at: support@eazydelivery.com
                """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
