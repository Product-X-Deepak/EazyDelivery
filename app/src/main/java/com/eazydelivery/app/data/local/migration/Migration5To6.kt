package com.eazydelivery.app.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import timber.log.Timber

/**
 * Migration from schema version 5 to 6
 *
 * This migration adds estimatedDistance and estimatedTime fields to the orders table.
 * Uses transactions to ensure data integrity during migration.
 */
class Migration5To6 : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Timber.d("Migrating database from version 5 to 6")

        // Begin transaction to ensure atomicity
        database.beginTransaction()
        try {
            // Add estimatedDistance and estimatedTime columns to orders table
            database.execSQL("ALTER TABLE orders ADD COLUMN estimatedDistance REAL")
            database.execSQL("ALTER TABLE orders ADD COLUMN estimatedTime INTEGER")

            // Set transaction successful only if all operations complete
            database.setTransactionSuccessful()
            Timber.d("Migration from version 5 to 6 completed successfully")
        } catch (e: Exception) {
            Timber.e(e, "Error during migration from version 5 to 6")
            // Transaction will be rolled back automatically
            throw e
        } finally {
            database.endTransaction()
        }
    }
}
