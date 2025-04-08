package com.eazydelivery.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.eazydelivery.app.service.ServiceMonitor
import com.eazydelivery.app.util.ErrorHandler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Worker that periodically checks if services are running and restarts them if necessary
 */
@HiltWorker
class ServiceMonitorWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val serviceMonitor: ServiceMonitor,
    private val errorHandler: ErrorHandler
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Timber.d("ServiceMonitorWorker: Starting service check")
            
            // Check services and restart if necessary
            serviceMonitor.checkServices()
            
            Timber.d("ServiceMonitorWorker: Service check completed")
            
            Result.success()
        } catch (e: Exception) {
            errorHandler.handleException("ServiceMonitorWorker.doWork", e)
            Result.retry()
        }
    }
}
