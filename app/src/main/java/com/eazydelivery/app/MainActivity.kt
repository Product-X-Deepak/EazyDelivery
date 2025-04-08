package com.eazydelivery.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.eazydelivery.app.ui.base.BaseActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.eazydelivery.app.ui.EazyDeliveryApp
import com.eazydelivery.app.ui.theme.EazyDeliveryTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.eazydelivery.app.util.PackageMigrationHelper
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
public class MainActivity : BaseActivity() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    @Inject
    lateinit var packageMigrationHelper: PackageMigrationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Handle intent migration if needed
        if (intent != null) {
            val migratedIntent = packageMigrationHelper.migrateIntent(intent)
            if (migratedIntent !== intent) {
                intent = migratedIntent
            }
        }

        // Keep the splash screen visible until the app is ready
        splashScreen.setKeepOnScreenCondition { false }

        // Initialize Firebase Analytics
        firebaseAnalytics = Firebase.analytics

        // Configure edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            EazyDeliveryTheme {
                // Log app open event
                LaunchedEffect(Unit) {
                    Timber.d("App started")
                    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, null)
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EazyDeliveryApp()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("MainActivity destroyed")
    }
}
