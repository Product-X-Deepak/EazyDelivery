package com.eazydelivery.app.feedback

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.eazydelivery.app.BuildConfig
import com.eazydelivery.app.analytics.AnalyticsManager
import com.eazydelivery.app.security.SecurePreferencesManager
import com.eazydelivery.app.util.ErrorHandler
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages user feedback collection and processing
 */
@Singleton
class UserFeedbackManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securePreferencesManager: SecurePreferencesManager,
    private val analyticsManager: AnalyticsManager,
    private val errorHandler: ErrorHandler
) {
    private val feedbackScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()
    
    // Feedback state
    private val _feedbackState = MutableStateFlow(FeedbackState())
    val feedbackState: StateFlow<FeedbackState> = _feedbackState.asStateFlow()
    
    // Flag to track if we're initialized
    private val isInitialized = AtomicBoolean(false)
    
    companion object {
        private const val KEY_FEEDBACK_ENABLED = "feedback_enabled"
        private const val KEY_LAST_FEEDBACK_PROMPT = "last_feedback_prompt"
        private const val KEY_FEEDBACK_PROMPT_COUNT = "feedback_prompt_count"
        private const val KEY_APP_RATING = "app_rating"
        private const val KEY_HAS_RATED_APP = "has_rated_app"
        
        // Feedback prompt thresholds
        private const val MIN_DAYS_BETWEEN_PROMPTS = 7
        private const val MIN_SESSIONS_BEFORE_PROMPT = 5
        private const val MAX_PROMPTS = 3
    }
    
    /**
     * Initialize the user feedback manager
     */
    fun initialize() {
        if (isInitialized.getAndSet(true)) {
            return
        }
        
        try {
            Timber.d("Initializing user feedback manager")
            
            // Load initial state
            loadState()
            
            // Check if we should show feedback prompt
            checkFeedbackPrompt()
            
            Timber.d("User feedback manager initialized")
        } catch (e: Exception) {
            errorHandler.handleException("UserFeedbackManager.initialize", e)
        }
    }
    
    /**
     * Load feedback state
     */
    private fun loadState() {
        try {
            // Get feedback enabled state
            val isFeedbackEnabled = securePreferencesManager.getBoolean(KEY_FEEDBACK_ENABLED, true)
            
            // Get last feedback prompt timestamp
            val lastFeedbackPrompt = securePreferencesManager.getLong(KEY_LAST_FEEDBACK_PROMPT, 0)
            
            // Get feedback prompt count
            val feedbackPromptCount = securePreferencesManager.getInt(KEY_FEEDBACK_PROMPT_COUNT, 0)
            
            // Get app rating
            val appRating = securePreferencesManager.getInt(KEY_APP_RATING, 0)
            
            // Get has rated app
            val hasRatedApp = securePreferencesManager.getBoolean(KEY_HAS_RATED_APP, false)
            
            // Update state
            _feedbackState.value = FeedbackState(
                isFeedbackEnabled = isFeedbackEnabled,
                lastFeedbackPrompt = lastFeedbackPrompt,
                feedbackPromptCount = feedbackPromptCount,
                appRating = appRating,
                hasRatedApp = hasRatedApp
            )
            
            Timber.d("Loaded feedback state: ${_feedbackState.value}")
        } catch (e: Exception) {
            errorHandler.handleException("UserFeedbackManager.loadState", e)
        }
    }
    
    /**
     * Save feedback state
     */
    private fun saveState(state: FeedbackState = feedbackState.value) {
        try {
            // Save to preferences
            securePreferencesManager.putBoolean(KEY_FEEDBACK_ENABLED, state.isFeedbackEnabled)
            securePreferencesManager.putLong(KEY_LAST_FEEDBACK_PROMPT, state.lastFeedbackPrompt)
            securePreferencesManager.putInt(KEY_FEEDBACK_PROMPT_COUNT, state.feedbackPromptCount)
            securePreferencesManager.putInt(KEY_APP_RATING, state.appRating)
            securePreferencesManager.putBoolean(KEY_HAS_RATED_APP, state.hasRatedApp)
            
            Timber.d("Saved feedback state: $state")
        } catch (e: Exception) {
            errorHandler.handleException("UserFeedbackManager.saveState", e)
        }
    }
    
    /**
     * Check if we should show feedback prompt
     */
    private fun checkFeedbackPrompt() {
        feedbackScope.launch {
            try {
                val currentState = feedbackState.value
                
                // If feedback is disabled or user has already rated the app, don't show prompt
                if (!currentState.isFeedbackEnabled || currentState.hasRatedApp) {
                    return@launch
                }
                
                // If we've already shown the maximum number of prompts, don't show again
                if (currentState.feedbackPromptCount >= MAX_PROMPTS) {
                    return@launch
                }
                
                // Get app start count
                val appStartCount = securePreferencesManager.getInt("app_start_count", 0)
                
                // If app hasn't been used enough, don't show prompt
                if (appStartCount < MIN_SESSIONS_BEFORE_PROMPT) {
                    return@launch
                }
                
                // Check if enough time has passed since last prompt
                val currentTime = System.currentTimeMillis()
                val daysSinceLastPrompt = (currentTime - currentState.lastFeedbackPrompt) / (24 * 60 * 60 * 1000)
                
                if (daysSinceLastPrompt >= MIN_DAYS_BETWEEN_PROMPTS || currentState.lastFeedbackPrompt == 0L) {
                    // Update state to show prompt
                    _feedbackState.value = currentState.copy(
                        shouldShowFeedbackPrompt = true
                    )
                    
                    Timber.d("Should show feedback prompt")
                }
            } catch (e: Exception) {
                errorHandler.handleException("UserFeedbackManager.checkFeedbackPrompt", e)
            }
        }
    }
    
    /**
     * Record feedback prompt shown
     */
    fun recordFeedbackPromptShown() {
        feedbackScope.launch {
            try {
                val currentState = feedbackState.value
                
                // Update state
                val updatedState = currentState.copy(
                    lastFeedbackPrompt = System.currentTimeMillis(),
                    feedbackPromptCount = currentState.feedbackPromptCount + 1,
                    shouldShowFeedbackPrompt = false
                )
                _feedbackState.value = updatedState
                
                // Save state
                saveState(updatedState)
                
                // Log analytics event
                analyticsManager.logEvent(
                    "feedback_prompt_shown",
                    mapOf(
                        "prompt_count" to updatedState.feedbackPromptCount,
                        "days_since_last_prompt" to ((updatedState.lastFeedbackPrompt - currentState.lastFeedbackPrompt) / (24 * 60 * 60 * 1000))
                    )
                )
                
                Timber.d("Recorded feedback prompt shown")
            } catch (e: Exception) {
                errorHandler.handleException("UserFeedbackManager.recordFeedbackPromptShown", e)
            }
        }
    }
    
    /**
     * Submit app rating
     */
    fun submitAppRating(rating: Int) {
        feedbackScope.launch {
            try {
                val currentState = feedbackState.value
                
                // Update state
                val updatedState = currentState.copy(
                    appRating = rating,
                    hasRatedApp = true,
                    shouldShowFeedbackPrompt = false
                )
                _feedbackState.value = updatedState
                
                // Save state
                saveState(updatedState)
                
                // Log analytics event
                analyticsManager.logEvent(
                    "app_rating_submitted",
                    mapOf("rating" to rating)
                )
                
                // If rating is high, prompt to rate on Play Store
                if (rating >= 4) {
                    _feedbackState.value = updatedState.copy(
                        shouldPromptPlayStoreRating = true
                    )
                } else {
                    // If rating is low, prompt for feedback
                    _feedbackState.value = updatedState.copy(
                        shouldPromptFeedbackForm = true
                    )
                }
                
                Timber.d("Submitted app rating: $rating")
            } catch (e: Exception) {
                errorHandler.handleException("UserFeedbackManager.submitAppRating", e)
            }
        }
    }
    
    /**
     * Submit detailed feedback
     */
    fun submitDetailedFeedback(feedback: UserFeedback) {
        feedbackScope.launch {
            try {
                // Save feedback to file
                saveFeedbackToFile(feedback)
                
                // Log analytics event
                analyticsManager.logEvent(
                    "detailed_feedback_submitted",
                    mapOf(
                        "category" to feedback.category,
                        "rating" to feedback.rating,
                        "has_comments" to (feedback.comments.isNotBlank())
                    )
                )
                
                // Update Crashlytics
                FirebaseCrashlytics.getInstance().setCustomKey("last_feedback_rating", feedback.rating)
                FirebaseCrashlytics.getInstance().setCustomKey("last_feedback_category", feedback.category)
                
                // Update state
                val currentState = feedbackState.value
                val updatedState = currentState.copy(
                    shouldPromptFeedbackForm = false,
                    lastFeedbackCategory = feedback.category
                )
                _feedbackState.value = updatedState
                
                // Save state
                saveState(updatedState)
                
                Timber.d("Submitted detailed feedback: $feedback")
            } catch (e: Exception) {
                errorHandler.handleException("UserFeedbackManager.submitDetailedFeedback", e)
            }
        }
    }
    
    /**
     * Save feedback to file
     */
    private fun saveFeedbackToFile(feedback: UserFeedback) {
        try {
            // Create feedback directory
            val feedbackDir = File(context.filesDir, "feedback")
            if (!feedbackDir.exists()) {
                feedbackDir.mkdirs()
            }
            
            // Create timestamp for filename
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val feedbackFile = File(feedbackDir, "feedback_${timestamp}.json")
            
            // Add device info to feedback
            val enhancedFeedback = feedback.copy(
                deviceInfo = DeviceInfo(
                    manufacturer = Build.MANUFACTURER,
                    model = Build.MODEL,
                    osVersion = Build.VERSION.RELEASE,
                    appVersion = BuildConfig.VERSION_NAME,
                    timestamp = System.currentTimeMillis()
                )
            )
            
            // Write feedback to file
            val feedbackJson = gson.toJson(enhancedFeedback)
            feedbackFile.writeText(feedbackJson)
            
            Timber.d("Saved feedback to ${feedbackFile.absolutePath}")
        } catch (e: Exception) {
            errorHandler.handleException("UserFeedbackManager.saveFeedbackToFile", e)
        }
    }
    
    /**
     * Open Play Store for rating
     */
    fun openPlayStoreForRating() {
        try {
            val packageName = context.packageName
            val uri = Uri.parse("market://details?id=$packageName")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            try {
                context.startActivity(intent)
                
                // Log analytics event
                analyticsManager.logEvent("play_store_rating_opened", null)
                
                // Update state
                val currentState = feedbackState.value
                val updatedState = currentState.copy(
                    shouldPromptPlayStoreRating = false
                )
                _feedbackState.value = updatedState
                
                Timber.d("Opened Play Store for rating")
            } catch (e: Exception) {
                // If Play Store app is not available, open in browser
                val webUri = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                
                Timber.d("Opened Play Store in browser for rating")
            }
        } catch (e: Exception) {
            errorHandler.handleException("UserFeedbackManager.openPlayStoreForRating", e)
        }
    }
    
    /**
     * Dismiss feedback prompt
     */
    fun dismissFeedbackPrompt() {
        feedbackScope.launch {
            try {
                val currentState = feedbackState.value
                
                // Update state
                val updatedState = currentState.copy(
                    shouldShowFeedbackPrompt = false
                )
                _feedbackState.value = updatedState
                
                // Log analytics event
                analyticsManager.logEvent("feedback_prompt_dismissed", null)
                
                Timber.d("Dismissed feedback prompt")
            } catch (e: Exception) {
                errorHandler.handleException("UserFeedbackManager.dismissFeedbackPrompt", e)
            }
        }
    }
    
    /**
     * Dismiss Play Store rating prompt
     */
    fun dismissPlayStoreRatingPrompt() {
        feedbackScope.launch {
            try {
                val currentState = feedbackState.value
                
                // Update state
                val updatedState = currentState.copy(
                    shouldPromptPlayStoreRating = false
                )
                _feedbackState.value = updatedState
                
                // Log analytics event
                analyticsManager.logEvent("play_store_rating_prompt_dismissed", null)
                
                Timber.d("Dismissed Play Store rating prompt")
            } catch (e: Exception) {
                errorHandler.handleException("UserFeedbackManager.dismissPlayStoreRatingPrompt", e)
            }
        }
    }
    
    /**
     * Dismiss feedback form prompt
     */
    fun dismissFeedbackFormPrompt() {
        feedbackScope.launch {
            try {
                val currentState = feedbackState.value
                
                // Update state
                val updatedState = currentState.copy(
                    shouldPromptFeedbackForm = false
                )
                _feedbackState.value = updatedState
                
                // Log analytics event
                analyticsManager.logEvent("feedback_form_prompt_dismissed", null)
                
                Timber.d("Dismissed feedback form prompt")
            } catch (e: Exception) {
                errorHandler.handleException("UserFeedbackManager.dismissFeedbackFormPrompt", e)
            }
        }
    }
    
    /**
     * Enable or disable feedback
     */
    fun setFeedbackEnabled(enabled: Boolean) {
        feedbackScope.launch {
            try {
                val currentState = feedbackState.value
                
                // Update state
                val updatedState = currentState.copy(
                    isFeedbackEnabled = enabled
                )
                _feedbackState.value = updatedState
                
                // Save state
                saveState(updatedState)
                
                // Log analytics event
                analyticsManager.logEvent(
                    "feedback_enabled_changed",
                    mapOf("enabled" to enabled)
                )
                
                Timber.d("Set feedback enabled: $enabled")
            } catch (e: Exception) {
                errorHandler.handleException("UserFeedbackManager.setFeedbackEnabled", e)
            }
        }
    }
    
    /**
     * Get all feedback files
     */
    fun getFeedbackFiles(): List<File> {
        try {
            val feedbackDir = File(context.filesDir, "feedback")
            if (!feedbackDir.exists()) {
                return emptyList()
            }
            
            return feedbackDir.listFiles { file ->
                file.isFile && file.name.startsWith("feedback_")
            }?.sortedByDescending { it.lastModified() } ?: emptyList()
        } catch (e: Exception) {
            errorHandler.handleException("UserFeedbackManager.getFeedbackFiles", e)
            return emptyList()
        }
    }
}

/**
 * Feedback state data class
 */
data class FeedbackState(
    val isFeedbackEnabled: Boolean = true,
    val lastFeedbackPrompt: Long = 0,
    val feedbackPromptCount: Int = 0,
    val appRating: Int = 0,
    val hasRatedApp: Boolean = false,
    val shouldShowFeedbackPrompt: Boolean = false,
    val shouldPromptPlayStoreRating: Boolean = false,
    val shouldPromptFeedbackForm: Boolean = false,
    val lastFeedbackCategory: String = ""
)

/**
 * User feedback data class
 */
data class UserFeedback(
    val rating: Int,
    val category: String,
    val comments: String,
    val contactEmail: String = "",
    val includeDeviceInfo: Boolean = true,
    val includeAppLogs: Boolean = false,
    val deviceInfo: DeviceInfo? = null
)

/**
 * Device info data class
 */
data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val osVersion: String,
    val appVersion: String,
    val timestamp: Long
)
