package com.eazydelivery.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.eazydelivery.app.EazyDeliveryApp
import com.eazydelivery.app.R
import com.eazydelivery.app.domain.model.Order
import com.eazydelivery.app.domain.model.Platform
import com.eazydelivery.app.domain.repository.AnalyticsRepository
import com.eazydelivery.app.domain.repository.PlatformRepository
import com.eazydelivery.app.domain.repository.ServiceRepository
import com.eazydelivery.app.domain.repository.SubscriptionRepository
import com.eazydelivery.app.ui.MainActivity
import com.eazydelivery.app.util.NotificationParser
import com.eazydelivery.app.util.SecureStorage
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class DeliveryNotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var platformRepository: PlatformRepository

    @Inject
    lateinit var analyticsRepository: AnalyticsRepository

    @Inject
    lateinit var serviceRepository: ServiceRepository

    @Inject
    lateinit var subscriptionRepository: SubscriptionRepository

    @Inject
    lateinit var secureStorage: SecureStorage

    @Inject
    lateinit var notificationParser: NotificationParser

    private val job = SupervisorJob()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "Error in DeliveryNotificationListenerService")
        FirebaseCrashlytics.getInstance().recordException(throwable)
    }
    private val scope = CoroutineScope(Dispatchers.IO + job + exceptionHandler)

    private var isServiceRunning = false
    private val NOTIFICATION_ID = 1002

    override fun onCreate() {
        super.onCreate()
        Timber.d("DeliveryNotificationListenerService created")
        startForeground()
    }

    private fun startForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = EazyDeliveryApplication.FOREGROUND_SERVICE_CHANNEL_ID
            val channelName = "EazyDelivery Service"
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Used for keeping the notification service running"
                enableLights(false)
                enableVibration(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            val notificationIntent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("EazyDelivery Notification Service")
                .setContentText("Monitoring for delivery notifications")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()

            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Timber.d("DeliveryNotificationListenerService connected")
        isServiceRunning = true

        // Check if service should be active
        scope.launch {
            try {
                if (serviceRepository.isServiceActive()) {
                    Timber.d("Service is active, starting monitoring")
                } else {
                    Timber.d("Service is not active, but listener is connected")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error checking service status")
            }
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Timber.d("DeliveryNotificationListenerService disconnected")
        isServiceRunning = false

        // Try to reconnect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestRebind(ComponentName(this, DeliveryNotificationListenerService::class.java))
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        if (!isServiceRunning) {
            Timber.d("Service not running, ignoring notification")
            return
        }

        scope.launch {
            try {
                processNotification(sbn)
            } catch (e: Exception) {
                Timber.e(e, "Error processing notification")
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }

    private suspend fun processNotification(sbn: StatusBarNotification) {
        // Check if service is active
        if (!serviceRepository.isServiceActive()) {
            Timber.d("Service is not active, ignoring notification")
            return
        }

        // Check if subscription is active
        val subscriptionStatus = subscriptionRepository.getSubscriptionStatus()
        val isSubscribed = subscriptionStatus.isSubscribed
        val isTrialActive = subscriptionRepository.isTrialActive()

        if (!isSubscribed && !isTrialActive) {
            Timber.d("Subscription not active and trial period ended, ignoring notification")
            return
        }

        // Get package name and check if it's a supported platform
        val packageName = sbn.packageName
        val platformName = getPlatformFromPackage(packageName) ?: return

        // Check if platform is enabled
        val enabledPlatforms = platformRepository.getEnabledPlatforms()
        if (enabledPlatforms.none { it.name == platformName }) {
            Timber.d("Platform $platformName is not enabled, ignoring notification")
            return
        }

        // Extract notification details
        val notification = sbn.notification
        val extras = notification.extras

        val title = extras.getString(Notification.EXTRA_TITLE) ?: return
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return

        Timber.d("Processing notification from $platformName: $title - $text")

        // Parse notification to extract order details
        val orderDetails = notificationParser.extractOrderDetails(platformName, title, text)
        if (orderDetails != null) {
            val (amount, isAccepted) = orderDetails

            // Get minimum amount for this platform
            val platform = enabledPlatforms.find { it.name == platformName }
            val minAmount = platform?.minAmount ?: 0

            if (amount >= minAmount) {
                Timber.d("Order amount $amount meets minimum requirement $minAmount, auto-accepting")

                // Auto-accept the order
                val accessibilityService = Intent(this@DeliveryNotificationListenerService, DeliveryAccessibilityService::class.java)
                accessibilityService.putExtra("platform", platformName)
                accessibilityService.putExtra("amount", amount)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(accessibilityService)
                } else {
                    startService(accessibilityService)
                }

                // Record the order
                val order = Order(
                    id = UUID.randomUUID().toString(),
                    platformName = platformName,
                    amount = amount.toDouble(),
                    timestamp = System.currentTimeMillis().toString(),
                    isAccepted = true
                )
                analyticsRepository.addOrder(order)

                // Show notification to user
                showOrderAcceptedNotification(platformName, amount)

                Timber.d("Order recorded and auto-accept initiated for $platformName order of amount $amount")
            } else {
                Timber.d("Order amount $amount does not meet minimum requirement $minAmount, ignoring")
            }
        } else {
            Timber.d("Could not extract order details from notification")
        }
    }

    private fun showOrderAcceptedNotification(platformName: String, amount: Int) {
        val channelId = EazyDeliveryApplication.NOTIFICATIONS_CHANNEL_ID

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Order Auto-Accepted")
            .setContentText("$platformName order for ₹$amount has been auto-accepted")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun getPlatformFromPackage(packageName: String): String? {
        return when (packageName) {
            "com.application.zomato" -> "Zomato"
            "in.swiggy.android" -> "Swiggy"
            "com.grofers.customerapp" -> "Blinkit"
            "com.zeptonow.app" -> "Zepto"
            "com.dunzo.user" -> "Dunzo"
            "com.ubercab.eats" -> "Uber Eats"
            else -> null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        isServiceRunning = false
        Timber.d("DeliveryNotificationListenerService destroyed")
    }
}

