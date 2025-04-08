package com.eazydelivery.app.ui.screens.auth

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.eazydelivery.app.R
import com.eazydelivery.app.ui.navigation.Screen
import com.lucide.compose.icons.LucideIcons
import com.lucide.compose.icons.lucide.Globe

@Composable
fun PhoneLoginScreen(
    navController: NavController,
    viewModel: PhoneLoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showLanguageDropdown by remember { mutableStateOf(false) }

    // Handle navigation when OTP is sent successfully
    LaunchedEffect(uiState.otpSent) {
        if (uiState.otpSent) {
            navController.navigate(Screen.OtpVerification.createRoute(uiState.phoneNumber))
        }
    }

    // Show error message if any
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Language selector in top-right corner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    IconButton(onClick = { showLanguageDropdown = true }) {
                        Icon(
                            imageVector = LucideIcons.Globe,
                            contentDescription = "Change Language"
                        )
                    }

                    DropdownMenu(
                        expanded = showLanguageDropdown,
                        onDismissRequest = { showLanguageDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("English") },
                            onClick = {
                                viewModel.setLanguage("en")
                                showLanguageDropdown = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("हिंदी") },
                            onClick = {
                                viewModel.setLanguage("hi")
                                showLanguageDropdown = false
                            }
                        )
                    }
                }

                Image(
                    painter = painterResource(id = R.drawable.logo_eazydelivery),
                    contentDescription = "EazyDelivery Logo",
                    modifier = Modifier.size(120.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = stringResource(R.string.login_title),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.phone_login_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Phone number input with support for international formats
                OutlinedTextField(
                    value = uiState.phoneNumber,
                    onValueChange = { viewModel.updatePhoneNumber(it) },
                    label = { Text(stringResource(R.string.phone_number)) },
                    placeholder = { Text("Enter phone number (e.g., +91 98765 43210)") },
                    isError = uiState.phoneNumberError != null,
                    supportingText = {
                        uiState.phoneNumberError?.let { Text(it) }
                        uiState.formattedPhoneNumber?.let {
                            if (uiState.phoneNumberError == null) {
                                Text(stringResource(R.string.phone_number_format, it))
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { viewModel.sendOtp() }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.sendOtp() },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.send_otp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.terms_and_conditions_agreement),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Phone number confirmation dialog
            if (uiState.showConfirmationDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.cancelConfirmation() },
                    title = { Text(stringResource(R.string.confirm_phone_number)) },
                    text = {
                        Text(
                            stringResource(
                                R.string.confirm_phone_number_message,
                                uiState.formattedPhoneNumber ?: "+91 ${uiState.phoneNumber}"
                            )
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.confirmPhoneNumber() }
                        ) {
                            Text(stringResource(R.string.confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { viewModel.cancelConfirmation() }
                        ) {
                            Text(stringResource(R.string.edit))
                        }
                    }
                )
            }
        }
    }
}
