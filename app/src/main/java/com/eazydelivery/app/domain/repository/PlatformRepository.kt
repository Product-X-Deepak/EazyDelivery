package com.eazydelivery.app.domain.repository

import com.eazydelivery.app.domain.model.Platform
import kotlinx.coroutines.flow.Flow

interface PlatformRepository {
    fun getAllPlatformsFlow(): Flow<Result<List<Platform>>>
    suspend fun getAllPlatforms(): Result<List<Platform>>
    suspend fun getPlatform(name: String): Result<Platform>
    suspend fun updatePlatform(platform: Platform): Result<Unit>
    suspend fun getEnabledPlatforms(): Result<List<Platform>>
    suspend fun togglePlatformStatus(name: String, isEnabled: Boolean): Result<Unit>
    suspend fun updateMinAmount(name: String, amount: Int): Result<Unit>
}
