package com.eazydelivery.app.ui.screens.home

import androidx.lifecycle.viewModelScope
import com.eazydelivery.app.domain.model.Platform
import com.eazydelivery.app.domain.repository.AnalyticsRepository
import com.eazydelivery.app.domain.repository.PlatformRepository
import com.eazydelivery.app.domain.repository.ServiceRepository
import com.eazydelivery.app.domain.repository.SubscriptionRepository
import com.eazydelivery.app.ui.base.BaseViewModel
import com.eazydelivery.app.util.ErrorHandler
import com.eazydelivery.app.util.SecureStorage
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val serviceRepository: ServiceRepository,
    private val platformRepository: PlatformRepository,
    private val analyticsRepository: AnalyticsRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val firebaseAnalytics: FirebaseAnalytics,
    private val secureStorage: SecureStorage,
    errorHandler: ErrorHandler
) : BaseViewModel(errorHandler) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Load all data in parallel for better performance
                val serviceJob = launch { loadServiceStatus() }
                val platformsJob = launch { loadPlatforms() }
                val statsJob = launch { loadTodayStats() }
                val subscriptionJob = launch { loadSubscriptionStatus() }
                val batteryJob = launch { loadBatteryOptimizationStatus() }

                // Wait for all jobs to complete
                serviceJob.join()
                platformsJob.join()
                statsJob.join()
                subscriptionJob.join()
                batteryJob.join()

                // Log screen view event
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
                    param(FirebaseAnalytics.Param.SCREEN_NAME, "Home Screen")
                    param(FirebaseAnalytics.Param.SCREEN_CLASS, "HomeScreen")
                }

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                errorHandler.handleException("HomeViewModel.loadInitialData", e)
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private suspend fun loadServiceStatus() {
        serviceRepository.isServiceActive().fold(
            onSuccess = { isActive ->
                _uiState.update { it.copy(isServiceActive = isActive) }
            },
            onFailure = { e ->
                errorHandler.handleException("HomeViewModel.loadServiceStatus", e)
                _uiState.update { it.copy(error = "Failed to load service status") }
            }
        )
    }

    private suspend fun loadPlatforms() {
        platformRepository.getAllPlatforms().fold(
            onSuccess = { platforms ->
                _uiState.update {
                    it.copy(
                        platforms = platforms.map { platform ->
                            PlatformUiState(
                                name = platform.name,
                                isEnabled = platform.isEnabled,
                                minAmount = platform.minAmount,
                                maxAmount = platform.maxAmount,
                                autoAccept = platform.autoAccept,
                                priority = platform.priority,
                                acceptMediumPriority = platform.acceptMediumPriority
                            )
                        }
                    )
                }
            },
            onFailure = { e ->
                errorHandler.handleException("HomeViewModel.loadPlatforms", e)
                _uiState.update { it.copy(error = "Failed to load platforms") }
            }
        )
    }

    private suspend fun loadTodayStats() {
        analyticsRepository.getTodayStats().fold(
            onSuccess = { stats ->
                _uiState.update {
                    it.copy(
                        todayStats = DailyStatsUiState(
                            totalOrders = stats.totalOrders,
                            totalEarnings = stats.totalEarnings
                        )
                    )
                }
            },
            onFailure = { e ->
                errorHandler.handleException("HomeViewModel.loadTodayStats", e)
                _uiState.update { it.copy(error = "Failed to load today's stats") }
            }
        )
    }

    private suspend fun loadSubscriptionStatus() {
        subscriptionRepository.getSubscriptionStatus().fold(
            onSuccess = { status ->
                _uiState.update {
                    it.copy(
                        isSubscribed = status.isSubscribed,
                        trialDaysLeft = status.trialDaysLeft
                    )
                }
            },
            onFailure = { e ->
                errorHandler.handleException("HomeViewModel.loadSubscriptionStatus", e)
                _uiState.update { it.copy(error = "Failed to load subscription status") }
            }
        )
    }

    /**
     * Load battery optimization status
     */
    private suspend fun loadBatteryOptimizationStatus() {
        serviceRepository.getBatteryOptimizationStatus().fold(
            onSuccess = { isIgnoringBatteryOptimizations ->
                _uiState.update {
                    it.copy(isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations)
                }
            },
            onFailure = { e ->
                errorHandler.handleException("HomeViewModel.loadBatteryOptimizationStatus", e)
                _uiState.update { it.copy(error = "Failed to check battery optimization status") }
            }
        )
    }

    fun toggleServiceActive() {
        launchSafe("HomeViewModel.toggleServiceActive") {
            _uiState.update { it.copy(isUpdating = true) }

            val newStatus = !uiState.value.isServiceActive
            serviceRepository.setServiceActive(newStatus).fold(
                onSuccess = {
                    _uiState.update { it.copy(isServiceActive = newStatus, isUpdating = false) }

                    // Log event
                    firebaseAnalytics.logEvent("service_toggled") {
                        param("status", if (newStatus) "active" else "inactive")
                    }

                    Timber.d("Service toggled to ${if (newStatus) "active" else "inactive"}")
                },
                onFailure = { e ->
                    errorHandler.handleException("HomeViewModel.toggleServiceActive", e)
                    _uiState.update { it.copy(isUpdating = false, error = "Failed to toggle service") }
                }
            )
        }
    }

    fun togglePlatform(platformName: String) {
        launchSafe("HomeViewModel.togglePlatform") {
            _uiState.update { it.copy(isUpdating = true) }

            val currentPlatform = uiState.value.platforms.find { it.name == platformName }
                ?: return@launchSafe

            val newStatus = !currentPlatform.isEnabled

            platformRepository.togglePlatformStatus(platformName, newStatus).fold(
                onSuccess = {
                    val updatedPlatforms = uiState.value.platforms.map { platform ->
                        if (platform.name == platformName) {
                            platform.copy(isEnabled = newStatus)
                        } else {
                            platform
                        }
                    }

                    _uiState.update { it.copy(platforms = updatedPlatforms, isUpdating = false) }

                    // Log event
                    firebaseAnalytics.logEvent("platform_toggled") {
                        param("platform", platformName)
                        param("status", if (newStatus) "enabled" else "disabled")
                    }

                    Timber.d("Platform $platformName toggled to ${if (newStatus) "enabled" else "disabled"}")
                },
                onFailure = { e ->
                    errorHandler.handleException("HomeViewModel.togglePlatform", e)
                    _uiState.update { it.copy(isUpdating = false, error = "Failed to toggle platform") }
                }
            )
        }
    }

    fun updateMinAmount(platformName: String, amount: Int) {
        launchSafe("HomeViewModel.updateMinAmount") {
            _uiState.update { it.copy(isUpdating = true) }

            platformRepository.updateMinAmount(platformName, amount).fold(
                onSuccess = {
                    val updatedPlatforms = uiState.value.platforms.map { platform ->
                        if (platform.name == platformName) {
                            platform.copy(minAmount = amount)
                        } else {
                            platform
                        }
                    }

                    _uiState.update { it.copy(platforms = updatedPlatforms, isUpdating = false) }

                    // Log event
                    firebaseAnalytics.logEvent("min_amount_updated") {
                        param("platform", platformName)
                        param("amount", amount.toDouble())
                    }

                    Timber.d("Platform $platformName min amount updated to $amount")
                },
                onFailure = { e ->
                    errorHandler.handleException("HomeViewModel.updateMinAmount", e)
                    _uiState.update { it.copy(isUpdating = false, error = "Failed to update minimum amount") }
                }
            )
        }
    }

    fun refreshData() {
        loadInitialData()
    }

    suspend fun getLastOrderDetails(): String? {
        return secureStorage.getString("last_order_details")
    }

    /**
     * Dismiss the battery optimization banner
     * This doesn't change the actual battery optimization setting, just hides the banner
     */
    fun dismissBatteryOptimizationBanner() {
        viewModelScope.launch {
            try {
                // Save the user's preference to not show the banner again
                secureStorage.saveBoolean("battery_optimization_banner_dismissed", true)

                // Update UI state
                _uiState.update { it.copy(isIgnoringBatteryOptimizations = true) }
            } catch (e: Exception) {
                errorHandler.handleException("HomeViewModel.dismissBatteryOptimizationBanner", e)
            }
        }
    }
}

data class HomeUiState(
    val isServiceActive: Boolean = false,
    val platforms: List<PlatformUiState> = emptyList(),
    val todayStats: DailyStatsUiState = DailyStatsUiState(),
    val isSubscribed: Boolean = false,
    val trialDaysLeft: Int = 0,
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val error: String? = null,
    val isIgnoringBatteryOptimizations: Boolean = false
)

data class PlatformUiState(
    val name: String,
    val isEnabled: Boolean,
    val minAmount: Int,
    val maxAmount: Int = 500,
    val autoAccept: Boolean = true,
    val priority: Int = 0,
    val acceptMediumPriority: Boolean = false
)

data class DailyStatsUiState(
    val totalOrders: Int = 0,
    val totalEarnings: Double = 0.0
)
