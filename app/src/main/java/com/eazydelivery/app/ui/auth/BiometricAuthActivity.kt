package com.eazydelivery.app.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eazydelivery.app.MainActivity
import com.eazydelivery.app.R
import com.eazydelivery.app.ui.theme.EazyDeliveryTheme
import com.eazydelivery.app.util.BiometricManager
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Activity for biometric authentication before accessing the app
 */
@AndroidEntryPoint
class BiometricAuthActivity : ComponentActivity() {
    
    @Inject
    lateinit var biometricManager: BiometricManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if biometric authentication is enabled
        if (!biometricManager.isBiometricEnabled()) {
            // If not enabled, proceed to MainActivity
            startMainActivity()
            return
        }
        
        // Check if biometric authentication is available
        if (!biometricManager.canAuthenticate()) {
            // If not available, proceed to MainActivity
            startMainActivity()
            return
        }
        
        setContent {
            EazyDeliveryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BiometricAuthScreen(
                        onAuthSuccess = { startMainActivity() },
                        onSkipAuth = { startMainActivity() }
                    )
                }
            }
        }
        
        // Show biometric prompt
        showBiometricPrompt()
    }
    
    /**
     * Show biometric prompt for authentication
     */
    private fun showBiometricPrompt() {
        try {
            biometricManager.showBiometricPrompt(
                activity = this,
                title = stringResource(R.string.biometric_authentication),
                subtitle = "Authenticate to access EazyDelivery",
                description = "Use your fingerprint or face to verify your identity",
                onSuccess = {
                    Timber.d("Biometric authentication successful")
                    startMainActivity()
                },
                onError = { errorCode, errString ->
                    Timber.e("Biometric authentication error: $errorCode - $errString")
                    // Don't automatically proceed to MainActivity on error
                    // Let the user try again or use the skip button
                },
                onFailed = {
                    Timber.e("Biometric authentication failed")
                    // Don't automatically proceed to MainActivity on failure
                    // Let the user try again or use the skip button
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Error showing biometric prompt")
            // If there's an error showing the prompt, proceed to MainActivity
            startMainActivity()
        }
    }
    
    /**
     * Start the MainActivity and finish this activity
     */
    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}

/**
 * Screen for biometric authentication
 */
@Composable
fun BiometricAuthScreen(
    onAuthSuccess: () -> Unit,
    onSkipAuth: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_eazydelivery),
            contentDescription = "EazyDelivery Logo",
            modifier = Modifier.size(120.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = stringResource(R.string.biometric_authentication),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Use your fingerprint or face to verify your identity",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                // Try authentication again
                onAuthSuccess()
            }
        ) {
            Text("Try Again")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onSkipAuth
        ) {
            Text("Skip Authentication")
        }
    }
}
