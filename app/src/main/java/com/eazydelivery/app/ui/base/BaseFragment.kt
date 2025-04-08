package com.eazydelivery.app.ui.base

import android.os.Bundle
import android.view.View
import com.eazydelivery.app.accessibility.AccessibilityUtils
import com.eazydelivery.app.ui.dialog.ErrorDialog
import com.eazydelivery.app.util.error.AppError
import com.eazydelivery.app.util.error.ErrorRecoveryManager
import dagger.hilt.android.AndroidEntryPoint

/**
 * Base fragment that includes error handling
 */
@AndroidEntryPoint
abstract class BaseFragment : AccessibleFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Apply edge-to-edge layout
        view.setOnApplyWindowInsetsListener { v, insets ->
            val systemInsets = insets.systemWindowInsets
            v.setPadding(systemInsets.left, 0, systemInsets.right, systemInsets.bottom)
            insets
        }

        // Apply additional accessibility improvements specific to BaseFragment
        applyAdditionalAccessibilityImprovements(view)
    }

    /**
     * Shows an error dialog for the given error
     *
     * @param error The error to show
     * @param onRecoveryActionListener Listener for recovery actions
     * @param onDismissListener Listener for dialog dismissal
     */
    fun showErrorDialog(
        error: AppError,
        onRecoveryActionListener: ((ErrorRecoveryManager.RecoveryAction) -> Unit)? = null,
        onDismissListener: (() -> Unit)? = null
    ) {
        val errorDialog = ErrorDialog.newInstance(
            error = error,
            onRecoveryActionListener = onRecoveryActionListener,
            onDismissListener = onDismissListener
        )

        errorDialog.show(childFragmentManager, "error_dialog")
    }

    /**
     * Shows or hides the performance status view in the parent activity
     *
     * @param show Whether to show or hide the performance status view
     */
    fun showPerformanceStatus(show: Boolean) {
        val baseActivity = activity as? BaseActivity
        if (show) {
            baseActivity?.showPerformanceStatus()
        } else {
            baseActivity?.hidePerformanceStatus()
        }
    }

    /**
     * Apply additional accessibility improvements specific to BaseFragment
     *
     * @param rootView The root view of the fragment
     */
    private fun applyAdditionalAccessibilityImprovements(rootView: View) {
        // Apply accessibility improvements to any error dialogs
        childFragmentManager.fragments.forEach { fragment ->
            if (fragment is ErrorDialog) {
                fragment.view?.let { dialogView ->
                    AccessibilityUtils.applyAccessibilityImprovements(dialogView)
                }
            }
        }

        // Add any additional fragment-specific accessibility improvements here
    }
}
