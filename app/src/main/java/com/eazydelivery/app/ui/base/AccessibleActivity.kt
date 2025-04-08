package com.eazydelivery.app.ui.base

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import androidx.appcompat.app.AppCompatActivity
import com.eazydelivery.app.accessibility.AccessibilityManager
import com.eazydelivery.app.accessibility.AccessibilityUtils
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Base activity with accessibility features
 */
@AndroidEntryPoint
abstract class AccessibleActivity : AppCompatActivity() {
    
    @Inject
    lateinit var accessibilityManager: AccessibilityManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Log accessibility services status
        logAccessibilityStatus()
    }
    
    override fun onContentChanged() {
        super.onContentChanged()
        
        // Apply accessibility improvements to the view hierarchy
        val rootView = findViewById<ViewGroup>(android.R.id.content)
        AccessibilityUtils.applyAccessibilityImprovements(rootView)
    }
    
    override fun onResume() {
        super.onResume()
        
        // Announce screen for screen readers
        announceScreenForAccessibility()
    }
    
    /**
     * Log the status of accessibility services
     */
    private fun logAccessibilityStatus() {
        val isScreenReaderEnabled = accessibilityManager.isScreenReaderEnabled()
        val isTouchExplorationEnabled = accessibilityManager.isTouchExplorationEnabled()
        val isLargeTextEnabled = accessibilityManager.isLargeTextEnabled()
        val isHighContrastEnabled = accessibilityManager.isHighContrastEnabled()
        val isReduceMotionEnabled = accessibilityManager.isReduceMotionEnabled()
        val fontScale = accessibilityManager.getFontScale()
        
        Timber.d(
            "Accessibility status: " +
            "Screen reader: $isScreenReaderEnabled, " +
            "Touch exploration: $isTouchExplorationEnabled, " +
            "Large text: $isLargeTextEnabled (scale: $fontScale), " +
            "High contrast: $isHighContrastEnabled, " +
            "Reduce motion: $isReduceMotionEnabled"
        )
    }
    
    /**
     * Announce the screen for accessibility
     */
    protected open fun announceScreenForAccessibility() {
        if (accessibilityManager.isScreenReaderEnabled()) {
            val screenName = getScreenNameForAccessibility()
            val rootView = findViewById<View>(android.R.id.content)
            
            rootView.postDelayed({
                rootView.announceForAccessibility(screenName)
                sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
                Timber.d("Announced screen for accessibility: $screenName")
            }, 500) // Slight delay to ensure the screen is fully loaded
        }
    }
    
    /**
     * Get the screen name for accessibility announcements
     * 
     * @return The screen name
     */
    protected open fun getScreenNameForAccessibility(): String {
        return title?.toString() ?: javaClass.simpleName
    }
    
    /**
     * Send an accessibility event
     * 
     * @param eventType The event type
     */
    protected fun sendAccessibilityEvent(eventType: Int) {
        val rootView = findViewById<View>(android.R.id.content)
        rootView.sendAccessibilityEvent(eventType)
    }
    
    /**
     * Announce a message for screen readers
     * 
     * @param message The message to announce
     */
    protected fun announce(message: String) {
        val rootView = findViewById<View>(android.R.id.content)
        accessibilityManager.announce(rootView, message)
    }
}
