package com.eazydelivery.app.ui.component

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.eazydelivery.app.R
import kotlin.math.min

/**
 * A custom view that displays the current performance status of the app.
 * Shows CPU usage, memory usage, and battery optimization status.
 */
class PerformanceStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    // Performance metrics
    var cpuUsage: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 100f)
            invalidate()
        }
    
    var memoryUsage: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 100f)
            invalidate()
        }
    
    var batteryOptimized: Boolean = true
        set(value) {
            field = value
            invalidate()
        }
    
    // Paint objects
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.background_light)
        style = Paint.Style.FILL
    }
    
    private val cpuPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    private val memoryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    private val batteryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_primary)
        textSize = context.resources.getDimension(R.dimen.text_size_small)
        textAlign = Paint.Align.CENTER
    }
    
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.border)
        style = Paint.Style.STROKE
        strokeWidth = context.resources.getDimension(R.dimen.border_width)
    }
    
    // Drawing areas
    private val cpuRect = RectF()
    private val memoryRect = RectF()
    private val batteryRect = RectF()
    
    // Dimensions
    private val padding = context.resources.getDimension(R.dimen.padding_small)
    private val cornerRadius = context.resources.getDimension(R.dimen.corner_radius)
    private val barHeight = context.resources.getDimension(R.dimen.bar_height)
    private val barSpacing = context.resources.getDimension(R.dimen.spacing_small)
    private val textOffset = context.resources.getDimension(R.dimen.text_offset)
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Calculate dimensions
        val contentWidth = width - (padding * 2)
        val contentHeight = height - (padding * 2)
        
        // Draw background
        canvas.drawRoundRect(
            padding,
            padding,
            width - padding,
            height - padding,
            cornerRadius,
            cornerRadius,
            backgroundPaint
        )
        
        // Draw border
        canvas.drawRoundRect(
            padding,
            padding,
            width - padding,
            height - padding,
            cornerRadius,
            cornerRadius,
            borderPaint
        )
        
        // Draw CPU usage
        cpuRect.set(
            padding * 2,
            padding * 2,
            padding * 2 + (contentWidth - padding * 2) * (cpuUsage / 100f),
            padding * 2 + barHeight
        )
        
        cpuPaint.color = when {
            cpuUsage < 30f -> ContextCompat.getColor(context, R.color.green_500)
            cpuUsage < 70f -> ContextCompat.getColor(context, R.color.yellow_500)
            else -> ContextCompat.getColor(context, R.color.red_500)
        }
        
        canvas.drawRoundRect(
            cpuRect,
            cornerRadius / 2,
            cornerRadius / 2,
            cpuPaint
        )
        
        // Draw CPU text
        canvas.drawText(
            "CPU: ${cpuUsage.toInt()}%",
            width / 2f,
            padding * 2 + barHeight + textOffset,
            textPaint
        )
        
        // Draw memory usage
        memoryRect.set(
            padding * 2,
            padding * 2 + barHeight + textOffset * 2,
            padding * 2 + (contentWidth - padding * 2) * (memoryUsage / 100f),
            padding * 2 + barHeight * 2 + textOffset * 2
        )
        
        memoryPaint.color = when {
            memoryUsage < 30f -> ContextCompat.getColor(context, R.color.green_500)
            memoryUsage < 70f -> ContextCompat.getColor(context, R.color.yellow_500)
            else -> ContextCompat.getColor(context, R.color.red_500)
        }
        
        canvas.drawRoundRect(
            memoryRect,
            cornerRadius / 2,
            cornerRadius / 2,
            memoryPaint
        )
        
        // Draw memory text
        canvas.drawText(
            "Memory: ${memoryUsage.toInt()}%",
            width / 2f,
            padding * 2 + barHeight * 2 + textOffset * 3,
            textPaint
        )
        
        // Draw battery optimization status
        batteryRect.set(
            padding * 2,
            padding * 2 + barHeight * 2 + textOffset * 4,
            width - padding * 2,
            padding * 2 + barHeight * 3 + textOffset * 4
        )
        
        batteryPaint.color = if (batteryOptimized) {
            ContextCompat.getColor(context, R.color.green_500)
        } else {
            ContextCompat.getColor(context, R.color.yellow_500)
        }
        
        canvas.drawRoundRect(
            batteryRect,
            cornerRadius / 2,
            cornerRadius / 2,
            batteryPaint
        )
        
        // Draw battery text
        canvas.drawText(
            if (batteryOptimized) "Battery: Optimized" else "Battery: Standard",
            width / 2f,
            padding * 2 + barHeight * 3 + textOffset * 5,
            textPaint
        )
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = suggestedMinimumWidth + paddingLeft + paddingRight
        val desiredHeight = (padding * 4 + barHeight * 3 + textOffset * 6).toInt()
        
        val width = resolveSize(desiredWidth, widthMeasureSpec)
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        
        setMeasuredDimension(width, height)
    }
}
