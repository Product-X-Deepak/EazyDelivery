package com.eazydelivery.app.ui.base

import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eazydelivery.app.R
import com.eazydelivery.app.ui.component.PerformanceStatusView
import com.eazydelivery.app.ui.dialog.ErrorDialog
import com.eazydelivery.app.util.PerformanceStatusManager
import com.eazydelivery.app.util.error.AppError
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import javax.inject.Inject

/**
 * Instrumented test for the BaseActivity
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class BaseActivityTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var performanceStatusManager: PerformanceStatusManager
    
    // Test implementation of BaseActivity
    class TestBaseActivity : BaseActivity() {
        var errorDialogShown = false
        
        override fun showErrorDialog(
            error: AppError,
            onRecoveryActionListener: ((com.eazydelivery.app.util.error.ErrorRecoveryManager.RecoveryAction) -> Unit)?,
            onDismissListener: (() -> Unit)?
        ) {
            errorDialogShown = true
            super.showErrorDialog(error, onRecoveryActionListener, onDismissListener)
        }
    }
    
    @Before
    fun setUp() {
        hiltRule.inject()
    }
    
    @Test
    fun testShowPerformanceStatus() {
        // Launch the test activity
        val scenario = ActivityScenario.launch(TestBaseActivity::class.java)
        
        // Initially, performance status view should not be visible
        scenario.onActivity { activity ->
            val rootView = activity.findViewById<View>(android.R.id.content)
            val performanceStatusView = rootView.findViewById<PerformanceStatusView>(R.id.performance_status_view)
            assert(performanceStatusView == null || performanceStatusView.visibility == View.GONE)
        }
        
        // Show performance status
        scenario.onActivity { activity ->
            activity.showPerformanceStatus()
        }
        
        // Now, performance status view should be visible
        scenario.onActivity { activity ->
            val rootView = activity.findViewById<View>(android.R.id.content)
            val performanceStatusView = rootView.findViewById<PerformanceStatusView>(R.id.performance_status_view)
            assert(performanceStatusView != null && performanceStatusView.visibility == View.VISIBLE)
        }
    }
    
    @Test
    fun testHidePerformanceStatus() {
        // Launch the test activity
        val scenario = ActivityScenario.launch(TestBaseActivity::class.java)
        
        // Show performance status
        scenario.onActivity { activity ->
            activity.showPerformanceStatus()
        }
        
        // Now, performance status view should be visible
        scenario.onActivity { activity ->
            val rootView = activity.findViewById<View>(android.R.id.content)
            val performanceStatusView = rootView.findViewById<PerformanceStatusView>(R.id.performance_status_view)
            assert(performanceStatusView != null && performanceStatusView.visibility == View.VISIBLE)
        }
        
        // Hide performance status
        scenario.onActivity { activity ->
            activity.hidePerformanceStatus()
        }
        
        // Now, performance status view should be gone
        scenario.onActivity { activity ->
            val rootView = activity.findViewById<View>(android.R.id.content)
            val performanceStatusView = rootView.findViewById<PerformanceStatusView>(R.id.performance_status_view)
            assert(performanceStatusView == null || performanceStatusView.visibility == View.GONE)
        }
    }
    
    @Test
    fun testShowErrorDialog() {
        // Launch the test activity
        val scenario = ActivityScenario.launch(TestBaseActivity::class.java)
        
        // Show error dialog
        scenario.onActivity { activity ->
            activity.showErrorDialog(AppError.Network.NoConnection())
        }
        
        // Check that the error dialog was shown
        scenario.onActivity { activity ->
            assert(activity.errorDialogShown)
        }
    }
}
