package com.eazydelivery.app.ui.component

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.accessibility.AccessibilityEvent
import com.eazydelivery.app.R
import com.eazydelivery.app.accessibility.AccessibilityUtils
import com.google.android.material.button.MaterialButton

/**
 * A Button with enhanced accessibility features
 */
class AccessibleButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.materialButtonStyle
) : MaterialButton(context, attrs, defStyleAttr) {
    
    private var accessibilityHint: String? = null
    private var accessibilityAction: String? = null
    
    init {
        // Get custom attributes
        val typedArray: TypedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.AccessibleButton,
            defStyleAttr,
            0
        )
        
        try {
            // Get accessibility hint
            accessibilityHint = typedArray.getString(R.styleable.AccessibleButton_accessibilityHint)
            
            // Get accessibility action
            accessibilityAction = typedArray.getString(R.styleable.AccessibleButton_accessibilityAction)
            
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
        // Set accessibility delegate
        AccessibilityUtils.setAccessibilityDelegate(
            view = this,
            customHint = accessibilityHint,
            customActions = if (accessibilityAction != null) {
                listOf(Pair(AccessibilityEvent.TYPE_VIEW_CLICKED, accessibilityAction!!))
            } else {
                null
            }
        )
        
        // Ensure minimum touch target size
        AccessibilityUtils.ensureMinimumTouchTargetSize(this)
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
    
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        
        // Ensure minimum text size for readability
        if (textSize < MIN_TEXT_SIZE_SP * resources.displayMetrics.scaledDensity) {
            textSize = MIN_TEXT_SIZE_SP
        }
    }
    
    companion object {
        private const val MIN_TEXT_SIZE_SP = 14f
    }
}
