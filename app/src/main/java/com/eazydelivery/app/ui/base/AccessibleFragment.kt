package com.eazydelivery.app.ui.base

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import androidx.fragment.app.Fragment
import com.eazydelivery.app.accessibility.AccessibilityManager
import com.eazydelivery.app.accessibility.AccessibilityUtils
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Base fragment with accessibility features
 */
@AndroidEntryPoint
abstract class AccessibleFragment : Fragment() {
    
    @Inject
    lateinit var accessibilityManager: AccessibilityManager
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Apply accessibility improvements to the view hierarchy
        AccessibilityUtils.applyAccessibilityImprovements(view)
    }
    
    override fun onResume() {
        super.onResume()
        
        // Announce fragment for screen readers
        announceFragmentForAccessibility()
    }
    
    /**
     * Announce the fragment for accessibility
     */
    protected open fun announceFragmentForAccessibility() {
        if (accessibilityManager.isScreenReaderEnabled()) {
            val fragmentName = getFragmentNameForAccessibility()
            view?.let { rootView ->
                rootView.postDelayed({
                    rootView.announceForAccessibility(fragmentName)
                    sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
                    Timber.d("Announced fragment for accessibility: $fragmentName")
                }, 500) // Slight delay to ensure the fragment is fully loaded
            }
        }
    }
    
    /**
     * Get the fragment name for accessibility announcements
     * 
     * @return The fragment name
     */
    protected open fun getFragmentNameForAccessibility(): String {
        return javaClass.simpleName
    }
    
    /**
     * Send an accessibility event
     * 
     * @param eventType The event type
     */
    protected fun sendAccessibilityEvent(eventType: Int) {
        view?.sendAccessibilityEvent(eventType)
    }
    
    /**
     * Announce a message for screen readers
     * 
     * @param message The message to announce
     */
    protected fun announce(message: String) {
        view?.let { rootView ->
            accessibilityManager.announce(rootView, message)
        }
    }
    
    /**
     * Make a view accessible with custom content description and actions
     * 
     * @param view The view to make accessible
     * @param contentDescription The content description
     * @param hint The accessibility hint
     * @param actions Custom accessibility actions
     */
    protected fun makeAccessible(
        view: View,
        contentDescription: String,
        hint: String? = null,
        actions: List<Pair<Int, String>>? = null
    ) {
        AccessibilityUtils.setAccessibilityDelegate(
            view = view,
            customContentDescription = contentDescription,
            customHint = hint,
            customActions = actions
        )
    }
}
