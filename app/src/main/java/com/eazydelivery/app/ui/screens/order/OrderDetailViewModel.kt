package com.eazydelivery.app.ui.screens.order

import androidx.lifecycle.viewModelScope
import com.eazydelivery.app.domain.model.Order
import com.eazydelivery.app.domain.repository.AnalyticsRepository
import com.eazydelivery.app.ui.base.BaseViewModel
import com.eazydelivery.app.util.ErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    errorHandler: ErrorHandler
) : BaseViewModel(errorHandler) {
    
    private val _uiState = MutableStateFlow(OrderDetailUiState())
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()
    
    fun loadOrder(orderId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                val result = analyticsRepository.getOrderById(orderId)
                
                result.fold(
                    onSuccess = { order ->
                        if (order != null) {
                            _uiState.update { 
                                it.copy(
                                    order = order,
                                    isLoading = false
                                ) 
                            }
                        } else {
                            _uiState.update { 
                                it.copy(
                                    error = "Order not found",
                                    isLoading = false
                                ) 
                            }
                        }
                    },
                    onFailure = { e ->
                        Timber.e(e, "Error loading order")
                        _uiState.update { 
                            it.copy(
                                error = e.message ?: "Failed to load order",
                                isLoading = false
                            ) 
                        }
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error loading order")
                _uiState.update { 
                    it.copy(
                        error = e.message ?: "An unexpected error occurred",
                        isLoading = false
                    ) 
                }
            }
        }
    }
    
    fun updateOrderStatus(orderId: String, isAccepted: Boolean) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                val result = analyticsRepository.updateOrderStatus(orderId, isAccepted)
                
                result.fold(
                    onSuccess = {
                        // Reload the order to get updated data
                        loadOrder(orderId)
                    },
                    onFailure = { e ->
                        Timber.e(e, "Error updating order status")
                        _uiState.update { 
                            it.copy(
                                error = e.message ?: "Failed to update order status",
                                isLoading = false
                            ) 
                        }
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error updating order status")
                _uiState.update { 
                    it.copy(
                        error = e.message ?: "An unexpected error occurred",
                        isLoading = false
                    ) 
                }
            }
        }
    }
    
    fun deleteOrder(orderId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                val result = analyticsRepository.deleteOrder(orderId)
                
                result.fold(
                    onSuccess = {
                        _uiState.update { 
                            it.copy(
                                orderDeleted = true,
                                isLoading = false
                            ) 
                        }
                    },
                    onFailure = { e ->
                        Timber.e(e, "Error deleting order")
                        _uiState.update { 
                            it.copy(
                                error = e.message ?: "Failed to delete order",
                                isLoading = false
                            ) 
                        }
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error deleting order")
                _uiState.update { 
                    it.copy(
                        error = e.message ?: "An unexpected error occurred",
                        isLoading = false
                    ) 
                }
            }
        }
    }
}

data class OrderDetailUiState(
    val order: Order? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val orderDeleted: Boolean = false
)
