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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OrderViewModel(
    private val repo: OrderRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders = _orders.asStateFlow()

    private val _orderDetails = MutableStateFlow<OrderDetails?>(null)
    val orderDetails = _orderDetails.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _orderDetailsFull = MutableStateFlow<List<FullOrderItem>>(emptyList())
    val orderDetailsFull = _orderDetailsFull.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    data class FullOrderItem(
        val item: OrderItem,
        val product: Product?
    )

    fun loadOrderDetailsFull() {
        viewModelScope.launch {
            val details = orderDetails.value ?: return@launch

            val result = mutableListOf<FullOrderItem>()

            try {
                for (item in details.items) {
                    val product = productRepository.getLocalById(item.productId)
                    result += FullOrderItem(item, product)
                }

                _orderDetailsFull.value = result
            } catch (e: Exception) {
                _errorMessage.value = e.toUserMessage()
            }
        }
    }


    fun loadOrders() {
        viewModelScope.launch {
            _loading.value = true
            try {
                _errorMessage.value = null
                val resp = repo.getMyOrders()
                _orders.value = resp.data
            } catch (e: Exception) {
                _errorMessage.value = e.toUserMessage()
            } finally {
                _loading.value = false
            }
        }
    }

    fun loadOrderDetails(id: Int) {
        viewModelScope.launch {
            _loading.value = true
            try {
                _errorMessage.value = null
                val detail = repo.getOrderDetails(id)
                _orderDetails.value = detail

                loadOrderDetailsFull()

            } catch (e: Exception) {
                _errorMessage.value = e.toUserMessage()
            } finally {
                _loading.value = false
            }
        }
    }

    private fun Exception.toUserMessage(): String {
        if (this is CancellationException) throw this
        return (this as? AppException)?.error?.userMessage
            ?: message
            ?: "Unexpected error occurred."
    }
}
