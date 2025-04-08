package com.eazydelivery.app.ui.settings

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.lifecycle.lifecycleScope
import com.eazydelivery.app.R
import com.eazydelivery.app.databinding.ActivitySecuritySettingsBinding
import com.eazydelivery.app.security.BiometricAuthManager
import com.eazydelivery.app.ui.base.BaseActivity
import com.eazydelivery.app.ui.dialog.PinSetupDialog
import com.eazydelivery.app.util.error.ErrorHandler
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Activity for security settings
 */
@AndroidEntryPoint
class SecuritySettingsActivity : BaseActivity() {
    
    private lateinit var binding: ActivitySecuritySettingsBinding
    
    private val viewModel: SecuritySettingsViewModel by viewModels()
    
    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager
    
    @Inject
    lateinit var errorHandler: ErrorHandler
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivitySecuritySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.security_settings)
        
        setupObservers()
        setupListeners()
        
        // Load initial data
        viewModel.loadSecuritySettings()
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun setupObservers() {
        // Observe loading state
        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.progressBar.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
            }
        }
        
        // Observe biometric availability
        lifecycleScope.launch {
            viewModel.biometricAvailability.collectLatest { availability ->
                updateBiometricAvailability(availability)
            }
        }
        
        // Observe biometric enabled state
        lifecycleScope.launch {
            viewModel.isBiometricEnabled.collectLatest { enabled ->
                binding.biometricSwitch.isChecked = enabled
                binding.biometricSwitch.isEnabled = viewModel.biometricAvailability.value == BiometricAuthManager.BiometricAvailability.AVAILABLE
            }
        }
        
        // Observe PIN enabled state
        lifecycleScope.launch {
            viewModel.isPinEnabled.collectLatest { enabled ->
                binding.pinSwitch.isChecked = enabled
            }
        }
    }
    
    private fun setupListeners() {
        // Biometric switch
        binding.biometricSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != viewModel.isBiometricEnabled.value) {
                if (isChecked) {
                    enableBiometricAuth()
                } else {
                    disableBiometricAuth()
                }
            }
        }
        
        // PIN switch
        binding.pinSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != viewModel.isPinEnabled.value) {
                if (isChecked) {
                    showPinSetupDialog()
                } else {
                    showPinDisableConfirmation()
                }
            }
        }
        
        // Change PIN button
        binding.changePinButton.setOnClickListener {
            showChangePinDialog()
        }
        
        // Clear all data button
        binding.clearDataButton.setOnClickListener {
            showClearDataConfirmation()
        }
    }
    
    /**
     * Update the biometric availability UI
     * 
     * @param availability The biometric availability
     */
    private fun updateBiometricAvailability(availability: BiometricAuthManager.BiometricAvailability) {
        when (availability) {
            BiometricAuthManager.BiometricAvailability.AVAILABLE -> {
                binding.biometricStatusText.text = getString(R.string.biometric_available)
                binding.biometricStatusText.setTextColor(getColor(R.color.success))
                binding.biometricSwitch.isEnabled = true
            }
            BiometricAuthManager.BiometricAvailability.NOT_ENROLLED -> {
                binding.biometricStatusText.text = getString(R.string.biometric_not_enrolled)
                binding.biometricStatusText.setTextColor(getColor(R.color.warning))
                binding.biometricSwitch.isEnabled = false
                binding.biometricSwitch.isChecked = false
            }
            else -> {
                binding.biometricStatusText.text = getString(R.string.biometric_not_available)
                binding.biometricStatusText.setTextColor(getColor(R.color.error))
                binding.biometricSwitch.isEnabled = false
                binding.biometricSwitch.isChecked = false
            }
        }
    }
    
    /**
     * Enable biometric authentication
     */
    private fun enableBiometricAuth() {
        biometricAuthManager.showBiometricPrompt(
            activity = this,
            title = getString(R.string.biometric_setup_title),
            subtitle = getString(R.string.biometric_setup_subtitle),
            description = getString(R.string.biometric_setup_description),
            onSuccess = {
                viewModel.setBiometricEnabled(true)
                Snackbar.make(binding.root, R.string.biometric_enabled, Snackbar.LENGTH_SHORT).show()
            },
            onError = { errorCode, errString ->
                binding.biometricSwitch.isChecked = false
                Snackbar.make(binding.root, getString(R.string.biometric_error, errString), Snackbar.LENGTH_LONG).show()
            },
            onFailed = {
                binding.biometricSwitch.isChecked = false
                Snackbar.make(binding.root, R.string.biometric_auth_failed, Snackbar.LENGTH_SHORT).show()
            }
        )
    }
    
    /**
     * Disable biometric authentication
     */
    private fun disableBiometricAuth() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.disable_biometric_title)
            .setMessage(R.string.disable_biometric_message)
            .setPositiveButton(R.string.disable) { _, _ ->
                viewModel.setBiometricEnabled(false)
                Snackbar.make(binding.root, R.string.biometric_disabled, Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                binding.biometricSwitch.isChecked = true
            }
            .show()
    }
    
    /**
     * Show the PIN setup dialog
     */
    private fun showPinSetupDialog() {
        val dialog = PinSetupDialog(
            context = this,
            onPinSet = { pin ->
                viewModel.setPin(pin)
                Snackbar.make(binding.root, R.string.pin_enabled, Snackbar.LENGTH_SHORT).show()
            },
            onCancel = {
                binding.pinSwitch.isChecked = false
            }
        )
        dialog.show()
    }
    
    /**
     * Show the PIN disable confirmation dialog
     */
    private fun showPinDisableConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.disable_pin_title)
            .setMessage(R.string.disable_pin_message)
            .setPositiveButton(R.string.disable) { _, _ ->
                viewModel.clearPin()
                Snackbar.make(binding.root, R.string.pin_disabled, Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                binding.pinSwitch.isChecked = true
            }
            .show()
    }
    
    /**
     * Show the change PIN dialog
     */
    private fun showChangePinDialog() {
        val dialog = PinSetupDialog(
            context = this,
            isChangingPin = true,
            onPinSet = { pin ->
                viewModel.setPin(pin)
                Snackbar.make(binding.root, R.string.pin_changed, Snackbar.LENGTH_SHORT).show()
            },
            onCancel = {
                // Do nothing
            }
        )
        dialog.show()
    }
    
    /**
     * Show the clear data confirmation dialog
     */
    private fun showClearDataConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.clear_data_title)
            .setMessage(R.string.clear_data_message)
            .setPositiveButton(R.string.clear) { _, _ ->
                viewModel.clearAllSecureData()
                Snackbar.make(binding.root, R.string.data_cleared, Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
