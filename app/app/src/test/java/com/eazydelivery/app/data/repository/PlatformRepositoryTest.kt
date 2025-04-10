package com.eazydelivery.app.data.repository

import com.eazydelivery.app.data.local.dao.PlatformDao
import com.eazydelivery.app.data.local.entity.PlatformEntity
import com.eazydelivery.app.domain.model.Platform
import com.eazydelivery.app.util.ErrorHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PlatformRepositoryTest {

    @Mock
    private lateinit var platformDao: PlatformDao

    @Mock
    private lateinit var errorHandler: ErrorHandler

    private lateinit var platformRepository: PlatformRepositoryImpl

    private val testPlatformEntity = PlatformEntity(
        name = "Zomato",
        isEnabled = true,
        minAmount = 100,
        maxAmount = 500,
        autoAccept = true,
        priority = 1
    )

    private val testPlatform = Platform(
        name = "Zomato",
        isEnabled = true,
        minAmount = 100,
        maxAmount = 500,
        autoAccept = true,
        priority = 1
    )

    @Before
    fun setup() {
        // Setup error handler to pass through the result
        whenever(errorHandler.safeRepositoryCall<List<Platform>>(any(), any(), any())).thenAnswer { invocation ->
            val block = invocation.getArgument<suspend () -> List<Platform>>(2)
            Result.success(block())
        }

        whenever(errorHandler.safeRepositoryCall<Platform>(any(), any(), any())).thenAnswer { invocation ->
            val block = invocation.getArgument<suspend () -> Platform>(2)
            Result.success(block())
        }

        whenever(errorHandler.safeRepositoryCall<Unit>(any(), any(), any())).thenAnswer { invocation ->
            val block = invocation.getArgument<suspend () -> Unit>(2)
            Result.success(block())
        }

        platformRepository = PlatformRepositoryImpl(platformDao, errorHandler)
    }

    @Test
    fun `getAllPlatforms returns list of platforms`() = runTest {
        // Given
        `when`(platformDao.getAllPlatforms()).thenReturn(listOf(testPlatformEntity))

        // When
        val result = platformRepository.getAllPlatforms()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals(testPlatform, result.getOrNull()?.first())
    }

    @Test
    fun `getPlatform returns platform by name`() = runTest {
        // Given
        `when`(platformDao.getPlatformByName("Zomato")).thenReturn(testPlatformEntity)

        // When
        val result = platformRepository.getPlatform("Zomato")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testPlatform, result.getOrNull())
    }

    @Test
    fun `getEnabledPlatforms returns only enabled platforms`() = runTest {
        // Given
        val enabledPlatform = testPlatformEntity.copy(isEnabled = true)
        `when`(platformDao.getEnabledPlatforms()).thenReturn(listOf(enabledPlatform))

        // When
        val result = platformRepository.getEnabledPlatforms()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertTrue(result.getOrNull()?.first()?.isEnabled ?: false)
    }

    @Test
    fun `togglePlatformStatus updates platform status`() = runTest {
        // When
        val result = platformRepository.togglePlatformStatus("Zomato", true)

        // Then
        assertTrue(result.isSuccess)
        verify(platformDao).updatePlatformStatus("Zomato", true)
    }

    @Test
    fun `updateMinAmount updates platform minimum amount`() = runTest {
        // When
        val result = platformRepository.updateMinAmount("Zomato", 150)

        // Then
        assertTrue(result.isSuccess)
        verify(platformDao).updateMinAmount("Zomato", 150)
    }

    @Test
    fun `updateMaxAmount updates platform maximum amount`() = runTest {
        // When
        val result = platformRepository.updateMaxAmount("Zomato", 600)

        // Then
        assertTrue(result.isSuccess)
        verify(platformDao).updateMaxAmount("Zomato", 600)
    }

    @Test
    fun `updateAutoAccept updates platform auto accept setting`() = runTest {
        // When
        val result = platformRepository.updateAutoAccept("Zomato", false)

        // Then
        assertTrue(result.isSuccess)
        verify(platformDao).updateAutoAccept("Zomato", false)
    }

    @Test
    fun `updatePriority updates platform priority`() = runTest {
        // When
        val result = platformRepository.updatePriority("Zomato", 2)

        // Then
        assertTrue(result.isSuccess)
        verify(platformDao).updatePriority("Zomato", 2)
    }

    @Test
    fun `updatePlatform updates all platform properties`() = runTest {
        // Given
        val updatedPlatform = testPlatform.copy(
            isEnabled = false,
            minAmount = 150,
            maxAmount = 600,
            autoAccept = false,
            priority = 2
        )

        // When
        val result = platformRepository.updatePlatform(updatedPlatform)

        // Then
        assertTrue(result.isSuccess)
        verify(platformDao).updatePlatform(any())
    }
}
