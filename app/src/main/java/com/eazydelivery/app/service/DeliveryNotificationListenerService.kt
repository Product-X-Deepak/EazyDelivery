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
import com.eazydelivery.app.MainActivity
import com.eazydelivery.app.R
import com.eazydelivery.app.domain.model.Order
import com.eazydelivery.app.domain.model.OrderNotification
import com.eazydelivery.app.domain.repository.AnalyticsRepository
import com.eazydelivery.app.domain.repository.PlatformRepository
import com.eazydelivery.app.domain.repository.ServiceRepository
import com.eazydelivery.app.ml.NotificationClassifier
import com.eazydelivery.app.ml.OrderPrioritizationEngine
import com.eazydelivery.app.util.CacheManager
import com.eazydelivery.app.util.ErrorHandler
import com.eazydelivery.app.util.NotificationParser
import com.eazydelivery.app.util.PackageMigrationHelper
import com.eazydelivery.app.util.SecureStorage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class DeliveryNotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var notificationParser: NotificationParser

    @Inject
    lateinit var platformRepository: PlatformRepository

    @Inject
    lateinit var analyticsRepository: AnalyticsRepository

    @Inject
    lateinit var serviceRepository: ServiceRepository

    @Inject
    lateinit var secureStorage: SecureStorage

    @Inject
    lateinit var cacheManager: CacheManager

    @Inject
    lateinit var notificationClassifier: NotificationClassifier

    @Inject
    lateinit var orderPrioritizationEngine: OrderPrioritizationEngine

    @Inject
    lateinit var errorHandler: ErrorHandler

    @Inject
    lateinit var packageMigrationHelper: PackageMigrationHelper

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isServiceActive = false

    // Cache to prevent duplicate processing of notifications
    private val processedNotifications = ConcurrentHashMap<String, Long>()
    private val processedNotificationsLock = Any()

    // Scheduled executor for cleanup tasks
    private val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()

    // Set of supported delivery app packages for faster lookups
    private val supportedPackages = setOf(
        "in.swiggy.deliveryapp",      // Swiggy and Instamart
        "com.zomato.delivery",        // Zomato
        "com.zepto.rider",            // Zepto
        "app.blinkit.onboarding",     // Blinkit
        "com.ubercab.driver",         // Uber Eats
        "com.bigbasket.delivery"      // BigBasket
    )

    override fun onCreate() {
        super.onCreate()
        Timber.d("Notification Listener Service created")
        startForeground()

        // Check if service should be active
        serviceScope.launch {
            isServiceActive = serviceRepository.isServiceActive().getOrDefault(false)
            Timber.d("Notification Listener Service active: $isServiceActive")
        }

        // Schedule periodic cleanup of processed notifications cache
        scheduledExecutor.scheduleAtFixedRate(
            { cleanupProcessedNotificationsCache() },
            15,
            15,
            TimeUnit.MINUTES
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Quick check if service is active before any processing
        if (!isServiceActive) return

        // Process only notifications from whitelisted delivery apps
        var packageName = sbn.packageName

        // Use the package migration helper to handle package migrations
        packageName = packageMigrationHelper.migratePackageName(packageName)

        // Early return if not a supported package (fast check)
        if (!supportedPackages.contains(packageName)) {
            return
        }

        // Extract notification data early to avoid processing unnecessary notifications
        val notification = sbn.notification
        val extras = notification.extras

        // Extract notification title and text
        val title = extras.getString(Notification.EXTRA_TITLE) ?: return
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return

        // Quick check if this looks like an order notification before further processing
        if (!isLikelyOrderNotification(title, text)) {
            return
        }

        // Generate a unique ID for this notification
        val notificationId = "${packageName}:${sbn.id}:${sbn.postTime}:${title.hashCode()}"

        // Use synchronized block to prevent race conditions
        synchronized(processedNotificationsLock) {
            // Check if we've already processed this notification
            if (processedNotifications.containsKey(notificationId)) {
                return
            }

            // Mark this notification as processed
            processedNotifications[notificationId] = System.currentTimeMillis()
        }

        // Process notification in background thread
        serviceScope.launch(Dispatchers.Default) {
            try {
                val startTime = System.currentTimeMillis()

                Timber.d("Processing notification from $packageName: $title - $text")

                // Parse the notification to extract order details
                val orderDetails = notificationParser.parseNotification(packageName, title, text)

                if (orderDetails != null) {
                    processOrderDetails(packageName, title, text, orderDetails, startTime)
                } else {
                    Timber.d("Could not parse order details from notification")
                }
            } catch (e: Exception) {
                errorHandler.handleException("NotificationListenerService.onNotificationPosted", e)
            }
        }
    }

    private suspend fun processOrderDetails(
        packageName: String,
        title: String,
        text: String,
        orderDetails: NotificationParser.OrderDetails,
        startTime: Long
    ) {
        try {
            // Create OrderNotification object for ML processing
            val orderNotification = OrderNotification(
                id = UUID.randomUUID().toString(),
                packageName = packageName,
                title = title,
                text = text,
                timestamp = System.currentTimeMillis(),
                amount = orderDetails.amount,
                platformName = orderDetails.platformName,
                estimatedDistance = orderDetails.estimatedDistance,
                estimatedTime = orderDetails.estimatedTime
            )

            // Classify notification using ML - ensure this runs on a background thread
            val classification = withContext(Dispatchers.Default) {
                notificationClassifier.classifyNotification(orderNotification)
            }

            // Score and prioritize the order - ensure this runs on a background thread
            val priority = withContext(Dispatchers.Default) {
                orderPrioritizationEngine.scoreOrder(orderNotification, classification)
            }

            // Cache the notification for later analysis
            cacheManager.cacheOrderNotification(orderNotification, classification, priority)

            // Process the order based on priority and user settings
            processOrder(orderDetails, priority)

            val processingTime = System.currentTimeMillis() - startTime
            Timber.d("Notification processed in $processingTime ms with priority: $priority")
        } catch (e: Exception) {
            errorHandler.handleException("NotificationListenerService.processOrderDetails", e)
        }
    }

    private suspend fun processOrder(orderDetails: NotificationParser.OrderDetails, priority: String) {
        try {
            // Get platform settings
            val platformResult = platformRepository.getPlatform(orderDetails.platformName)

            platformResult.fold(
                onSuccess = { platform ->
                    if (platform.isEnabled && orderDetails.amount >= platform.minAmount) {
                        // Auto-accept the order if it meets the criteria and has high priority
                        val shouldAutoAccept = platform.autoAccept &&
                                              (priority == "HIGH" ||
                                               (priority == "MEDIUM" && platform.acceptMediumPriority))

                        if (shouldAutoAccept) {
                            // Perform auto-accept action
                            notificationParser.performAcceptAction(orderDetails.platformName)
                            Timber.d("Auto-accepting order from ${orderDetails.platformName} with amount ${orderDetails.amount}")
                        }

                        // Save order to database
                        val order = Order(
                            id = UUID.randomUUID().toString(),
                            platformName = orderDetails.platformName,
                            amount = orderDetails.amount,
                            timestamp = System.currentTimeMillis().toString(),
                            isAccepted = shouldAutoAccept,
                            deliveryStatus = if (shouldAutoAccept) "ACCEPTED" else "PENDING",
                            priority = priority,
                            estimatedDistance = orderDetails.estimatedDistance,
                            estimatedTime = orderDetails.estimatedTime
                        )

                        analyticsRepository.addOrder(order)

                        // Save last order details for quick access
                        secureStorage.secureStore("last_order_details",
                            "Platform: ${orderDetails.platformName}, Amount: ₹${orderDetails.amount}, Priority: $priority"
                        )

                        // Show notification about the order on the main thread
                        withContext(Dispatchers.Main) {
                            showOrderNotification(orderDetails, shouldAutoAccept, priority)
                        }
                    } else {
                        Timber.d("Order doesn't meet criteria: platform enabled: ${platform.isEnabled}, " +
                                "amount: ${orderDetails.amount}, min amount: ${platform.minAmount}")
                    }
                },
                onFailure = { exception ->
                    errorHandler.handleException("NotificationListenerService.processOrder", exception)
                }
            )
        } catch (e: Exception) {
            errorHandler.handleException("NotificationListenerService.processOrder", e)
        }
    }

    private fun showOrderNotification(
        orderDetails: NotificationParser.OrderDetails,
        autoAccepted: Boolean,
        priority: String
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                EazyDeliveryApplication.NOTIFICATIONS_CHANNEL_ID,
                "EazyDelivery Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent for when notification is tapped
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Determine notification color based on priority
        val priorityColor = when(priority) {
            "HIGH" -> R.color.priority_high
            "MEDIUM" -> R.color.priority_medium
            else -> R.color.priority_low
        }

        // Build the notification
        val notification = NotificationCompat.Builder(this, EazyDeliveryApp.NOTIFICATIONS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (autoAccepted) "Order Auto-Accepted" else "New Order Available")
            .setContentText("${orderDetails.platformName}: ₹${orderDetails.amount} (${priority.lowercase()} priority)")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setColor(getColor(priorityColor))
            .setAutoCancel(true)
            .build()

        // Show the notification
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun startForeground() {
        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                EazyDeliveryApp.FOREGROUND_SERVICE_CHANNEL_ID,
                "EazyDelivery Service",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Used for keeping the auto-accept service running"

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent for when notification is tapped
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val notification = NotificationCompat.Builder(this, EazyDeliveryApplication.FOREGROUND_SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("EazyDelivery is running")
            .setContentText("Monitoring for delivery notifications")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()

        // Start as a foreground service
        startForeground(1, notification)
    }

    /**
     * Quick check if a notification is likely to be an order notification
     * This is a fast pre-filter before the more expensive parsing
     */
    private fun isLikelyOrderNotification(title: String, text: String): Boolean {
        val combinedText = "$title $text".lowercase()
        return combinedText.contains("order") ||
               combinedText.contains("delivery") ||
               combinedText.contains("₹") ||
               combinedText.contains("rs") ||
               combinedText.contains("rupee")
    }

    private fun cleanupProcessedNotificationsCache() {
        try {
            val currentTime = System.currentTimeMillis()
            val expirationTime = 30 * 60 * 1000 // 30 minutes in milliseconds

            // Remove entries older than 30 minutes
            val iterator = processedNotifications.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (currentTime - entry.value > expirationTime) {
                    iterator.remove()
                }
            }

            Timber.d("Cleaned up processed notifications cache. Remaining entries: ${processedNotifications.size}")
        } catch (e: Exception) {
            errorHandler.handleException("NotificationListenerService.cleanupProcessedNotificationsCache", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Cancel all coroutines to prevent memory leaks
        serviceScope.coroutineContext.cancelChildren()

        // Shutdown the scheduled executor to prevent memory leaks
        try {
            scheduledExecutor.shutdown()
            if (!scheduledExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow()
                if (!scheduledExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                    Timber.e("ScheduledExecutor did not terminate")
                }
            }
        } catch (e: InterruptedException) {
            scheduledExecutor.shutdownNow()
            Thread.currentThread().interrupt()
            Timber.e(e, "ScheduledExecutor shutdown interrupted")
        } catch (e: Exception) {
            Timber.e(e, "Error shutting down ScheduledExecutor")
        }

        // Clear any cached data
        processedNotifications.clear()

        Timber.d("Notification Listener Service destroyed")
    }
}

