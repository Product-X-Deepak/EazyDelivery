package com.eazydelivery.app.ui.dialog

import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eazydelivery.app.R
import com.eazydelivery.app.util.error.AppError
import com.eazydelivery.app.util.error.ErrorMessageProvider
import com.eazydelivery.app.util.error.ErrorRecoveryManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import javax.inject.Inject

/**
 * Instrumented test for the ErrorDialog
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ErrorDialogTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var errorMessageProvider: ErrorMessageProvider
    
    @Inject
    lateinit var errorRecoveryManager: ErrorRecoveryManager
    
    @Before
    fun setUp() {
        hiltRule.inject()
    }
    
    @Test
    fun testErrorDialogDisplaysCorrectly() {
        // Create a network error
        val error = AppError.Network.NoConnection()
        
        // Mock the error message provider
        val mockErrorMessageProvider = mock(ErrorMessageProvider::class.java)
        `when`(mockErrorMessageProvider.getMessage(error)).thenReturn("No internet connection")
        `when`(mockErrorMessageProvider.getSuggestion(error)).thenReturn("Check your connection")
        
        // Mock the error recovery manager
        val mockErrorRecoveryManager = mock(ErrorRecoveryManager::class.java)
        `when`(mockErrorRecoveryManager.getRecoveryAction(error))
            .thenReturn(ErrorRecoveryManager.RecoveryAction.OpenNetworkSettings)
        
        // Create the dialog
        val dialog = ErrorDialog.newInstance(error)
        
        // Set the mocked dependencies
        dialog.errorMessageProvider = mockErrorMessageProvider
        dialog.errorRecoveryManager = mockErrorRecoveryManager
        
        // Launch the dialog
        launchFragmentInContainer<ErrorDialog>(
            fragmentArgs = Bundle().apply {
                // No arguments needed as we're setting the error directly
            },
            themeResId = R.style.Theme_Eazydelivery
        ).onFragment { fragment ->
            // Set the error directly
            fragment.error = error
        }
        
        // Check that the dialog is displayed
        onView(withId(R.id.error_title)).check(matches(isDisplayed()))
        onView(withId(R.id.error_message)).check(matches(isDisplayed()))
        onView(withId(R.id.error_suggestion)).check(matches(isDisplayed()))
        onView(withId(R.id.primary_action_button)).check(matches(isDisplayed()))
        onView(withId(R.id.secondary_action_button)).check(matches(isDisplayed()))
        
        // Check the content
        onView(withId(R.id.error_title)).check(matches(withText(R.string.error_title_network)))
        onView(withId(R.id.error_message)).check(matches(withText("No internet connection")))
        onView(withId(R.id.error_suggestion)).check(matches(withText("Check your connection")))
        onView(withId(R.id.primary_action_button)).check(matches(withText(R.string.action_open_network_settings)))
        onView(withId(R.id.secondary_action_button)).check(matches(withText(R.string.action_dismiss)))
    }
    
    @Test
    fun testErrorDialogWithNoRecoveryAction() {
        // Create an unexpected error
        val error = AppError.Unexpected("Something went wrong")
        
        // Mock the error message provider
        val mockErrorMessageProvider = mock(ErrorMessageProvider::class.java)
        `when`(mockErrorMessageProvider.getMessage(error)).thenReturn("Something went wrong")
        `when`(mockErrorMessageProvider.getSuggestion(error)).thenReturn("")
        
        // Mock the error recovery manager
        val mockErrorRecoveryManager = mock(ErrorRecoveryManager::class.java)
        `when`(mockErrorRecoveryManager.getRecoveryAction(error)).thenReturn(null)
        
        // Create the dialog
        val dialog = ErrorDialog.newInstance(error)
        
        // Set the mocked dependencies
        dialog.errorMessageProvider = mockErrorMessageProvider
        dialog.errorRecoveryManager = mockErrorRecoveryManager
        
        // Launch the dialog
        launchFragmentInContainer<ErrorDialog>(
            fragmentArgs = Bundle().apply {
                // No arguments needed as we're setting the error directly
            },
            themeResId = R.style.Theme_Eazydelivery
        ).onFragment { fragment ->
            // Set the error directly
            fragment.error = error
        }
        
        // Check that the dialog is displayed
        onView(withId(R.id.error_title)).check(matches(isDisplayed()))
        onView(withId(R.id.error_message)).check(matches(isDisplayed()))
        
        // Check the content
        onView(withId(R.id.error_title)).check(matches(withText(R.string.error_title_unexpected)))
        onView(withId(R.id.error_message)).check(matches(withText("Something went wrong")))
        
        // Only the dismiss button should be visible
        onView(withId(R.id.primary_action_button)).check(matches(withText(R.string.action_dismiss)))
    }
    
    @Test
    fun testErrorDialogButtonClicks() {
        // Create a permission error
        val error = AppError.Permission.Denied("Camera permission denied")
        
        // Create a callback flag
        var recoveryActionCalled = false
        var dismissCalled = false
        
        // Create the dialog with callbacks
        val dialog = ErrorDialog.newInstance(
            error = error,
            onRecoveryActionListener = { 
                recoveryActionCalled = true
            },
            onDismissListener = {
                dismissCalled = true
            }
        )
        
        // Launch the dialog
        launchFragmentInContainer<ErrorDialog>(
            fragmentArgs = Bundle().apply {
                // No arguments needed as we're setting the error directly
            },
            themeResId = R.style.Theme_Eazydelivery
        ).onFragment { fragment ->
            // Set the error and callbacks directly
            fragment.error = error
            fragment.onRecoveryActionListener = { recoveryActionCalled = true }
            fragment.onDismissListener = { dismissCalled = true }
        }
        
        // Click the primary action button
        onView(withId(R.id.primary_action_button)).perform(click())
        
        // Check that the recovery action callback was called
        assert(recoveryActionCalled)
    }
}
