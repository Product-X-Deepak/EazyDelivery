package com.eazydelivery.app.data.repository

import com.eazydelivery.app.data.local.dao.PlatformDao
import com.eazydelivery.app.data.local.entity.PlatformEntity
import com.eazydelivery.app.domain.model.Platform
import com.eazydelivery.app.domain.repository.BaseRepositoryImpl
import com.eazydelivery.app.domain.repository.PlatformRepository
import com.eazydelivery.app.util.ErrorHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlatformRepositoryImpl @Inject constructor(
    private val platformDao: PlatformDao,
    override val errorHandler: ErrorHandler
) : PlatformRepository, BaseRepositoryImpl() {

    override suspend fun getAllPlatforms(): Result<List<Platform>> = safeDatabaseOperation("getAllPlatforms") {
        platformDao.getAllPlatforms().map { it.toDomainModel() }
    }

    override suspend fun getPlatform(name: String): Result<Platform> = safeDatabaseOperation("getPlatform") {
        val platformEntity = platformDao.getPlatformByName(name)
            ?: throw IllegalArgumentException("Platform not found: $name")
        platformEntity.toDomainModel()
    }

    override suspend fun togglePlatformStatus(name: String, isEnabled: Boolean): Result<Unit> =
        safeDatabaseOperation("togglePlatformStatus") {
            platformDao.updatePlatformStatus(name, isEnabled)
        }

    override suspend fun updateMinAmount(name: String, amount: Int): Result<Unit> =
        safeDatabaseOperation("updateMinAmount") {
            if (amount < 0) {
                throw IllegalArgumentException("Amount cannot be negative")
            }
            platformDao.updateMinAmount(name, amount)
        }

    override suspend fun updateMaxAmount(name: String, amount: Int): Result<Unit> =
        safeDatabaseOperation("updateMaxAmount") {
            if (amount < 0) {
                throw IllegalArgumentException("Amount cannot be negative")
            }
            platformDao.updateMaxAmount(name, amount)
        }

    override suspend fun updateAutoAccept(name: String, autoAccept: Boolean): Result<Unit> =
        safeDatabaseOperation("updateAutoAccept") {
            platformDao.updateAutoAccept(name, autoAccept)
        }

    override suspend fun updatePriority(name: String, priority: Int): Result<Unit> =
        safeDatabaseOperation("updatePriority") {
            platformDao.updatePriority(name, priority)
        }

    override suspend fun updateAcceptMediumPriority(name: String, accept: Boolean): Result<Unit> =
        safeDatabaseOperation("updateAcceptMediumPriority") {
            platformDao.updateAcceptMediumPriority(name, accept)
        }

    override suspend fun updateNotificationSound(name: String, soundUri: String?): Result<Unit> =
        safeDatabaseOperation("updateNotificationSound") {
            platformDao.updateNotificationSound(name, soundUri)
        }

    private fun PlatformEntity.toDomainModel(): Platform {
        return Platform(
            name = name,
            isEnabled = isEnabled,
            minAmount = minAmount,
            maxAmount = maxAmount,
            autoAccept = autoAccept,
            notificationSound = notificationSound,
            priority = priority,
            acceptMediumPriority = acceptMediumPriority,
            packageName = packageName,
            shouldRemove = shouldRemove
        )
    }

    override suspend fun updatePlatform(platform: Platform): Result<Unit> = safeDatabaseOperation("updatePlatform") {
        val entity = PlatformEntity(
            name = platform.name,
            isEnabled = platform.isEnabled,
            minAmount = platform.minAmount,
            maxAmount = platform.maxAmount,
            autoAccept = platform.autoAccept,
            notificationSound = platform.notificationSound,
            priority = platform.priority,
            acceptMediumPriority = platform.acceptMediumPriority,
            packageName = platform.packageName,
            shouldRemove = platform.shouldRemove
        )
        platformDao.updatePlatform(entity)
    }
}
