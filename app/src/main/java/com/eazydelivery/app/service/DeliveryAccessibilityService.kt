package com.eazydelivery.app.service

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import com.eazydelivery.app.EazyDeliveryApp
import com.eazydelivery.app.MainActivity
import com.eazydelivery.app.R
import com.eazydelivery.app.domain.repository.PlatformRepository
import com.eazydelivery.app.domain.repository.ServiceRepository
import com.eazydelivery.app.ml.ScreenAnalyzer
import com.eazydelivery.app.util.CacheManager
import com.eazydelivery.app.util.Constants
import com.eazydelivery.app.util.Constants.PACKAGE_SWIGGY
import com.eazydelivery.app.util.Constants.PACKAGE_ZOMATO
import com.eazydelivery.app.util.Constants.PACKAGE_ZEPTO
import com.eazydelivery.app.util.Constants.PACKAGE_BLINKIT
import com.eazydelivery.app.util.Constants.PACKAGE_UBER_EATS
import com.eazydelivery.app.util.Constants.PACKAGE_BIGBASKET
import com.eazydelivery.app.util.Constants.PLATFORM_SWIGGY
import com.eazydelivery.app.util.Constants.PLATFORM_INSTAMART
import com.eazydelivery.app.util.Constants.PLATFORM_ZOMATO
import com.eazydelivery.app.util.Constants.PLATFORM_ZEPTO
import com.eazydelivery.app.util.Constants.PLATFORM_BLINKIT
import com.eazydelivery.app.util.Constants.PLATFORM_UBER_EATS
import com.eazydelivery.app.util.Constants.PLATFORM_BIGBASKET
import com.eazydelivery.app.util.ErrorHandler
import com.eazydelivery.app.util.NotificationParser
import com.eazydelivery.app.util.PackageMigrationHelper
import com.eazydelivery.app.util.ScreenshotUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import com.eazydelivery.app.BuildConfig
import javax.inject.Inject

