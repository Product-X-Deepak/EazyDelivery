package com.eazydelivery.app.ui.screens.analytics

import androidx.lifecycle.viewModelScope
import com.eazydelivery.app.domain.model.Order
import com.eazydelivery.app.domain.model.PlatformStat
import com.eazydelivery.app.domain.repository.AnalyticsRepository
import com.eazydelivery.app.ui.base.BaseViewModel
import com.eazydelivery.app.util.ErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    errorHandler: ErrorHandler
) : BaseViewModel(errorHandler) {
    
    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()
    
    init {
        loadTodayStats()
        loadRecentOrders()
    }
    
    fun loadTodayStats() {
        _uiState.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            try {
                val todayStats = analyticsRepository.getTodayStats().getOrThrow()
                val platformStats = todayStats.platformBreakdown.values.toList()
                
                _uiState.update {
                    it.copy(
                        totalEarnings = todayStats.totalEarnings,
                        totalOrders = todayStats.totalOrders,
                        platformStats = platformStats,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading today's stats")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    fun loadWeekStats() {
        loadStatsForDateRange(Calendar.DAY_OF_MONTH, -7, "week")
    }
    
    fun loadMonthStats() {
        loadStatsForDateRange(Calendar.MONTH, -1, "month")
    }
    
    private fun loadStatsForDateRange(calendarField: Int, amount: Int, periodName: String) {
        _uiState.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            try {
                val calendar = Calendar.getInstance()
                val endDate = calendar.time
                
                calendar.add(calendarField, amount)
                val startDate = calendar.time
                
                val earnings = analyticsRepository.getEarningsForDateRange(startDate, endDate).getOrDefault(0.0)
                val orderCount = analyticsRepository.getOrderCountForDateRange(startDate, endDate).getOrDefault(0)
                val platformStats = analyticsRepository.getPlatformStats().getOrDefault(emptyList())
                
                _uiState.update {
                    it.copy(
                        totalEarnings = earnings,
                        totalOrders = orderCount,
                        platformStats = platformStats,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading $periodName stats")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    private fun loadRecentOrders() {
        viewModelScope.launch {
            try {
                val recentOrders = analyticsRepository.getRecentOrders(10).getOrDefault(emptyList())
                _uiState.update { it.copy(recentOrders = recentOrders) }
            } catch (e: Exception) {
                Timber.e(e, "Error loading recent orders")
            }
        }
    }
}

data class AnalyticsUiState(
    val totalEarnings: Double = 0.0,
    val totalOrders: Int = 0,
    val platformStats: List<PlatformStat> = emptyList(),
    val recentOrders: List<Order> = emptyList(),
    val isLoading: Boolean = false
)
