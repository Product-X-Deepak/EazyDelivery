package com.eazydelivery.app.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import com.eazydelivery.app.util.ErrorHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optimizes network usage based on network conditions
 */
@Singleton
class NetworkOptimizer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val errorHandler: ErrorHandler
) {
    private val optimizerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Network state
    private val _networkState = MutableStateFlow(NetworkState())
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()
    
    // Network callback
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    
    // Pending requests that are waiting for network
    private val pendingRequests = ConcurrentHashMap<String, PendingRequest>()
    
    // Flag to track if we're initialized
    private val isInitialized = AtomicBoolean(false)
    
    companion object {
        // Network quality thresholds
        private const val POOR_BANDWIDTH_THRESHOLD_KBPS = 150
        private const val MEDIUM_BANDWIDTH_THRESHOLD_KBPS = 1000
        private const val GOOD_BANDWIDTH_THRESHOLD_KBPS = 5000
        
        // Retry intervals
        private const val RETRY_INTERVAL_POOR_MS = 10000L // 10 seconds
        private const val RETRY_INTERVAL_MEDIUM_MS = 5000L // 5 seconds
        private const val RETRY_INTERVAL_GOOD_MS = 2000L // 2 seconds
        
        // Request priorities
        const val PRIORITY_LOW = 0
        const val PRIORITY_MEDIUM = 1
        const val PRIORITY_HIGH = 2
        const val PRIORITY_CRITICAL = 3
    }
    
    /**
     * Initialize the network optimizer
     */
    fun initialize() {
        if (isInitialized.getAndSet(true)) {
            return
        }
        
        try {
            Timber.d("Initializing network optimizer")
            
            // Register network callback
            registerNetworkCallback()
            
            // Update initial network state
            updateNetworkState()
            
            Timber.d("Network optimizer initialized")
        } catch (e: Exception) {
            errorHandler.handleException("NetworkOptimizer.initialize", e)
        }
    }
    
    /**
     * Register network callback to monitor network changes
     */
    private fun registerNetworkCallback() {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            // Create network request
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            
            // Create network callback
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Timber.d("Network available")
                    updateNetworkState()
                    processPendingRequests()
                }
                
                override fun onLost(network: Network) {
                    Timber.d("Network lost")
                    updateNetworkState()
                }
                
                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    Timber.d("Network capabilities changed")
                    updateNetworkState(capabilities)
                }
            }
            
            // Register callback
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
        } catch (e: Exception) {
            errorHandler.handleException("NetworkOptimizer.registerNetworkCallback", e)
        }
    }
    
    /**
     * Update the current network state
     */
    private fun updateNetworkState(capabilities: NetworkCapabilities? = null) {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            // Check if we have an active network
            val activeNetwork = connectivityManager.activeNetwork
            val isConnected = activeNetwork != null
            
            // Get network capabilities
            val networkCapabilities = capabilities ?: connectivityManager.getNetworkCapabilities(activeNetwork)
            
            // Determine network type
            var networkType = NetworkType.NONE
            var bandwidthKbps = 0
            
            if (networkCapabilities != null) {
                // Check network type
                when {
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        networkType = NetworkType.WIFI
                    }
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        networkType = NetworkType.CELLULAR
                    }
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                        networkType = NetworkType.ETHERNET
                    }
                }
                
                // Get bandwidth
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    bandwidthKbps = networkCapabilities.linkDownstreamBandwidthKbps
                }
            }
            
            // Determine network quality
            val networkQuality = when {
                !isConnected -> NetworkQuality.NONE
                bandwidthKbps < POOR_BANDWIDTH_THRESHOLD_KBPS -> NetworkQuality.POOR
                bandwidthKbps < MEDIUM_BANDWIDTH_THRESHOLD_KBPS -> NetworkQuality.MEDIUM
                bandwidthKbps < GOOD_BANDWIDTH_THRESHOLD_KBPS -> NetworkQuality.GOOD
                else -> NetworkQuality.EXCELLENT
            }
            
            // Update network state
            _networkState.value = NetworkState(
                isConnected = isConnected,
                networkType = networkType,
                networkQuality = networkQuality,
                bandwidthKbps = bandwidthKbps
            )
            
            Timber.d("Network state updated: $networkType, $networkQuality, ${bandwidthKbps}kbps")
        } catch (e: Exception) {
            errorHandler.handleException("NetworkOptimizer.updateNetworkState", e)
        }
    }
    
    /**
     * Process pending requests
     */
    private fun processPendingRequests() {
        optimizerScope.launch {
            try {
                if (pendingRequests.isEmpty()) {
                    return@launch
                }
                
                Timber.d("Processing ${pendingRequests.size} pending requests")
                
                // Get current network state
                val currentState = networkState.value
                
                // Process requests if we have a network connection
                if (currentState.isConnected) {
                    // Sort requests by priority (highest first)
                    val sortedRequests = pendingRequests.values.sortedByDescending { it.priority }
                    
                    for (request in sortedRequests) {
                        // Check if the request should be processed based on network quality
                        val shouldProcess = when (currentState.networkQuality) {
                            NetworkQuality.POOR -> request.priority >= PRIORITY_HIGH
                            NetworkQuality.MEDIUM -> request.priority >= PRIORITY_MEDIUM
                            NetworkQuality.GOOD, NetworkQuality.EXCELLENT -> true
                            NetworkQuality.NONE -> false
                        }
                        
                        if (shouldProcess) {
                            Timber.d("Processing pending request: ${request.id}")
                            request.callback()
                            pendingRequests.remove(request.id)
                        }
                    }
                }
            } catch (e: Exception) {
                errorHandler.handleException("NetworkOptimizer.processPendingRequests", e)
            }
        }
    }
    
    /**
     * Add a request to the pending queue
     * 
     * @param id The request ID
     * @param priority The request priority
     * @param callback The callback to execute when the request is processed
     */
    fun addPendingRequest(id: String, priority: Int, callback: () -> Unit) {
        try {
            Timber.d("Adding pending request: $id with priority $priority")
            
            // Create pending request
            val request = PendingRequest(
                id = id,
                priority = priority,
                timestamp = System.currentTimeMillis(),
                callback = callback
            )
            
            // Add to pending requests
            pendingRequests[id] = request
            
            // Process pending requests if we have a network connection
            if (networkState.value.isConnected) {
                processPendingRequests()
            }
        } catch (e: Exception) {
            errorHandler.handleException("NetworkOptimizer.addPendingRequest", e)
        }
    }
    
    /**
     * Remove a pending request
     * 
     * @param id The request ID
     */
    fun removePendingRequest(id: String) {
        try {
            Timber.d("Removing pending request: $id")
            pendingRequests.remove(id)
        } catch (e: Exception) {
            errorHandler.handleException("NetworkOptimizer.removePendingRequest", e)
        }
    }
    
    /**
     * Get the current retry interval based on network quality
     */
    fun getRetryInterval(): Long {
        return when (networkState.value.networkQuality) {
            NetworkQuality.POOR -> RETRY_INTERVAL_POOR_MS
            NetworkQuality.MEDIUM -> RETRY_INTERVAL_MEDIUM_MS
            NetworkQuality.GOOD, NetworkQuality.EXCELLENT -> RETRY_INTERVAL_GOOD_MS
            NetworkQuality.NONE -> RETRY_INTERVAL_POOR_MS
        }
    }
    
    /**
     * Check if the network is suitable for the given priority
     * 
     * @param priority The request priority
     * @return true if the network is suitable, false otherwise
     */
    fun isNetworkSuitable(priority: Int): Boolean {
        val currentState = networkState.value
        
        if (!currentState.isConnected) {
            return false
        }
        
        return when (currentState.networkQuality) {
            NetworkQuality.POOR -> priority >= PRIORITY_HIGH
            NetworkQuality.MEDIUM -> priority >= PRIORITY_MEDIUM
            NetworkQuality.GOOD, NetworkQuality.EXCELLENT -> true
            NetworkQuality.NONE -> false
        }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        try {
            Timber.d("Cleaning up network optimizer")
            
            // Unregister network callback
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            networkCallback?.let {
                connectivityManager.unregisterNetworkCallback(it)
                networkCallback = null
            }
            
            // Clear pending requests
            pendingRequests.clear()
            
            // Reset initialization flag
            isInitialized.set(false)
            
            Timber.d("Network optimizer cleaned up")
        } catch (e: Exception) {
            errorHandler.handleException("NetworkOptimizer.cleanup", e)
        }
    }
}

/**
 * Network state
 */
data class NetworkState(
    val isConnected: Boolean = false,
    val networkType: NetworkType = NetworkType.NONE,
    val networkQuality: NetworkQuality = NetworkQuality.NONE,
    val bandwidthKbps: Int = 0
)

/**
 * Network type
 */
enum class NetworkType {
    NONE,
    WIFI,
    CELLULAR,
    ETHERNET
}

/**
 * Network quality
 */
enum class NetworkQuality {
    NONE,
    POOR,
    MEDIUM,
    GOOD,
    EXCELLENT
}

/**
 * Pending network request
 */
data class PendingRequest(
    val id: String,
    val priority: Int,
    val timestamp: Long,
    val callback: () -> Unit
)
