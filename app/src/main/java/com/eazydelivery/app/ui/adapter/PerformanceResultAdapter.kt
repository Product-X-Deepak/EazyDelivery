package com.eazydelivery.app.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.eazydelivery.app.R

/**
 * Adapter for displaying performance validation results
 */
class PerformanceResultAdapter : RecyclerView.Adapter<PerformanceResultAdapter.ResultViewHolder>() {
    
    private val results = mutableListOf<ResultItem>()
    
    /**
     * Sets the results to display
     * 
     * @param newResults The results to display
     */
    fun setResults(newResults: List<ResultItem>) {
        results.clear()
        results.addAll(newResults)
        notifyDataSetChanged()
    }
    
    /**
     * Clears all results
     */
    fun clearResults() {
        results.clear()
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_performance_result, parent, false)
        return ResultViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        holder.bind(results[position])
    }
    
    override fun getItemCount(): Int = results.size
    
    /**
     * ViewHolder for performance result items
     */
    class ResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.result_name)
        private val detailsTextView: TextView = itemView.findViewById(R.id.result_details)
        private val statusImageView: ImageView = itemView.findViewById(R.id.result_status)
        
        /**
         * Binds a result item to the view
         * 
         * @param item The result item to bind
         */
        fun bind(item: ResultItem) {
            nameTextView.text = item.name
            detailsTextView.text = item.details
            
            // Set status icon and color
            if (item.passed) {
                statusImageView.setImageResource(R.drawable.ic_check_circle)
                statusImageView.setColorFilter(
                    ContextCompat.getColor(itemView.context, R.color.green_500)
                )
            } else {
                statusImageView.setImageResource(R.drawable.ic_error)
                statusImageView.setColorFilter(
                    ContextCompat.getColor(itemView.context, R.color.red_500)
                )
            }
        }
    }
    
    /**
     * Data class for performance result items
     */
    data class ResultItem(
        val id: String,
        val name: String,
        val passed: Boolean,
        val details: String
    )
}
