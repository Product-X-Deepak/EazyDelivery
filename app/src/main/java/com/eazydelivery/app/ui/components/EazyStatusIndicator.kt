package com.eazydelivery.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eazydelivery.app.ui.theme.ActiveGreen
import com.eazydelivery.app.ui.theme.InactiveRed
import com.eazydelivery.app.ui.theme.PendingYellow
import com.eazydelivery.app.ui.theme.ProcessingBlue

/**
 * Status types for the indicator
 */
enum class StatusType {
    ACTIVE,
    INACTIVE,
    PENDING,
    PROCESSING
}

/**
 * Status indicator with animated color changes
 */
@Composable
fun EazyStatusIndicator(
    status: StatusType,
    text: String? = null,
    modifier: Modifier = Modifier
) {
    val statusColor by animateColorAsState(
        targetValue = when (status) {
            StatusType.ACTIVE -> ActiveGreen
            StatusType.INACTIVE -> InactiveRed
            StatusType.PENDING -> PendingYellow
            StatusType.PROCESSING -> ProcessingBlue
        },
        animationSpec = tween(durationMillis = 300),
        label = "StatusColorAnimation"
    )
    
    val statusText = text ?: when (status) {
        StatusType.ACTIVE -> "Active"
        StatusType.INACTIVE -> "Inactive"
        StatusType.PENDING -> "Pending"
        StatusType.PROCESSING -> "Processing"
    }
    
    Surface(
        shape = MaterialTheme.shapes.small,
        color = statusColor.copy(alpha = 0.1f),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium,
                color = statusColor
            )
        }
    }
}

/**
 * Large status indicator with more prominent display
 */
@Composable
fun EazyLargeStatusIndicator(
    status: StatusType,
    text: String? = null,
    modifier: Modifier = Modifier
) {
    val statusColor by animateColorAsState(
        targetValue = when (status) {
            StatusType.ACTIVE -> ActiveGreen
            StatusType.INACTIVE -> InactiveRed
            StatusType.PENDING -> PendingYellow
            StatusType.PROCESSING -> ProcessingBlue
        },
        animationSpec = tween(durationMillis = 300),
        label = "LargeStatusColorAnimation"
    )
    
    val statusText = text ?: when (status) {
        StatusType.ACTIVE -> "Active"
        StatusType.INACTIVE -> "Inactive"
        StatusType.PENDING -> "Pending"
        StatusType.PROCESSING -> "Processing"
    }
    
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = statusColor.copy(alpha = 0.1f),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = statusText,
                style = MaterialTheme.typography.titleMedium,
                color = statusColor
            )
        }
    }
}

