package com.eazydelivery.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack as AutoMirroredArrowBack
import androidx.compose.material.icons.filled.ArrowBack as LegacyArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * Utility class for handling icon compatibility
 * This helps transition from deprecated icons to newer versions
 * without breaking existing code
 */
object IconUtils {
    /**
     * Returns the appropriate ArrowBack icon based on layout direction
     * Uses the new AutoMirrored version when in RTL mode
     */
    @Composable
    fun getArrowBackIcon() = when (LocalLayoutDirection.current) {
        LayoutDirection.Rtl -> AutoMirroredArrowBack
        else -> LegacyArrowBack
    }
}
