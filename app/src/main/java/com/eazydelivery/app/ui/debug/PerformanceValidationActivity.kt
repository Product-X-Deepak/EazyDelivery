package com.eazydelivery.app.ui.debug

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eazydelivery.app.R
import com.eazydelivery.app.ui.adapter.PerformanceResultAdapter
import com.eazydelivery.app.util.PerformanceValidator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Activity for running performance validation tests
 * This is a debug-only activity for validating performance optimizations
 */
@AndroidEntryPoint
class PerformanceValidationActivity : AppCompatActivity() {
    
    @Inject
    lateinit var performanceValidator: PerformanceValidator
    
    private lateinit var startButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var resultsRecyclerView: RecyclerView
    private lateinit var resultAdapter: PerformanceResultAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_performance_validation)
        
        // Initialize views
        startButton = findViewById(R.id.start_validation_button)
        progressBar = findViewById(R.id.validation_progress)
        statusText = findViewById(R.id.validation_status)
        resultsRecyclerView = findViewById(R.id.results_recycler_view)
        
        // Setup RecyclerView
        resultAdapter = PerformanceResultAdapter()
        resultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@PerformanceValidationActivity)
            adapter = resultAdapter
        }
        
        // Setup button click listener
        startButton.setOnClickListener {
            startValidation()
        }
    }
    
    /**
     * Starts the performance validation
     */
    private fun startValidation() {
        // Update UI
        startButton.isEnabled = false
        progressBar.visibility = View.VISIBLE
        statusText.text = getString(R.string.validation_in_progress)
        resultAdapter.clearResults()
        
        // Run validation
        performanceValidator.validatePerformance { results ->
            // Update UI with results
            startButton.isEnabled = true
            progressBar.visibility = View.GONE
            statusText.text = getString(R.string.validation_complete)
            
            // Convert results to adapter items
            val items = results.map { (key, result) ->
                PerformanceResultAdapter.ResultItem(
                    id = key,
                    name = result.name,
                    passed = result.passed,
                    details = when {
                        result.executionTime > 0 -> "${result.executionTime}ms (Threshold: ${result.threshold}ms)"
                        result.memoryUsage > 0 -> "${formatSize(result.memoryUsage)} (Threshold: ${formatSize(result.threshold)})"
                        result.batteryUsage > 0 -> "${result.batteryUsage}% (Threshold: ${result.threshold}%)"
                        else -> result.notes
                    }
                )
            }
            
            // Update adapter
            resultAdapter.setResults(items)
        }
    }
    
    /**
     * Formats a size in bytes to a human-readable string
     * 
     * @param bytes The size in bytes
     * @return A human-readable string
     */
    private fun formatSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0
        
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        
        return String.format("%.1f %s", size, units[unitIndex])
    }
}
