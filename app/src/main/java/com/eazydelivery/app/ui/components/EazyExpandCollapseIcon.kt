package com.eazydelivery.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp

/**
 * Animated expand/collapse icon
 */
@Composable
fun EazyExpandCollapseIcon(
    expanded: Boolean,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "ExpandIconRotation"
    )
    
    val iconModifier = if (onClick != null) {
        modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
            .size(24.dp)
    } else {
        modifier
            .padding(8.dp)
            .size(24.dp)
    }
    
    Icon(
        imageVector = Icons.Default.KeyboardArrowDown,
        contentDescription = if (expanded) "Collapse" else "Expand",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = iconModifier.rotate(rotation)
    )
}

