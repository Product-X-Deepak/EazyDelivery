package com.eazydelivery.app.ui.dialog

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialog
import com.eazydelivery.app.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Dialog for setting up a PIN
 */
class PinSetupDialog(
    context: Context,
    private val isChangingPin: Boolean = false,
    private val onPinSet: (String) -> Unit,
    private val onCancel: () -> Unit
) : AppCompatDialog(context) {
    
    private lateinit var titleText: TextView
    private lateinit var messageText: TextView
    private lateinit var pinInput: EditText
    private lateinit var confirmPinInput: EditText
    private lateinit var errorText: TextView
    private lateinit var nextButton: Button
    private lateinit var cancelButton: Button
    
    private var currentStep = 1
    private var firstPin = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set up the dialog
        setContentView(R.layout.dialog_pin_setup)
        
        // Find views
        titleText = findViewById(R.id.title_text)!!
        messageText = findViewById(R.id.message_text)!!
        pinInput = findViewById(R.id.pin_input)!!
        confirmPinInput = findViewById(R.id.confirm_pin_input)!!
        errorText = findViewById(R.id.error_text)!!
        nextButton = findViewById(R.id.next_button)!!
        cancelButton = findViewById(R.id.cancel_button)!!
        
        // Set up initial state
        updateUI()
        
        // Set up listeners
        setupListeners()
        
        // Show keyboard
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }
    
    /**
     * Set up listeners
     */
    private fun setupListeners() {
        // PIN input text watcher
        pinInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                validateInput()
            }
        })
        
        // Confirm PIN input text watcher
        confirmPinInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                validateInput()
            }
        })
        
        // Next button click listener
        nextButton.setOnClickListener {
            if (currentStep == 1) {
                // Save the first PIN and move to the next step
                firstPin = pinInput.text.toString()
                currentStep = 2
                updateUI()
            } else {
                // Validate the second PIN
                val secondPin = confirmPinInput.text.toString()
                if (secondPin == firstPin) {
                    // PINs match, set the PIN
                    onPinSet(firstPin)
                    dismiss()
                } else {
                    // PINs don't match, show error
                    errorText.text = context.getString(R.string.pin_mismatch)
                    errorText.visibility = View.VISIBLE
                }
            }
        }
        
        // Cancel button click listener
        cancelButton.setOnClickListener {
            onCancel()
            dismiss()
        }
    }
    
    /**
     * Update the UI based on the current step
     */
    private fun updateUI() {
        if (currentStep == 1) {
            // First step: Enter PIN
            titleText.text = context.getString(if (isChangingPin) R.string.change_pin_title else R.string.set_pin_title)
            messageText.text = context.getString(R.string.enter_pin_message)
            pinInput.visibility = View.VISIBLE
            confirmPinInput.visibility = View.GONE
            pinInput.text.clear()
            pinInput.requestFocus()
            nextButton.isEnabled = false
            nextButton.text = context.getString(R.string.next)
        } else {
            // Second step: Confirm PIN
            titleText.text = context.getString(R.string.confirm_pin_title)
            messageText.text = context.getString(R.string.confirm_pin_message)
            pinInput.visibility = View.GONE
            confirmPinInput.visibility = View.VISIBLE
            confirmPinInput.text.clear()
            confirmPinInput.requestFocus()
            nextButton.isEnabled = false
            nextButton.text = context.getString(R.string.confirm)
        }
        
        // Clear error
        errorText.visibility = View.GONE
    }
    
    /**
     * Validate the input
     */
    private fun validateInput() {
        if (currentStep == 1) {
            val pin = pinInput.text.toString()
            
            try {
                // Validate PIN
                validatePin(pin)
                
                // PIN is valid
                errorText.visibility = View.GONE
                nextButton.isEnabled = true
            } catch (e: IllegalArgumentException) {
                // PIN is invalid
                errorText.text = e.message
                errorText.visibility = View.VISIBLE
                nextButton.isEnabled = false
            }
        } else {
            // Enable the next button if the confirm PIN is not empty
            nextButton.isEnabled = confirmPinInput.text.isNotEmpty()
        }
    }
    
    /**
     * Validate a PIN
     * 
     * @param pin The PIN to validate
     * @throws IllegalArgumentException if the PIN is invalid
     */
    private fun validatePin(pin: String) {
        // Check PIN length
        if (pin.isEmpty()) {
            throw IllegalArgumentException(context.getString(R.string.pin_empty))
        }
        
        if (pin.length < 4 || pin.length > 8) {
            throw IllegalArgumentException(context.getString(R.string.pin_length_invalid))
        }
        
        // Check if PIN contains only digits
        if (!pin.all { it.isDigit() }) {
            throw IllegalArgumentException(context.getString(R.string.pin_digits_only))
        }
        
        // Check for sequential digits
        for (i in 0 until pin.length - 2) {
            val a = pin[i].digitToInt()
            val b = pin[i + 1].digitToInt()
            val c = pin[i + 2].digitToInt()
            
            if ((a + 1 == b && b + 1 == c) || (a - 1 == b && b - 1 == c)) {
                throw IllegalArgumentException(context.getString(R.string.pin_sequential_digits))
            }
        }
        
        // Check for repeated digits
        if (pin.groupBy { it }.any { it.value.size >= 3 }) {
            throw IllegalArgumentException(context.getString(R.string.pin_repeated_digits))
        }
    }
}
