package com.eazydelivery.app.ui.component

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import android.view.accessibility.AccessibilityEvent
import androidx.appcompat.widget.AppCompatImageView
import com.eazydelivery.app.R
import com.eazydelivery.app.accessibility.AccessibilityUtils

/**
 * An ImageView with enhanced accessibility features
 */
class AccessibleImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {
    
    private var accessibilityHint: String? = null
    private var accessibilityAction: String? = null
    private var isDecorative: Boolean = false
    
    init {
        // Get custom attributes
        val typedArray: TypedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.AccessibleImageView,
            defStyleAttr,
            0
        )
        
        try {
            // Get accessibility hint
            accessibilityHint = typedArray.getString(R.styleable.AccessibleImageView_accessibilityHint)
            
            // Get accessibility action
            accessibilityAction = typedArray.getString(R.styleable.AccessibleImageView_accessibilityAction)
            
            // Get decorative flag
            isDecorative = typedArray.getBoolean(R.styleable.AccessibleImageView_isDecorative, false)
            
            // Apply accessibility improvements
            applyAccessibilityImprovements()
        } finally {
            typedArray.recycle()
        }
    }
    
    /**
     * Apply accessibility improvements
     */
    private fun applyAccessibilityImprovements() {
        if (isDecorative) {
            // If the image is decorative, hide it from accessibility services
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        } else if (contentDescription == null && accessibilityHint != null) {
            // If no content description is set, use the accessibility hint as a fallback
            contentDescription = accessibilityHint
        }
        
        // Set accessibility delegate for additional features
        if (!isDecorative) {
            AccessibilityUtils.setAccessibilityDelegate(
                view = this,
                customHint = accessibilityHint,
                customActions = if (accessibilityAction != null) {
                    listOf(Pair(AccessibilityEvent.TYPE_VIEW_CLICKED, accessibilityAction!!))
                } else {
                    null
                }
            )
        }
        
        // Ensure minimum touch target size if clickable
        if (isClickable || isLongClickable) {
            AccessibilityUtils.ensureMinimumTouchTargetSize(this)
        }
    }
    
    /**
     * Set whether this image is decorative
     * 
     * @param decorative Whether the image is decorative
     */
    fun setDecorative(decorative: Boolean) {
        isDecorative = decorative
        applyAccessibilityImprovements()
    }
    
    /**
     * Set accessibility hint
     * 
     * @param hint The accessibility hint
     */
    fun setAccessibilityHint(hint: String?) {
        accessibilityHint = hint
        applyAccessibilityImprovements()
    }
    
    /**
     * Set accessibility action
     * 
     * @param action The accessibility action
     */
    fun setAccessibilityAction(action: String?) {
        accessibilityAction = action
        applyAccessibilityImprovements()
    }
    
    override fun setContentDescription(contentDescription: CharSequence?) {
        super.setContentDescription(contentDescription)
        
        // Update accessibility status when content description changes
        if (contentDescription != null) {
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        } else if (isDecorative) {
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
    }
    
    override fun setClickable(clickable: Boolean) {
        super.setClickable(clickable)
        
        // Update touch target size when clickable changes
        if (clickable) {
            AccessibilityUtils.ensureMinimumTouchTargetSize(this)
        }
    }
}
