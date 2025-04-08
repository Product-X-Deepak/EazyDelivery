package com.eazydelivery.app.util

import com.eazydelivery.app.domain.model.Platform

object PlatformResources {

    fun getAllPlatforms(): List<Platform> {
        return listOf(
            Platform(
                name = "zomato",
                isEnabled = false,
                minAmount = 100,
                packageName = "com.zomato.delivery"
            ),
            Platform(
                name = "swiggy",
                isEnabled = false,
                minAmount = 100,
                packageName = "in.swiggy.deliveryapp"
            ),
            Platform(
                name = "blinkit",
                isEnabled = false,
                minAmount = 100,
                packageName = "app.blinkit.onboarding"
            ),
            Platform(
                name = "zepto",
                isEnabled = false,
                minAmount = 100,
                packageName = "com.zepto.rider"
            ),
            Platform(
                name = "instamart",
                isEnabled = false,
                minAmount = 100,
                packageName = "in.swiggy.deliveryapp"
            ),
            Platform(
                name = "bigbasket",
                isEnabled = false,
                minAmount = 100,
                packageName = "com.bigbasket.delivery"
            ),
            Platform(
                name = "ubereats",
                isEnabled = false,
                minAmount = 100,
                packageName = "com.ubercab.driver"
            )
        )
    }

    fun getPlatformDisplayName(platformName: String): String {
        return when (platformName.lowercase()) {
            "zomato" -> "Zomato"
            "swiggy" -> "Swiggy"
            "blinkit" -> "Blinkit"
            "zepto" -> "Zepto"
            "instamart" -> "Instamart"
            "bigbasket" -> "BigBasket"
            "ubereats" -> "Uber Eats"
            else -> platformName.capitalize()
        }
    }

    /**
     * Get the package name for a platform
     */
    fun getPlatformPackageName(platformName: String): String {
        return when (platformName.lowercase()) {
            "zomato" -> "com.zomato.delivery"
            "swiggy" -> "in.swiggy.deliveryapp"
            "blinkit" -> "app.blinkit.onboarding"
            "zepto" -> "com.zepto.rider"
            "instamart" -> "in.swiggy.deliveryapp"
            "bigbasket" -> "com.bigbasket.delivery"
            "ubereats" -> "com.ubercab.driver"
            else -> ""
        }
    }

    private fun String.capitalize(): String {
        return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
