package com.eazydelivery.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eazydelivery.app.R
import com.eazydelivery.app.util.error.RecoveryAction
import com.lucide.compose.icons.LucideIcons
import com.lucide.compose.icons.lucide.AlertCircle
import com.lucide.compose.icons.lucide.RefreshCw
import com.lucide.compose.icons.lucide.Settings
import com.lucide.compose.icons.lucide.Wifi

/**
 * A reusable error dialog component that displays an error message and provides recovery options
 * 
 * @param title The title of the error dialog
 * @param message The error message to display
 * @param suggestion A suggestion for how to resolve the error (optional)
 * @param recoveryAction The recovery action to offer (optional)
 * @param onDismiss Callback for when the dialog is dismissed
 * @param onRecoveryActionClick Callback for when the recovery action button is clicked
 */
@Composable
fun ErrorDialog(
    title: String,
    message: String,
    suggestion: String? = null,
    recoveryAction: RecoveryAction? = null,
    onDismiss: () -> Unit,
    onRecoveryActionClick: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = LucideIcons.AlertCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (suggestion != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (recoveryAction != null) {
                Button(
                    onClick = onRecoveryActionClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Show different icons based on recovery action
                    val icon = when (recoveryAction) {
                        RecoveryAction.Retry -> LucideIcons.RefreshCw
                        RecoveryAction.OpenNetworkSettings -> LucideIcons.Wifi
                        RecoveryAction.OpenAppSettings -> LucideIcons.Settings
                        RecoveryAction.Relogin -> LucideIcons.RefreshCw
                        else -> LucideIcons.RefreshCw
                    }
                    
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    
                    Text(getRecoveryActionText(recoveryAction))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.dismiss))
            }
        }
    )
}

/**
 * Get the text to display for a recovery action
 */
@Composable
private fun getRecoveryActionText(recoveryAction: RecoveryAction): String {
    return when (recoveryAction) {
        RecoveryAction.Retry -> stringResource(R.string.retry)
        RecoveryAction.OpenNetworkSettings -> stringResource(R.string.open_network_settings)
        RecoveryAction.OpenAppSettings -> stringResource(R.string.open_app_settings)
        RecoveryAction.Relogin -> stringResource(R.string.relogin)
        else -> stringResource(R.string.try_again)
    }
}
