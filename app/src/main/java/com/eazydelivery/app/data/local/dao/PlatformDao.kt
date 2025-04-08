package com.eazydelivery.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.eazydelivery.app.data.local.entity.PlatformEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlatformDao {
    @Query("SELECT * FROM platforms")
    fun getAllPlatformsFlow(): Flow<List<PlatformEntity>>
    
    @Query("SELECT * FROM platforms")
    suspend fun getAllPlatforms(): List<PlatformEntity>
    
    @Query("SELECT * FROM platforms WHERE name = :name")
    suspend fun getPlatformByName(name: String): PlatformEntity?
    
    @Query("SELECT * FROM platforms WHERE isEnabled = 1")
    suspend fun getEnabledPlatforms(): List<PlatformEntity>
    
    @Insert(onConflictStrategy = OnConflictStrategy.REPLACE)
    suspend fun insertPlatform(platform: PlatformEntity)
    
    @Insert(onConflictStrategy = OnConflictStrategy.REPLACE)
    suspend fun insertAllPlatforms(platforms: List<PlatformEntity>)
    
    @Update
    suspend fun updatePlatform(platform: PlatformEntity)
    
    @Query("UPDATE platforms SET isEnabled = :isEnabled WHERE name = :name")
    suspend fun updatePlatformStatus(name: String, isEnabled: Boolean)
    
    @Query("UPDATE platforms SET minAmount = :minAmount WHERE name = :name")
    suspend fun updateMinAmount(name: String, minAmount: Int)
    
    @Query("UPDATE platforms SET maxAmount = :maxAmount WHERE name = :name")
    suspend fun updateMaxAmount(name: String, maxAmount: Int)
    
    @Query("UPDATE platforms SET autoAccept = :autoAccept WHERE name = :name")
    suspend fun updateAutoAccept(name: String, autoAccept: Boolean)
    
    @Query("UPDATE platforms SET priority = :priority WHERE name = :name")
    suspend fun updatePriority(name: String, priority: Int)
    
    @Query("UPDATE platforms SET acceptMediumPriority = :accept WHERE name = :name")
    suspend fun updateAcceptMediumPriority(name: String, accept: Boolean)
    
    @Query("UPDATE platforms SET notificationSound = :soundUri WHERE name = :name")
    suspend fun updateNotificationSound(name: String, soundUri: String?)
}
