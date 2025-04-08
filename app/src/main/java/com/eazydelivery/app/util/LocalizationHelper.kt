package com.eazydelivery.app.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for handling localization and language settings
 */
@Singleton
class LocalizationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureStorage: SecureStorage
) {
    companion object {
        private const val LANGUAGE_PREF_KEY = "app_language"
        
        // List of supported languages
        val SUPPORTED_LANGUAGES = mapOf(
            "en" to "English",
            "hi" to "हिन्दी (Hindi)",
            "ta" to "தமிழ் (Tamil)",
            "te" to "తెలుగు (Telugu)",
            "kn" to "ಕನ್ನಡ (Kannada)",
            "ml" to "മലയാളം (Malayalam)",
            "mr" to "मराठी (Marathi)",
            "bn" to "বাংলা (Bengali)",
            "gu" to "ગુજરાતી (Gujarati)",
            "pa" to "ਪੰਜਾਬੀ (Punjabi)"
        )
    }
    
    /**
     * Gets the current app language
     * @return The current language code (e.g., "en", "hi")
     */
    fun getCurrentLanguage(): String {
        // First try to get from storage
        val storedLanguage = secureStorage.getString(LANGUAGE_PREF_KEY, "")
        if (storedLanguage.isNotEmpty() && SUPPORTED_LANGUAGES.containsKey(storedLanguage)) {
            return storedLanguage
        }
        
        // If not stored, get from system
        val systemLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Resources.getSystem().configuration.locales.get(0)
        } else {
            Resources.getSystem().configuration.locale
        }
        
        // Check if system language is supported
        val systemLanguage = systemLocale.language
        return if (SUPPORTED_LANGUAGES.containsKey(systemLanguage)) {
            systemLanguage
        } else {
            // Default to English if not supported
            "en"
        }
    }
    
    /**
     * Sets the app language
     * @param languageCode The language code to set (e.g., "en", "hi")
     * @return Context with updated configuration
     */
    fun setAppLanguage(languageCode: String): Context {
        // Validate language code
        if (!SUPPORTED_LANGUAGES.containsKey(languageCode)) {
            return context
        }
        
        // Save to storage
        secureStorage.saveString(LANGUAGE_PREF_KEY, languageCode)
        
        // Update configuration
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val localeList = LocaleList(locale)
            config.setLocales(localeList)
        } else {
            config.locale = locale
        }
        
        return context.createConfigurationContext(config)
    }
    
    /**
     * Gets a list of supported languages
     * @return Map of language codes to language names
     */
    fun getSupportedLanguages(): Map<String, String> {
        return SUPPORTED_LANGUAGES
    }
    
    /**
     * Gets the display name for a language code
     * @param languageCode The language code
     * @return The display name for the language
     */
    fun getLanguageDisplayName(languageCode: String): String {
        return SUPPORTED_LANGUAGES[languageCode] ?: "Unknown"
    }
}
