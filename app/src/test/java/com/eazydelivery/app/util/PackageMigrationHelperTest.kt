package com.eazydelivery.app.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.eazydelivery.app.data.model.Platform
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(MockitoJUnitRunner::class)
class PackageMigrationHelperTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockErrorHandler: ErrorHandler

    @Mock
    private lateinit var mockPackageManager: PackageManager

    private lateinit var packageMigrationHelper: PackageMigrationHelper

    @Before
    fun setup() {
        `when`(mockContext.packageManager).thenReturn(mockPackageManager)
        packageMigrationHelper = PackageMigrationHelper(mockContext, mockErrorHandler)
    }

    @Test
    fun `migratePackageName should convert old package to new package`() {
        val oldPackage = "com.application.eazydelivery.service.SomeService"
        val expectedNewPackage = "com.eazydelivery.app.service.SomeService"

        val result = packageMigrationHelper.migratePackageName(oldPackage)

        assertEquals(expectedNewPackage, result)
    }

    @Test
    fun `migratePackageName should handle Dunzo package correctly`() {
        val dunzoPackage = "com.dunzo.delivery"
        
        val result = packageMigrationHelper.migratePackageName(dunzoPackage)
        
        // Dunzo should be removed, so it should return the original package
        // but the platform should be marked for removal when used
        assertEquals(dunzoPackage, result)
    }

    @Test
    fun `migratePackageName should handle Uber Eats package correctly`() {
        val oldUberPackage = "com.ubercab.eats"
        val expectedNewPackage = "com.ubercab.driver"
        
        val result = packageMigrationHelper.migratePackageName(oldUberPackage)
        
        // Should be updated to the new package
        assertEquals(expectedNewPackage, result)
    }

    @Test
    fun `migratePlatform should mark Dunzo platform for removal`() {
        val dunzoPlatform = Platform(
            name = "dunzo",
            isEnabled = true,
            minAmount = 100,
            packageName = "com.dunzo.delivery"
        )
        
        val result = packageMigrationHelper.migratePlatform(dunzoPlatform)
        
        // Should be marked for removal
        assertFalse(result.isEnabled)
        assertTrue(result.shouldRemove)
    }

    @Test
    fun `migratePlatform should update Uber Eats package`() {
        val uberPlatform = Platform(
            name = "ubereats",
            isEnabled = true,
            minAmount = 100,
            packageName = "com.ubercab.eats"
        )
        
        // Mock the package migration mapping
        val result = packageMigrationHelper.migratePlatform(uberPlatform)
        
        // Should have updated package name
        assertEquals("com.ubercab.driver", result.packageName)
    }

    @Test
    fun `isAppInstalled should return false for removed packages`() {
        val dunzoPackage = "com.dunzo.delivery"
        
        val result = packageMigrationHelper.isAppInstalled(dunzoPackage)
        
        // Should return false for Dunzo
        assertFalse(result)
    }

    @Test
    fun `migrateIntent should handle old package intents`() {
        val oldIntent = Intent()
        oldIntent.setClassName("com.application.eazydelivery", "com.application.eazydelivery.MainActivity")
        
        val result = packageMigrationHelper.migrateIntent(oldIntent)
        
        // Should have updated component
        assertEquals("com.eazydelivery.app", result.component?.packageName)
        assertEquals("com.eazydelivery.app.MainActivity", result.component?.className)
    }
}
