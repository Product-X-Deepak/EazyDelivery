package com.eazydelivery.app.initializer

import android.content.Context
import androidx.startup.Initializer
import androidx.work.Configuration
import androidx.work.WorkManager
import com.eazydelivery.app.BuildConfig
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

/**
 * Initializes WorkManager using AndroidX Startup
 */
class WorkManagerInitializer : Initializer<WorkManager> {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkManagerInitializerEntryPoint {
        fun workerFactory(): androidx.work.WorkerFactory
    }

    override fun create(context: Context): WorkManager {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WorkManagerInitializerEntryPoint::class.java
        )

        val configuration = Configuration.Builder()
            .setWorkerFactory(entryPoint.workerFactory())
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.INFO)
            .build()

        WorkManager.initialize(context, configuration)
        return WorkManager.getInstance(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        // No dependencies on other libraries
        return emptyList()
    }
}
