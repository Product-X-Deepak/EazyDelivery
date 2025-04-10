package com.eazydelivery.app.util

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import com.eazydelivery.app.domain.repository.ServiceRepository
import com.eazydelivery.app.security.SecurePreferencesManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages app state and provides mechanisms to save and restore state
 */
@Singleton
class AppStateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securePreferencesManager: SecurePreferencesManager,
    private val serviceRepository: ServiceRepository,
    private val errorHandler: ErrorHandler
) {
    private val stateScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()
    
    // App state
    private val _appState = MutableStateFlow(AppState())
    val appState: StateFlow<AppState> = _appState.asStateFlow()
    
    // Preferences for app state
    private val statePreferences: SharedPreferences by lazy {
        context.getSharedPreferences("app_state_prefs", Context.MODE_PRIVATE)
    }
    
    // Flag to track if we're initialized
    private val isInitialized = AtomicBoolean(false)
    
    companion object {
        private const val KEY_APP_STATE = "app_state"
        private const val KEY_LAST_SAVE_TIME = "last_save_time"
        private const val KEY_APP_START_COUNT = "app_start_count"
        private const val KEY_APP_CRASH_COUNT = "app_crash_count"
        private const val KEY_LAST_KNOWN_GOOD_STATE = "last_known_good_state"
        
        // State save interval
        private const val STATE_SAVE_INTERVAL_MS = 60000L // 1 minute
    }
    
    /**
     * Initialize the app state manager
     */
    fun initialize() {
        if (isInitialized.getAndSet(true)) {
            return
        }
        
        try {
            Timber.d("Initializing app state manager")
            
            // Load initial state
            loadState()
            
            // Increment app start count
            incrementAppStartCount()
            
            // Start periodic state saving
            startPeriodicStateSaving()
            
            Timber.d("App state manager initialized")
        } catch (e: Exception) {
            errorHandler.handleException("AppStateManager.initialize", e)
        }
    }
    
    /**
     * Load app state
     */
    private fun loadState() {
        stateScope.launch {
            try {
                // Check if we're recovering from a crash
                val isCrashRecovery = statePreferences.getBoolean("is_crash_recovery", false)
                
                // Get the state JSON
                val stateJson = if (isCrashRecovery) {
                    // If recovering from crash, use last known good state
                    statePreferences.getString(KEY_LAST_KNOWN_GOOD_STATE, null)
                } else {
                    // Otherwise use the regular state
                    statePreferences.getString(KEY_APP_STATE, null)
                }
                
                if (stateJson != null) {
                    // Parse the state
                    val type = object : TypeToken<AppState>() {}.type
                    val loadedState = gson.fromJson<AppState>(stateJson, type)
                    
                    // Update the state
                    _appState.value = loadedState
                    
                    Timber.d("Loaded app state: $loadedState")
                    
                    // If we recovered from a crash, clear the flag
                    if (isCrashRecovery) {
                        statePreferences.edit().putBoolean("is_crash_recovery", false).apply()
                        Timber.d("Cleared crash recovery flag")
                    }
                } else {
                    // If no state found, initialize with default values
                    initializeDefaultState()
                }
            } catch (e: Exception) {
                errorHandler.handleException("AppStateManager.loadState", e)
                
                // If loading fails, initialize with default values
                initializeDefaultState()
            }
        }
    }
    
    /**
     * Initialize default state
     */
    private suspend fun initializeDefaultState() {
        try {
            Timber.d("Initializing default app state")
            
            // Get service active state
            val isServiceActive = serviceRepository.isServiceActive().getOrDefault(false)
            
            // Create default state
            val defaultState = AppState(
                isServiceActive = isServiceActive,
                lastActiveTimestamp = System.currentTimeMillis(),
                appStartCount = getAppStartCount(),
                appCrashCount = getAppCrashCount()
            )
            
            // Update the state
            _appState.value = defaultState
            
            // Save the default state
            saveState(defaultState)
            
            Timber.d("Initialized default app state: $defaultState")
        } catch (e: Exception) {
            errorHandler.handleException("AppStateManager.initializeDefaultState", e)
        }
    }
    
    /**
     * Save app state
     */
    private fun saveState(state: AppState = appState.value) {
        try {
            // Convert state to JSON
            val stateJson = gson.toJson(state)
            
            // Save to preferences
            statePreferences.edit()
                .putString(KEY_APP_STATE, stateJson)
                .putLong(KEY_LAST_SAVE_TIME, System.currentTimeMillis())
                .apply()
            
            // Every 5 saves, also save as last known good state
            val saveCount = statePreferences.getInt("state_save_count", 0) + 1
            if (saveCount % 5 == 0) {
                statePreferences.edit()
                    .putString(KEY_LAST_KNOWN_GOOD_STATE, stateJson)
                    .putInt("state_save_count", saveCount)
                    .apply()
                
                Timber.d("Saved last known good state (save count: $saveCount)")
            } else {
                statePreferences.edit().putInt("state_save_count", saveCount).apply()
            }
            
            Timber.d("Saved app state: $state")
        } catch (e: Exception) {
            errorHandler.handleException("AppStateManager.saveState", e)
        }
    }
    
    /**
     * Start periodic state saving
     */
    private fun startPeriodicStateSaving() {
        stateScope.launch {
            try {
                while (true) {
                    // Wait for the save interval
                    kotlinx.coroutines.delay(STATE_SAVE_INTERVAL_MS)
                    
                    // Update last active timestamp
                    val currentState = appState.value
                    val updatedState = currentState.copy(lastActiveTimestamp = System.currentTimeMillis())
                    _appState.value = updatedState
                    
                    // Save the state
                    saveState(updatedState)
                }
            } catch (e: Exception) {
                errorHandler.handleException("AppStateManager.startPeriodicStateSaving", e)
            }
        }
    }
    
    /**
     * Update service active state
     */
    fun updateServiceActiveState(isActive: Boolean) {
        stateScope.launch {
            try {
                // Update the state
                val currentState = appState.value
                val updatedState = currentState.copy(
                    isServiceActive = isActive,
                    lastServiceStateChangeTimestamp = System.currentTimeMillis()
                )
                _appState.value = updatedState
                
                // Save the state
                saveState(updatedState)
                
                Timber.d("Updated service active state: $isActive")
            } catch (e: Exception) {
                errorHandler.handleException("AppStateManager.updateServiceActiveState", e)
            }
        }
    }
    
    /**
     * Mark crash recovery
     */
    fun markCrashRecovery() {
        stateScope.launch {
            try {
                // Set crash recovery flag
                statePreferences.edit().putBoolean("is_crash_recovery", true).apply()
                
                // Increment crash count
                incrementAppCrashCount()
                
                Timber.d("Marked crash recovery")
            } catch (e: Exception) {
                errorHandler.handleException("AppStateManager.markCrashRecovery", e)
            }
        }
    }
    
    /**
     * Get app start count
     */
    private fun getAppStartCount(): Int {
        return statePreferences.getInt(KEY_APP_START_COUNT, 0)
    }
    
    /**
     * Increment app start count
     */
    private fun incrementAppStartCount() {
        try {
            val startCount = getAppStartCount() + 1
            statePreferences.edit().putInt(KEY_APP_START_COUNT, startCount).apply()
            
            // Update Crashlytics
            FirebaseCrashlytics.getInstance().setCustomKey("app_start_count", startCount)
            
            Timber.d("Incremented app start count to $startCount")
        } catch (e: Exception) {
            errorHandler.handleException("AppStateManager.incrementAppStartCount", e)
        }
    }
    
    /**
     * Get app crash count
     */
    private fun getAppCrashCount(): Int {
        return statePreferences.getInt(KEY_APP_CRASH_COUNT, 0)
    }
    
    /**
     * Increment app crash count
     */
    private fun incrementAppCrashCount() {
        try {
            val crashCount = getAppCrashCount() + 1
            statePreferences.edit().putInt(KEY_APP_CRASH_COUNT, crashCount).apply()
            
            // Update state
            val currentState = appState.value
            val updatedState = currentState.copy(appCrashCount = crashCount)
            _appState.value = updatedState
            
            // Update Crashlytics
            FirebaseCrashlytics.getInstance().setCustomKey("app_crash_count", crashCount)
            
            Timber.d("Incremented app crash count to $crashCount")
        } catch (e: Exception) {
            errorHandler.handleException("AppStateManager.incrementAppCrashCount", e)
        }
    }
    
    /**
     * Reset app state
     */
    fun resetAppState() {
        stateScope.launch {
            try {
                Timber.w("Resetting app state")
                
                // Clear preferences
                statePreferences.edit().clear().apply()
                
                // Initialize default state
                initializeDefaultState()
                
                Timber.d("App state reset completed")
            } catch (e: Exception) {
                errorHandler.handleException("AppStateManager.resetAppState", e)
            }
        }
    }
    
    /**
     * Export app state to file
     */
    fun exportAppState(): File? {
        try {
            // Create state export directory
            val exportDir = File(context.filesDir, "state_exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            
            // Create export file
            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
                .format(java.util.Date())
            val exportFile = File(exportDir, "app_state_$timestamp.json")
            
            // Write state to file
            val stateJson = gson.toJson(appState.value)
            exportFile.writeText(stateJson)
            
            Timber.d("Exported app state to ${exportFile.absolutePath}")
            
            return exportFile
        } catch (e: Exception) {
            errorHandler.handleException("AppStateManager.exportAppState", e)
            return null
        }
    }
    
    /**
     * Import app state from file
     */
    fun importAppState(file: File): Boolean {
        try {
            // Read state from file
            val stateJson = file.readText()
            
            // Parse the state
            val type = object : TypeToken<AppState>() {}.type
            val importedState = gson.fromJson<AppState>(stateJson, type)
            
            // Update the state
            _appState.value = importedState
            
            // Save the state
            saveState(importedState)
            
            Timber.d("Imported app state from ${file.absolutePath}")
            
            return true
        } catch (e: Exception) {
            errorHandler.handleException("AppStateManager.importAppState", e)
            return false
        }
    }
}

/**
 * App state data class
 */
data class AppState(
    val isServiceActive: Boolean = false,
    val lastActiveTimestamp: Long = 0,
    val lastServiceStateChangeTimestamp: Long = 0,
    val appStartCount: Int = 0,
    val appCrashCount: Int = 0,
    val appVersion: String = "1.0.0",
    val deviceId: String = ""
)
