package com.am24.brickstemple.ui.screens.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.am24.brickstemple.domain.model.Order
import com.am24.brickstemple.domain.model.OrderDetails
import com.am24.brickstemple.domain.model.OrderItem
import com.am24.brickstemple.domain.model.Product
import com.am24.brickstemple.domain.repository.OrderRepository
import com.am24.brickstemple.domain.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OrderViewModel(
    private val repo: OrderRepository
) : ViewModel() {

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders = _orders.asStateFlow()

    private val _orderDetails = MutableStateFlow<OrderDetails?>(null)
    val orderDetails = _orderDetails.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _orderDetailsFull = MutableStateFlow<List<FullOrderItem>>(emptyList())
    val orderDetailsFull = _orderDetailsFull.asStateFlow()

    data class FullOrderItem(
        val item: OrderItem,
        val product: Product?
    )

    fun loadOrderDetailsFull(productRepository: ProductRepository) {
        viewModelScope.launch {
            val details = orderDetails.value ?: return@launch

            val result = mutableListOf<FullOrderItem>()

            for (item in details.items) {
                val product = productRepository.getLocalById(item.productId)
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

    fun loadOrderDetails(id: Int, productRepository: ProductRepository) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val detail = repo.getOrderDetails(id)
                _orderDetails.value = detail

                loadOrderDetailsFull(productRepository)

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
