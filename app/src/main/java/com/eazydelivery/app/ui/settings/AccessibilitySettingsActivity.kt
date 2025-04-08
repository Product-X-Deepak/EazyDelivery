package com.eazydelivery.app.ui.settings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.eazydelivery.app.R
import com.eazydelivery.app.databinding.ActivityAccessibilitySettingsBinding
import com.eazydelivery.app.ui.base.BaseActivity
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Activity for accessibility settings
 */
@AndroidEntryPoint
class AccessibilitySettingsActivity : BaseActivity() {
    
    private lateinit var binding: ActivityAccessibilitySettingsBinding
    
    private val viewModel: AccessibilitySettingsViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityAccessibilitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.accessibility_settings)
        
        setupObservers()
        setupListeners()
        
        // Load initial data
        viewModel.loadAccessibilitySettings()
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
    
    override fun getScreenNameForAccessibility(): String {
        return getString(R.string.accessibility_settings)
    }
    
    private fun setupObservers() {
        // Observe loading state
        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.progressBar.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
            }
        }
        
        // Observe accessibility settings
        lifecycleScope.launch {
            viewModel.accessibilitySettings.collectLatest { settings ->
                updateUI(settings)
            }
        }
    }
    
    private fun setupListeners() {
        // Screen reader info button
        binding.screenReaderInfoButton.setOnClickListener {
            openAccessibilitySettings()
        }
        
        // Text size info button
        binding.textSizeInfoButton.setOnClickListener {
            openDisplaySettings()
        }
        
        // High contrast switch
        binding.highContrastSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setHighContrastEnabled(isChecked)
        }
        
        // Reduce motion switch
        binding.reduceMotionSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setReduceMotionEnabled(isChecked)
        }
        
        // Touch target size switch
        binding.touchTargetSizeSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setLargeTouchTargetsEnabled(isChecked)
        }
        
        // Simplified UI switch
        binding.simplifiedUiSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setSimplifiedUIEnabled(isChecked)
        }
    }
    
    /**
     * Update the UI with the current accessibility settings
     * 
     * @param settings The accessibility settings
     */
    private fun updateUI(settings: AccessibilitySettingsViewModel.AccessibilitySettings) {
        // Screen reader status
        val screenReaderEnabled = accessibilityManager.isScreenReaderEnabled()
        binding.screenReaderStatusText.text = if (screenReaderEnabled) {
            getString(R.string.screen_reader_enabled)
        } else {
            getString(R.string.screen_reader_disabled)
        }
        binding.screenReaderStatusText.setTextColor(
            getColor(if (screenReaderEnabled) R.color.success else R.color.text_primary)
        )
        
        // Text size status
        val fontScale = accessibilityManager.getFontScale()
        binding.textSizeStatusText.text = getString(R.string.text_size_scale, fontScale)
        
        // High contrast switch
        binding.highContrastSwitch.isChecked = settings.highContrastEnabled
        
        // Reduce motion switch
        binding.reduceMotionSwitch.isChecked = settings.reduceMotionEnabled
        
        // Touch target size switch
        binding.touchTargetSizeSwitch.isChecked = settings.largeTouchTargetsEnabled
        
        // Simplified UI switch
        binding.simplifiedUiSwitch.isChecked = settings.simplifiedUIEnabled
    }
    
    /**
     * Open the system accessibility settings
     */
    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            Snackbar.make(binding.root, R.string.cannot_open_accessibility_settings, Snackbar.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Open the system display settings
     */
    private fun openDisplaySettings() {
        try {
            val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            Snackbar.make(binding.root, R.string.cannot_open_display_settings, Snackbar.LENGTH_SHORT).show()
        }
    }
}
