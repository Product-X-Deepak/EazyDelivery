package com.eazydelivery.app.ui.screens.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.eazydelivery.app.domain.model.DailyStats
import com.eazydelivery.app.domain.model.Platform
import com.eazydelivery.app.domain.model.SubscriptionStatus
import com.eazydelivery.app.domain.repository.AnalyticsRepository
import com.eazydelivery.app.domain.repository.PlatformRepository
import com.eazydelivery.app.domain.repository.ServiceRepository
import com.eazydelivery.app.domain.repository.SubscriptionRepository
import com.eazydelivery.app.util.ErrorHandler
import com.eazydelivery.app.util.SecureStorage
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class HomeViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = StandardTestDispatcher()
    
    @Mock
    private lateinit var serviceRepository: ServiceRepository
    
    @Mock
    private lateinit var platformRepository: PlatformRepository
    
    @Mock
    private lateinit var analyticsRepository: AnalyticsRepository
    
    @Mock
    private lateinit var subscriptionRepository: SubscriptionRepository
    
    @Mock
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    
    @Mock
    private lateinit var secureStorage: SecureStorage
    
    @Mock
    private lateinit var errorHandler: ErrorHandler
    
    private lateinit var viewModel: HomeViewModel
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        // Setup default mock responses
        `when`(serviceRepository.isServiceActive()).thenReturn(Result.success(false))
        `when`(platformRepository.getAllPlatforms()).thenReturn(Result.success(emptyList()))
        `when`(analyticsRepository.getTodayStats()).thenReturn(
            Result.success(
                DailyStats(
                    date = "2023-01-01",
                    totalOrders = 0,
                    totalEarnings = 0.0,
                    platformBreakdown = emptyMap()
                )
            )
        )
        `when`(subscriptionRepository.getSubscriptionStatus()).thenReturn(
            Result.success(
                SubscriptionStatus(
                    isSubscribed = false,
                    trialDaysLeft = 0,
                    endDate = ""
                )
            )
        )
        
        viewModel = HomeViewModel(
            serviceRepository,
            platformRepository,
            analyticsRepository,
            subscriptionRepository,
            firebaseAnalytics,
            secureStorage,
            errorHandler
        )
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `initial state is correct`() = runTest {
        val initialState = viewModel.uiState.first()
        
        assertFalse(initialState.isServiceActive)
        assertTrue(initialState.platforms.isEmpty())
        assertEquals(0, initialState.todayStats.totalOrders)
        assertEquals(0.0, initialState.todayStats.totalEarnings, 0.01)
        assertFalse(initialState.isSubscribed)
        assertEquals(0, initialState.trialDaysLeft)
    }
    
    @Test
    fun `toggleServiceActive updates service status`() = runTest {
        // Given
        val initialState = viewModel.uiState.first()
        assertFalse(initialState.isServiceActive)
        
        // When
        whenever(serviceRepository.setServiceActive(true)).thenReturn(Result.success(Unit))
        viewModel.toggleServiceActive()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val updatedState = viewModel.uiState.first()
        assertTrue(updatedState.isServiceActive)
    }
    
    @Test
    fun `togglePlatform updates platform status`() = runTest {
        // Given
        val platform = Platform(
            name = "Zomato",
            isEnabled = false,
            minAmount = 100
        )
        whenever(platformRepository.getAllPlatforms()).thenReturn(Result.success(listOf(platform)))
        
        // Initialize viewModel to load platforms
        viewModel = HomeViewModel(
            serviceRepository,
            platformRepository,
            analyticsRepository,
            subscriptionRepository,
            firebaseAnalytics,
            secureStorage,
            errorHandler
        )
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        whenever(platformRepository.togglePlatformStatus("Zomato", true)).thenReturn(Result.success(Unit))
        viewModel.togglePlatform("Zomato")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val updatedState = viewModel.uiState.first()
        val updatedPlatform = updatedState.platforms.find { it.name == "Zomato" }
        assertTrue(updatedPlatform?.isEnabled ?: false)
    }
    
    @Test
    fun `updateMinAmount updates platform min amount`() = runTest {
        // Given
        val platform = Platform(
            name = "Zomato",
            isEnabled = true,
            minAmount = 100
        )
        whenever(platformRepository.getAllPlatforms()).thenReturn(Result.success(listOf(platform)))
        
        // Initialize viewModel to load platforms
        viewModel = HomeViewModel(
            serviceRepository,
            platformRepository,
            analyticsRepository,
            subscriptionRepository,
            firebaseAnalytics,
            secureStorage,
            errorHandler
        )
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        whenever(platformRepository.updateMinAmount("Zomato", 150)).thenReturn(Result.success(Unit))
        viewModel.updateMinAmount("Zomato", 150)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val updatedState = viewModel.uiState.first()
        val updatedPlatform = updatedState.platforms.find { it.name == "Zomato" }
        assertEquals(150, updatedPlatform?.minAmount ?: 0)
    }
}
