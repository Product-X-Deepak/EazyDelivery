package com.eazydelivery.app.di

import android.content.Context
import com.eazydelivery.app.ml.MLModelManager
import com.eazydelivery.app.util.ErrorHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing ML-related components
 */
@Module
@InstallIn(SingletonComponent::class)
object MLModule {
    
    /**
     * Provide ML model manager
     */
    @Provides
    @Singleton
    fun provideMLModelManager(
        @ApplicationContext context: Context,
        errorHandler: ErrorHandler
    ): MLModelManager {
        return MLModelManager(context, errorHandler)
    }
}
