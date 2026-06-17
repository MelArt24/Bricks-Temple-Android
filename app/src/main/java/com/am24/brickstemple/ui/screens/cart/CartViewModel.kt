package com.am24.brickstemple.ui.screens.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.am24.brickstemple.domain.model.Product
import com.am24.brickstemple.domain.repository.CartRepository
import com.am24.brickstemple.domain.repository.ProductRepository
import io.ktor.client.plugins.ClientRequestException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CartViewModel(
    private val repo: CartRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    val cart = repo.cart
    val isUpdating = repo.isUpdating
    val isClearing = repo.isClearing
    val isLoading = repo.isLoading
    val loaded = repo.isLoaded

    private val _updatingQuantity = MutableStateFlow<Int?>(null)
    val updatingQuantity = _updatingQuantity.asStateFlow()

    private val _checkoutInProgress = MutableStateFlow(false)
    val checkoutInProgress = _checkoutInProgress.asStateFlow()

    private val _checkoutResult = MutableStateFlow<Int?>(null)
    val checkoutResult = _checkoutResult.asStateFlow()

    private val _unauthorized = MutableStateFlow(false)
    val unauthorized = _unauthorized.asStateFlow()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products = _products.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                repo.refresh()
                val productIds = repo.cart.value.keys.toList()
                _products.value = productRepository.getCachedByIds(productIds)
            } catch (_: Exception) { }
        }
    }

    fun clearUnauthorized() {
        _unauthorized.value = false
    }

    fun checkout() {
        viewModelScope.launch {
            _checkoutInProgress.value = true

            try {
                val orderId = repo.checkout()
                _checkoutResult.value = orderId

            } catch (e: ClientRequestException) {
                if (e.response.status.value == 401) {
                    _unauthorized.value = true
                } else {
                    e.printStackTrace()
                }


            } catch (e: Exception) {
                e.printStackTrace()

            } finally {
                _checkoutInProgress.value = false
            }
        }
    }


    fun refresh() {
        viewModelScope.launch {
            repo.refresh()
            val productIds = repo.cart.value.keys.toList()
            _products.value = productRepository.getCachedByIds(productIds)
        }
    }

    fun loadProducts() {
        viewModelScope.launch {
            val productIds = repo.cart.value.keys.toList()
            _products.value = productRepository.getCachedByIds(productIds)
        }
    }

    fun toggle(productId: Int) {
        viewModelScope.launch {
            repo.toggle(productId)
        }
    }

    fun addProduct(productId: Int) {
        viewModelScope.launch {
            repo.add(productId)
        }
    }

    fun updateQuantity(productId: Int, delta: Int) {
        viewModelScope.launch {
            val currentQty = repo.cart.value[productId] ?: 0
            val newQty = currentQty + delta

            _updatingQuantity.value = productId

            try {
                when {
                    newQty <= 0 -> repo.removeCompletely(productId)
                    else -> repo.updateQuantity(productId, newQty)
                }
            } finally {
                _updatingQuantity.value = null
            }
        }
    }

    fun removeCompletely(productId: Int) {
        viewModelScope.launch {
            repo.removeCompletely(productId)
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            repo.clearCart()
        }
    }

    fun clearCheckoutResult() {
        _checkoutResult.value = null
    }


    fun reset() {
        viewModelScope.launch {
            repo.clearLocal()
            _updatingQuantity.value = null
        }
    }

    class Factory(
        private val repo: CartRepository,
        private val productRepository: ProductRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CartViewModel(repo, productRepository) as T
        }
    }
}
