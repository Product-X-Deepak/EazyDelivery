package com.eazydelivery.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standard divider with consistent styling
 */
@Composable
fun EazyDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
    color: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
    startIndent: Dp = 0.dp,
    endIndent: Dp = 0.dp
) {
    Divider(
        modifier = modifier.fillMaxWidth(),
        thickness = thickness,
        color = color,
        startIndent = startIndent
    )
}

/**
 * Divider with vertical padding
 */
@Composable
fun EazySpacedDivider(
    verticalPadding: Dp = 16.dp,
    thickness: Dp = 1.dp,
    color: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
) {
    Divider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = verticalPadding),
        thickness = thickness,
        color = color
    )
}

