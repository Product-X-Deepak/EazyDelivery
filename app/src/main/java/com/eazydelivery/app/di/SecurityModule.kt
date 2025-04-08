package com.eazydelivery.app.di

import android.content.Context
import com.eazydelivery.app.security.BiometricAuthManager
import com.eazydelivery.app.security.CertificatePinner
import com.eazydelivery.app.security.PinAuthManager
import com.eazydelivery.app.security.SecureNetworkInterceptor
import com.eazydelivery.app.security.SecurePreferencesManager
import com.eazydelivery.app.security.SecurityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import javax.inject.Singleton

/**
 * Hilt module for providing security components
 */
@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {
    
    /**
     * Provide security manager
     */
    @Provides
    @Singleton
    fun provideSecurityManager(
        @ApplicationContext context: Context
    ): SecurityManager {
        return SecurityManager(context)
    }
    
    /**
     * Provide secure preferences manager
     */
    @Provides
    @Singleton
    fun provideSecurePreferencesManager(
        @ApplicationContext context: Context,
        securityManager: SecurityManager
    ): SecurePreferencesManager {
        return SecurePreferencesManager(context, securityManager)
    }
    
    /**
     * Provide biometric authentication manager
     */
    @Provides
    @Singleton
    fun provideBiometricAuthManager(
        @ApplicationContext context: Context,
        securePreferencesManager: SecurePreferencesManager
    ): BiometricAuthManager {
        return BiometricAuthManager(context, securePreferencesManager)
    }
    
    /**
     * Provide PIN authentication manager
     */
    @Provides
    @Singleton
    fun providePinAuthManager(
        @ApplicationContext context: Context,
        securityManager: SecurityManager,
        securePreferencesManager: SecurePreferencesManager
    ): PinAuthManager {
        return PinAuthManager(context, securityManager, securePreferencesManager)
    }
    
    /**
     * Provide certificate pinner
     */
    @Provides
    @Singleton
    fun provideCertificatePinner(): CertificatePinner {
        return CertificatePinner()
    }
    
    /**
     * Provide secure network interceptor
     */
    @Provides
    @Singleton
    fun provideSecureNetworkInterceptor(
        securityManager: SecurityManager
    ): SecureNetworkInterceptor {
        return SecureNetworkInterceptor(securityManager)
    }
    
    /**
     * Provide OkHttp certificate pinner
     */
    @Provides
    @Singleton
    fun provideOkHttpCertificatePinner(
        certificatePinner: CertificatePinner
    ): okhttp3.CertificatePinner {
        return certificatePinner.createCertificatePinner()
    }
    
    /**
     * Provide secure network interceptor as an OkHttp interceptor
     */
    @Provides
    @Singleton
    fun provideSecureNetworkInterceptorAsOkHttpInterceptor(
        secureNetworkInterceptor: SecureNetworkInterceptor
    ): Interceptor {
        return secureNetworkInterceptor
    }
}
