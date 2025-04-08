package com.eazydelivery.app.di

import android.content.Context
import com.eazydelivery.app.accessibility.AccessibilityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing accessibility components
 */
@Module
@InstallIn(SingletonComponent::class)
object AccessibilityModule {
    
    /**
     * Provide accessibility manager
     */
    @Provides
    @Singleton
    fun provideAccessibilityManager(
        @ApplicationContext context: Context
    ): AccessibilityManager {
        return AccessibilityManager(context)
    }
}
