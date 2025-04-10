package com.eazydelivery.app.util

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.os.Process
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages memory usage and performs optimizations
 */
@Singleton
class MemoryManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val errorHandler: ErrorHandler
) {
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    companion object {
        private const val LOW_MEMORY_THRESHOLD_PERCENTAGE = 15
        private const val MEDIUM_MEMORY_THRESHOLD_PERCENTAGE = 30
        private const val MEMORY_CHECK_INTERVAL_MS = 60000L // 1 minute
        private const val CACHE_CLEANUP_THRESHOLD_MB = 50
        private const val MAX_BITMAP_CACHE_SIZE_MB = 20
    }
    
    // Memory state
    private var isLowMemory = false
    private var availableMemoryPercentage = 100
    
    /**
     * Initialize the memory manager
     */
    fun initialize() {
        try {
            Timber.d("Initializing memory manager")
            
            // Start memory monitoring
            startMemoryMonitoring()
            
            // Perform initial cleanup
            cleanupMemory()
            
            Timber.d("Memory manager initialized")
        } catch (e: Exception) {
            errorHandler.handleException("MemoryManager.initialize", e)
        }
    }
    
    /**
     * Start monitoring memory usage
     */
    private fun startMemoryMonitoring() {
        managerScope.launch {
            try {
                while (true) {
                    checkMemoryState()
                    delay(MEMORY_CHECK_INTERVAL_MS)
                }
            } catch (e: Exception) {
                errorHandler.handleException("MemoryManager.startMemoryMonitoring", e)
            }
        }
    }
    
    /**
     * Check the current memory state
     */
    private fun checkMemoryState() {
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            
            val totalMemory = memoryInfo.totalMem
            val availableMemory = memoryInfo.availMem
            
            // Calculate available memory percentage
            availableMemoryPercentage = ((availableMemory.toFloat() / totalMemory.toFloat()) * 100).toInt()
            
            // Check if we're in a low memory state
            val wasLowMemory = isLowMemory
            isLowMemory = availableMemoryPercentage < LOW_MEMORY_THRESHOLD_PERCENTAGE
            
            // Log memory state
            Timber.d("Memory state: ${availableMemoryPercentage}% available, low memory: $isLowMemory")
            
            // If we've entered a low memory state, perform cleanup
            if (isLowMemory && !wasLowMemory) {
                Timber.w("Entered low memory state, performing cleanup")
                cleanupMemory()
            }
            
            // If we're in a medium memory state, perform light cleanup
            if (availableMemoryPercentage < MEDIUM_MEMORY_THRESHOLD_PERCENTAGE) {
                Timber.d("Medium memory state, performing light cleanup")
                cleanupCache()
            }
        } catch (e: Exception) {
            errorHandler.handleException("MemoryManager.checkMemoryState", e)
        }
    }
    
    /**
     * Clean up memory
     */
    fun cleanupMemory() {
        try {
            Timber.d("Cleaning up memory")
            
            // Clear caches
            cleanupCache()
            
            // Suggest garbage collection
            System.gc()
            
            // Log memory usage after cleanup
            logMemoryUsage()
            
            Timber.d("Memory cleanup completed")
        } catch (e: Exception) {
            errorHandler.handleException("MemoryManager.cleanupMemory", e)
        }
    }
    
    /**
     * Clean up cache files
     */
    private fun cleanupCache() {
        try {
            // Clean up external cache
            val externalCacheDir = context.externalCacheDir
            if (externalCacheDir != null) {
                val cacheSize = getFolderSize(externalCacheDir) / (1024 * 1024) // Convert to MB
                if (cacheSize > CACHE_CLEANUP_THRESHOLD_MB) {
                    Timber.d("External cache size: $cacheSize MB, cleaning up")
                    deleteOldCacheFiles(externalCacheDir)
                }
            }
            
            // Clean up internal cache
            val internalCacheDir = context.cacheDir
            val cacheSize = getFolderSize(internalCacheDir) / (1024 * 1024) // Convert to MB
            if (cacheSize > CACHE_CLEANUP_THRESHOLD_MB) {
                Timber.d("Internal cache size: $cacheSize MB, cleaning up")
                deleteOldCacheFiles(internalCacheDir)
            }
            
            // Trim memory for bitmap cache
            trimBitmapCache()
        } catch (e: Exception) {
            errorHandler.handleException("MemoryManager.cleanupCache", e)
        }
    }
    
    /**
     * Delete old cache files
     */
    private fun deleteOldCacheFiles(directory: File) {
        try {
            val files = directory.listFiles() ?: return
            
            // Sort files by last modified time (oldest first)
            val sortedFiles = files.filter { it.isFile }.sortedBy { it.lastModified() }
            
            // Delete oldest files until we're under the threshold
            var currentSize = getFolderSize(directory) / (1024 * 1024) // Convert to MB
            for (file in sortedFiles) {
                if (currentSize <= CACHE_CLEANUP_THRESHOLD_MB) {
                    break
                }
                
                val fileSize = file.length() / (1024 * 1024) // Convert to MB
                if (file.delete()) {
                    currentSize -= fileSize
                    Timber.d("Deleted cache file: ${file.name}, size: $fileSize MB")
                }
            }
        } catch (e: Exception) {
            errorHandler.handleException("MemoryManager.deleteOldCacheFiles", e)
        }
    }
    
    /**
     * Trim the bitmap cache
     */
    private fun trimBitmapCache() {
        try {
            // Get the current process memory info
            val memoryInfo = Debug.MemoryInfo()
            Debug.getMemoryInfo(memoryInfo)
            
            // Check if bitmap memory usage is high
            val bitmapMemory = memoryInfo.getMemoryStat("summary.graphics") ?: "0"
            val bitmapMemoryMb = bitmapMemory.toIntOrNull() ?: 0
            
            if (bitmapMemoryMb > MAX_BITMAP_CACHE_SIZE_MB) {
                Timber.d("Bitmap memory usage: $bitmapMemoryMb MB, trimming cache")
                
                // Trim bitmap memory
                System.gc()
            }
        } catch (e: Exception) {
            errorHandler.handleException("MemoryManager.trimBitmapCache", e)
        }
    }
    
    /**
     * Get the size of a folder in bytes
     */
    private fun getFolderSize(folder: File): Long {
        var size: Long = 0
        
        try {
            val files = folder.listFiles() ?: return 0
            
            for (file in files) {
                size += if (file.isDirectory) {
                    getFolderSize(file)
                } else {
                    file.length()
                }
            }
        } catch (e: Exception) {
            errorHandler.handleException("MemoryManager.getFolderSize", e)
        }
        
        return size
    }
    
    /**
     * Log current memory usage
     */
    private fun logMemoryUsage() {
        try {
            val runtime = Runtime.getRuntime()
            val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
            val freeMemory = runtime.freeMemory() / (1024 * 1024)
            val totalMemory = runtime.totalMemory() / (1024 * 1024)
            val maxMemory = runtime.maxMemory() / (1024 * 1024)
            
            Timber.d("Memory usage: Used: $usedMemory MB, Free: $freeMemory MB, Total: $totalMemory MB, Max: $maxMemory MB")
            
            // Get process memory info
            val pid = Process.myPid()
            val pss = getPss(pid)
            
            Timber.d("Process memory: PSS: $pss MB")
        } catch (e: Exception) {
            errorHandler.handleException("MemoryManager.logMemoryUsage", e)
        }
    }
    
    /**
     * Get the PSS memory usage for a process
     */
    private fun getPss(pid: Int): Int {
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val pInfo = activityManager.getProcessMemoryInfo(intArrayOf(pid))
            return pInfo[0].totalPss / 1024 // Convert to MB
        } catch (e: Exception) {
            errorHandler.handleException("MemoryManager.getPss", e)
            return 0
        }
    }
    
    /**
     * Check if the device is in a low memory state
     */
    fun isLowMemory(): Boolean {
        return isLowMemory
    }
    
    /**
     * Get the available memory percentage
     */
    fun getAvailableMemoryPercentage(): Int {
        return availableMemoryPercentage
    }
}
