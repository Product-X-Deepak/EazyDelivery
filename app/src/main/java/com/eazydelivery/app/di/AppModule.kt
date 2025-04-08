package com.eazydelivery.app.di

import android.content.Context
import androidx.room.Room
import com.eazydelivery.app.data.local.AppDatabase
import com.eazydelivery.app.data.local.dao.FeedbackDao
import com.eazydelivery.app.data.local.dao.OrderNotificationDao
import com.eazydelivery.app.data.repository.AnalyticsRepositoryImpl
import com.eazydelivery.app.data.repository.AuthRepositoryImpl
import com.eazydelivery.app.data.repository.EmailRepositoryImpl
import com.eazydelivery.app.data.repository.FeedbackRepositoryImpl
import com.eazydelivery.app.data.repository.PermissionRepositoryImpl
import com.eazydelivery.app.data.repository.PlatformRepositoryImpl
import com.eazydelivery.app.data.repository.ServiceRepositoryImpl
import com.eazydelivery.app.data.repository.SubscriptionRepositoryImpl
import com.eazydelivery.app.data.repository.UserPreferencesRepositoryImpl
import com.eazydelivery.app.domain.repository.AnalyticsRepository
import com.eazydelivery.app.domain.repository.AuthRepository
import com.eazydelivery.app.domain.repository.EmailRepository
import com.eazydelivery.app.domain.repository.FeedbackRepository
import com.eazydelivery.app.domain.repository.PermissionRepository
import com.eazydelivery.app.domain.repository.PlatformRepository
import com.eazydelivery.app.domain.repository.ServiceRepository
import com.eazydelivery.app.domain.repository.SubscriptionRepository
import com.eazydelivery.app.domain.repository.UserPreferencesRepository
import com.eazydelivery.app.ml.NotificationClassifier
import com.eazydelivery.app.ml.OrderPrioritizationEngine
import com.eazydelivery.app.ml.ScreenAnalyzer
import com.eazydelivery.app.util.CacheManager
import com.eazydelivery.app.util.ConnectivityManager
import com.eazydelivery.app.util.ErrorHandler
import com.eazydelivery.app.util.NotificationParser
import com.eazydelivery.app.util.ScreenshotUtil
import com.eazydelivery.app.util.BiometricManager
import com.eazydelivery.app.util.KeystoreManager
import com.eazydelivery.app.util.LocalizationHelper
import com.eazydelivery.app.util.SecurityManager
import com.eazydelivery.app.util.SecureStorage
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "eazydelivery_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun providePlatformDao(database: AppDatabase) = database.platformDao()

    @Provides
    @Singleton
    fun provideOrderDao(database: AppDatabase) = database.orderDao()

    @Provides
    @Singleton
    fun provideOrderNotificationDao(database: AppDatabase) = database.orderNotificationDao()

    @Provides
    @Singleton
    fun provideFeedbackDao(database: AppDatabase) = database.feedbackDao()

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth

    @Provides
    @Singleton
    fun provideFirebaseAnalytics(@ApplicationContext context: Context): FirebaseAnalytics {
        return Firebase.analytics
    }

    @Provides
    @Singleton
    fun provideErrorHandler(): ErrorHandler {
        return ErrorHandler()
    }

    @Provides
    @Singleton
    fun provideConnectivityManager(@ApplicationContext context: Context): ConnectivityManager {
        return ConnectivityManager(context)
    }

    @Provides
    @Singleton
    fun provideSecurityManager(@ApplicationContext context: Context): SecurityManager {
        return SecurityManager(context)
    }

    @Provides
    @Singleton
    fun provideSecureStorage(
        @ApplicationContext context: Context,
        securityManager: SecurityManager
    ): SecureStorage {
        return SecureStorage(context, securityManager)
    }

    @Provides
    @Singleton
    fun provideKeystoreManager(
        @ApplicationContext context: Context,
        secureStorage: SecureStorage
    ): KeystoreManager {
        return KeystoreManager(context, secureStorage)
    }

    @Provides
    @Singleton
    fun provideAdminContactSettings(
        secureStorage: SecureStorage
    ): AdminContactSettings {
        return AdminContactSettings(secureStorage)
    }

    @Provides
    @Singleton
    fun provideLocalizationHelper(
        @ApplicationContext context: Context,
        secureStorage: SecureStorage
    ): LocalizationHelper {
        return LocalizationHelper(context, secureStorage)
    }

    @Provides
    @Singleton
    fun provideBiometricManager(
        @ApplicationContext context: Context,
        secureStorage: SecureStorage,
        errorHandler: ErrorHandler
    ): BiometricManager {
        return BiometricManager(context, secureStorage, errorHandler)
    }

    @Provides
    @Singleton
    fun provideNotificationParser(): NotificationParser {
        return NotificationParser()
    }

    @Provides
    @Singleton
    fun provideScreenshotUtil(
        @ApplicationContext context: Context,
        errorHandler: ErrorHandler
    ): ScreenshotUtil {
        return ScreenshotUtil(context, errorHandler)
    }

    @Provides
    @Singleton
    fun provideScreenAnalyzer(
        @ApplicationContext context: Context,
        errorHandler: ErrorHandler
    ): ScreenAnalyzer {
        return ScreenAnalyzer(context, errorHandler)
    }

    @Provides
    @Singleton
    fun provideNotificationClassifier(
        @ApplicationContext context: Context,
        analyticsRepository: AnalyticsRepository,
        errorHandler: ErrorHandler
    ): NotificationClassifier {
        return NotificationClassifier(context, analyticsRepository, errorHandler)
    }

    @Provides
    @Singleton
    fun provideOrderPrioritizationEngine(
        @ApplicationContext context: Context,
        userPreferencesRepository: UserPreferencesRepository,
        errorHandler: ErrorHandler
    ): OrderPrioritizationEngine {
        return OrderPrioritizationEngine(context, userPreferencesRepository, errorHandler)
    }

    @Provides
    @Singleton
    fun provideCacheManager(
        @ApplicationContext context: Context,
        secureStorage: SecureStorage,
        errorHandler: ErrorHandler
    ): CacheManager {
        return CacheManager(context, secureStorage, errorHandler)
    }

    @Provides
    @Singleton
    fun providePlatformRepository(platformRepositoryImpl: PlatformRepositoryImpl): PlatformRepository {
        return platformRepositoryImpl
    }

    @Provides
    @Singleton
    fun provideAnalyticsRepository(analyticsRepositoryImpl: AnalyticsRepositoryImpl): AnalyticsRepository {
        return analyticsRepositoryImpl
    }

    @Provides
    @Singleton
    fun provideServiceRepository(serviceRepositoryImpl: ServiceRepositoryImpl): ServiceRepository {
        return serviceRepositoryImpl
    }

    @Provides
    @Singleton
    fun provideAuthRepository(authRepositoryImpl: AuthRepositoryImpl): AuthRepository {
        return authRepositoryImpl
    }

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(userPreferencesRepositoryImpl: UserPreferencesRepositoryImpl): UserPreferencesRepository {
        return userPreferencesRepositoryImpl
    }

    @Provides
    @Singleton
    fun provideEmailRepository(emailRepositoryImpl: EmailRepositoryImpl): EmailRepository {
        return emailRepositoryImpl
    }

    @Provides
    @Singleton
    fun providePermissionRepository(permissionRepositoryImpl: PermissionRepositoryImpl): PermissionRepository {
        return permissionRepositoryImpl
    }

    @Provides
    @Singleton
    fun provideSubscriptionRepository(subscriptionRepositoryImpl: SubscriptionRepositoryImpl): SubscriptionRepository {
        return subscriptionRepositoryImpl
    }

    @Provides
    @Singleton
    fun provideFeedbackRepository(feedbackRepositoryImpl: FeedbackRepositoryImpl): FeedbackRepository {
        return feedbackRepositoryImpl
    }
}
