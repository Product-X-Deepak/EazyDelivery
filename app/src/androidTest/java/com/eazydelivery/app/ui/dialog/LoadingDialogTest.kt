package com.eazydelivery.app.ui.dialog

import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eazydelivery.app.R
import org.hamcrest.Matchers.not
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for the LoadingDialog
 */
@RunWith(AndroidJUnit4::class)
class LoadingDialogTest {
    
    @Test
    fun testLoadingDialogWithoutMessage() {
        // Create the dialog without a message
        val dialog = LoadingDialog.newInstance()
        
        // Launch the dialog
        launchFragmentInContainer<LoadingDialog>(
            fragmentArgs = Bundle().apply {
                // No arguments needed as we're using the newInstance method
            },
            themeResId = R.style.Theme_Eazydelivery
        )
        
        // Check that the progress bar is displayed
        onView(withId(R.id.loading_progress)).check(matches(isDisplayed()))
        
        // Check that the message is not displayed
        onView(withId(R.id.loading_message)).check(matches(not(isDisplayed())))
    }
    
    @Test
    fun testLoadingDialogWithMessage() {
        // Create the dialog with a message
        val message = "Loading data..."
        val dialog = LoadingDialog.newInstance(message)
        
        // Launch the dialog
        launchFragmentInContainer<LoadingDialog>(
            fragmentArgs = Bundle().apply {
                // No arguments needed as we're setting the message directly
            },
            themeResId = R.style.Theme_Eazydelivery
        ).onFragment { fragment ->
            // Set the message directly
            fragment.message = message
        }
        
        // Check that the progress bar is displayed
        onView(withId(R.id.loading_progress)).check(matches(isDisplayed()))
        
        // Check that the message is displayed with the correct text
        onView(withId(R.id.loading_message)).check(matches(isDisplayed()))
        onView(withId(R.id.loading_message)).check(matches(withText(message)))
    }
    
    @Test
    fun testUpdateMessage() {
        // Create the dialog without a message
        val dialog = LoadingDialog.newInstance()
        
        // Launch the dialog
        launchFragmentInContainer<LoadingDialog>(
            fragmentArgs = Bundle().apply {
                // No arguments needed as we're using the newInstance method
            },
            themeResId = R.style.Theme_Eazydelivery
        ).onFragment { fragment ->
            // Update the message
            fragment.updateMessage("Processing...")
        }
        
        // Check that the message is now displayed with the updated text
        onView(withId(R.id.loading_message)).check(matches(isDisplayed()))
        onView(withId(R.id.loading_message)).check(matches(withText("Processing...")))
    }
}
