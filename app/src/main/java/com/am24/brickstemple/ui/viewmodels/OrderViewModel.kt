package com.am24.brickstemple.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.am24.brickstemple.data.remote.OrderApiService
import com.am24.brickstemple.data.remote.dto.ProductDto
import com.am24.brickstemple.data.repositories.OrderRepository
import com.am24.brickstemple.data.repositories.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OrderViewModel(
    private val repo: OrderRepository
) : ViewModel() {

    private val _orders = MutableStateFlow<List<OrderApiService.OrderResponse>>(emptyList())
    val orders = _orders.asStateFlow()

    private val _orderDetails = MutableStateFlow<OrderApiService.OrderWithItemsResponse?>(null)
    val orderDetails = _orderDetails.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _orderDetailsFull = MutableStateFlow<List<FullOrderItem>>(emptyList())
    val orderDetailsFull = _orderDetailsFull.asStateFlow()

    data class FullOrderItem(
        val item: OrderApiService.OrderItemResponse,
        val product: ProductDto?
    )

    fun loadOrderDetailsFull(orderId: Int, repo: ProductRepository) {
        viewModelScope.launch {
            val details = orderDetails.value ?: return@launch

            val result = mutableListOf<FullOrderItem>()

            for (item in details.items) {
                val product = repo.getLocalById(item.productId)
                result += FullOrderItem(item, product)
            }

            _orderDetailsFull.value = result
        }
    }


    fun loadOrders() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val resp = repo.getMyOrders()
                _orders.value = resp.data
            } finally {
                _loading.value = false
            }
        }
    }

    fun loadOrderDetails(id: Int, productRepo: ProductRepository) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val detail = repo.getOrderDetails(id)
                _orderDetails.value = detail

                loadOrderDetailsFull(id, productRepo)

            } finally {
                _loading.value = false
            }
        }
    }

    class Factory(
        private val repo: OrderRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OrderViewModel(repo) as T
        }
    }
}
