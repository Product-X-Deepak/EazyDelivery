package com.eazydelivery.app.util

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(MockitoJUnitRunner::class)
class NotificationParserTest {

    @Mock
    private lateinit var mockPackageMigrationHelper: PackageMigrationHelper

    @Mock
    private lateinit var mockErrorHandler: ErrorHandler

    private lateinit var notificationParser: NotificationParser

    @Before
    fun setup() {
        // Setup the mock to return the same package name by default
        `when`(mockPackageMigrationHelper.migratePackageName(any())).thenAnswer { invocation ->
            invocation.getArgument(0)
        }
        
        notificationParser = NotificationParser(mockPackageMigrationHelper, mockErrorHandler)
    }

    @Test
    fun `isDeliveryApp should return true for supported packages`() {
        // Test with Swiggy package
        assertTrue(notificationParser.isDeliveryApp("in.swiggy.deliveryapp"))
        
        // Test with Zomato package
        assertTrue(notificationParser.isDeliveryApp("com.zomato.delivery"))
    }

    @Test
    fun `parseNotification should extract order details from Swiggy notification`() {
        val packageName = "in.swiggy.deliveryapp"
        val title = "New Order: ₹150"
        val text = "Pickup from Restaurant A, 3.5 km away, estimated time 25 min"
        
        val result = notificationParser.parseNotification(packageName, title, text)
        
        assertNotNull(result)
        assertEquals("swiggy", result.platformName)
        assertEquals(150.0, result.amount)
        assertEquals(3.5, result.estimatedDistance)
        assertEquals(25, result.estimatedTime)
    }

    @Test
    fun `parseNotification should extract order details from Zomato notification`() {
        val packageName = "com.zomato.delivery"
        val title = "Zomato Order: ₹200"
        val text = "New delivery request, 2 km away, 15 min delivery time"
        
        val result = notificationParser.parseNotification(packageName, title, text)
        
        assertNotNull(result)
        assertEquals("zomato", result.platformName)
        assertEquals(200.0, result.amount)
        assertEquals(2.0, result.estimatedDistance)
        assertEquals(15, result.estimatedTime)
    }

    @Test
    fun `parseNotification should handle migrated packages`() {
        val oldPackage = "com.ubercab.eats"
        val newPackage = "com.ubercab.driver"
        val title = "Uber Eats Order: ₹180"
        val text = "New delivery, 4 km away, 30 min"
        
        // Setup the mock to return the new package name
        `when`(mockPackageMigrationHelper.migratePackageName(oldPackage)).thenReturn(newPackage)
        
        val result = notificationParser.parseNotification(oldPackage, title, text)
        
        assertNotNull(result)
        assertEquals("ubereats", result.platformName)
        assertEquals(180.0, result.amount)
    }

    @Test
    fun `parseNotification should return null for non-order notifications`() {
        val packageName = "in.swiggy.deliveryapp"
        val title = "Swiggy"
        val text = "Your account has been updated"
        
        val result = notificationParser.parseNotification(packageName, title, text)
        
        assertNull(result)
    }

    @Test
    fun `parseNotification should return null for unsupported packages`() {
        val packageName = "com.unsupported.app"
        val title = "New Order: ₹150"
        val text = "Pickup from Restaurant A, 3.5 km away, estimated time 25 min"
        
        val result = notificationParser.parseNotification(packageName, title, text)
        
        assertNull(result)
    }

    @Test
    fun `parseNotification should handle Dunzo package correctly`() {
        val dunzoPackage = "com.dunzo.delivery"
        val title = "New Order: ₹150"
        val text = "Pickup from Restaurant A, 3.5 km away, estimated time 25 min"
        
        // Setup the mock to return empty string for Dunzo package
        `when`(mockPackageMigrationHelper.migratePackageName(dunzoPackage)).thenReturn("")
        
        val result = notificationParser.parseNotification(dunzoPackage, title, text)
        
        // Should return null since Dunzo is removed
        assertNull(result)
    }

    private fun any(): String = org.mockito.kotlin.any()
}
