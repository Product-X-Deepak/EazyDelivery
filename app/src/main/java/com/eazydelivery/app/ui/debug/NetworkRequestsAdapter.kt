package com.eazydelivery.app.ui.debug

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eazydelivery.app.databinding.ItemNetworkRequestBinding

/**
 * Adapter for network requests
 */
class NetworkRequestsAdapter : ListAdapter<NetworkFragment.NetworkRequestItem, NetworkRequestsAdapter.ViewHolder>(DIFF_CALLBACK) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNetworkRequestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ViewHolder(private val binding: ItemNetworkRequestBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: NetworkFragment.NetworkRequestItem) {
            binding.apply {
                endpoint.text = item.endpoint
                requestCount.text = "Count: ${item.count}"
                averageTime.text = "Avg: ${item.average} ms"
                minTime.text = "Min: ${item.min} ms"
                maxTime.text = "Max: ${item.max} ms"
            }
        }
    }
    
    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<NetworkFragment.NetworkRequestItem>() {
            override fun areItemsTheSame(
                oldItem: NetworkFragment.NetworkRequestItem,
                newItem: NetworkFragment.NetworkRequestItem
            ): Boolean {
                return oldItem.endpoint == newItem.endpoint
            }
            
            override fun areContentsTheSame(
                oldItem: NetworkFragment.NetworkRequestItem,
                newItem: NetworkFragment.NetworkRequestItem
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
