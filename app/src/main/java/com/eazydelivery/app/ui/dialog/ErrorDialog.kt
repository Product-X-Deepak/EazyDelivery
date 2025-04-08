package com.eazydelivery.app.ui.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.eazydelivery.app.R
import com.eazydelivery.app.util.error.AppError
import com.eazydelivery.app.util.error.ErrorMessageProvider
import com.eazydelivery.app.util.error.ErrorRecoveryManager
import com.eazydelivery.app.util.error.ErrorRecoveryManager.RecoveryAction
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * A user-friendly error dialog that provides clear error messages and recovery options.
 */
@AndroidEntryPoint
class ErrorDialog : DialogFragment() {
    
    @Inject
    lateinit var errorMessageProvider: ErrorMessageProvider
    
    @Inject
    lateinit var errorRecoveryManager: ErrorRecoveryManager
    
    private var error: AppError? = null
    private var onRecoveryActionListener: ((RecoveryAction) -> Unit)? = null
    private var onDismissListener: (() -> Unit)? = null
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setCanceledOnTouchOutside(true)
            setCancelable(true)
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_error, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get views
        val iconView = view.findViewById<ImageView>(R.id.error_icon)
        val titleView = view.findViewById<TextView>(R.id.error_title)
        val messageView = view.findViewById<TextView>(R.id.error_message)
        val suggestionView = view.findViewById<TextView>(R.id.error_suggestion)
        val primaryButton = view.findViewById<Button>(R.id.primary_action_button)
        val secondaryButton = view.findViewById<Button>(R.id.secondary_action_button)
        
        // Set error icon based on error type
        error?.let { appError ->
            // Set icon based on error type
            val iconRes = when (appError) {
                is AppError.Network -> R.drawable.ic_error_network
                is AppError.Api -> R.drawable.ic_error_api
                is AppError.Database -> R.drawable.ic_error_database
                is AppError.Permission -> R.drawable.ic_error_permission
                is AppError.Feature -> R.drawable.ic_error_feature
                is AppError.Unexpected -> R.drawable.ic_error_unexpected
            }
            
            iconView.setImageResource(iconRes)
            
            // Set title based on error type
            val title = when (appError) {
                is AppError.Network -> getString(R.string.error_title_network)
                is AppError.Api -> getString(R.string.error_title_api)
                is AppError.Database -> getString(R.string.error_title_database)
                is AppError.Permission -> getString(R.string.error_title_permission)
                is AppError.Feature -> getString(R.string.error_title_feature)
                is AppError.Unexpected -> getString(R.string.error_title_unexpected)
            }
            
            titleView.text = title
            
            // Set message and suggestion
            messageView.text = errorMessageProvider.getMessage(appError)
            val suggestion = errorMessageProvider.getSuggestion(appError)
            
            if (suggestion.isNotBlank()) {
                suggestionView.text = suggestion
                suggestionView.visibility = View.VISIBLE
            } else {
                suggestionView.visibility = View.GONE
            }
            
            // Set recovery action buttons
            val recoveryAction = errorRecoveryManager.getRecoveryAction(appError)
            
            if (recoveryAction != null) {
                // Set primary button to recovery action
                primaryButton.text = getRecoveryActionText(recoveryAction)
                primaryButton.setOnClickListener {
                    onRecoveryActionListener?.invoke(recoveryAction)
                    dismiss()
                }
                primaryButton.visibility = View.VISIBLE
                
                // Set secondary button to dismiss
                secondaryButton.text = getString(R.string.action_dismiss)
                secondaryButton.setOnClickListener {
                    dismiss()
                }
                secondaryButton.visibility = View.VISIBLE
            } else {
                // No recovery action, just show dismiss button
                primaryButton.visibility = View.GONE
                
                secondaryButton.text = getString(R.string.action_dismiss)
                secondaryButton.setOnClickListener {
                    dismiss()
                }
                secondaryButton.visibility = View.VISIBLE
            }
        }
    }
    
    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissListener?.invoke()
    }
    
    private fun getRecoveryActionText(recoveryAction: RecoveryAction): String {
        return when (recoveryAction) {
            RecoveryAction.Retry -> getString(R.string.action_retry)
            RecoveryAction.RestartApp -> getString(R.string.action_restart_app)
            RecoveryAction.UpdateApp -> getString(R.string.action_update_app)
            RecoveryAction.OpenAppSettings -> getString(R.string.action_open_settings)
            RecoveryAction.OpenNetworkSettings -> getString(R.string.action_open_network_settings)
            RecoveryAction.RequestPermission -> getString(R.string.action_grant_permission)
            RecoveryAction.Relogin -> getString(R.string.action_login_again)
            RecoveryAction.Upgrade -> getString(R.string.action_upgrade)
        }
    }
    
    companion object {
        /**
         * Creates a new instance of ErrorDialog.
         *
         * @param error The error to display
         * @param onRecoveryActionListener Listener for recovery actions
         * @param onDismissListener Listener for dialog dismissal
         * @return A new instance of ErrorDialog
         */
        fun newInstance(
            error: AppError,
            onRecoveryActionListener: ((RecoveryAction) -> Unit)? = null,
            onDismissListener: (() -> Unit)? = null
        ): ErrorDialog {
            return ErrorDialog().apply {
                this.error = error
                this.onRecoveryActionListener = onRecoveryActionListener
                this.onDismissListener = onDismissListener
            }
        }
    }
}
