package com.eazydelivery.app.ui.base

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.eazydelivery.app.accessibility.AccessibilityUtils
import com.eazydelivery.app.accessibility.ThemeManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.eazydelivery.app.R
import com.eazydelivery.app.ui.component.PerformanceStatusView
import com.eazydelivery.app.ui.dialog.ErrorDialog
import com.eazydelivery.app.util.PerformanceStatusManager
import com.eazydelivery.app.util.error.AppError
import com.eazydelivery.app.util.error.ErrorHandler
import com.eazydelivery.app.util.error.ErrorRecoveryManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Base activity that includes error handling and performance status
 */
@AndroidEntryPoint
abstract class BaseActivity : AccessibleActivity() {

    @Inject
    lateinit var performanceStatusManager: PerformanceStatusManager

    @Inject
    lateinit var errorHandler: ErrorHandler

    @Inject
    lateinit var themeManager: ThemeManager

    private var performanceStatusView: PerformanceStatusView? = null
    private var performanceStatusVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme before calling super.onCreate
        themeManager.applyTheme(this)

        super.onCreate(savedInstanceState)

        // Apply edge-to-edge layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, 0, insets.right, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    override fun onContentChanged() {
        super.onContentChanged()

        // Apply additional accessibility improvements specific to BaseActivity
        applyAdditionalAccessibilityImprovements()
    }

    override fun onStart() {
        super.onStart()

        // Create and add performance status view if needed
        if (performanceStatusVisible && performanceStatusView == null) {
            showPerformanceStatus()
        }
    }

    override fun onStop() {
        super.onStop()

        // Stop performance monitoring
        performanceStatusManager.stopMonitoring()
    }

    /**
     * Shows the performance status view
     */
    fun showPerformanceStatus() {
        val rootView = findViewById<ViewGroup>(android.R.id.content)

        // Create performance status view if it doesn't exist
        if (performanceStatusView == null) {
            performanceStatusView = PerformanceStatusView(this).apply {
                id = R.id.performance_status_view
                visibility = View.VISIBLE
                elevation = resources.getDimension(R.dimen.elevation_status)
            }

            // Add to root view with layout params
            val layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = resources.getDimensionPixelSize(R.dimen.status_margin_top)
                marginStart = resources.getDimensionPixelSize(R.dimen.status_margin_horizontal)
                marginEnd = resources.getDimensionPixelSize(R.dimen.status_margin_horizontal)
            }

            rootView.addView(performanceStatusView, layoutParams)

            // Start monitoring
            performanceStatusManager.startMonitoring(performanceStatusView!!)
        }

        performanceStatusVisible = true
        performanceStatusView?.visibility = View.VISIBLE
    }

    /**
     * Hides the performance status view
     */
    fun hidePerformanceStatus() {
        performanceStatusVisible = false
        performanceStatusView?.visibility = View.GONE
        performanceStatusManager.stopMonitoring()
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

        errorDialog.show(supportFragmentManager, "error_dialog")
    }

    /**
     * Apply additional accessibility improvements specific to BaseActivity
     */
    private fun applyAdditionalAccessibilityImprovements() {
        // Make performance status view accessible if it exists
        performanceStatusView?.let { statusView ->
            AccessibilityUtils.setAccessibilityDelegate(
                view = statusView,
                customContentDescription = getString(R.string.performance_status_content_description),
                customHint = getString(R.string.performance_status_hint)
            )
        }

        // Apply accessibility improvements to any error dialogs
        supportFragmentManager.fragments.forEach { fragment ->
            if (fragment is ErrorDialog) {
                fragment.view?.let { dialogView ->
                    AccessibilityUtils.applyAccessibilityImprovements(dialogView)
                }
            }
        }
    }
}
