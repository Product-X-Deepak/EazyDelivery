package com.eazydelivery.app.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
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
import kotlinx.coroutines.delay

@Composable
fun OtpVerificationScreen(
    navController: NavController,
    phoneNumber: String,
    viewModel: OtpVerificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val focusRequesters = remember { List(6) { FocusRequester() } }
    var resendEnabled by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(30) }
    
    // Set phone number in view model
    LaunchedEffect(phoneNumber) {
        viewModel.setPhoneNumber(phoneNumber)
    }
    
    // Handle countdown for resend OTP
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
        resendEnabled = true
    }
    
    // Handle navigation when verification is successful
    LaunchedEffect(uiState.verificationSuccess) {
        if (uiState.verificationSuccess) {
            if (uiState.isNewUser) {
                navController.navigate(Screen.UserOnboarding.route) {
                    popUpTo(Screen.PhoneLogin.route) { inclusive = true }
                }
            } else {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.PhoneLogin.route) { inclusive = true }
                }
            }
        }
    }
    
    // Show error message if any
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    
    // Auto-focus on first OTP field
    LaunchedEffect(Unit) {
        focusRequesters[0].requestFocus()
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
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_eazydelivery),
                    contentDescription = "EazyDelivery Logo",
                    modifier = Modifier.size(100.dp)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = stringResource(R.string.verify_otp),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.otp_sent_to, phoneNumber),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // OTP input fields
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (i in 0 until 6) {
                        OtpDigitField(
                            value = if (i < uiState.otp.length) uiState.otp[i].toString() else "",
                            onValueChange = { newValue ->
                                if (newValue.length <= 1 && newValue.all { it.isDigit() }) {
                                    viewModel.updateOtp(
                                        uiState.otp.replaceRange(
                                            i.coerceAtMost(uiState.otp.length),
                                            (i + 1).coerceAtMost(uiState.otp.length + 1),
                                            newValue
                                        )
                                    )
                                    
                                    // Auto-advance focus
                                    if (newValue.isNotEmpty() && i < 5) {
                                        focusRequesters[i + 1].requestFocus()
                                    }
                                }
                            },
                            focusRequester = focusRequesters[i],
                            onBackspace = {
                                if (i > 0) {
                                    focusRequesters[i - 1].requestFocus()
                                }
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { 
                        focusManager.clearFocus()
                        viewModel.verifyOtp() 
                    },
                    enabled = !uiState.isLoading && uiState.otp.length == 6,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.verify))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.didnt_receive_otp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    TextButton(
                        onClick = { 
                            if (resendEnabled) {
                                viewModel.resendOtp()
                                resendEnabled = false
                                countdown = 30
                            }
                        },
                        enabled = resendEnabled
                    ) {
                        Text(
                            text = if (resendEnabled) 
                                stringResource(R.string.resend) 
                            else 
                                stringResource(R.string.resend_in_seconds, countdown)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OtpDigitField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onBackspace: () -> Unit
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = {
                // Handle in parent
            }
        ),
        singleLine = true,
        modifier = Modifier
            .width(48.dp)
            .height(56.dp)
            .focusRequester(focusRequester)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp)
            ),
        decorationBox = { innerTextField ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.Gray
                    )
                }
                innerTextField()
            }
        },
        textStyle = MaterialTheme.typography.headlineMedium.copy(
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    )
}
