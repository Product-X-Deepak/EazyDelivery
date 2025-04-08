package com.eazydelivery.app.domain.repository

import android.content.Context

interface PermissionRepository {
    suspend fun requestNotificationPermission(context: Context): Result<Boolean>
    suspend fun requestAccessibilityPermission(context: Context): Result<Boolean>
    suspend fun requestBatteryOptimizationPermission(context: Context): Result<Boolean>
    suspend fun checkNotificationPermission(context: Context): Result<Boolean>
    suspend fun checkAccessibilityPermission(context: Context): Result<Boolean>
    suspend fun checkBatteryOptimizationPermission(context: Context): Result<Boolean>
}
