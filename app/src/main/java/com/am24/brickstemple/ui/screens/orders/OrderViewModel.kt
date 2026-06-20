package com.am24.brickstemple.ui.screens.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.am24.brickstemple.domain.model.Order
import com.am24.brickstemple.domain.model.OrderDetails
import com.am24.brickstemple.domain.model.OrderItem
import com.am24.brickstemple.domain.model.Product
import com.am24.brickstemple.domain.error.AppException
import com.am24.brickstemple.domain.repository.OrderRepository
import com.am24.brickstemple.domain.repository.ProductRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrderHistoryUiState(
    val isLoading: Boolean = false,
    val orders: List<Order> = emptyList(),
    val errorMessage: String? = null
)

data class OrderDetailsUiState(
    val isLoading: Boolean = false,
    val details: OrderDetails? = null,
    val fullItems: List<OrderViewModel.FullOrderItem> = emptyList(),
    val errorMessage: String? = null
)

class OrderViewModel(
    private val repo: OrderRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _historyState = MutableStateFlow(OrderHistoryUiState())
    val historyState: StateFlow<OrderHistoryUiState> = _historyState.asStateFlow()

    private val _detailsState = MutableStateFlow(OrderDetailsUiState())
    val detailsState: StateFlow<OrderDetailsUiState> = _detailsState.asStateFlow()

    data class FullOrderItem(
        val item: OrderItem,
        val product: Product?
    )

    fun loadOrderDetailsFull() {
        viewModelScope.launch {
            val details = detailsState.value.details ?: return@launch

            try {
                _detailsState.update { it.copy(fullItems = details.toFullItems()) }
            } catch (e: Exception) {
                _detailsState.update { it.copy(errorMessage = e.toUserMessage()) }
            }
        }
    }


    fun loadOrders() {
        viewModelScope.launch {
            _historyState.update { it.copy(isLoading = true) }
            try {
                _historyState.update { it.copy(errorMessage = null) }
                val resp = repo.getMyOrders()
                _historyState.update { it.copy(orders = resp.data) }
            } catch (e: Exception) {
                _historyState.update { it.copy(errorMessage = e.toUserMessage()) }
            } finally {
                _historyState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun loadOrderDetails(id: Int) {
        viewModelScope.launch {
            _detailsState.update { it.copy(isLoading = true) }
            try {
                _detailsState.update { it.copy(errorMessage = null) }
                val detail = repo.getOrderDetails(id)
                _detailsState.update { it.copy(details = detail) }
                _detailsState.update { it.copy(fullItems = detail.toFullItems()) }

            } catch (e: Exception) {
                _detailsState.update { it.copy(errorMessage = e.toUserMessage()) }
            } finally {
                _detailsState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun OrderDetails.toFullItems(): List<FullOrderItem> {
        return items.map { item ->
            FullOrderItem(
                item = item,
                product = productRepository.getLocalById(item.productId)
            )
        }
    }

    private fun Exception.toUserMessage(): String {
        if (this is CancellationException) throw this
        return (this as? AppException)?.error?.userMessage
            ?: message
            ?: "Unexpected error occurred."
    }
}
