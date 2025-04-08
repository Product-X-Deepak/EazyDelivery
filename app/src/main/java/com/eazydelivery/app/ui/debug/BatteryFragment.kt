package com.eazydelivery.app.ui.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.eazydelivery.app.databinding.FragmentBatteryBinding
import com.eazydelivery.app.ui.base.BaseFragment
import timber.log.Timber

/**
 * Fragment for displaying battery metrics
 */
class BatteryFragment : BaseFragment() {
    
    private var _binding: FragmentBatteryBinding? = null
    private val binding get() = _binding!!
    
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                updateBatteryInfo(intent)
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBatteryBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Register battery receiver
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        requireContext().registerReceiver(batteryReceiver, filter)
        
        // Get initial battery info
        val batteryStatus = requireContext().registerReceiver(null, filter)
        batteryStatus?.let { updateBatteryInfo(it) }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        
        // Unregister battery receiver
        try {
            requireContext().unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            Timber.e(e, "Error unregistering battery receiver")
        }
        
        _binding = null
    }
    
    /**
     * Update battery information
     * 
     * @param intent The battery intent
     */
    private fun updateBatteryInfo(intent: Intent) {
        try {
            // Get battery level
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryPct = level * 100 / scale.toFloat()
            
            // Get battery status
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            
            // Get charging method
            val chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            val usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
            val acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC
            val wirelessCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS
            
            // Get battery health
            val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
            val healthString = when (health) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheated"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Unspecified Failure"
                BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                else -> "Unknown"
            }
            
            // Get battery temperature
            val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10f
            
            // Get battery voltage
            val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) / 1000f
            
            // Get battery technology
            val technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"
            
            // Update UI
            binding.apply {
                // Battery level
                batteryLevelText.text = "Battery Level: ${batteryPct.toInt()}%"
                batteryLevelProgress.progress = batteryPct.toInt()
                
                // Battery status
                val statusText = when (status) {
                    BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                    BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                    BatteryManager.BATTERY_STATUS_FULL -> "Full"
                    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
                    else -> "Unknown"
                }
                batteryStatusText.text = "Status: $statusText"
                
                // Charging method
                val chargingMethod = when {
                    usbCharge -> "USB"
                    acCharge -> "AC"
                    wirelessCharge -> "Wireless"
                    else -> "None"
                }
                chargingMethodText.text = "Charging Method: $chargingMethod"
                
                // Battery health
                batteryHealthText.text = "Health: $healthString"
                
                // Battery temperature
                batteryTemperatureText.text = "Temperature: $temperature°C"
                
                // Battery voltage
                batteryVoltageText.text = "Voltage: $voltage V"
                
                // Battery technology
                batteryTechnologyText.text = "Technology: $technology"
                
                // Power save mode
                val powerManager = requireContext().getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                val isPowerSaveMode = powerManager.isPowerSaveMode
                powerSaveModeText.text = "Power Save Mode: ${if (isPowerSaveMode) "Enabled" else "Disabled"}"
                
                // Battery optimization
                val batteryOptimizationEnabled = isBatteryOptimizationEnabled()
                batteryOptimizationText.text = "Battery Optimization: ${if (batteryOptimizationEnabled) "Enabled" else "Disabled"}"
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating battery info")
        }
    }
    
    /**
     * Check if battery optimization is enabled for the app
     * 
     * @return true if battery optimization is enabled, false otherwise
     */
    private fun isBatteryOptimizationEnabled(): Boolean {
        val powerManager = requireContext().getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return !powerManager.isIgnoringBatteryOptimizations(requireContext().packageName)
    }
}
