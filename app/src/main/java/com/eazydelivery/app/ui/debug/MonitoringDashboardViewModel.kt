package com.eazydelivery.app.ui.debug

import androidx.lifecycle.viewModelScope
import com.eazydelivery.app.monitoring.NetworkMonitoringManager
import com.eazydelivery.app.monitoring.PerformanceMonitoringManager
import com.eazydelivery.app.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the monitoring dashboard
 */
@HiltViewModel
class MonitoringDashboardViewModel @Inject constructor(
    private val performanceMonitoringManager: PerformanceMonitoringManager,
    private val networkMonitoringManager: NetworkMonitoringManager
) : BaseViewModel() {
    
    // Performance metrics
    private val _performanceMetrics = MutableStateFlow<Map<String, PerformanceMonitoringManager.MetricStatistics>>(emptyMap())
    val performanceMetrics: StateFlow<Map<String, PerformanceMonitoringManager.MetricStatistics>> = _performanceMetrics
    
    // Network statistics
    private val _networkStatistics = MutableStateFlow<NetworkMonitoringManager.NetworkStatistics?>(null)
    val networkStatistics: StateFlow<NetworkMonitoringManager.NetworkStatistics?> = _networkStatistics
    
    // Network request times by endpoint
    private val _requestTimesByEndpoint = MutableStateFlow<Map<String, List<Long>>>(emptyMap())
    val requestTimesByEndpoint: StateFlow<Map<String, List<Long>>> = _requestTimesByEndpoint
    
    /**
     * Refresh all data
     */
    fun refreshData() {
        viewModelScope.launch {
            try {
                setLoading(true)
                
                // Get performance metrics
                val metrics = performanceMonitoringManager.getAllMetricStatistics()
                _performanceMetrics.value = metrics
                
                // Get network statistics
                val networkStats = networkMonitoringManager.getNetworkStatistics()
                _networkStatistics.value = networkStats
                
                // Get request times by endpoint
                val requestTimes = networkMonitoringManager.getRequestTimesByEndpoint()
                _requestTimesByEndpoint.value = requestTimes
                
                Timber.d("Monitoring data refreshed")
            } catch (e: Exception) {
                errorHandler.handleException("MonitoringDashboardViewModel.refreshData", e)
            } finally {
                setLoading(false)
            }
        }
    }
    
    /**
     * Reset all statistics
     */
    fun resetStatistics() {
        viewModelScope.launch {
            try {
                setLoading(true)
                
                // Reset performance metrics
                performanceMonitoringManager.clearCustomMetrics()
                
                // Reset network statistics
                networkMonitoringManager.resetStatistics()
                
                // Refresh data
                refreshData()
                
                Timber.d("Statistics reset")
            } catch (e: Exception) {
                errorHandler.handleException("MonitoringDashboardViewModel.resetStatistics", e)
            } finally {
                setLoading(false)
            }
        }
    }
}
