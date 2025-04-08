package com.eazydelivery.app.accessibility

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import com.eazydelivery.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class for handling theme-related functionality
 */
@Singleton
class ThemeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "theme_preferences"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_HIGH_CONTRAST = "high_contrast_enabled"
        
        const val THEME_MODE_SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        const val THEME_MODE_LIGHT = AppCompatDelegate.MODE_NIGHT_NO
        const val THEME_MODE_DARK = AppCompatDelegate.MODE_NIGHT_YES
    }
    
    // Shared preferences for storing theme settings
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Initialize the theme manager
     */
    fun initialize() {
        // Set the theme mode
        val themeMode = getThemeMode()
        AppCompatDelegate.setDefaultNightMode(themeMode)
        
        Timber.d("Theme manager initialized with mode: $themeMode, high contrast: ${isHighContrastEnabled()}")
    }
    
    /**
     * Get the current theme mode
     * 
     * @return The theme mode
     */
    fun getThemeMode(): Int {
        return sharedPreferences.getInt(KEY_THEME_MODE, THEME_MODE_SYSTEM)
    }
    
    /**
     * Set the theme mode
     * 
     * @param themeMode The theme mode to set
     */
    fun setThemeMode(themeMode: Int) {
        // Save the theme mode
        sharedPreferences.edit().putInt(KEY_THEME_MODE, themeMode).apply()
        
        // Apply the theme mode
        AppCompatDelegate.setDefaultNightMode(themeMode)
        
        Timber.d("Theme mode set to: $themeMode")
    }
    
    /**
     * Check if high contrast mode is enabled
     * 
     * @return true if high contrast mode is enabled, false otherwise
     */
    fun isHighContrastEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_HIGH_CONTRAST, false)
    }
    
    /**
     * Set high contrast mode
     * 
     * @param enabled Whether high contrast mode is enabled
     */
    fun setHighContrastEnabled(enabled: Boolean) {
        // Save the high contrast setting
        sharedPreferences.edit().putBoolean(KEY_HIGH_CONTRAST, enabled).apply()
        
        Timber.d("High contrast mode ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Apply the current theme to an activity
     * 
     * @param activity The activity to apply the theme to
     */
    fun applyTheme(activity: Activity) {
        // Apply high contrast theme if enabled
        if (isHighContrastEnabled()) {
            activity.setTheme(R.style.Theme_EazyDelivery_HighContrast)
            Timber.d("Applied high contrast theme to ${activity.javaClass.simpleName}")
        }
    }
    
    /**
     * Get the theme resource ID for the current theme
     * 
     * @return The theme resource ID
     */
    fun getThemeResId(): Int {
        return if (isHighContrastEnabled()) {
            R.style.Theme_EazyDelivery_HighContrast
        } else {
            R.style.Theme_EazyDelivery
        }
    }
}
