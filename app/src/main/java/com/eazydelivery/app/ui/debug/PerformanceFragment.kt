package com.eazydelivery.app.ui.debug

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.eazydelivery.app.databinding.FragmentPerformanceBinding
import com.eazydelivery.app.monitoring.PerformanceMonitoringManager
import com.eazydelivery.app.ui.base.BaseFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Fragment for displaying performance metrics
 */
class PerformanceFragment : BaseFragment() {
    
    private var _binding: FragmentPerformanceBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: MonitoringDashboardViewModel by activityViewModels()
    
    private lateinit var adapter: PerformanceMetricsAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerformanceBinding.inflate(inflater, container, false)
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
        adapter = PerformanceMetricsAdapter()
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@PerformanceFragment.adapter
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }
    }
    
    private fun setupObservers() {
        // Observe performance metrics
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.performanceMetrics.collectLatest { metrics ->
                if (metrics.isEmpty()) {
                    binding.emptyView.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                } else {
                    binding.emptyView.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                    
                    // Convert metrics to list of items
                    val items = metrics.map { (name, stats) ->
                        PerformanceMetricItem(
                            name = name,
                            count = stats.count,
                            average = stats.average,
                            min = stats.min,
                            max = stats.max,
                            p90 = stats.p90
                        )
                    }.sortedBy { it.name }
                    
                    adapter.submitList(items)
                }
            }
        }
    }
    
    /**
     * Data class for performance metric item
     */
    data class PerformanceMetricItem(
        val name: String,
        val count: Int,
        val average: Double,
        val min: Long,
        val max: Long,
        val p90: Long
    )
}
