package com.eazydelivery.app.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
* Utility class for securely storing sensitive data using EncryptedSharedPreferences
*/
@Singleton
class SecureStorage @Inject constructor(
   @ApplicationContext private val context: Context,
   private val securityManager: SecurityManager
) {
   private val masterKey: MasterKey by lazy {
       MasterKey.Builder(context)
           .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
           .setKeyGenParameterSpec(
               KeyGenParameterSpec.Builder(
                   "_eazy_delivery_master_key_",
                   KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
               )
               .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
               .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
               .setKeySize(256)
               .build()
           )
           .build()
   }

   private val securePreferences by lazy {
       EncryptedSharedPreferences.create(
           context,
           SECURE_PREFS_FILENAME,
           masterKey,
           EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
           EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
       )
   }

   fun saveString(key: String, value: String) {
       // For highly sensitive data, use additional encryption layer
       val encryptedValue = if (SENSITIVE_KEYS.contains(key)) {
           securityManager.encryptString(value)
       } else {
           value
       }
       securePreferences.edit().putString(key, encryptedValue).apply()
   }

   fun getString(key: String, defaultValue: String = ""): String {
       val storedValue = securePreferences.getString(key, defaultValue) ?: defaultValue
       // Decrypt if it's a sensitive key
       return if (SENSITIVE_KEYS.contains(key) && storedValue.isNotEmpty()) {
           securityManager.decryptString(storedValue)
       } else {
           storedValue
       }
   }

   fun saveBoolean(key: String, value: Boolean) {
       securePreferences.edit().putBoolean(key, value).apply()
   }

   fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
       return securePreferences.getBoolean(key, defaultValue)
   }

   fun saveInt(key: String, value: Int) {
       securePreferences.edit().putInt(key, value).apply()
   }

   fun getInt(key: String, defaultValue: Int = 0): Int {
       return securePreferences.getInt(key, defaultValue)
   }

   fun saveLong(key: String, value: Long) {
       securePreferences.edit().putLong(key, value).apply()
   }

   fun getLong(key: String, defaultValue: Long = 0L): Long {
       return securePreferences.getLong(key, defaultValue)
   }

   fun saveFloat(key: String, value: Float) {
       securePreferences.edit().putFloat(key, value).apply()
   }

   fun getFloat(key: String, defaultValue: Float = 0f): Float {
       return securePreferences.getFloat(key, defaultValue)
   }

   fun remove(key: String) {
       securePreferences.edit().remove(key).apply()
   }

   fun clear() {
       securePreferences.edit().clear().apply()
   }

   companion object {
       private const val SECURE_PREFS_FILENAME = "eazy_delivery_secure_prefs"

       // Keys for stored values
       const val KEY_AUTH_TOKEN = "auth_token"
       const val KEY_USER_ID = "user_id"
       const val KEY_USER_EMAIL = "user_email"
       const val KEY_USER_PHONE = "user_phone"
       const val KEY_USER_NAME = "user_name"
       const val KEY_SUBSCRIPTION_STATUS = "subscription_status"
       const val KEY_SUBSCRIPTION_EXPIRY = "subscription_expiry"
       const val KEY_TRIAL_END_DATE = "trial_end_date"

       // List of keys that need additional encryption
       private val SENSITIVE_KEYS = setOf(
           KEY_AUTH_TOKEN,
           KEY_USER_ID,
           KEY_USER_EMAIL,
           KEY_USER_PHONE
       )
   }
}


