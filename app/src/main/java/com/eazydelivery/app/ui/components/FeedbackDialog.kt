package com.eazydelivery.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.eazydelivery.app.R
import com.eazydelivery.app.feedback.UserFeedback
import com.lucide.compose.icons.LucideIcons
import com.lucide.compose.icons.lucide.Star
import com.lucide.compose.icons.lucide.StarOff

/**
 * Dialog for collecting app rating feedback
 */
@Composable
fun RatingDialog(
    onDismiss: () -> Unit,
    onSubmit: (Int) -> Unit
) {
    var rating by remember { mutableStateOf(0) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.rate_app_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.rate_app_message),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Star rating
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (i in 1..5) {
                        IconButton(
                            onClick = { rating = i },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (i <= rating) LucideIcons.Star else LucideIcons.StarOff,
                                contentDescription = stringResource(R.string.star_rating, i),
                                tint = if (i <= rating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(rating) },
                enabled = rating > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.submit))
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
 * Dialog for collecting detailed feedback
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedFeedbackDialog(
    onDismiss: () -> Unit,
    onSubmit: (UserFeedback) -> Unit
) {
    var rating by remember { mutableStateOf(3) }
    var category by remember { mutableStateOf("") }
    var comments by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var includeDeviceInfo by remember { mutableStateOf(true) }
    var includeAppLogs by remember { mutableStateOf(false) }
    
    var expanded by remember { mutableStateOf(false) }
    
    val categories = listOf(
        stringResource(R.string.feedback_category_general),
        stringResource(R.string.feedback_category_bug),
        stringResource(R.string.feedback_category_feature),
        stringResource(R.string.feedback_category_performance),
        stringResource(R.string.feedback_category_ui)
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.feedback_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.feedback_message),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Rating
                Text(
                    text = stringResource(R.string.rating),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Row(
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup()
                ) {
                    for (i in 1..5) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .selectable(
                                    selected = rating == i,
                                    onClick = { rating = i },
                                    role = Role.RadioButton
                                )
                                .padding(end = 8.dp)
                        ) {
                            RadioButton(
                                selected = rating == i,
                                onClick = null
                            )
                            Text(
                                text = i.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Category
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    TextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.category)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    category = item
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Comments
                OutlinedTextField(
                    value = comments,
                    onValueChange = { comments = it },
                    label = { Text(stringResource(R.string.comments)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Contact email
                OutlinedTextField(
                    value = contactEmail,
                    onValueChange = { contactEmail = it },
                    label = { Text(stringResource(R.string.contact_email_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Include device info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = includeDeviceInfo,
                        onCheckedChange = { includeDeviceInfo = it }
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = stringResource(R.string.include_device_info),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                // Include app logs
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = includeAppLogs,
                        onCheckedChange = { includeAppLogs = it }
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = stringResource(R.string.include_app_logs),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(
                        UserFeedback(
                            rating = rating,
                            category = category.ifEmpty { categories[0] },
                            comments = comments,
                            contactEmail = contactEmail,
                            includeDeviceInfo = includeDeviceInfo,
                            includeAppLogs = includeAppLogs
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.submit))
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
 * Dialog for prompting user to rate on Play Store
 */
@Composable
fun PlayStoreRatingDialog(
    onDismiss: () -> Unit,
    onRate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.rate_on_play_store_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = stringResource(R.string.rate_on_play_store_message),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onRate,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.rate_on_play_store))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.not_now))
            }
        }
    )
}
