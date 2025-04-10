package com.eazydelivery.app.di

import android.content.Context
import androidx.work.WorkerFactory
import com.eazydelivery.app.worker.EazyDeliveryWorkerFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing WorkManager related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {
    
    /**
     * Provides the WorkerFactory for WorkManager
     * This is required for Hilt to inject dependencies into Workers
     */
    @Provides
    @Singleton
    fun provideWorkerFactory(
        @ApplicationContext context: Context
    ): WorkerFactory {
        return EazyDeliveryWorkerFactory(context)
    }
}
