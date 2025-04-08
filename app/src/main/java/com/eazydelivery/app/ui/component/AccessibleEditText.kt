package com.eazydelivery.app.ui.component

import android.content.Context
import android.content.res.TypedArray
import android.text.TextUtils
import android.util.AttributeSet
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.eazydelivery.app.R
import com.eazydelivery.app.accessibility.AccessibilityUtils
import com.google.android.material.textfield.TextInputEditText

/**
 * An EditText with enhanced accessibility features
 */
class AccessibleEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.editTextStyle
) : TextInputEditText(context, attrs, defStyleAttr) {
    
    private var accessibilityHint: String? = null
    private var accessibilityAction: String? = null
    private var errorMessage: String? = null
    private var hasError: Boolean = false
    
    init {
        // Get custom attributes
        val typedArray: TypedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.AccessibleEditText,
            defStyleAttr,
            0
        )
        
        try {
            // Get accessibility hint
            accessibilityHint = typedArray.getString(R.styleable.AccessibleEditText_accessibilityHint)
            
            // Get accessibility action
            accessibilityAction = typedArray.getString(R.styleable.AccessibleEditText_accessibilityAction)
            
            // Get error message
            errorMessage = typedArray.getString(R.styleable.AccessibleEditText_errorMessage)
            
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
    
    /**
     * Set error message
     * 
     * @param error The error message
     */
    fun setErrorMessage(error: String?) {
        errorMessage = error
        hasError = !TextUtils.isEmpty(error)
        
        // Update error state
        if (hasError) {
            setError(error)
        } else {
            setError(null)
        }
        
        // Announce error for screen readers
        if (hasError) {
            announceForAccessibility(error)
        }
    }
    
    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        
        // Add error state to accessibility info
        if (hasError && !TextUtils.isEmpty(errorMessage)) {
            info.error = errorMessage
        }
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