@AndroidEntryPoint
class DeliveryAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var notificationParser: NotificationParser

    @Inject
    lateinit var platformRepository: PlatformRepository

    @Inject
    lateinit var serviceRepository: ServiceRepository

    @Inject
    lateinit var screenAnalyzer: ScreenAnalyzer

    @Inject
    lateinit var screenshotUtil: ScreenshotUtil

    @Inject
    lateinit var cacheManager: CacheManager

    @Inject
    lateinit var serviceOptimizer: ServiceOptimizer

    @Inject
    lateinit var errorHandler: ErrorHandler

    @Inject
    lateinit var packageMigrationHelper: PackageMigrationHelper

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isServiceActive = false

    // List of supported delivery app packages
    private val supportedPackages = Constants.SUPPORTED_DELIVERY_PACKAGES

    override fun onCreate() {
        super.onCreate()
        Timber.d("Accessibility Service created")
        startForeground()

        // Check if service should be active
        serviceScope.launch {
            isServiceActive = serviceRepository.isServiceActive().getOrDefault(false)
            Timber.d("Accessibility Service active: $isServiceActive")

            // Start the service optimizer
            serviceOptimizer.startOptimizer(serviceScope)
        }
    }

    // Track the last time we processed an event for each package
    private val lastProcessedTime = ConcurrentHashMap<String, Long>()

    // Minimum time between screenshot analyses for the same package (in milliseconds)
    private val MIN_ANALYSIS_INTERVAL = Constants.MIN_ANALYSIS_INTERVAL_MS

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!isServiceActive) return

        var packageName = event.packageName?.toString() ?: return

        // Check if this is using the old package structure
        if (packageName.startsWith("com.application.eazydelivery")) {
            // Convert to new package structure
            packageName = packageName.replace("com.application.eazydelivery", "com.eazydelivery.app")
        }

        // Process only events from delivery apps
        if (!supportedPackages.contains(packageName)) {
            return
        }

        // Process only specific event types
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        // Check if we've processed this package recently
        val currentTime = System.currentTimeMillis()
        val lastTime = lastProcessedTime[packageName] ?: 0L

        // Skip if we've processed this package too recently, except for WINDOW_STATE_CHANGED
        // which is more important as it indicates a new screen is being shown
        if (currentTime - lastTime < MIN_ANALYSIS_INTERVAL &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        // Update the last processed time
        lastProcessedTime[packageName] = currentTime

        // Use the withWakeLock method to ensure the wake lock is properly released
        serviceOptimizer.withWakeLock("AccessibilityEvent") {
            try {
                Timber.d("Processing accessibility event from $packageName: ${event.eventType}")

                val rootNode = rootInActiveWindow ?: return@withWakeLock

                // First try to find accept buttons without taking a screenshot
                val acceptButtonNode = findAcceptButtonInNode(rootNode, packageName)

                if (acceptButtonNode != null) {
                    // If we found an accept button directly, process it
                    processAcceptButton(packageName, acceptButtonNode)
                } else {
                    // Only take a screenshot if we couldn't find an accept button directly
                    val screenshot = screenshotUtil.takeScreenshot()
                    if (screenshot != null) {
                        // Save screenshot only in debug builds
                        if (BuildConfig.DEBUG) {
                            saveScreenshotForDebug(screenshot)
                        }

                        // Analyze the screen
                        val analysisResult = screenAnalyzer.analyzeScreen(screenshot, packageName, rootNode)

                        // Process the UI based on the analysis result and app
                        processUIBasedOnAnalysis(packageName, rootNode, analysisResult)
                    }
                }

                rootNode.recycle()
            } catch (e: Exception) {
                errorHandler.handleException("AccessibilityService.onAccessibilityEvent", e)
            }
        }
    }

    private fun saveScreenshotForDebug(bitmap: Bitmap) {
        serviceScope.launch {
            try {
                val tempDir = File(cacheDir, "temp_screenshots")
                if (!tempDir.exists()) {
                    tempDir.mkdirs()
                }

                val file = File(tempDir, "screenshot_${UUID.randomUUID()}.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }

                Timber.d("Saved debug screenshot to ${file.absolutePath}")
            } catch (e: Exception) {
                errorHandler.handleException("AccessibilityService.saveScreenshotForDebug", e)
            }
        }
    }

    private fun processUIBasedOnAnalysis(
        packageName: String,
        rootNode: AccessibilityNodeInfo,
        analysisResult: ScreenAnalyzer.AnalysisResult
    ) {
        serviceScope.launch {
            try {
                // Check if platform is enabled and auto-accept is on
                // Determine platform name from package name
                // For Swiggy/Instamart (same package), we need to determine which one based on UI context
                val platformName = when (packageName) {
                    PACKAGE_SWIGGY -> {
                        // Check if this is Instamart by looking for Instamart-specific UI elements
                        if (isInstamart(rootNode)) PLATFORM_INSTAMART else PLATFORM_SWIGGY
                    }
                    PACKAGE_ZOMATO -> PLATFORM_ZOMATO
                    PACKAGE_ZEPTO -> PLATFORM_ZEPTO
                    PACKAGE_BLINKIT -> PLATFORM_BLINKIT
                    PACKAGE_UBER_EATS -> PLATFORM_UBER_EATS
                    PACKAGE_BIGBASKET -> PLATFORM_BIGBASKET
                    else -> return@launch
                }

                val platformResult = platformRepository.getPlatform(platformName)

                platformResult.fold(
                    onSuccess = { platform ->
                        if (platform.isEnabled && platform.autoAccept) {
                            // If accept button is found with high confidence, click it
                            if (analysisResult.acceptButtonFound && analysisResult.confidence > 0.8) {
                                analysisResult.acceptButtonNode?.let { node ->
                                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                    Timber.d("Clicked accept button for $platformName with confidence ${analysisResult.confidence}")
                                }
                            } else if (analysisResult.needsConfirmation) {
                                // Handle confirmation dialog if needed
                                findAndClickConfirmButton(rootNode)
                            }
                        }
                    },
                    onFailure = { exception ->
                        errorHandler.handleException("AccessibilityService.processUIBasedOnAnalysis", exception)
                    }
                )
            } catch (e: Exception) {
                errorHandler.handleException("AccessibilityService.processUIBasedOnAnalysis", e)
            }
        }
    }

    /**
     * Finds an accept button in the accessibility node tree without using image analysis
     * @param rootNode The root accessibility node
     * @param packageName The package name of the app
     * @return The accept button node if found, null otherwise
     */
    private fun findAcceptButtonInNode(rootNode: AccessibilityNodeInfo, packageName: String): AccessibilityNodeInfo? {
        try {
            // Define accept button texts based on the delivery app
            val acceptTexts = when (packageName) {
                PACKAGE_SWIGGY -> listOf("Accept Order", "Accept", "ACCEPT")
                PACKAGE_ZOMATO -> listOf("Accept", "ACCEPT", "Accept Order")
                PACKAGE_ZEPTO -> listOf("Accept", "ACCEPT", "Accept Order")
                PACKAGE_BLINKIT -> listOf("Accept", "ACCEPT", "Accept Order")
                PACKAGE_UBER_EATS -> listOf("Accept", "ACCEPT", "Accept Delivery")
                PACKAGE_BIGBASKET -> listOf("Accept", "ACCEPT", "Accept Order")
                else -> listOf("Accept", "ACCEPT")
            }

            // Look for accept button texts
            for (text in acceptTexts) {
                val node = findNodeByText(rootNode, text)
                if (node != null && node.isClickable) {
                    Timber.d("Found accept button with text: $text")
                    return node
                }
            }

            // If no text match, try to find buttons by description
            val acceptDescriptions = listOf("accept", "accept order", "accept delivery")
            for (description in acceptDescriptions) {
                val node = findNodeByDescription(rootNode, description)
                if (node != null && node.isClickable) {
                    Timber.d("Found accept button with description: $description")
                    return node
                }
            }

            // If still not found, look for buttons with specific IDs
            val acceptIds = when (packageName) {
                "in.swiggy.deliveryapp" -> listOf("accept_button", "btnAccept")
                "com.zomato.delivery" -> listOf("accept_order_button", "btnAccept")
                else -> listOf("accept_button", "btnAccept", "acceptButton")
            }

            for (id in acceptIds) {
                val node = findNodeById(rootNode, id)
                if (node != null && node.isClickable) {
                    Timber.d("Found accept button with ID: $id")
                    return node
                }
            }

            return null
        } catch (e: Exception) {
            errorHandler.handleException("AccessibilityService.findAcceptButtonInNode", e)
            return null
        }
    }

    /**
     * Processes an accept button by clicking it
     * @param packageName The package name of the app
     * @param node The accept button node
     */
    private fun processAcceptButton(packageName: String, node: AccessibilityNodeInfo) {
        serviceScope.launch {
            try {
                // Check if platform is enabled and auto-accept is on
                // Determine platform name from package name
                // For Swiggy/Instamart (same package), we need to determine which one based on UI context
                val platformName = when (packageName) {
                    "in.swiggy.deliveryapp" -> {
                        // Check if this is Instamart by looking for Instamart-specific UI elements
                        if (isInstamart(rootInActiveWindow)) "instamart" else "swiggy"
                    }
                    "com.zomato.delivery" -> "zomato"
                    "com.zepto.rider" -> "zepto"
                    "app.blinkit.onboarding" -> "blinkit"
                    "com.ubercab.driver" -> "ubereats"
                    "com.bigbasket.delivery" -> "bigbasket"
                    else -> return@launch
                }

                val platformResult = platformRepository.getPlatform(platformName)

                platformResult.fold(
                    onSuccess = { platform ->
                        if (platform.isEnabled && platform.autoAccept) {
                            // Click the accept button
                            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            Timber.d("Clicked accept button for $platformName")

                            // Look for confirmation dialog if needed
                            kotlinx.coroutines.delay(500) // Wait for confirmation dialog to appear
                            val rootNode = rootInActiveWindow
                            if (rootNode != null) {
                                findAndClickConfirmButton(rootNode)
                                rootNode.recycle()
                            }
                        }
                    },
                    onFailure = { exception ->
                        errorHandler.handleException("AccessibilityService.processAcceptButton", exception)
                    }
                )
            } catch (e: Exception) {
                errorHandler.handleException("AccessibilityService.processAcceptButton", e)
            }
        }
    }

    private fun findAndClickConfirmButton(rootNode: AccessibilityNodeInfo) {
        try {
            // Look for common confirmation button texts
            val confirmTexts = listOf("Confirm", "Yes", "Accept", "OK", "Okay")

            for (text in confirmTexts) {
                val node = findNodeByText(rootNode, text)
                if (node != null && node.isClickable) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Timber.d("Clicked confirmation button with text: $text")
                    return
                }
            }

            // If no text match, try to find buttons by description
            val node = findNodeByDescription(rootNode, "confirm")
            if (node != null && node.isClickable) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Timber.d("Clicked confirmation button by description")
                return
            }

            Timber.d("Could not find confirmation button")
        } catch (e: Exception) {
            errorHandler.handleException("AccessibilityService.findAndClickConfirmButton", e)
        }
    }

    private fun findNodeByText(rootNode: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(rootNode)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()

            // Check if this node contains the text
            val nodeText = node.text?.toString() ?: ""
            if (nodeText.contains(text, ignoreCase = true)) {
                return node
            }

            // Add child nodes to the queue
            for (i in 0 until node.childCount) {
                val childNode = node.getChild(i)
                if (childNode != null) {
                    queue.add(childNode)
                }
            }
        }

        return null
    }

    private fun findNodeByDescription(rootNode: AccessibilityNodeInfo, description: String): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(rootNode)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()

            // Check if this node contains the description
            val nodeDesc = node.contentDescription?.toString() ?: ""
            if (nodeDesc.contains(description, ignoreCase = true)) {
                return node
            }

            // Add child nodes to the queue
            for (i in 0 until node.childCount) {
                val childNode = node.getChild(i)
                if (childNode != null) {
                    queue.add(childNode)
                }
            }
        }

        return null
    }

    /**
     * Finds a node by its view ID
     * @param rootNode The root accessibility node
     * @param id The view ID to search for
     * @return The node with the specified ID if found, null otherwise
     */
    private fun findNodeById(rootNode: AccessibilityNodeInfo, id: String): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(rootNode)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()

            // Check if this node has the specified ID
            val viewId = node.viewIdResourceName
            if (viewId != null && viewId.endsWith(id)) {
                return node
            }

            // Add child nodes to the queue
            for (i in 0 until node.childCount) {
                val childNode = node.getChild(i)
                if (childNode != null) {
                    queue.add(childNode)
                }
            }
        }

        return null
    }

    private fun startForeground() {
        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.CHANNEL_ID_FOREGROUND,
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
        val notification = NotificationCompat.Builder(this, Constants.CHANNEL_ID_FOREGROUND)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("EazyDelivery is running")
            .setContentText("Monitoring delivery apps")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()

        // Start as a foreground service
        startForeground(2, notification)
    }

    override fun onInterrupt() {
        Timber.d("Accessibility Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()

        // Cancel all coroutines to prevent memory leaks
        serviceScope.coroutineContext.cancelChildren()

        // Clean up any temporary files
        cleanupTempFiles()

        Timber.d("Accessibility Service destroyed")
    }

    /**
     * Cleans up temporary screenshot files
     */
    private fun cleanupTempFiles() {
        try {
            val tempDir = File(cacheDir, "temp_screenshots")
            if (tempDir.exists()) {
                val files = tempDir.listFiles()
                files?.forEach { file ->
                    if (file.isFile && file.name.startsWith("screenshot_")) {
                        file.delete()
                    }
                }
                Timber.d("Cleaned up temporary screenshot files")
            }
        } catch (e: Exception) {
            errorHandler.handleException("AccessibilityService.cleanupTempFiles", e)
        }
    }

    /**
     * Determines if the current UI is from Instamart rather than Swiggy
     * @param rootNode The root accessibility node
     * @return true if this is Instamart, false otherwise
     */
    private fun isInstamart(rootNode: AccessibilityNodeInfo?): Boolean {
        if (rootNode == null) return false

        try {
            // Look for Instamart-specific UI elements
            val instamartKeywords = listOf(
                "instamart", "grocery", "groceries", "household", "essentials",
                "instant delivery", "minutes delivery"
            )

            // Check window title
            val title = rootNode.packageName?.toString() ?: ""
            if (title.contains("instamart", ignoreCase = true)) {
                return true
            }

            // Search for Instamart-specific text in the UI
            val queue = ArrayDeque<AccessibilityNodeInfo>()
            queue.add(rootNode)

            while (queue.isNotEmpty()) {
                val node = queue.removeFirst()

                // Check node text
                val nodeText = node.text?.toString() ?: ""
                if (instamartKeywords.any { nodeText.contains(it, ignoreCase = true) }) {
                    return true
                }

                // Check content description
                val nodeDesc = node.contentDescription?.toString() ?: ""
                if (instamartKeywords.any { nodeDesc.contains(it, ignoreCase = true) }) {
                    return true
                }

                // Add child nodes to the queue
                for (i in 0 until node.childCount) {
                    val childNode = node.getChild(i)
                    if (childNode != null) {
                        queue.add(childNode)
                    }
                }
            }

            // Default to Swiggy if no Instamart indicators found
            return false
        } catch (e: Exception) {
            errorHandler.handleException("AccessibilityService.isInstamart", e)
            return false
        }
    }
}


