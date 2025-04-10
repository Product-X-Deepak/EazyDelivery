package com.eazydelivery.app.util

import android.content.Context
import android.os.Build
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.os.Process
import androidx.annotation.RequiresApi
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Utility class for performance optimization and monitoring
 */
object PerformanceUtils {
    private val backgroundExecutor = Executors.newCachedThreadPool()
    private val mainHandler = Handler(Looper.getMainLooper())
    
    /**
     * Runs a task in the background to avoid blocking the main thread
     * 
     * @param task The task to run in the background
     * @param onComplete Optional callback to run on the main thread when the task completes
     */
    fun runInBackground(task: () -> Unit, onComplete: (() -> Unit)? = null) {
        backgroundExecutor.submit {
            try {
                task()
                onComplete?.let { callback ->
                    mainHandler.post { callback() }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error in background task")
            }
        }
    }
    
    /**
     * Gets the current memory usage information
     * 
     * @return A string with memory usage details
     */
    fun getMemoryInfo(): String {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val totalMemory = runtime.totalMemory() / (1024 * 1024)
        val maxMemory = runtime.maxMemory() / (1024 * 1024)
        
        return "Memory: $usedMemory MB used / $totalMemory MB total / $maxMemory MB max"
    }
    
    /**
     * Suggests a garbage collection
     * Note: This doesn't guarantee GC will run, it's just a suggestion to the runtime
     */
    fun suggestGC() {
        System.gc()
        Runtime.getRuntime().gc()
    }
    
    /**
     * Captures a heap dump for debugging memory issues
     * Only available on API 28+
     * 
     * @param context Application context
     * @return The file where the heap dump was saved, or null if it failed
     */
    @RequiresApi(Build.VERSION_CODES.P)
    fun captureHeapDump(context: Context): File? {
        return try {
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val timestamp = dateFormat.format(Date())
            val heapDumpFile = File(context.filesDir, "heap_dump_$timestamp.hprof")
            
            Debug.dumpHprofData(heapDumpFile.absolutePath)
            Timber.d("Heap dump saved to ${heapDumpFile.absolutePath}")
            heapDumpFile
        } catch (e: Exception) {
            Timber.e(e, "Failed to capture heap dump")
            null
        }
    }
    
    /**
     * Sets the current thread's priority to background
     * Useful for long-running operations that shouldn't impact UI responsiveness
     */
    fun setThreadPriorityBackground() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
    }
    
    /**
     * Sets the current thread's priority back to normal
     */
    fun resetThreadPriority() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT)
    }
}
