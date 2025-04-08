package com.eazydelivery.app.data.repository

import com.eazydelivery.app.domain.repository.BaseRepository
import com.eazydelivery.app.domain.repository.UserPreferencesRepository
import com.eazydelivery.app.util.ErrorHandler
import com.eazydelivery.app.util.SecureStorage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    private val secureStorage: SecureStorage,
    override val errorHandler: ErrorHandler
) : UserPreferencesRepository, BaseRepository {
    
    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_AUTO_START_ENABLED = "auto_start_enabled"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_TERMS_ACCEPTED = "terms_accepted"
        
        // Order prioritization preferences
        private const val KEY_EARNINGS_WEIGHT = "earnings_weight"
        private const val KEY_DISTANCE_WEIGHT = "distance_weight"
        private const val KEY_TIME_WEIGHT = "time_weight"
        private const val KEY_ACCEPT_MEDIUM_PRIORITY = "accept_medium_priority"
    }
    
    override suspend fun isOnboardingCompleted(): Result<Boolean> = safeDbCall("UserPreferencesRepository.isOnboardingCompleted") {
        secureStorage.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }
    
    override suspend fun setOnboardingCompleted(completed: Boolean): Result<Unit> = safeDbCall("UserPreferencesRepository.setOnboardingCompleted") {
        secureStorage.saveBoolean(KEY_ONBOARDING_COMPLETED, completed)
    }
    
    override suspend fun getThemeMode(): Result<Int> = safeDbCall("UserPreferencesRepository.getThemeMode") {
        secureStorage.getInt(KEY_THEME_MODE, 0) // 0 = System default, 1 = Light, 2 = Dark
    }
    
    override suspend fun setThemeMode(mode: Int): Result<Unit> = safeDbCall("UserPreferencesRepository.setThemeMode") {
        secureStorage.saveInt(KEY_THEME_MODE, mode)
    }
    
    override suspend fun getNotificationsEnabled(): Result<Boolean> = safeDbCall("UserPreferencesRepository.getNotificationsEnabled") {
        secureStorage.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }
    
    override suspend fun setNotificationsEnabled(enabled: Boolean): Result<Unit> = safeDbCall("UserPreferencesRepository.setNotificationsEnabled") {
        secureStorage.saveBoolean(KEY_NOTIFICATIONS_ENABLED, enabled)
    }
    
    override suspend fun getAutoStartEnabled(): Result<Boolean> = safeDbCall("UserPreferencesRepository.getAutoStartEnabled") {
        secureStorage.getBoolean(KEY_AUTO_START_ENABLED, true)
    }
    
    override suspend fun setAutoStartEnabled(enabled: Boolean): Result<Unit> = safeDbCall("UserPreferencesRepository.setAutoStartEnabled") {
        secureStorage.saveBoolean(KEY_AUTO_START_ENABLED, enabled)
    }
    
    override suspend fun getLanguage(): Result<String> = safeDbCall("UserPreferencesRepository.getLanguage") {
        secureStorage.getString(KEY_LANGUAGE, "en") // Default to English
    }
    
    override suspend fun setLanguage(languageCode: String): Result<Unit> = safeDbCall("UserPreferencesRepository.setLanguage") {
        secureStorage.saveString(KEY_LANGUAGE, languageCode)
    }
    
    override suspend fun hasAcceptedTerms(): Result<Boolean> = safeDbCall("UserPreferencesRepository.hasAcceptedTerms") {
        secureStorage.getBoolean(KEY_TERMS_ACCEPTED, false)
    }
    
    override suspend fun setTermsAccepted(accepted: Boolean): Result<Unit> = safeDbCall("UserPreferencesRepository.setTermsAccepted") {
        secureStorage.saveBoolean(KEY_TERMS_ACCEPTED, accepted)
    }
    
    // Order prioritization preferences
    
    override suspend fun getEarningsWeight(): Result<Float> = safeDbCall("UserPreferencesRepository.getEarningsWeight") {
        secureStorage.getFloat(KEY_EARNINGS_WEIGHT, 0.5f)
    }
    
    override suspend fun setEarningsWeight(weight: Float): Result<Unit> = safeDbCall("UserPreferencesRepository.setEarningsWeight") {
        secureStorage.saveFloat(KEY_EARNINGS_WEIGHT, weight)
    }
    
    override suspend fun getDistanceWeight(): Result<Float> = safeDbCall("UserPreferencesRepository.getDistanceWeight") {
        secureStorage.getFloat(KEY_DISTANCE_WEIGHT, 0.3f)
    }
    
    override suspend fun setDistanceWeight(weight: Float): Result<Unit> = safeDbCall("UserPreferencesRepository.setDistanceWeight") {
        secureStorage.saveFloat(KEY_DISTANCE_WEIGHT, weight)
    }
    
    override suspend fun getTimeWeight(): Result<Float> = safeDbCall("UserPreferencesRepository.getTimeWeight") {
        secureStorage.getFloat(KEY_TIME_WEIGHT, 0.2f)
    }
    
    override suspend fun setTimeWeight(weight: Float): Result<Unit> = safeDbCall("UserPreferencesRepository.setTimeWeight") {
        secureStorage.saveFloat(KEY_TIME_WEIGHT, weight)
    }
    
    override suspend fun getAcceptMediumPriority(): Result<Boolean> = safeDbCall("UserPreferencesRepository.getAcceptMediumPriority") {
        secureStorage.getBoolean(KEY_ACCEPT_MEDIUM_PRIORITY, false)
    }
    
    override suspend fun setAcceptMediumPriority(accept: Boolean): Result<Unit> = safeDbCall("UserPreferencesRepository.setAcceptMediumPriority") {
        secureStorage.saveBoolean(KEY_ACCEPT_MEDIUM_PRIORITY, accept)
    }
}
