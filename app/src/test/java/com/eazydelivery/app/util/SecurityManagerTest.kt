package com.eazydelivery.app.util

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import java.lang.reflect.Field

@RunWith(MockitoJUnitRunner::class)
class SecurityManagerTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences

    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    private lateinit var securityManager: SecurityManager

    @Before
    fun setup() {
        // Mock the shared preferences
        Mockito.`when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        Mockito.`when`(mockEditor.putString(Mockito.anyString(), Mockito.anyString())).thenReturn(mockEditor)
        Mockito.`when`(mockEditor.putInt(Mockito.anyString(), Mockito.anyInt())).thenReturn(mockEditor)
        Mockito.`when`(mockEditor.remove(Mockito.anyString())).thenReturn(mockEditor)
        
        // Create a test instance with mocked dependencies
        securityManager = SecurityManager(mockContext)
        
        // Use reflection to set the encryptedSharedPreferences field
        setPrivateField(securityManager, "encryptedSharedPreferences", mockSharedPreferences)
    }

    @Test
    fun `secureStore should store value in encrypted shared preferences`() {
        // Given
        val key = "test_key"
        val value = "test_value"
        
        // When
        val result = securityManager.secureStore(key, value)
        
        // Then
        assertTrue(result)
        Mockito.verify(mockEditor).putString(key, value)
        Mockito.verify(mockEditor).apply()
    }

    @Test
    fun `secureRetrieve should retrieve value from encrypted shared preferences`() {
        // Given
        val key = "test_key"
        val value = "test_value"
        Mockito.`when`(mockSharedPreferences.getString(key, "")).thenReturn(value)
        
        // When
        val result = securityManager.secureRetrieve(key)
        
        // Then
        assertEquals(value, result)
    }

    @Test
    fun `secureRemove should remove value from encrypted shared preferences`() {
        // Given
        val key = "test_key"
        
        // When
        val result = securityManager.secureRemove(key)
        
        // Then
        assertTrue(result)
        Mockito.verify(mockEditor).remove(key)
        Mockito.verify(mockEditor).apply()
    }
    
    @Test
    fun `rotateEncryptionKey should update key version`() {
        // Given
        Mockito.`when`(mockSharedPreferences.getInt("encryption_key_version", 1)).thenReturn(1)
        
        // When
        val result = securityManager.rotateEncryptionKey()
        
        // Then
        assertTrue(result)
        Mockito.verify(mockEditor).putInt("encryption_key_version", 2)
        Mockito.verify(mockEditor).apply()
    }
    
    /**
     * Helper method to set private fields using reflection
     */
    private fun setPrivateField(target: Any, fieldName: String, value: Any?) {
        val field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(target, value)
    }
}
