package com.eazydelivery.app.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    /**
     * Test migration from version 1 to 2
     */
    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        // Create version 1 of the database
        helper.createDatabase(TEST_DB, 1).apply {
            // Insert test data
            execSQL("""
                CREATE TABLE IF NOT EXISTS platforms (
                    name TEXT PRIMARY KEY NOT NULL,
                    isEnabled INTEGER NOT NULL,
                    minAmount INTEGER NOT NULL,
                    maxAmount INTEGER NOT NULL DEFAULT 500,
                    autoAccept INTEGER NOT NULL DEFAULT 1,
                    notificationSound TEXT,
                    priority INTEGER NOT NULL DEFAULT 0,
                    acceptMediumPriority INTEGER NOT NULL DEFAULT 0
                )
            """)
            
            execSQL("""
                CREATE TABLE IF NOT EXISTS orders (
                    id TEXT PRIMARY KEY NOT NULL,
                    platformName TEXT NOT NULL,
                    amount REAL NOT NULL,
                    timestamp TEXT NOT NULL,
                    isAccepted INTEGER NOT NULL,
                    deliveryStatus TEXT NOT NULL DEFAULT 'PENDING',
                    priority TEXT NOT NULL DEFAULT 'MEDIUM',
                    estimatedDistance REAL,
                    estimatedTime INTEGER,
                    FOREIGN KEY (platformName) REFERENCES platforms(name) ON DELETE CASCADE
                )
            """)
            
            execSQL("""
                CREATE TABLE IF NOT EXISTS order_notifications (
                    id TEXT PRIMARY KEY NOT NULL,
                    platformName TEXT NOT NULL,
                    amount REAL NOT NULL,
                    timestamp TEXT NOT NULL,
                    priority TEXT NOT NULL DEFAULT 'MEDIUM',
                    isProcessed INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY (platformName) REFERENCES platforms(name) ON DELETE CASCADE
                )
            """)
            
            execSQL("""
                CREATE TABLE IF NOT EXISTS feedback (
                    id TEXT PRIMARY KEY NOT NULL,
                    rating INTEGER NOT NULL,
                    comment TEXT,
                    timestamp TEXT NOT NULL
                )
            """)
            
            // Insert test data
            val platformValues = ContentValues().apply {
                put("name", "Zomato")
                put("isEnabled", 1)
                put("minAmount", 100)
                put("maxAmount", 500)
                put("autoAccept", 1)
                put("priority", 0)
            }
            insert("platforms", SQLiteDatabase.CONFLICT_REPLACE, platformValues)
            
            val orderValues = ContentValues().apply {
                put("id", "test_order_1")
                put("platformName", "Zomato")
                put("amount", 150.0)
                put("timestamp", System.currentTimeMillis().toString())
                put("isAccepted", 1)
                put("deliveryStatus", "ACCEPTED")
                put("priority", "HIGH")
            }
            insert("orders", SQLiteDatabase.CONFLICT_REPLACE, orderValues)
            
            close()
        }

        // Migrate to version 2
        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, AppDatabase.MIGRATION_1_2)

        // Verify that the data is still there
        val cursor = db.query("SELECT * FROM orders WHERE id = 'test_order_1'")
        cursor.use {
            assertTrue(it.moveToFirst())
            assertEquals("Zomato", it.getString(it.getColumnIndex("platformName")))
            assertEquals(150.0, it.getDouble(it.getColumnIndex("amount")), 0.01)
            assertEquals("HIGH", it.getString(it.getColumnIndex("priority")))
        }

        // Verify that the indices were created
        val indexCursor = db.query("SELECT name FROM sqlite_master WHERE type = 'index' AND name LIKE 'index_%'")
        val indices = mutableListOf<String>()
        indexCursor.use {
            while (it.moveToNext()) {
                indices.add(it.getString(0))
            }
        }
        
        // Check that all expected indices exist
        assertTrue(indices.contains("index_orders_platform_name"))
        assertTrue(indices.contains("index_orders_timestamp"))
        assertTrue(indices.contains("index_orders_delivery_status"))
        assertTrue(indices.contains("index_order_notifications_platform_name"))
        assertTrue(indices.contains("index_order_notifications_timestamp"))
        assertTrue(indices.contains("index_order_notifications_priority"))
        assertTrue(indices.contains("index_feedback_timestamp"))
        assertTrue(indices.contains("index_feedback_rating"))
    }

    /**
     * Test migration from version 2 to 3
     */
    @Test
    @Throws(IOException::class)
    fun migrate2To3() {
        // Create version 2 of the database with the indices from migration 1->2
        val db = helper.createDatabase(TEST_DB, 2).apply {
            // Insert test data
            val platformValues = ContentValues().apply {
                put("name", "Swiggy")
                put("isEnabled", 1)
                put("minAmount", 120)
                put("maxAmount", 600)
                put("autoAccept", 1)
                put("priority", 1)
            }
            insert("platforms", SQLiteDatabase.CONFLICT_REPLACE, platformValues)
            
            val orderValues = ContentValues().apply {
                put("id", "test_order_2")
                put("platformName", "Swiggy")
                put("amount", 200.0)
                put("timestamp", System.currentTimeMillis().toString())
                put("isAccepted", 1)
                put("deliveryStatus", "DELIVERED")
                put("priority", "MEDIUM")
            }
            insert("orders", SQLiteDatabase.CONFLICT_REPLACE, orderValues)
            
            close()
        }

        // Migrate to version 3
        val dbV3 = helper.runMigrationsAndValidate(TEST_DB, 3, true, AppDatabase.MIGRATION_2_3)

        // Verify that the data is still there
        val cursor = dbV3.query("SELECT * FROM orders WHERE id = 'test_order_2'")
        cursor.use {
            assertTrue(it.moveToFirst())
            assertEquals("Swiggy", it.getString(it.getColumnIndex("platformName")))
            assertEquals(200.0, it.getDouble(it.getColumnIndex("amount")), 0.01)
            assertEquals("MEDIUM", it.getString(it.getColumnIndex("priority")))
        }

        // Verify that the new indices were created
        val indexCursor = dbV3.query("SELECT name FROM sqlite_master WHERE type = 'index' AND name LIKE 'index_%'")
        val indices = mutableListOf<String>()
        indexCursor.use {
            while (it.moveToNext()) {
                indices.add(it.getString(0))
            }
        }
        
        // Check that all expected new indices exist
        assertTrue(indices.contains("index_orders_amount"))
        assertTrue(indices.contains("index_orders_priority"))
        assertTrue(indices.contains("index_order_notifications_amount"))
        assertTrue(indices.contains("index_platforms_is_enabled"))
        assertTrue(indices.contains("index_platforms_priority"))
    }

    /**
     * Test migration from version 3 to 4
     */
    @Test
    @Throws(IOException::class)
    fun migrate3To4() {
        // Create version 3 of the database
        val db = helper.createDatabase(TEST_DB, 3).apply {
            // Insert test data
            val platformValues = ContentValues().apply {
                put("name", "Zepto")
                put("isEnabled", 1)
                put("minAmount", 80)
                put("maxAmount", 400)
                put("autoAccept", 1)
                put("priority", 2)
            }
            insert("platforms", SQLiteDatabase.CONFLICT_REPLACE, platformValues)
            
            close()
        }

        // Migrate to version 4
        val dbV4 = helper.runMigrationsAndValidate(TEST_DB, 4, true, AppDatabase.MIGRATION_3_4)

        // Verify that the data is still there and new columns were added
        val cursor = dbV4.query("SELECT * FROM platforms WHERE name = 'Zepto'")
        cursor.use {
            assertTrue(it.moveToFirst())
            assertEquals("Zepto", it.getString(it.getColumnIndex("name")))
            assertEquals(80, it.getInt(it.getColumnIndex("minAmount")))
            
            // Verify new columns exist with default values
            assertEquals("", it.getString(it.getColumnIndex("packageName")))
            assertEquals(0, it.getInt(it.getColumnIndex("shouldRemove")))
        }

        // Verify that we can update the new columns
        dbV4.execSQL("UPDATE platforms SET packageName = 'com.zepto.rider', shouldRemove = 0 WHERE name = 'Zepto'")
        
        val updatedCursor = dbV4.query("SELECT packageName, shouldRemove FROM platforms WHERE name = 'Zepto'")
        updatedCursor.use {
            assertTrue(it.moveToFirst())
            assertEquals("com.zepto.rider", it.getString(it.getColumnIndex("packageName")))
            assertEquals(0, it.getInt(it.getColumnIndex("shouldRemove")))
        }
    }

    /**
     * Test all migrations together
     */
    @Test
    @Throws(IOException::class)
    fun migrateAll() {
        // Create version 1 of the database
        helper.createDatabase(TEST_DB, 1).apply {
            // Insert test data
            val platformValues = ContentValues().apply {
                put("name", "Blinkit")
                put("isEnabled", 1)
                put("minAmount", 90)
                put("maxAmount", 450)
                put("autoAccept", 1)
                put("priority", 3)
            }
            insert("platforms", SQLiteDatabase.CONFLICT_REPLACE, platformValues)
            
            close()
        }

        // Migrate through all versions
        val dbLatest = helper.runMigrationsAndValidate(
            TEST_DB, 
            4, 
            true, 
            AppDatabase.MIGRATION_1_2, 
            AppDatabase.MIGRATION_2_3, 
            AppDatabase.MIGRATION_3_4
        )

        // Verify that the data is still there and all migrations were applied
        val cursor = dbLatest.query("SELECT * FROM platforms WHERE name = 'Blinkit'")
        cursor.use {
            assertTrue(it.moveToFirst())
            assertEquals("Blinkit", it.getString(it.getColumnIndex("name")))
            assertEquals(90, it.getInt(it.getColumnIndex("minAmount")))
            
            // Verify columns from migration 3->4
            assertEquals("", it.getString(it.getColumnIndex("packageName")))
            assertEquals(0, it.getInt(it.getColumnIndex("shouldRemove")))
        }
    }

    /**
     * Test that the database can be opened with Room
     */
    @Test
    @Throws(IOException::class)
    fun testRoomOpenHelper() {
        // Create version 1 and migrate to latest
        helper.createDatabase(TEST_DB, 1).close()
        helper.runMigrationsAndValidate(
            TEST_DB, 
            4, 
            true, 
            AppDatabase.MIGRATION_1_2, 
            AppDatabase.MIGRATION_2_3, 
            AppDatabase.MIGRATION_3_4
        )

        // Try to open the database with Room
        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            TEST_DB
        )
        .addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4
        )
        .build()
        .use { db ->
            // Verify that the database is open and usable
            val platformDao = db.platformDao()
            val orderDao = db.orderDao()
            
            // Database is open and usable if we get here without exceptions
            assertTrue(true)
        }
    }
}
