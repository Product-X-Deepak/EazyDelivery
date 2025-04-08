package com.eazydelivery.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class DeliveryAccessibilityService : AccessibilityService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    private var currentPlatform: String? = null
    private var currentAmount: Int = 0
    private var maxRetries = 5
    private var currentRetry = 0
    private var isProcessingEvent = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        Timber.d("DeliveryAccessibilityService connected")

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_VIEW_FOCUSED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED

            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS

            packageNames = arrayOf(
                "com.application.zomato",
                "in.swiggy.android",
                "com.grofers.customerapp",
                "com.zeptonow.app",
                "in.swiggy.deliveryapp",
                "com.ubercab.driver",
                "com.bigbasket.delivery"
            )
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        Timber.d("onAccessibilityEvent: type = ${event.eventType}, package = ${event.packageName}")
        if (isProcessingEvent) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            if (packageName == currentPlatform) {
                scope.launch {
                    delay(1000)
                    performAutoAccept(currentPlatform!!)
                }
            }
        }
    }

    override fun onInterrupt() {
        Timber.w("Accessibility service interrupted!")
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        Timber.d("DeliveryAccessibilityService destroyed")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            currentPlatform = it.getStringExtra("platform")
            currentAmount = it.getIntExtra("amount", 0)
            currentRetry = 0

            if (currentPlatform != null) {
                Timber.d("Starting auto-accept for platform: $currentPlatform, amount: $currentAmount")
                scope.launch {
                    // Give some time for the notification to be processed and app to open
                    delay(1000)
                    performAutoAccept(currentPlatform!!)
                }
            }
        }

        return START_STICKY
    }

    private suspend fun performAutoAccept(platformName: String) {
        isProcessingEvent = true
        try {
            when (platformName) {
                "Zomato" -> acceptZomatoOrder()
                "Swiggy" -> acceptSwiggyOrder()
                "Blinkit" -> acceptBlinkitOrder()
                "Zepto" -> acceptZeptoOrder()
                "Instamart" -> acceptInstamartOrder()
                "BigBasket" -> acceptBigBasketOrder()
                "Uber Eats" -> acceptUberEatsOrder()
                else -> Timber.w("Unsupported platform: $platformName")
            }
        } finally {
            isProcessingEvent = false
            Timber.d("Auto-accept process completed for $platformName")
            // Redirect user to the original delivery application
            launchOriginalApp(platformName)
            stopSelf()
        }
    }

    private fun acceptZomatoOrder() {
        // Implement Zomato auto-acceptance logic here
        Timber.d("Accepting Zomato order...")
    }

    private fun acceptSwiggyOrder() {
        // Implement Swiggy auto-acceptance logic here
        Timber.d("Accepting Swiggy order...")
    }

    private fun acceptBlinkitOrder() {
        // Implement Blinkit auto-acceptance logic here
        Timber.d("Accepting Blinkit order...")
    }

    private fun acceptZeptoOrder() {
        // Implement Zepto auto-acceptance logic here
        Timber.d("Accepting Zepto order...")
    }

    private fun acceptInstamartOrder() {
        // Implement Instamart auto-acceptance logic here
        Timber.d("Accepting Instamart order...")
    }

    private fun acceptBigBasketOrder() {
        // Implement BigBasket auto-acceptance logic here
        Timber.d("Accepting BigBasket order...")
    }

    private fun acceptUberEatsOrder() {
        // Implement Uber Eats auto-acceptance logic here
        Timber.d("Accepting Uber Eats order...")
    }

    private fun launchOriginalApp(platformName: String) {
        val packageName = when (platformName) {
            "Zomato" -> "com.application.zomato"
            "Swiggy" -> "in.swiggy.android"
            "Blinkit" -> "com.grofers.customerapp"
            "Zepto" -> "com.zeptonow.app"
            "Instamart" -> "in.swiggy.deliveryapp"
            "BigBasket" -> "com.bigbasket.delivery"
            "Uber Eats" -> "com.ubercab.driver"
            else -> null
        }

        if (packageName != null) {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(launchIntent)
            } else {
                Timber.e("Unable to launch app: $packageName")
            }
        } else {
            Timber.e("Unknown platform: $platformName")
        }
    }
}

