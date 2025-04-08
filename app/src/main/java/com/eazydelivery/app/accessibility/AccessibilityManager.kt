package com.eazydelivery.app.accessibility

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class for handling accessibility features
 */
@Singleton
class AccessibilityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // System accessibility manager
    private val systemAccessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    
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
}
