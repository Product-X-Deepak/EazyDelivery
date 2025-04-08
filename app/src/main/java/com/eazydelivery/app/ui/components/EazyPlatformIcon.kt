package com.eazydelivery.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eazydelivery.app.util.PlatformResources

/**
 * Platform icon with branded colors and logo
 */
@Composable
fun EazyPlatformIcon(
    platform: String,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    try {
        val platformData = PlatformResources.getPlatformData(platform)
        val alpha = if (isEnabled) 1f else 0.5f

        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(platformData.brandColor.copy(alpha = alpha))
        ) {
            Image(
                painter = painterResource(id = platformData.logoRes),
                contentDescription = platform,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(size * 0.7f)
                    .clip(CircleShape)
            )
        }
    } catch (e: Exception) {
        // Fallback to the initial implementation if platform data is not found
        val (backgroundColor, textColor, initial) = when (platform) {
            "Zomato" -> Triple(com.eazydelivery.app.ui.theme.ZomatoRed, Color.White, "Z")
            "Swiggy" -> Triple(com.eazydelivery.app.ui.theme.SwiggyOrange, Color.White, "S")
            "Blinkit" -> Triple(com.eazydelivery.app.ui.theme.BlinkitGreen, Color.White, "B")
            "Zepto" -> Triple(com.eazydelivery.app.ui.theme.ZeptoBlue, Color.White, "Z")
            "Instamart" -> Triple(com.eazydelivery.app.ui.theme.InstamartOrange, Color.White, "I")
            "BigBasket" -> Triple(com.eazydelivery.app.ui.theme.BigBasketGreen, Color.White, "B")
            "Uber Eats" -> Triple(com.eazydelivery.app.ui.theme.UberEatsBlack, Color.White, "U")
            else -> Triple(MaterialTheme.colorScheme.primary, Color.White, platform.first().toString())
        }

        val finalBackgroundColor = if (isEnabled) backgroundColor else backgroundColor.copy(alpha = 0.5f)

        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(finalBackgroundColor)
        )
    }
}

/**
 * Platform icon with outline for selection states
 */
@Composable
fun EazyPlatformIconWithBorder(
    platform: String,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    try {
        val platformData = PlatformResources.getPlatformData(platform)

        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(platformData.brandColor)
                .then(
                    if (isSelected) {
                        Modifier.border(2.dp, Color.White, CircleShape)
                    } else {
                        Modifier
                    }
                )
        ) {
            Image(
                painter = painterResource(id = platformData.logoRes),
                contentDescription = platform,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(size * 0.7f)
                    .clip(CircleShape)
            )
        }
    } catch (e: Exception) {
        // Fallback to the initial implementation
        val (backgroundColor, textColor, initial) = when (platform) {
            "Zomato" -> Triple(com.eazydelivery.app.ui.theme.ZomatoRed, Color.White, "Z")
            "Swiggy" -> Triple(com.eazydelivery.app.ui.theme.SwiggyOrange, Color.White, "S")
            "Blinkit" -> Triple(com.eazydelivery.app.ui.theme.BlinkitGreen, Color.White, "B")
            "Zepto" -> Triple(com.eazydelivery.app.ui.theme.ZeptoBlue, Color.White, "Z")
            "Instamart" -> Triple(com.eazydelivery.app.ui.theme.InstamartOrange, Color.White, "I")
            "BigBasket" -> Triple(com.eazydelivery.app.ui.theme.BigBasketGreen, Color.White, "B")
            "Uber Eats" -> Triple(com.eazydelivery.app.ui.theme.UberEatsBlack, Color.White, "U")
            else -> Triple(MaterialTheme.colorScheme.primary, Color.White, platform.first().toString())
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(backgroundColor)
                .then(
                    if (isSelected) {
                        Modifier.border(2.dp, Color.White, CircleShape)
                    } else {
                        Modifier
                    }
                )
        )
    }
}

