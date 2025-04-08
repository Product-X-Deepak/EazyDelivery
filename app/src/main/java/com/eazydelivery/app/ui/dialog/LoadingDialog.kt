package com.eazydelivery.app.ui.dialog

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.eazydelivery.app.R

/**
 * A dialog that shows a loading indicator with an optional message
 */
class LoadingDialog : DialogFragment() {
    
    private var message: String? = null
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setCanceledOnTouchOutside(false)
            setCancelable(false)
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_loading, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Set message if provided
        val messageTextView = view.findViewById<TextView>(R.id.loading_message)
        message?.let {
            messageTextView.text = it
            messageTextView.visibility = View.VISIBLE
        } ?: run {
            messageTextView.visibility = View.GONE
        }
    }
    
    /**
     * Updates the loading message
     * 
     * @param message The new message to display
     */
    fun updateMessage(message: String) {
        this.message = message
        view?.findViewById<TextView>(R.id.loading_message)?.apply {
            text = message
            visibility = View.VISIBLE
        }
    }
    
    companion object {
        /**
         * Creates a new instance of LoadingDialog
         * 
         * @param message Optional message to display
         * @return A new instance of LoadingDialog
         */
        fun newInstance(message: String? = null): LoadingDialog {
            return LoadingDialog().apply {
                this.message = message
            }
        }
    }
}
