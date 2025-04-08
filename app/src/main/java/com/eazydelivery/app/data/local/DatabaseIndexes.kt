package com.eazydelivery.app.data.local

import androidx.room.Index
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import timber.log.Timber

/**
 * Database indexes for optimizing query performance
 * This class defines the indexes used in the database and provides migration to add them
 */
object DatabaseIndexes {
    /**
     * Migration to add indexes to the database
     * This should be applied after all table creation migrations
     */
    val MIGRATION_ADD_INDEXES = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            Timber.d("Applying database index migration")
            
            try {
                // Add index on orders.timestamp for faster date-based queries
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_orders_timestamp ON orders(timestamp)"
                )
                
                // Add index on orders.platformName for faster platform-based queries
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_orders_platformName ON orders(platformName)"
                )
                
                // Add index on orders.deliveryStatus for faster status-based queries
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_orders_deliveryStatus ON orders(deliveryStatus)"
                )
                
                // Add index on order_notifications.timestamp for faster date-based queries
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_order_notifications_timestamp ON order_notifications(timestamp)"
                )
                
                // Add index on order_notifications.platformName for faster platform-based queries
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_order_notifications_platformName ON order_notifications(platformName)"
                )
                
                // Add index on order_notifications.priority for faster priority-based queries
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_order_notifications_priority ON order_notifications(priority)"
                )
                
                // Add index on feedback.timestamp for faster date-based queries
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_feedback_timestamp ON feedback(timestamp)"
                )
                
                // Add index on feedback.rating for faster rating-based queries
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_feedback_rating ON feedback(rating)"
                )
                
                Timber.d("Database index migration completed successfully")
            } catch (e: Exception) {
                Timber.e(e, "Error applying database index migration")
                throw e
            }
        }
    }
    
    /**
     * Optimizes the database by analyzing tables and rebuilding indexes
     * This should be called periodically to maintain performance
     * 
     * @param database The database to optimize
     */
    fun optimizeDatabase(database: SupportSQLiteDatabase) {
        try {
            // Analyze tables to update statistics
            database.execSQL("ANALYZE")
            
            // Rebuild indexes for better performance
            database.execSQL("REINDEX")
            
            Timber.d("Database optimization completed successfully")
        } catch (e: Exception) {
            Timber.e(e, "Error optimizing database")
        }
    }
}
