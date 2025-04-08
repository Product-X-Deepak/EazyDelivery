package com.eazydelivery.app.ui.debug

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eazydelivery.app.databinding.ItemPerformanceMetricBinding

/**
 * Adapter for performance metrics
 */
class PerformanceMetricsAdapter : ListAdapter<PerformanceFragment.PerformanceMetricItem, PerformanceMetricsAdapter.ViewHolder>(DIFF_CALLBACK) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPerformanceMetricBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ViewHolder(private val binding: ItemPerformanceMetricBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: PerformanceFragment.PerformanceMetricItem) {
            binding.apply {
                metricName.text = item.name
                metricCount.text = "Count: ${item.count}"
                metricAverage.text = "Avg: ${item.average.toInt()} ms"
                metricMin.text = "Min: ${item.min} ms"
                metricMax.text = "Max: ${item.max} ms"
                metricP90.text = "P90: ${item.p90} ms"
            }
        }
    }
    
    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PerformanceFragment.PerformanceMetricItem>() {
            override fun areItemsTheSame(
                oldItem: PerformanceFragment.PerformanceMetricItem,
                newItem: PerformanceFragment.PerformanceMetricItem
            ): Boolean {
                return oldItem.name == newItem.name
            }
            
            override fun areContentsTheSame(
                oldItem: PerformanceFragment.PerformanceMetricItem,
                newItem: PerformanceFragment.PerformanceMetricItem
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
