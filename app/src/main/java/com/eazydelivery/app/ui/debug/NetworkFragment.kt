package com.eazydelivery.app.ui.debug

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.eazydelivery.app.databinding.FragmentNetworkBinding
import com.eazydelivery.app.monitoring.NetworkMonitoringManager
import com.eazydelivery.app.ui.base.BaseFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Fragment for displaying network metrics
 */
class NetworkFragment : BaseFragment() {
    
    private var _binding: FragmentNetworkBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: MonitoringDashboardViewModel by activityViewModels()
    
    private lateinit var adapter: NetworkRequestsAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNetworkBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupObservers()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    private fun setupRecyclerView() {
        adapter = NetworkRequestsAdapter()
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@NetworkFragment.adapter
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }
    }
    
    private fun setupObservers() {
        // Observe network statistics
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.networkStatistics.collectLatest { stats ->
                if (stats == null) {
                    binding.statsCard.visibility = View.GONE
                } else {
                    binding.statsCard.visibility = View.VISIBLE
                    
                    // Update statistics
                    binding.apply {
                        networkType.text = "Network Type: ${stats.networkType}"
                        requestCount.text = "Requests: ${stats.requestCount}"
                        successCount.text = "Success: ${stats.successCount}"
                        failureCount.text = "Failures: ${stats.failureCount}"
                        
                        val successRate = if (stats.requestCount > 0) {
                            (stats.successCount.toDouble() / stats.requestCount.toDouble() * 100).toInt()
                        } else {
                            0
                        }
                        
                        successRate.text = "Success Rate: $successRate%"
                        
                        averageTime.text = "Avg Time: ${stats.averageRequestTime} ms"
                        totalRequestSize.text = "Request Size: ${formatSize(stats.totalRequestSize)}"
                        totalResponseSize.text = "Response Size: ${formatSize(stats.totalResponseSize)}"
                    }
                }
            }
        }
        
        // Observe request times by endpoint
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.requestTimesByEndpoint.collectLatest { requestTimes ->
                if (requestTimes.isEmpty()) {
                    binding.emptyView.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                } else {
                    binding.emptyView.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                    
                    // Convert request times to list of items
                    val items = requestTimes.map { (endpoint, times) ->
                        NetworkRequestItem(
                            endpoint = endpoint,
                            count = times.size,
                            average = times.average().toLong(),
                            min = times.minOrNull() ?: 0,
                            max = times.maxOrNull() ?: 0
                        )
                    }.sortedBy { it.endpoint }
                    
                    adapter.submitList(items)
                }
            }
        }
    }
    
    /**
     * Format size in bytes to human-readable format
     * 
     * @param bytes Size in bytes
     * @return Formatted size
     */
    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
    
    /**
     * Data class for network request item
     */
    data class NetworkRequestItem(
        val endpoint: String,
        val count: Int,
        val average: Long,
        val min: Long,
        val max: Long
    )
}
