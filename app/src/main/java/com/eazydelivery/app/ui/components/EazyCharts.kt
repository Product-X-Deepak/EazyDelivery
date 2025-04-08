package com.eazydelivery.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Data class for pie chart segments
 */
data class PieChartData(
    val label: String,
    val value: Float,
    val color: Color
)

/**
 * Animated pie chart with legend
 */
@Composable
fun EazyPieChart(
    data: List<PieChartData>,
    modifier: Modifier = Modifier,
    showLegend: Boolean = true,
    animationDuration: Int = 1000
) {
    if (data.isEmpty()) return
    
    val totalValue = data.sumOf { it.value.toDouble() }.toFloat()
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(animationDuration),
        label = "PieChartAnimation"
    )
    
    LaunchedEffect(key1 = true) {
        animationPlayed = true
    }
    
    Column(modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxWidth()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val radius = min(canvasWidth, canvasHeight) / 2
                val center = Offset(canvasWidth / 2, canvasHeight / 2)
                
                var startAngle = -90f
                
                data.forEach { pieData ->
                    val sweepAngle = (pieData.value / totalValue) * 360f * animatedProgress
                    
                    drawArc(
                        color = pieData.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = 32f, cap = StrokeCap.Butt)
                    )
                    
                    startAngle += sweepAngle
                }
            }
            
            // Center text showing total
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = totalValue.toInt().toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        if (showLegend) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                data.forEach { pieData ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(pieData.color)
                        )
                        
                        Text(
                            text = pieData.label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .weight(1f)
                        )
                        
                        Text(
                            text = pieData.value.toInt().toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

/**
 * Data class for bar chart items
 */
data class BarChartData(
    val label: String,
    val value: Float,
    val color: Color = MaterialTheme.colorScheme.primary
)

/**
 * Animated bar chart with customizable colors
 */
@Composable
fun EazyBarChart(
    data: List<BarChartData>,
    modifier: Modifier = Modifier,
    maxValue: Float? = null,
    animationDuration: Int = 1000
) {
    if (data.isEmpty()) return
    
    val chartMaxValue = maxValue ?: data.maxOf { it.value }
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(animationDuration),
        label = "BarChartAnimation"
    )
    
    LaunchedEffect(key1 = true) {
        animationPlayed = true
    }
    
    Column(modifier = modifier.fillMaxWidth()) {
        data.forEach { barData ->
            val percentage = (barData.value / chartMaxValue) * animatedProgress
            
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = barData.label,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = barData.value.toInt().toString(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(percentage)
                            .height(16.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(barData.color)
                    )
                }
            }
        }
    }
}

