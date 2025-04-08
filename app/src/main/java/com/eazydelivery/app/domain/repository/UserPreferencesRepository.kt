package com.eazydelivery.app.domain.repository

interface UserPreferencesRepository {
    suspend fun isOnboardingCompleted(): Result<Boolean>
    suspend fun setOnboardingCompleted(completed: Boolean): Result<Unit>
    suspend fun getThemeMode(): Result<Int>
    suspend fun setThemeMode(mode: Int): Result<Unit>
    suspend fun getNotificationsEnabled(): Result<Boolean>
    suspend fun setNotificationsEnabled(enabled: Boolean): Result<Unit>
    suspend fun getAutoStartEnabled(): Result<Boolean>
    suspend fun setAutoStartEnabled(enabled: Boolean): Result<Unit>
    suspend fun getLanguage(): Result<String>
    suspend fun setLanguage(languageCode: String): Result<Unit>
    suspend fun hasAcceptedTerms(): Result<Boolean>
    suspend fun setTermsAccepted(accepted: Boolean): Result<Unit>
    
    // New methods for order prioritization
    suspend fun getEarningsWeight(): Result<Float>
    suspend fun setEarningsWeight(weight: Float): Result<Unit>
    suspend fun getDistanceWeight(): Result<Float>
    suspend fun setDistanceWeight(weight: Float): Result<Unit>
    suspend fun getTimeWeight(): Result<Float>
    suspend fun setTimeWeight(weight: Float): Result<Unit>
    suspend fun getAcceptMediumPriority(): Result<Boolean>
    suspend fun setAcceptMediumPriority(accept: Boolean): Result<Unit>
}
