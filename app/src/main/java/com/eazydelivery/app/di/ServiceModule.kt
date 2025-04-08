package com.eazydelivery.app.di

import android.content.Context
import com.eazydelivery.app.domain.repository.ServiceRepository
import com.eazydelivery.app.service.ServiceMonitor
import com.eazydelivery.app.service.ServiceOptimizer
import com.eazydelivery.app.util.ErrorHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing service-related components
 */
@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {
    
    /**
     * Provide service monitor
     */
    @Provides
    @Singleton
    fun provideServiceMonitor(
        @ApplicationContext context: Context,
        serviceRepository: ServiceRepository,
        errorHandler: ErrorHandler
    ): ServiceMonitor {
        return ServiceMonitor(context, serviceRepository, errorHandler)
    }
    
    /**
     * Provide service optimizer
     */
    @Provides
    @Singleton
    fun provideServiceOptimizer(
        @ApplicationContext context: Context,
        errorHandler: ErrorHandler
    ): ServiceOptimizer {
        return ServiceOptimizer(context, errorHandler)
    }
}
