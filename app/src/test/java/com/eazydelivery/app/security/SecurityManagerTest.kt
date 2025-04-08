package com.eazydelivery.app.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Unit test for the SecurityManager
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class SecurityManagerTest {
    
    private lateinit var context: Context
    private lateinit var securityManager: SecurityManager
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        securityManager = SecurityManager(context)
        securityManager.initialize()
    }
    
    @Test
    fun testEncryptDecrypt() {
        // Given
        val plaintext = "This is a test message"
        
        // When
        val encrypted = securityManager.encrypt(plaintext)
        val decrypted = securityManager.decrypt(encrypted)
        
        // Then
        assertNotNull(encrypted)
        assertNotEquals(plaintext, encrypted)
        assertEquals(plaintext, decrypted)
    }
    
    @Test
    fun testStoreRetrieveSecurely() {
        // Given
        val key = "test_key"
        val value = "test_value"
        
        // When
        securityManager.storeSecurely(key, value)
        val retrieved = securityManager.retrieveSecurely(key)
        
        // Then
        assertEquals(value, retrieved)
    }
    
    @Test
    fun testRemoveSecurely() {
        // Given
        val key = "test_key"
        val value = "test_value"
        securityManager.storeSecurely(key, value)
        
        // When
        securityManager.removeSecurely(key)
        val retrieved = securityManager.retrieveSecurely(key)
        
        // Then
        assertEquals(null, retrieved)
    }
    
    @Test
    fun testClearAllSecurely() {
        // Given
        val key1 = "test_key1"
        val value1 = "test_value1"
        val key2 = "test_key2"
        val value2 = "test_value2"
        securityManager.storeSecurely(key1, value1)
        securityManager.storeSecurely(key2, value2)
        
        // When
        securityManager.clearAllSecurely()
        val retrieved1 = securityManager.retrieveSecurely(key1)
        val retrieved2 = securityManager.retrieveSecurely(key2)
        
        // Then
        assertEquals(null, retrieved1)
        assertEquals(null, retrieved2)
    }
    
    @Test
    fun testHashSha256() {
        // Given
        val input = "test_input"
        
        // When
        val hash1 = securityManager.hashSha256(input)
        val hash2 = securityManager.hashSha256(input)
        
        // Then
        assertNotNull(hash1)
        assertEquals(64, hash1.length) // SHA-256 hash is 64 characters in hex
        assertEquals(hash1, hash2) // Same input should produce same hash
    }
    
    @Test
    fun testGenerateSecureToken() {
        // Given
        val length = 32
        
        // When
        val token1 = securityManager.generateSecureToken(length)
        val token2 = securityManager.generateSecureToken(length)
        
        // Then
        assertNotNull(token1)
        assertEquals(length * 2, token1.length) // Each byte is 2 hex characters
        assertNotEquals(token1, token2) // Different tokens should be generated
    }
}
