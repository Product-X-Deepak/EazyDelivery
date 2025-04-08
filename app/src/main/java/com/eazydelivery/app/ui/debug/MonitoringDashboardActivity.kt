package com.eazydelivery.app.ui.debug

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.eazydelivery.app.R
import com.eazydelivery.app.databinding.ActivityMonitoringDashboardBinding
import com.eazydelivery.app.monitoring.NetworkMonitoringManager
import com.eazydelivery.app.monitoring.PerformanceMonitoringManager
import com.eazydelivery.app.ui.base.BaseActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Activity for displaying monitoring information
 * This activity is only available in debug builds
 */
@AndroidEntryPoint
class MonitoringDashboardActivity : BaseActivity() {
    
    private lateinit var binding: ActivityMonitoringDashboardBinding
    
    private val viewModel: MonitoringDashboardViewModel by viewModels()
    
    @Inject
    lateinit var performanceMonitoringManager: PerformanceMonitoringManager
    
    @Inject
    lateinit var networkMonitoringManager: NetworkMonitoringManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMonitoringDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        setupTabs()
        setupObservers()
        setupRefreshButton()
        
        // Load initial data
        viewModel.refreshData()
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_monitoring_dashboard, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_reset_statistics -> {
                viewModel.resetStatistics()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun setupTabs() {
        // Set up tabs
        binding.tabLayout.setupWithViewPager(binding.viewPager)
        
        // Set up view pager
        val adapter = MonitoringPagerAdapter(supportFragmentManager)
        adapter.addFragment(PerformanceFragment(), "Performance")
        adapter.addFragment(NetworkFragment(), "Network")
        adapter.addFragment(MemoryFragment(), "Memory")
        adapter.addFragment(BatteryFragment(), "Battery")
        binding.viewPager.adapter = adapter
    }
    
    private fun setupObservers() {
        // Observe loading state
        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.swipeRefreshLayout.isRefreshing = isLoading
            }
        }
        
        // Observe network state
        lifecycleScope.launch {
            networkMonitoringManager.networkState.collectLatest { state ->
                binding.networkStatusText.text = "Network: $state"
            }
        }
    }
    
    private fun setupRefreshButton() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshData()
        }
    }
}
