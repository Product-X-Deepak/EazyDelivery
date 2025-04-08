package com.eazydelivery.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Refined shape system with consistent rounding
val Shapes = Shapes(
    // For small UI elements like chips and small buttons
    extraSmall = RoundedCornerShape(4.dp),
    
    // For buttons, text fields, and small cards
    small = RoundedCornerShape(8.dp),
    
    // For medium-sized cards and dialogs
    medium = RoundedCornerShape(12.dp),
    
    // For large cards and bottom sheets
    large = RoundedCornerShape(16.dp),
    
    // For full-screen dialogs and major UI elements
    extraLarge = RoundedCornerShape(24.dp)
)

