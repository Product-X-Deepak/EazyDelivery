package com.eazydelivery.app.ui.base

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.eazydelivery.app.util.ErrorHandler
import com.eazydelivery.app.util.error.AppError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

/**
 * Unit test for the BaseViewModel
 */
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class BaseViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    @Mock
    private lateinit var errorHandler: ErrorHandler
    
    @Mock
    private lateinit var appErrorObserver: Observer<BaseViewModel.Event<AppError>>
    
    @Mock
    private lateinit var isLoadingObserver: Observer<Boolean>
    
    @Mock
    private lateinit var errorObserver: Observer<String>
    
    @Mock
    private lateinit var successObserver: Observer<String>
    
    private lateinit var viewModel: TestBaseViewModel
    
    private val testDispatcher = TestCoroutineDispatcher()
    
    // Test implementation of BaseViewModel
    class TestBaseViewModel(errorHandler: ErrorHandler) : BaseViewModel(errorHandler) {
        fun testLaunchSafe(block: suspend () -> Unit) {
            launchSafe("TestBaseViewModel", true, Dispatchers.Main, block)
        }
        
        fun testLaunchWithRetry(block: suspend () -> Unit) {
            launchWithRetry("TestBaseViewModel", 3, true, Dispatchers.Main, block)
        }
        
        suspend fun testEmitError(message: String) {
            emitError(message)
        }
        
        suspend fun testEmitSuccess(message: String) {
            emitSuccess(message)
        }
        
        fun testSetLoading(isLoading: Boolean) {
            setLoading(isLoading)
        }
    }
    
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = TestBaseViewModel(errorHandler)
        viewModel.appError.observeForever(appErrorObserver)
        viewModel.isLoading.observeForever(isLoadingObserver)
        viewModel.error.observeForever(errorObserver)
        viewModel.success.observeForever(successObserver)
    }
    
    @After
    fun tearDown() {
        viewModel.appError.removeObserver(appErrorObserver)
        viewModel.isLoading.removeObserver(isLoadingObserver)
        viewModel.error.removeObserver(errorObserver)
        viewModel.success.removeObserver(successObserver)
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }
    
    @Test
    fun `launchSafe should set loading to true and then false`() = testDispatcher.runBlockingTest {
        // Given
        val block: suspend () -> Unit = {}
        
        // When
        viewModel.testLaunchSafe(block)
        
        // Then
        verify(isLoadingObserver).onChanged(true)
        verify(isLoadingObserver).onChanged(false)
    }
    
    @Test
    fun `launchSafe should handle exceptions`() = testDispatcher.runBlockingTest {
        // Given
        val exception = RuntimeException("Test exception")
        val block: suspend () -> Unit = { throw exception }
        val appError = AppError.Unexpected("Test exception")
        
        // Mock the error handler
        `when`(errorHandler.handleException("TestBaseViewModel", exception)).thenReturn(appError)
        
        // When
        viewModel.testLaunchSafe(block)
        
        // Then
        verify(errorHandler).handleException("TestBaseViewModel", exception)
        verify(appErrorObserver).onChanged(org.mockito.ArgumentMatchers.any())
    }
    
    @Test
    fun `launchWithRetry should retry on failure`() = testDispatcher.runBlockingTest {
        // Given
        var attempts = 0
        val block: suspend () -> Unit = {
            attempts++
            if (attempts < 2) {
                throw RuntimeException("Test exception")
            }
        }
        
        // When
        viewModel.testLaunchWithRetry(block)
        
        // Then
        assert(attempts == 2) // Should have retried once
    }
    
    @Test
    fun `launchWithRetry should handle exceptions after max retries`() = testDispatcher.runBlockingTest {
        // Given
        val exception = RuntimeException("Test exception")
        val block: suspend () -> Unit = { throw exception }
        val appError = AppError.Unexpected("Test exception")
        
        // Mock the error handler
        `when`(errorHandler.handleException("TestBaseViewModel", exception)).thenReturn(appError)
        
        // When
        viewModel.testLaunchWithRetry(block)
        
        // Then
        verify(errorHandler).handleException("TestBaseViewModel", exception)
        verify(appErrorObserver).onChanged(org.mockito.ArgumentMatchers.any())
    }
    
    @Test
    fun `emitError should emit error message`() = testDispatcher.runBlockingTest {
        // Given
        val errorMessage = "Test error"
        
        // When
        viewModel.testEmitError(errorMessage)
        
        // Then
        verify(errorObserver).onChanged(errorMessage)
    }
    
    @Test
    fun `emitSuccess should emit success message`() = testDispatcher.runBlockingTest {
        // Given
        val successMessage = "Test success"
        
        // When
        viewModel.testEmitSuccess(successMessage)
        
        // Then
        verify(successObserver).onChanged(successMessage)
    }
    
    @Test
    fun `setLoading should update loading state`() = testDispatcher.runBlockingTest {
        // When
        viewModel.testSetLoading(true)
        
        // Then
        verify(isLoadingObserver).onChanged(true)
        
        // When
        viewModel.testSetLoading(false)
        
        // Then
        verify(isLoadingObserver).onChanged(false)
    }
    
    @Test
    fun `Event getContentIfNotHandled should return content only once`() {
        // Given
        val content = "Test content"
        val event = BaseViewModel.Event(content)
        
        // When
        val firstResult = event.getContentIfNotHandled()
        val secondResult = event.getContentIfNotHandled()
        
        // Then
        assert(firstResult == content)
        assert(secondResult == null)
    }
    
    @Test
    fun `Event peekContent should always return content`() {
        // Given
        val content = "Test content"
        val event = BaseViewModel.Event(content)
        
        // When
        event.getContentIfNotHandled() // Mark as handled
        val result = event.peekContent()
        
        // Then
        assert(result == content)
    }
}
