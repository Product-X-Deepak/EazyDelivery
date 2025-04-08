package com.eazydelivery.app.domain.model

data class DailyStats(
    val date: String,
    val totalOrders: Int,
    val totalEarnings: Double,
    val platformBreakdown: Map<String, PlatformStat>
)

data class PlatformStat(
    val platformName: String,
    val orderCount: Int,
    val earnings: Double
)
