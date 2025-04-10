package com.eazydelivery.app.accessibility

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.eazydelivery.app.security.SecurePreferencesManager
import com.eazydelivery.app.util.ErrorHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class for handling accessibility features
 */
@Singleton
class AccessibilityManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securePreferencesManager: SecurePreferencesManager,
    private val errorHandler: ErrorHandler
) {
    // System accessibility manager
    private val systemAccessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

    private val accessibilityScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Accessibility state
    private val _accessibilityState = MutableStateFlow(AccessibilityState())
    val accessibilityState: StateFlow<AccessibilityState> = _accessibilityState.asStateFlow()

    // Flag to track if we're initialized
    private val isInitialized = AtomicBoolean(false)

    companion object {
        private const val KEY_FONT_SCALE = "font_scale"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_HIGH_CONTRAST = "high_contrast"
        private const val KEY_REDUCE_MOTION = "reduce_motion"
        private const val KEY_REDUCE_TRANSPARENCY = "reduce_transparency"

        // Theme modes
        const val THEME_MODE_SYSTEM = 0
        const val THEME_MODE_LIGHT = 1
        const val THEME_MODE_DARK = 2

        // Font scales
        val FONT_SCALES = mapOf(
            "small" to 0.85f,
            "normal" to 1.0f,
            "large" to 1.15f,
            "larger" to 1.3f,
            "largest" to 1.5f
        )
    }

    /**
     * Check if a screen reader is enabled
     *
     * @return true if a screen reader is enabled, false otherwise
     */
    fun isScreenReaderEnabled(): Boolean {
        return systemAccessibilityManager.isEnabled &&
               (systemAccessibilityManager.isTouchExplorationEnabled ||
                isScreenReaderRunning())
    }

    /**
     * Check if any accessibility service is running
     *
     * @return true if any accessibility service is running, false otherwise
     */
    fun isAccessibilityServiceEnabled(): Boolean {
        return systemAccessibilityManager.isEnabled
    }

    /**
     * Check if touch exploration is enabled
     *
     * @return true if touch exploration is enabled, false otherwise
     */
    fun isTouchExplorationEnabled(): Boolean {
        return systemAccessibilityManager.isTouchExplorationEnabled
    }

    /**
     * Check if a screen reader is running
     *
     * @return true if a screen reader is running, false otherwise
     */
    private fun isScreenReaderRunning(): Boolean {
        // Check if TalkBack or other screen readers are running
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.contains("talkback") ||
               enabledServices.contains("screenreader") ||
               enabledServices.contains("accessibilityservice")
    }

    /**
     * Check if large text is enabled
     *
     * @return true if large text is enabled, false otherwise
     */
    fun isLargeTextEnabled(): Boolean {
        val fontScale = context.resources.configuration.fontScale
        return fontScale > 1.0f
    }

    /**
     * Check if high contrast is enabled
     *
     * @return true if high contrast is enabled, false otherwise
     */
    fun isHighContrastEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contrast = context.resources.configuration.contrast
            contrast > Configuration.CONTRAST_NORMAL
        } else {
            false
        }
    }

    /**
     * Check if reduce motion is enabled
     *
     * @return true if reduce motion is enabled, false otherwise
     */
    fun isReduceMotionEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val animationScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            animationScale == 0f
        } else {
            false
        }
    }

    /**
     * Announce a message for screen readers
     *
     * @param view The view to announce from
     * @param message The message to announce
     * @param queueMode The queue mode for the announcement
     */
    fun announce(
        view: View,
        message: String,
        queueMode: Int = AccessibilityEvent.TYPE_ANNOUNCEMENT
    ) {
        if (isScreenReaderEnabled()) {
            view.announceForAccessibility(message)
            Timber.d("Announced for accessibility: $message")
        }
    }

    /**
     * Set content description on a view
     *
     * @param view The view to set content description on
     * @param description The content description
     */
    fun setContentDescription(view: View, description: String) {
        view.contentDescription = description
    }

    /**
     * Make a view accessible with custom actions
     *
     * @param view The view to make accessible
     * @param contentDescription The content description
     * @param clickLabel The click action label
     * @param longClickLabel The long click action label
     */
    fun makeAccessible(
        view: View,
        contentDescription: String,
        clickLabel: String? = null,
        longClickLabel: String? = null
    ) {
        // Set content description
        view.contentDescription = contentDescription

        // Set accessibility delegates if needed
        if (clickLabel != null || longClickLabel != null) {
            val delegate = view.accessibilityDelegate ?: View.AccessibilityDelegate()

            view.accessibilityDelegate = object : View.AccessibilityDelegate() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    info: android.view.accessibility.AccessibilityNodeInfo
                ) {
                    delegate.onInitializeAccessibilityNodeInfo(host, info)

                    // Add click action
                    if (clickLabel != null && host.isClickable) {
                        val clickAction = android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction(
                            android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK,
                            clickLabel
                        )
                        info.addAction(clickAction)
                    }

                    // Add long click action
                    if (longClickLabel != null && host.isLongClickable) {
                        val longClickAction = android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction(
                            android.view.accessibility.AccessibilityNodeInfo.ACTION_LONG_CLICK,
                            longClickLabel
                        )
                        info.addAction(longClickAction)
                    }
                }
            }
        }
    }

    /**
     * Get the current font scale
     *
     * @return The font scale
     */
    fun getFontScale(): Float {
        return context.resources.configuration.fontScale
    }

    /**
     * Get the recommended touch target size in pixels
     *
     * @return The recommended touch target size in pixels
     */
    fun getRecommendedTouchTargetSize(): Int {
        // 48dp is the recommended minimum touch target size
        return (48 * context.resources.displayMetrics.density).toInt()
    }

    /**
     * Check if the device is in dark mode
     *
     * @return true if the device is in dark mode, false otherwise
     */
    fun isInDarkMode(): Boolean {
        return when (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }
    }

    /**
     * Initialize the accessibility manager
     */
    fun initialize() {
        if (isInitialized.getAndSet(true)) {
            return
        }

        try {
            Timber.d("Initializing accessibility manager")

            // Load initial state
            loadState()

            Timber.d("Accessibility manager initialized")
        } catch (e: Exception) {
            errorHandler.handleException("AccessibilityManager.initialize", e)
        }
    }

    /**
     * Load accessibility state
     */
    private fun loadState() {
        accessibilityScope.launch {
            try {
                // Get font scale
                val fontScale = securePreferencesManager.getString(KEY_FONT_SCALE) ?: "normal"

                // Get theme mode
                val themeMode = securePreferencesManager.getInt(KEY_THEME_MODE, THEME_MODE_SYSTEM)

                // Get high contrast
                val highContrast = securePreferencesManager.getBoolean(KEY_HIGH_CONTRAST, false)

                // Get reduce motion
                val reduceMotion = securePreferencesManager.getBoolean(KEY_REDUCE_MOTION, false)

                // Get reduce transparency
                val reduceTransparency = securePreferencesManager.getBoolean(KEY_REDUCE_TRANSPARENCY, false)

                // Update state
                _accessibilityState.value = AccessibilityState(
                    fontScale = fontScale,
                    themeMode = themeMode,
                    highContrast = highContrast,
                    reduceMotion = reduceMotion,
                    reduceTransparency = reduceTransparency
                )

                Timber.d("Loaded accessibility state: ${_accessibilityState.value}")
            } catch (e: Exception) {
                errorHandler.handleException("AccessibilityManager.loadState", e)
            }
        }
    }

    /**
     * Set font scale
     */
    fun setFontScale(scale: String) {
        accessibilityScope.launch {
            try {
                // Validate scale
                if (!FONT_SCALES.containsKey(scale)) {
                    Timber.w("Invalid font scale: $scale")
                    return@launch
                }

                // Update state
                val currentState = accessibilityState.value
                val updatedState = currentState.copy(fontScale = scale)
                _accessibilityState.value = updatedState

                // Save to preferences
                securePreferencesManager.putString(KEY_FONT_SCALE, scale)

                Timber.d("Set font scale: $scale")
            } catch (e: Exception) {
                errorHandler.handleException("AccessibilityManager.setFontScale", e)
            }
        }
    }

    /**
     * Set theme mode
     */
    fun setThemeMode(mode: Int) {
        accessibilityScope.launch {
            try {
                // Validate mode
                if (mode !in listOf(THEME_MODE_SYSTEM, THEME_MODE_LIGHT, THEME_MODE_DARK)) {
                    Timber.w("Invalid theme mode: $mode")
                    return@launch
                }

                // Update state
                val currentState = accessibilityState.value
                val updatedState = currentState.copy(themeMode = mode)
                _accessibilityState.value = updatedState

                // Save to preferences
                securePreferencesManager.putInt(KEY_THEME_MODE, mode)

                Timber.d("Set theme mode: $mode")
            } catch (e: Exception) {
                errorHandler.handleException("AccessibilityManager.setThemeMode", e)
            }
        }
    }

    /**
     * Set high contrast
     */
    fun setHighContrast(enabled: Boolean) {
        accessibilityScope.launch {
            try {
                // Update state
                val currentState = accessibilityState.value
                val updatedState = currentState.copy(highContrast = enabled)
                _accessibilityState.value = updatedState

                // Save to preferences
                securePreferencesManager.putBoolean(KEY_HIGH_CONTRAST, enabled)

                Timber.d("Set high contrast: $enabled")
            } catch (e: Exception) {
                errorHandler.handleException("AccessibilityManager.setHighContrast", e)
            }
        }
    }

    /**
     * Set reduce motion
     */
    fun setReduceMotion(enabled: Boolean) {
        accessibilityScope.launch {
            try {
                // Update state
                val currentState = accessibilityState.value
                val updatedState = currentState.copy(reduceMotion = enabled)
                _accessibilityState.value = updatedState

                // Save to preferences
                securePreferencesManager.putBoolean(KEY_REDUCE_MOTION, enabled)

                Timber.d("Set reduce motion: $enabled")
            } catch (e: Exception) {
                errorHandler.handleException("AccessibilityManager.setReduceMotion", e)
            }
        }
    }

    /**
     * Set reduce transparency
     */
    fun setReduceTransparency(enabled: Boolean) {
        accessibilityScope.launch {
            try {
                // Update state
                val currentState = accessibilityState.value
                val updatedState = currentState.copy(reduceTransparency = enabled)
                _accessibilityState.value = updatedState

                // Save to preferences
                securePreferencesManager.putBoolean(KEY_REDUCE_TRANSPARENCY, enabled)

                Timber.d("Set reduce transparency: $enabled")
            } catch (e: Exception) {
                errorHandler.handleException("AccessibilityManager.setReduceTransparency", e)
            }
        }
    }

    /**
     * Get the current font scale factor
     */
    fun getFontScaleFactor(): Float {
        return FONT_SCALES[accessibilityState.value.fontScale] ?: 1.0f
    }

    /**
     * Check if dark theme should be used
     */
    @Composable
    fun isInDarkTheme(): Boolean {
        val accessibilityState by accessibilityState.collectAsState()
        val context = LocalContext.current

        return when (accessibilityState.themeMode) {
            THEME_MODE_LIGHT -> false
            THEME_MODE_DARK -> true
            else -> {
                // Use system setting
                val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                uiMode == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    /**
     * Get the current system font scale
     */
    fun getSystemFontScale(): Float {
        return context.resources.configuration.fontScale
    }
}

/**
 * Accessibility state data class
 */
data class AccessibilityState(
    val fontScale: String = "normal",
    val themeMode: Int = AccessibilityManager.THEME_MODE_SYSTEM,
    val highContrast: Boolean = false,
    val reduceMotion: Boolean = false,
    val reduceTransparency: Boolean = false
)
