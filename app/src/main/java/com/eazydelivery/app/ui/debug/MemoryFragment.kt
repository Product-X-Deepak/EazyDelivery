package com.eazydelivery.app.ui.debug

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.eazydelivery.app.databinding.FragmentMemoryBinding
import com.eazydelivery.app.ui.base.BaseFragment
import timber.log.Timber
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException

/**
 * Fragment for displaying memory metrics
 */
class MemoryFragment : BaseFragment() {
    
    private var _binding: FragmentMemoryBinding? = null
    private val binding get() = _binding!!
    
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateMemoryInfo()
            handler.postDelayed(this, UPDATE_INTERVAL_MS)
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMemoryBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initial update
        updateMemoryInfo()
    }
    
    override fun onResume() {
        super.onResume()
        
        // Start periodic updates
        handler.post(updateRunnable)
    }
    
    override fun onPause() {
        super.onPause()
        
        // Stop periodic updates
        handler.removeCallbacks(updateRunnable)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    /**
     * Update memory information
     */
    private fun updateMemoryInfo() {
        try {
            // Get app memory info
            val activityManager = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            
            // Get app memory usage
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val maxMemory = runtime.maxMemory()
            val appMemoryUsage = usedMemory.toFloat() / maxMemory.toFloat() * 100f
            
            // Get system memory usage
            val systemMemoryUsage = (memoryInfo.totalMem - memoryInfo.availMem).toFloat() / memoryInfo.totalMem.toFloat() * 100f
            
            // Update UI
            binding.apply {
                // App memory
                appMemoryUsageText.text = "App Memory Usage: ${appMemoryUsage.toInt()}%"
                appMemoryUsageProgress.progress = appMemoryUsage.toInt()
                
                appMemoryUsedText.text = "Used: ${formatSize(usedMemory)}"
                appMemoryTotalText.text = "Max: ${formatSize(maxMemory)}"
                
                // System memory
                systemMemoryUsageText.text = "System Memory Usage: ${systemMemoryUsage.toInt()}%"
                systemMemoryUsageProgress.progress = systemMemoryUsage.toInt()
                
                systemMemoryUsedText.text = "Used: ${formatSize(memoryInfo.totalMem - memoryInfo.availMem)}"
                systemMemoryTotalText.text = "Total: ${formatSize(memoryInfo.totalMem)}"
                
                // Low memory warning
                if (memoryInfo.lowMemory) {
                    lowMemoryWarning.visibility = View.VISIBLE
                } else {
                    lowMemoryWarning.visibility = View.GONE
                }
                
                // Heap size
                heapSizeText.text = "Heap Size: ${formatSize(runtime.totalMemory())}"
                heapAllocatedText.text = "Heap Allocated: ${formatSize(usedMemory)}"
                heapFreeText.text = "Heap Free: ${formatSize(runtime.freeMemory())}"
                
                // Get CPU usage
                val cpuUsage = getCpuUsage()
                cpuUsageText.text = "CPU Usage: ${cpuUsage.toInt()}%"
                cpuUsageProgress.progress = cpuUsage.toInt()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating memory info")
        }
    }
    
    /**
     * Get CPU usage
     * 
     * @return CPU usage as a percentage (0-100)
     */
    private fun getCpuUsage(): Float {
        try {
            val pid = android.os.Process.myPid()
            
            // Read CPU usage from proc filesystem
            val reader = BufferedReader(FileReader("/proc/$pid/stat"))
            val line = reader.readLine()
            reader.close()
            
            // Parse CPU usage
            val parts = line.split(" ")
            val utime = parts[13].toLong()
            val stime = parts[14].toLong()
            val cutime = parts[15].toLong()
            val cstime = parts[16].toLong()
            
            val totalTime = utime + stime + cutime + cstime
            
            // Read total CPU time
            val totalReader = BufferedReader(FileReader("/proc/stat"))
            val totalLine = totalReader.readLine()
            totalReader.close()
            
            // Parse total CPU time
            val totalParts = totalLine.split(" ")
            var totalCpuTime = 0L
            for (i in 1 until totalParts.size) {
                if (totalParts[i].isNotEmpty()) {
                    totalCpuTime += totalParts[i].toLong()
                }
            }
            
            // Calculate CPU usage
            return (totalTime.toFloat() / totalCpuTime.toFloat()) * 100f
        } catch (e: IOException) {
            Timber.e(e, "Error getting CPU usage")
            return 0f
        }
    }
    
    /**
     * Format size in bytes to human-readable format
     * 
     * @param bytes Size in bytes
     * @return Formatted size
     */
    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
    
    companion object {
        private const val UPDATE_INTERVAL_MS = 1000L
    }
}
