package com.eazydelivery.app.util

import android.content.Context
import androidx.room.Room
import com.eazydelivery.app.data.local.AppDatabase
import com.eazydelivery.app.domain.model.OrderNotification
import com.eazydelivery.app.ml.NotificationClassifier
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages caching of data and cleanup of temporary files
 */
@Singleton
class CacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureStorage: SecureStorage,
    private val errorHandler: ErrorHandler
) {
    private val cacheScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // LRU cache for recent notifications
    private val notificationCache = LruCache<String, OrderNotification>(100)

    // Database for persistent storage
    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "eazydelivery_db"
        ).build()
    }

    init {
        // Schedule periodic cleanup
        scheduleCleanup()
    }

    /**
     * Caches an order notification and its classification
     */
    fun cacheOrderNotification(
        notification: OrderNotification,
        classification: Map<NotificationClassifier.Label, Float>,
        priority: String
    ) {
        cacheScope.launch {
            try {
                // Add to in-memory cache with weak reference
                notificationCache.put(notification.id, WeakReference(notification))

                // Store in database for persistence and later analysis
                val notificationEntity = com.eazydelivery.app.data.local.entity.OrderNotificationEntity(
                    id = notification.id,
                    packageName = notification.packageName,
                    title = notification.title,
                    text = notification.text,
                    timestamp = notification.timestamp,
                    amount = notification.amount,
                    platformName = notification.platformName,
                    estimatedDistance = notification.estimatedDistance,
                    estimatedTime = notification.estimatedTime,
                    priority = priority,
                    highEarningScore = classification[NotificationClassifier.Label.HIGH_EARNING] ?: 0f,
                    lowDistanceScore = classification[NotificationClassifier.Label.LOW_DISTANCE] ?: 0f,
                    busyTimeScore = classification[NotificationClassifier.Label.BUSY_TIME] ?: 0f,
                    lowPriorityScore = classification[NotificationClassifier.Label.LOW_PRIORITY] ?: 0f
                )

                database.orderNotificationDao().insertOrderNotification(notificationEntity)

                Timber.d("Cached notification ${notification.id} with priority $priority")

            } catch (e: Exception) {
                errorHandler.handleException("CacheManager.cacheOrderNotification", e)
            }
        }
    }

    /**
     * Retrieves a cached notification by ID
     */
    fun getNotification(id: String): OrderNotification? {
        return notificationCache.get(id)
    }

    /**
     * Schedules periodic cleanup of cache and temporary files
     */
    private fun scheduleCleanup() {
        cacheScope.launch {
            try {
                // Run cleanup every hour
                while (true) {
                    cleanupTemporaryFiles()
                    cleanupOldLogs()
                    cleanupOldNotifications()

                    // Wait for 1 hour
                    kotlinx.coroutines.delay(TimeUnit.HOURS.toMillis(1))
                }
            } catch (e: Exception) {
                errorHandler.handleException("CacheManager.scheduleCleanup", e)
            }
        }
    }

    /**
     * Cleans up temporary screenshot files
     */
    private fun cleanupTemporaryFiles() {
        try {
            val tempDir = File(context.cacheDir, "temp_screenshots")
            if (tempDir.exists()) {
                val files = tempDir.listFiles() ?: return
                val now = System.currentTimeMillis()

                for (file in files) {
                    // Delete files older than 24 hours
                    if (now - file.lastModified() > TimeUnit.HOURS.toMillis(24)) {
                        file.delete()
                        Timber.d("Deleted temporary file: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            errorHandler.handleException("CacheManager.cleanupTemporaryFiles", e)
        }
    }

    /**
     * Cleans up old log files
     */
    private fun cleanupOldLogs() {
        try {
            val logsDir = File(context.filesDir, "logs")
            if (logsDir.exists()) {
                val files = logsDir.listFiles() ?: return
                val now = System.currentTimeMillis()

                for (file in files) {
                    // Delete log files older than 7 days
                    if (now - file.lastModified() > TimeUnit.DAYS.toMillis(7)) {
                        file.delete()
                        Timber.d("Deleted old log file: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            errorHandler.handleException("CacheManager.cleanupOldLogs", e)
        }
    }

    /**
     * Cleans up old notification records from the database
     */
    private fun cleanupOldNotifications() {
        cacheScope.launch {
            try {
                // Delete notifications older than 30 days
                val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
                val count = database.orderNotificationDao().deleteOldNotifications(cutoffTime)

                if (count > 0) {
                    Timber.d("Deleted $count old notification records")
                }
            } catch (e: Exception) {
                errorHandler.handleException("CacheManager.cleanupOldNotifications", e)
            }
        }
    }

    /**
     * Enhanced LRU cache implementation with cleanup of expired weak references
     */
    private class LruCache<K, V>(private val maxSize: Int) {
        private val map = LinkedHashMap<K, V>(maxSize, 0.75f, true)

        fun put(key: K, value: V) {
            synchronized(map) {
                map[key] = value
                trimToSize()
            }
        }

        fun get(key: K): V? {
            synchronized(map) {
                return map[key]
            }
        }

        private fun trimToSize() {
            synchronized(map) {
                // First, remove any entries with null values (expired weak references)
                val iterator = map.entries.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    if (entry.value is WeakReference<*> && (entry.value as WeakReference<*>).get() == null) {
                        iterator.remove()
                    }
                }

                // Then trim to max size if still needed
                while (map.size > maxSize) {
                    val eldest = map.entries.iterator().next()
                    map.remove(eldest.key)
                }
            }
        }

        /**
         * Clears all entries from the cache
         */
        fun clear() {
            synchronized(map) {
                map.clear()
            }
        }
    }
}


