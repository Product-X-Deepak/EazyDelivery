package com.eazydelivery.app.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eazydelivery.app.R
import com.eazydelivery.app.util.error.AppError
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for the BaseFragment
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class BaseFragmentTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    // Test implementation of BaseFragment
    class TestBaseFragment : BaseFragment() {
        var errorDialogShown = false
        var performanceStatusShown = false
        
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return View(requireContext())
        }
        
        override fun showErrorDialog(
            error: AppError,
            onRecoveryActionListener: ((com.eazydelivery.app.util.error.ErrorRecoveryManager.RecoveryAction) -> Unit)?,
            onDismissListener: (() -> Unit)?
        ) {
            errorDialogShown = true
            super.showErrorDialog(error, onRecoveryActionListener, onDismissListener)
        }
        
        override fun showPerformanceStatus(show: Boolean) {
            performanceStatusShown = show
            super.showPerformanceStatus(show)
        }
    }
    
    @Before
    fun setUp() {
        hiltRule.inject()
    }
    
    @Test
    fun testShowErrorDialog() {
        // Launch the test fragment
        val scenario = launchFragmentInContainer<TestBaseFragment>(
            fragmentArgs = Bundle(),
            themeResId = R.style.Theme_Eazydelivery
        )
        
        // Show error dialog
        scenario.onFragment { fragment ->
            fragment.showErrorDialog(AppError.Network.NoConnection())
        }
        
        // Check that the error dialog was shown
        scenario.onFragment { fragment ->
            assert(fragment.errorDialogShown)
        }
    }
    
    @Test
    fun testShowPerformanceStatus() {
        // Launch the test fragment
        val scenario = launchFragmentInContainer<TestBaseFragment>(
            fragmentArgs = Bundle(),
            themeResId = R.style.Theme_Eazydelivery
        )
        
        // Show performance status
        scenario.onFragment { fragment ->
            fragment.showPerformanceStatus(true)
        }
        
        // Check that the performance status was shown
        scenario.onFragment { fragment ->
            assert(fragment.performanceStatusShown)
        }
    }
    
    @Test
    fun testHidePerformanceStatus() {
        // Launch the test fragment
        val scenario = launchFragmentInContainer<TestBaseFragment>(
            fragmentArgs = Bundle(),
            themeResId = R.style.Theme_Eazydelivery
        )
        
        // Show performance status
        scenario.onFragment { fragment ->
            fragment.showPerformanceStatus(true)
        }
        
        // Check that the performance status was shown
        scenario.onFragment { fragment ->
            assert(fragment.performanceStatusShown)
        }
        
        // Hide performance status
        scenario.onFragment { fragment ->
            fragment.showPerformanceStatus(false)
        }
        
        // Check that the performance status was hidden
        scenario.onFragment { fragment ->
            assert(!fragment.performanceStatusShown)
        }
    }
}
