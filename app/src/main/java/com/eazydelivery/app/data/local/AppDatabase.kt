package com.eazydelivery.app.data.local

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.RoomDatabase.JournalMode
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.eazydelivery.app.data.local.converter.DateConverter
import com.eazydelivery.app.data.local.dao.FeedbackDao
import com.eazydelivery.app.data.local.dao.OptimizedQueries
import com.eazydelivery.app.data.local.dao.OrderDao
import com.eazydelivery.app.data.local.dao.OrderNotificationDao
import com.eazydelivery.app.data.local.dao.PlatformDao
import com.eazydelivery.app.data.local.entity.FeedbackEntity
import com.eazydelivery.app.data.local.entity.OrderEntity
import com.eazydelivery.app.data.local.entity.OrderNotificationEntity
import com.eazydelivery.app.data.local.entity.PlatformEntity
import com.eazydelivery.app.util.PlatformResources
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Main database for the EazyDelivery app.
 *
 * This database stores information about delivery platforms, orders, notifications, and user feedback.
 * It uses Room as the persistence library and includes migrations to handle schema changes.
 *
 * See DatabaseMigrations.md for detailed information about the database migrations.
 */
@Database(
    entities = [
        PlatformEntity::class,
        OrderEntity::class,
        OrderNotificationEntity::class,
        FeedbackEntity::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    /**
     * Data Access Object for platform operations
     */
    abstract fun platformDao(): PlatformDao

    /**
     * Data Access Object for order operations
     */
    abstract fun orderDao(): OrderDao

    /**
     * Data Access Object for order notification operations
     */
    abstract fun orderNotificationDao(): OrderNotificationDao

    /**
     * Data Access Object for feedback operations
     */
    abstract fun feedbackDao(): FeedbackDao

    /**
     * Optimized queries for better performance
     */
    abstract fun optimizedQueries(): OptimizedQueries

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration from version 1 to 2
         *
         * This migration adds performance indices to improve query performance.
         * See DatabaseMigrations.md for more details.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Create indices for better query performance
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_orders_platform_name ON orders(platformName)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_orders_timestamp ON orders(timestamp)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_orders_delivery_status ON orders(deliveryStatus)")

                    database.execSQL("CREATE INDEX IF NOT EXISTS index_order_notifications_platform_name ON order_notifications(platformName)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_order_notifications_timestamp ON order_notifications(timestamp)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_order_notifications_priority ON order_notifications(priority)")

                    database.execSQL("CREATE INDEX IF NOT EXISTS index_feedback_timestamp ON feedback(timestamp)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_feedback_rating ON feedback(rating)")

                    Timber.d("Database migration 1->2 completed successfully")
                } catch (e: Exception) {
                    Timber.e(e, "Error during database migration 1->2")
                }
            }
        }

        /**
         * Migration from version 2 to 3
         *
         * This migration adds additional indices for better query performance.
         * See DatabaseMigrations.md for more details.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Create additional indices for better query performance
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_orders_amount ON orders(amount)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_orders_priority ON orders(priority)")

                    database.execSQL("CREATE INDEX IF NOT EXISTS index_order_notifications_amount ON order_notifications(amount)")

                    database.execSQL("CREATE INDEX IF NOT EXISTS index_platforms_is_enabled ON platforms(isEnabled)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_platforms_priority ON platforms(priority)")

                    // Note: VACUUM is now performed in a separate transaction to avoid ANR
                    // We'll schedule it to run in a background thread instead

                    Timber.d("Database migration 2->3 completed successfully")
                } catch (e: Exception) {
                    Timber.e(e, "Error during database migration 2->3")
                }
            }
        }

        /**
         * Migration from version 3 to 4
         *
         * This migration adds new columns to the platforms table:
         * - packageName: The package name of the delivery app
         * - shouldRemove: Flag to indicate if the platform should be removed
         *
         * See DatabaseMigrations.md for more details.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Add new columns to platforms table
                    database.execSQL("ALTER TABLE platforms ADD COLUMN packageName TEXT NOT NULL DEFAULT ''")
                    database.execSQL("ALTER TABLE platforms ADD COLUMN shouldRemove INTEGER NOT NULL DEFAULT 0")

                    // Update package names for existing platforms
                    database.execSQL("UPDATE platforms SET packageName = 'in.swiggy.deliveryapp' WHERE name = 'swiggy'")
                    database.execSQL("UPDATE platforms SET packageName = 'com.zomato.delivery' WHERE name = 'zomato'")
                    database.execSQL("UPDATE platforms SET packageName = 'in.swiggy.deliveryapp' WHERE name = 'instamart'")
                    database.execSQL("UPDATE platforms SET packageName = 'com.zepto.rider' WHERE name = 'zepto'")
                    database.execSQL("UPDATE platforms SET packageName = 'app.blinkit.onboarding' WHERE name = 'blinkit'")
                    database.execSQL("UPDATE platforms SET packageName = 'com.ubercab.driver' WHERE name = 'ubereats'")
                    database.execSQL("UPDATE platforms SET packageName = 'com.bigbasket.delivery' WHERE name = 'bigbasket'")

                    // Create index on packageName for better query performance
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_platforms_package_name ON platforms(packageName)")

                    Timber.d("Database migration 3->4 completed successfully")
                } catch (e: Exception) {
                    Timber.e(e, "Error during database migration 3->4")
                }
            }
        }

        /**
         * Gets the database instance, creating it if it doesn't exist.
         *
         * @param context The application context
         * @param scope The coroutine scope for background operations
         * @return The database instance
         */
        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "eazydelivery_db"
                )
                .addCallback(AppDatabaseCallback(scope))
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, DatabaseIndexes.MIGRATION_ADD_INDEXES)
                .fallbackToDestructiveMigration()
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING) // Improve write performance
                .build()
                INSTANCE = instance

                // Schedule VACUUM operation in a background thread
                scheduleVacuum(instance, context, scope)

                // Schedule periodic optimizations
                schedulePeriodicOptimizations(instance, context, scope)

                instance
            }
        }

        /**
         * Schedules a VACUUM operation to run in a background thread during idle time
         * This helps optimize database storage without impacting app performance
         */
        private fun scheduleVacuum(database: AppDatabase, context: Context, scope: CoroutineScope) {
            scope.launch(Dispatchers.IO) {
                try {
                    // Add a significant delay to ensure this doesn't interfere with app startup
                    // and runs when the app is likely to be idle
                    kotlinx.coroutines.delay(60000) // 1 minute delay

                    // Check if device is charging and has sufficient battery
                    if (isDeviceChargingOrSufficientBattery(context)) {
                        // Run VACUUM with ANALYZE to optimize both storage and query planning
                        database.openHelper.writableDatabase.execSQL("VACUUM ANALYZE")
                        Timber.d("Database VACUUM ANALYZE completed successfully")
                    } else {
                        Timber.d("Skipping database VACUUM due to battery constraints")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error during database VACUUM operation")
                }
            }
        }

        /**
         * Schedules periodic optimizations for the database.
         * This includes ANALYZE, integrity check, and other maintenance tasks.
         *
         * @param db The database instance
         * @param context The application context
         * @param scope The coroutine scope for background operations
         */
        private fun schedulePeriodicOptimizations(db: AppDatabase, context: Context, scope: CoroutineScope) {
            scope.launch(Dispatchers.IO) {
                try {
                    // Run periodic optimizations every 24 hours
                    while (true) {
                        // Wait for 24 hours
                        delay(TimeUnit.HOURS.toMillis(24))

                        // Only run optimizations if the device is charging or has sufficient battery
                        if (isDeviceChargingOrSufficientBattery(context)) {
                            // Use the DatabaseIndexes utility to optimize the database
                            DatabaseIndexes.optimizeDatabase(db.openHelper.writableDatabase)
                            Timber.d("Database optimization completed successfully")

                            // Run integrity check
                            val cursor = db.openHelper.writableDatabase.query("PRAGMA integrity_check")
                            cursor.use {
                                if (it.moveToFirst()) {
                                    val result = it.getString(0)
                                    if (result == "ok") {
                                        Timber.d("Database integrity check passed")
                                    } else {
                                        Timber.w("Database integrity check failed: $result")
                                    }
                                }
                            }

                            // Clean up old data
                            cleanupOldData(db)
                        } else {
                            Timber.d("Skipping periodic optimizations due to battery constraints")
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error during periodic database optimizations")
                }
            }
        }

        /**
         * Cleans up old data to prevent database bloat
         */
        private suspend fun cleanupOldData(db: AppDatabase) {
            try {
                // Keep the last 1000 notifications
                val keepCount = 1000
                val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30) // 30 days

                // Use the optimized query to clean up old notifications
                val deletedCount = db.optimizedQueries().cleanupOldNotifications(cutoffTime, keepCount)

                if (deletedCount > 0) {
                    Timber.d("Cleaned up $deletedCount old notifications")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error cleaning up old data")
            }
        }

        /**
         * Checks if the device is charging or has sufficient battery.
         *
         * @param context The application context
         * @return true if the device is charging or has more than 20% battery, false otherwise
         */
        private fun isDeviceChargingOrSufficientBattery(context: Context): Boolean {
            val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

            // Check if the device is charging
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL

            // Check battery level
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryPct = if (scale > 0) level * 100 / scale.toFloat() else 0f

            // Return true if charging or battery level is above 20%
            return isCharging || batteryPct > 20
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    try {
                        // Prepopulate platforms
                        val platformDao = database.platformDao()

                        // Get all platforms from PlatformResources
                        val platforms = PlatformResources.getAllPlatforms().map { platformData ->
                            val packageName = when(platformData.name) {
                                "swiggy" -> "in.swiggy.deliveryapp"
                                "zomato" -> "com.zomato.delivery"
                                "instamart" -> "in.swiggy.deliveryapp"
                                "zepto" -> "com.zepto.rider"
                                "blinkit" -> "app.blinkit.onboarding"
                                "ubereats" -> "com.ubercab.driver"
                                "bigbasket" -> "com.bigbasket.delivery"
                                else -> ""
                            }

                            PlatformEntity(
                                name = platformData.name,
                                isEnabled = false,
                                minAmount = 100, // Default minimum amount
                                maxAmount = 500,
                                autoAccept = true,
                                notificationSound = null,
                                priority = 0,
                                acceptMediumPriority = false,
                                packageName = packageName,
                                shouldRemove = false
                            )
                        }

                        // Insert all platforms
                        platformDao.insertAllPlatforms(platforms)
                        Timber.d("Database prepopulated with ${platforms.size} platforms")
                    } catch (e: Exception) {
                        Timber.e(e, "Error prepopulating database")
                    }
                }
            }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            // You can perform operations when the database is opened
            Timber.d("Database opened")
        }
    }
}
