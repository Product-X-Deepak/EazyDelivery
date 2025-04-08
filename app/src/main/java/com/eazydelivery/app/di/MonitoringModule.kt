package com.eazydelivery.app.di

import android.content.Context
import com.eazydelivery.app.analytics.AnalyticsManager
import com.eazydelivery.app.monitoring.AppLifecycleObserver
import com.eazydelivery.app.monitoring.CrashReportingManager
import com.eazydelivery.app.monitoring.NetworkMonitoringManager
import com.eazydelivery.app.monitoring.PerformanceMonitoringManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import javax.inject.Singleton

/**
 * Hilt module for providing monitoring components
 */
@Module
@InstallIn(SingletonComponent::class)
object MonitoringModule {
    
    /**
     * Provide analytics manager
     */
    @Provides
    @Singleton
    fun provideAnalyticsManager(
        @ApplicationContext context: Context
    ): AnalyticsManager {
        return AnalyticsManager(context)
    }
    
    /**
     * Provide performance monitoring manager
     */
    @Provides
    @Singleton
    fun providePerformanceMonitoringManager(
        @ApplicationContext context: Context,
        analyticsManager: AnalyticsManager
    ): PerformanceMonitoringManager {
        return PerformanceMonitoringManager(context, analyticsManager)
    }
    
    /**
     * Provide crash reporting manager
     */
    @Provides
    @Singleton
    fun provideCrashReportingManager(
        @ApplicationContext context: Context
    ): CrashReportingManager {
        return CrashReportingManager(context)
    }
    
    /**
     * Provide network monitoring manager
     */
    @Provides
    @Singleton
    fun provideNetworkMonitoringManager(
        @ApplicationContext context: Context,
        analyticsManager: AnalyticsManager,
        performanceMonitoringManager: PerformanceMonitoringManager
    ): NetworkMonitoringManager {
        return NetworkMonitoringManager(context, analyticsManager, performanceMonitoringManager)
    }
    
    /**
     * Provide app lifecycle observer
     */
    @Provides
    @Singleton
    fun provideAppLifecycleObserver(
        @ApplicationContext context: Context,
        analyticsManager: AnalyticsManager,
        performanceMonitoringManager: PerformanceMonitoringManager
    ): AppLifecycleObserver {
        return AppLifecycleObserver(context, analyticsManager, performanceMonitoringManager)
    }
    
    /**
     * Provide network interceptor
     */
    @Provides
    @Singleton
    fun provideNetworkInterceptor(
        networkMonitoringManager: NetworkMonitoringManager
    ): Interceptor {
        return networkMonitoringManager.createNetworkInterceptor()
    }
}
