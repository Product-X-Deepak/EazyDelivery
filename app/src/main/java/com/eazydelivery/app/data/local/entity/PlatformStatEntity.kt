package com.eazydelivery.app.data.local.entity

/**
 * Entity class for platform statistics
 */
data class PlatformStatEntity(
    val platformName: String,
    val orderCount: Int,
    val totalEarnings: Double
)
