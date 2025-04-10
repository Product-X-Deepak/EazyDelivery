package com.eazydelivery.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.eazydelivery.app.util.CacheCleanupUtil
import com.eazydelivery.app.util.ErrorHandler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Worker that performs periodic cleanup of cache files
 */
@HiltWorker
class CacheCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val errorHandler: ErrorHandler
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        private const val TAG = "CacheCleanupWorker"
        private const val TEMP_FILES_MAX_AGE_HOURS = 24
        private const val LOG_FILES_MAX_AGE_DAYS = 7
        private const val NOTIFICATIONS_MAX_AGE_DAYS = 30
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Timber.d("Starting cache cleanup")
            
            // Clean up temporary files
            cleanupTemporaryFiles()
            
            // Clean up old log files
            cleanupOldLogs()
            
            // Clean up old database records
            cleanupOldDatabaseRecords()
            
            Timber.d("Cache cleanup completed successfully")
            
            Result.success()
        } catch (e: Exception) {
            errorHandler.handleException(TAG, e)
            Result.retry()
        }
    }
    
    /**
     * Cleans up temporary screenshot files
     */
    private fun cleanupTemporaryFiles() {
        // Clean up temporary screenshot files
        val deletedCount = CacheCleanupUtil.cleanupTemporaryFiles(
            context = applicationContext,
            subDir = "temp_screenshots",
            maxAgeHours = TEMP_FILES_MAX_AGE_HOURS,
            errorHandler = errorHandler
        )
        
        if (deletedCount > 0) {
            Timber.d("Cleaned up $deletedCount temporary files")
        }
        
        // Clean up general cache files
        val cacheDir = applicationContext.cacheDir
        val externalCacheDir = applicationContext.externalCacheDir
        
        if (cacheDir != null && cacheDir.exists()) {
            val cacheDeletedCount = CacheCleanupUtil.cleanupOldFiles(
                directory = cacheDir,
                maxAgeMillis = TimeUnit.DAYS.toMillis(1),
                errorHandler = errorHandler
            )
            
            if (cacheDeletedCount > 0) {
                Timber.d("Cleaned up $cacheDeletedCount cache files")
            }
        }
        
        if (externalCacheDir != null && externalCacheDir.exists()) {
            val externalCacheDeletedCount = CacheCleanupUtil.cleanupOldFiles(
                directory = externalCacheDir,
                maxAgeMillis = TimeUnit.DAYS.toMillis(1),
                errorHandler = errorHandler
            )
            
            if (externalCacheDeletedCount > 0) {
                Timber.d("Cleaned up $externalCacheDeletedCount external cache files")
            }
        }
    }
    
    /**
     * Cleans up old log files
     */
    private fun cleanupOldLogs() {
        val logsDir = File(applicationContext.filesDir, "logs")
        val maxAgeMillis = TimeUnit.DAYS.toMillis(LOG_FILES_MAX_AGE_DAYS)
        
        if (logsDir.exists()) {
            val deletedCount = CacheCleanupUtil.cleanupOldFiles(
                directory = logsDir,
                maxAgeMillis = maxAgeMillis,
                errorHandler = errorHandler
            )
            
            if (deletedCount > 0) {
                Timber.d("Cleaned up $deletedCount old log files")
            }
        }
    }
    
    /**
     * Cleans up old database records
     * Note: This is a placeholder. In a real implementation, you would inject the database
     * and delete old records directly.
     */
    private fun cleanupOldDatabaseRecords() {
        // This would typically involve deleting old records from the database
        // For example:
        // val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(NOTIFICATIONS_MAX_AGE_DAYS)
        // val count = database.orderNotificationDao().deleteOldNotifications(cutoffTime)
        
        Timber.d("Database cleanup would happen here")
    }
}
