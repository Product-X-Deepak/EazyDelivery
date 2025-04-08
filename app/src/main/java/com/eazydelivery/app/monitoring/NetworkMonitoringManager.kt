package com.eazydelivery.app.monitoring

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.SystemClock
import com.eazydelivery.app.analytics.AnalyticsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class for monitoring network performance and connectivity
 */
@Singleton
class NetworkMonitoringManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyticsManager: AnalyticsManager,
    private val performanceMonitoringManager: PerformanceMonitoringManager
) {
    // Network state
    private val _networkState = MutableStateFlow<NetworkState>(NetworkState.Unknown)
    val networkState: StateFlow<NetworkState> = _networkState
    
    // Network statistics
    private val requestCount = AtomicLong(0)
    private val successCount = AtomicLong(0)
    private val failureCount = AtomicLong(0)
    private val totalRequestTime = AtomicLong(0)
    private val totalRequestSize = AtomicLong(0)
    private val totalResponseSize = AtomicLong(0)
    
    // Request times by endpoint
    private val requestTimesByEndpoint = ConcurrentHashMap<String, MutableList<Long>>()
    
    // Connectivity manager
    private val connectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    
    // Network callback
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateNetworkState()
            Timber.d("Network available")
        }
        
        override fun onLost(network: Network) {
            updateNetworkState()
            Timber.d("Network lost")
        }
        
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            updateNetworkState()
            Timber.d("Network capabilities changed")
        }
    }
    
    /**
     * Initialize network monitoring
     */
    fun initialize() {
        try {
            // Register network callback
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            
            connectivityManager.registerNetworkCallback(request, networkCallback)
            
            // Update initial network state
            updateNetworkState()
            
            Timber.d("Network monitoring initialized")
        } catch (e: Exception) {
            Timber.e(e, "Error initializing network monitoring")
        }
    }
    
    /**
     * Update the network state
     */
    private fun updateNetworkState() {
        try {
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            
            val state = when {
                capabilities == null -> NetworkState.Unavailable
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkState.Wifi
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    when {
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) -> NetworkState.UnmeteredCellular
                        else -> NetworkState.Cellular
                    }
                }
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkState.Ethernet
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> NetworkState.Bluetooth
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkState.Vpn
                else -> NetworkState.Other
            }
            
            _networkState.value = state
            
            Timber.d("Network state updated: $state")
        } catch (e: Exception) {
            Timber.e(e, "Error updating network state")
        }
    }
    
    /**
     * Check if the network is available
     * 
     * @return true if the network is available, false otherwise
     */
    fun isNetworkAvailable(): Boolean {
        return _networkState.value != NetworkState.Unavailable && _networkState.value != NetworkState.Unknown
    }
    
    /**
     * Check if the network is unmetered (WiFi, Ethernet, etc.)
     * 
     * @return true if the network is unmetered, false otherwise
     */
    fun isNetworkUnmetered(): Boolean {
        return when (_networkState.value) {
            NetworkState.Wifi, NetworkState.Ethernet, NetworkState.UnmeteredCellular -> true
            else -> false
        }
    }
    
    /**
     * Get the current network type
     * 
     * @return The network type
     */
    fun getNetworkType(): String {
        return _networkState.value.name
    }
    
    /**
     * Create an OkHttp interceptor for monitoring network requests
     * 
     * @return The interceptor
     */
    fun createNetworkInterceptor(): Interceptor {
        return object : Interceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                val request = chain.request()
                val startTime = SystemClock.elapsedRealtime()
                
                // Increment request count
                requestCount.incrementAndGet()
                
                // Get request size
                val requestSize = request.body?.contentLength() ?: 0
                totalRequestSize.addAndGet(requestSize)
                
                // Get endpoint
                val endpoint = "${request.method} ${request.url.encodedPath}"
                
                try {
                    // Execute request
                    val response = chain.proceed(request)
                    
                    // Calculate request time
                    val requestTime = SystemClock.elapsedRealtime() - startTime
                    
                    // Update statistics
                    successCount.incrementAndGet()
                    totalRequestTime.addAndGet(requestTime)
                    
                    // Get response size
                    val responseSize = response.body?.contentLength() ?: 0
                    totalResponseSize.addAndGet(responseSize)
                    
                    // Record request time by endpoint
                    requestTimesByEndpoint.getOrPut(endpoint) { mutableListOf() }.add(requestTime)
                    
                    // Record custom metric
                    performanceMonitoringManager.recordCustomMetric("network_request_$endpoint", requestTime)
                    
                    // Log request
                    Timber.d("Network request: $endpoint, time: $requestTime ms, size: $requestSize bytes, response size: $responseSize bytes")
                    
                    return response
                } catch (e: Exception) {
                    // Increment failure count
                    failureCount.incrementAndGet()
                    
                    // Log error
                    Timber.e(e, "Network request failed: $endpoint")
                    
                    throw e
                }
            }
        }
    }
    
    /**
     * Get network statistics
     * 
     * @return The network statistics
     */
    fun getNetworkStatistics(): NetworkStatistics {
        val requestTimesList = requestTimesByEndpoint.values.flatten()
        
        val averageRequestTime = if (requestTimesList.isNotEmpty()) {
            requestTimesList.average().toLong()
        } else {
            0
        }
        
        return NetworkStatistics(
            requestCount = requestCount.get(),
            successCount = successCount.get(),
            failureCount = failureCount.get(),
            totalRequestTime = totalRequestTime.get(),
            averageRequestTime = averageRequestTime,
            totalRequestSize = totalRequestSize.get(),
            totalResponseSize = totalResponseSize.get(),
            networkType = _networkState.value.name
        )
    }
    
    /**
     * Get request times by endpoint
     * 
     * @return Map of endpoint to list of request times
     */
    fun getRequestTimesByEndpoint(): Map<String, List<Long>> {
        return requestTimesByEndpoint.toMap()
    }
    
    /**
     * Get average request time by endpoint
     * 
     * @return Map of endpoint to average request time
     */
    fun getAverageRequestTimeByEndpoint(): Map<String, Long> {
        return requestTimesByEndpoint.mapValues { (_, times) ->
            if (times.isNotEmpty()) {
                times.average().toLong()
            } else {
                0
            }
        }
    }
    
    /**
     * Reset network statistics
     */
    fun resetStatistics() {
        requestCount.set(0)
        successCount.set(0)
        failureCount.set(0)
        totalRequestTime.set(0)
        totalRequestSize.set(0)
        totalResponseSize.set(0)
        requestTimesByEndpoint.clear()
        
        Timber.d("Network statistics reset")
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            Timber.d("Network monitoring cleaned up")
        } catch (e: Exception) {
            Timber.e(e, "Error cleaning up network monitoring")
        }
    }
    
    /**
     * Network state enum
     */
    enum class NetworkState {
        Wifi,
        Cellular,
        UnmeteredCellular,
        Ethernet,
        Bluetooth,
        Vpn,
        Other,
        Unavailable,
        Unknown
    }
    
    /**
     * Data class for network statistics
     */
    data class NetworkStatistics(
        val requestCount: Long,
        val successCount: Long,
        val failureCount: Long,
        val totalRequestTime: Long,
        val averageRequestTime: Long,
        val totalRequestSize: Long,
        val totalResponseSize: Long,
        val networkType: String
    )
}
