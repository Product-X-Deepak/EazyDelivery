package com.eazydelivery.app.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Custom WorkerFactory for creating Workers with dependencies
 * This factory is used by WorkManager to create Worker instances
 */
@Singleton
class EazyDeliveryWorkerFactory @Inject constructor(
    private val context: Context
) : WorkerFactory() {
    
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        Timber.d("Creating worker: $workerClassName")
        
        return try {
            // Get the Worker class from the class name
            val workerClass = Class.forName(workerClassName).asSubclass(ListenableWorker::class.java)
            
            // Create an instance of the Worker
            when {
                // Handle specific worker types that need custom initialization
                workerClassName.contains("CacheCleanupWorker") -> {
                    CacheCleanupWorker(appContext, workerParameters)
                }
                workerClassName.contains("ServiceMonitorWorker") -> {
                    ServiceMonitorWorker(appContext, workerParameters)
                }
                workerClassName.contains("DataSyncWorker") -> {
                    DataSyncWorker(appContext, workerParameters)
                }
                // For other workers, use the default constructor
                else -> {
                    val constructor = workerClass.getDeclaredConstructor(
                        Context::class.java,
                        WorkerParameters::class.java
                    )
                    constructor.newInstance(appContext, workerParameters)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error creating worker: $workerClassName")
            // Return null to let WorkManager's default factory handle it
            null
        }
    }
}
