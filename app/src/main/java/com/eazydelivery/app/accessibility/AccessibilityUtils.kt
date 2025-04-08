package com.eazydelivery.app.accessibility

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.eazydelivery.app.R
import com.google.android.material.button.MaterialButton
import timber.log.Timber

/**
 * Utility class for accessibility-related functions
 */
object AccessibilityUtils {
    
    /**
     * Apply accessibility improvements to a view hierarchy
     * 
     * @param rootView The root view of the hierarchy
     */
    fun applyAccessibilityImprovements(rootView: View) {
        if (rootView is ViewGroup) {
            // Process all children
            for (i in 0 until rootView.childCount) {
                val child = rootView.getChildAt(i)
                
                // Apply improvements to this child
                improveAccessibility(child)
                
                // Recursively process if it's a ViewGroup
                if (child is ViewGroup) {
                    applyAccessibilityImprovements(child)
                }
            }
        } else {
            // Apply improvements to the root view
            improveAccessibility(rootView)
        }
    }
    
    /**
     * Improve accessibility for a specific view
     * 
     * @param view The view to improve
     */
    private fun improveAccessibility(view: View) {
        when (view) {
            is Button, is MaterialButton -> {
                // Ensure buttons have content descriptions
                if (view.contentDescription == null && view is TextView && !view.text.isNullOrEmpty()) {
                    view.contentDescription = view.text
                }
                
                // Ensure buttons have minimum touch target size
                ensureMinimumTouchTargetSize(view)
            }
            is ImageView -> {
                // Ensure images have content descriptions or are marked as decorative
                if (view.contentDescription == null) {
                    view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }
            }
            is TextView -> {
                // Ensure text has sufficient contrast
                // This is a simplified check - in a real app, you'd use a contrast ratio calculator
                if (view.textColors.defaultColor == 0) {
                    view.setTextColor(ContextCompat.getColor(view.context, R.color.text_primary))
                }
            }
        }
    }
    
    /**
     * Ensure a view has the minimum recommended touch target size
     * 
     * @param view The view to check
     */
    fun ensureMinimumTouchTargetSize(view: View) {
        val minSize = (48 * view.resources.displayMetrics.density).toInt() // 48dp
        
        if (view.minimumWidth < minSize || view.minimumHeight < minSize) {
            view.minimumWidth = minSize
            view.minimumHeight = minSize
            Timber.d("Adjusted touch target size for view: ${view.javaClass.simpleName}")
        }
    }
    
    /**
     * Create an accessible spannable string with different styles
     * 
     * @param context The context
     * @param text The text to style
     * @param highlightedParts Parts of the text to highlight
     * @param highlightColor The color to use for highlighting
     * @param isBold Whether the highlighted parts should be bold
     * @param sizeMultiplier Size multiplier for the highlighted parts
     * @return A spannable string with the specified styling
     */
    fun createAccessibleSpannable(
        context: Context,
        text: String,
        highlightedParts: List<String>,
        highlightColor: Int = R.color.primary,
        isBold: Boolean = true,
        sizeMultiplier: Float = 1.0f
    ): SpannableString {
        val spannable = SpannableString(text)
        
        for (part in highlightedParts) {
            val startIndex = text.indexOf(part)
            if (startIndex >= 0) {
                val endIndex = startIndex + part.length
                
                // Apply color
                spannable.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(context, highlightColor)),
                    startIndex,
                    endIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                
                // Apply bold if needed
                if (isBold) {
                    spannable.setSpan(
                        StyleSpan(Typeface.BOLD),
                        startIndex,
                        endIndex,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                
                // Apply size if needed
                if (sizeMultiplier != 1.0f) {
                    spannable.setSpan(
                        RelativeSizeSpan(sizeMultiplier),
                        startIndex,
                        endIndex,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }
        
        return spannable
    }
    
    /**
     * Set a custom accessibility delegate on a view
     * 
     * @param view The view to set the delegate on
     * @param customContentDescription Custom content description
     * @param customHint Custom hint
     * @param customActions List of custom actions
     */
    fun setAccessibilityDelegate(
        view: View,
        customContentDescription: String? = null,
        customHint: String? = null,
        customActions: List<Pair<Int, String>>? = null
    ) {
        ViewCompat.setAccessibilityDelegate(view, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                
                // Set custom content description if provided
                if (customContentDescription != null) {
                    info.contentDescription = customContentDescription
                }
                
                // Set custom hint if provided
                if (customHint != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        info.hintText = customHint
                    } else {
                        // For older versions, append to content description
                        val currentDescription = info.contentDescription ?: ""
                        info.contentDescription = "$currentDescription. $customHint"
                    }
                }
                
                // Add custom actions if provided
                customActions?.forEach { (actionId, label) ->
                    info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat(actionId, label))
                }
            }
        })
    }
    
    /**
     * Create a high contrast color state list
     * 
     * @param context The context
     * @param normalColorRes The normal color resource
     * @param disabledColorRes The disabled color resource
     * @return A color state list with high contrast colors
     */
    fun createHighContrastColorStateList(
        context: Context,
        normalColorRes: Int,
        disabledColorRes: Int
    ): ColorStateList {
        val states = arrayOf(
            intArrayOf(-android.R.attr.state_enabled),
            intArrayOf()
        )
        
        val colors = intArrayOf(
            ContextCompat.getColor(context, disabledColorRes),
            ContextCompat.getColor(context, normalColorRes)
        )
        
        return ColorStateList(states, colors)
    }
    
    /**
     * Get a color with sufficient contrast for a background color
     * 
     * @param context The context
     * @param backgroundColor The background color
     * @return A color with sufficient contrast
     */
    fun getAccessibleTextColor(context: Context, backgroundColor: Int): Int {
        // Calculate luminance (simplified)
        val red = android.graphics.Color.red(backgroundColor) / 255.0
        val green = android.graphics.Color.green(backgroundColor) / 255.0
        val blue = android.graphics.Color.blue(backgroundColor) / 255.0
        
        val luminance = 0.2126 * red + 0.7152 * green + 0.0722 * blue
        
        // Use white text on dark backgrounds, black text on light backgrounds
        return if (luminance < 0.5) {
            ContextCompat.getColor(context, android.R.color.white)
        } else {
            ContextCompat.getColor(context, android.R.color.black)
        }
    }
}
