package com.eazydelivery.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.eazydelivery.app.util.BackgroundTaskManager
import com.eazydelivery.app.util.ErrorHandler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Worker that periodically optimizes background tasks
 */
@HiltWorker
class OptimizationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val backgroundTaskManager: BackgroundTaskManager,
    private val errorHandler: ErrorHandler
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Timber.d("OptimizationWorker: Starting optimization")
            
            // Optimize background tasks
            backgroundTaskManager.optimizeBackgroundTasks()
            
            Timber.d("OptimizationWorker: Optimization completed")
            
            Result.success()
        } catch (e: Exception) {
            errorHandler.handleException("OptimizationWorker.doWork", e)
            Result.retry()
        }
    }
}
