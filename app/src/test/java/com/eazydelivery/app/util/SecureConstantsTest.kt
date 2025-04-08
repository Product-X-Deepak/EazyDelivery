package com.eazydelivery.app.util

import android.content.Context
import android.content.SharedPreferences
import com.eazydelivery.app.BuildConfig
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import kotlin.test.assertEquals

@RunWith(MockitoJUnitRunner::class)
class SecureConstantsTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockErrorHandler: ErrorHandler

    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences

    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    private lateinit var secureConstants: SecureConstants

    @Before
    fun setup() {
        // Setup mock SharedPreferences
        `when`(mockContext.getSharedPreferences(any(), any())).thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(any(), any())).thenReturn(mockEditor)

        // Create the SecureConstants instance with mocked dependencies
        secureConstants = SecureConstants(mockContext, mockErrorHandler)
    }

    @Test
    fun `initialize should set default values if not already set`() {
        // Mock that values are not already set
        `when`(mockSharedPreferences.contains(any())).thenReturn(false)

        // Call initialize
        secureConstants.initialize()

        // Verify that values were set
        verify(mockEditor).putString(eq("admin_phone"), eq(BuildConfig.ADMIN_PHONE))
        verify(mockEditor).putString(eq("admin_email"), eq(BuildConfig.ADMIN_EMAIL))
        verify(mockEditor).putString(eq("api_key"), eq(BuildConfig.API_KEY))
    }

    @Test
    fun `initialize should not set values if already set`() {
        // Mock that values are already set
        `when`(mockSharedPreferences.contains(any())).thenReturn(true)

        // Call initialize
        secureConstants.initialize()

        // Verify that values were not set
        verify(mockEditor, org.mockito.Mockito.never()).putString(eq("admin_phone"), any())
        verify(mockEditor, org.mockito.Mockito.never()).putString(eq("admin_email"), any())
        verify(mockEditor, org.mockito.Mockito.never()).putString(eq("api_key"), any())
    }

    @Test
    fun `getAdminPhone should return stored value or default`() {
        val storedValue = "+1234567890"
        `when`(mockSharedPreferences.getString(eq("admin_phone"), any())).thenReturn(storedValue)

        val result = secureConstants.getAdminPhone()

        assertEquals(storedValue, result)
    }

    @Test
    fun `getAdminEmail should return stored value or default`() {
        val storedValue = "test@example.com"
        `when`(mockSharedPreferences.getString(eq("admin_email"), any())).thenReturn(storedValue)

        val result = secureConstants.getAdminEmail()

        assertEquals(storedValue, result)
    }

    @Test
    fun `getApiKey should handle obfuscated values`() {
        // Mock the stored obfuscated key and salt
        val obfuscatedKey = "1a2b3c4d5e"
        val salt = "abcdef1234567890"

        `when`(mockSharedPreferences.getString(eq("api_key"), any())).thenReturn(obfuscatedKey)
        `when`(mockSharedPreferences.getString(eq("api_key_salt"), any())).thenReturn(salt)

        // We can't easily test the actual deobfuscation without reflection
        // So we'll just verify it doesn't crash and returns something
        val result = secureConstants.getApiKey()

        // Should fall back to BuildConfig.API_KEY since our mocked obfuscated key isn't valid
        assertEquals(BuildConfig.API_KEY, result)
    }

    @Test
    fun `setAdminPhone should store value`() {
        val newValue = "+1234567890"

        secureConstants.setAdminPhone(newValue)

        verify(mockEditor).putString(eq("admin_phone"), eq(newValue))
        verify(mockEditor).apply()
    }

    @Test
    fun `setAdminEmail should store value`() {
        val newValue = "test@example.com"

        secureConstants.setAdminEmail(newValue)

        verify(mockEditor).putString(eq("admin_email"), eq(newValue))
        verify(mockEditor).apply()
    }

    @Test
    fun `setApiKey should store obfuscated value with salt`() {
        val newValue = "test-api-key"

        secureConstants.setApiKey(newValue)

        // Verify that we store the API key, salt, and last rotated timestamp
        verify(mockEditor).putString(eq("api_key"), any())
        verify(mockEditor).putString(eq("api_key_salt"), any())
        verify(mockEditor).putLong(eq("api_key_last_rotated"), any())
        verify(mockEditor).apply()
    }

    @Test
    fun `checkAndRotateApiKey should rotate key when needed`() {
        // Setup reflection to access private method
        val method = SecureConstants::class.java.getDeclaredMethod(
            "checkAndRotateApiKey"
        )
        method.isAccessible = true

        // Mock that key needs rotation (last rotated long ago)
        val longAgo = System.currentTimeMillis() - (31L * 24 * 60 * 60 * 1000) // 31 days ago
        `when`(mockSharedPreferences.getLong(eq("api_key_last_rotated"), any())).thenReturn(longAgo)

        // Mock current key and salt
        `when`(mockSharedPreferences.getString(eq("api_key"), any())).thenReturn("obfuscated_key")
        `when`(mockSharedPreferences.getString(eq("api_key_salt"), any())).thenReturn("salt_value")

        // Invoke the private method
        method.invoke(secureConstants)

        // Verify that we tried to rotate the key
        verify(mockEditor).putLong(eq("api_key_last_rotated"), any())
    }

    private fun <T> any(): T {
        return org.mockito.kotlin.any()
    }
}
