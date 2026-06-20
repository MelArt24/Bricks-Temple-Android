package com.am24.brickstemple.ui.screens.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.am24.brickstemple.domain.error.AppError
import com.am24.brickstemple.domain.error.AppException
import com.am24.brickstemple.domain.model.Product
import com.am24.brickstemple.domain.repository.CartRepository
import com.am24.brickstemple.domain.repository.ProductRepository
import kotlinx.coroutines.CancellationException
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

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products = _products.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                repo.refresh()
                val productIds = repo.cart.value.keys.toList()
                _products.value = productRepository.getCachedByIds(productIds)
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun clearUnauthorized() {
        _unauthorized.value = false
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun checkout() {
        viewModelScope.launch {
            _checkoutInProgress.value = true
            _errorMessage.value = null

            try {
                val orderId = repo.checkout()
                _checkoutResult.value = orderId

            } catch (e: Exception) {
                handleError(e)
            } finally {
                _checkoutInProgress.value = false
            }
        }
    }


    fun refresh() {
        viewModelScope.launch {
            try {
                repo.refresh()
                val productIds = repo.cart.value.keys.toList()
                _products.value = productRepository.getCachedByIds(productIds)
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun loadProducts() {
        viewModelScope.launch {
            try {
                val productIds = repo.cart.value.keys.toList()
                _products.value = productRepository.getCachedByIds(productIds)
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun toggle(productId: Int) {
        viewModelScope.launch {
            try {
                repo.toggle(productId)
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun addProduct(productId: Int) {
        viewModelScope.launch {
            try {
                repo.add(productId)
            } catch (e: Exception) {
                handleError(e)
            }
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
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _updatingQuantity.value = null
            }
        }
    }

    fun removeCompletely(productId: Int) {
        viewModelScope.launch {
            try {
                repo.removeCompletely(productId)
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            try {
                repo.clearCart()
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun clearCheckoutResult() {
        _checkoutResult.value = null
    }


    fun reset() {
        viewModelScope.launch {
            repo.clearLocal()
            _updatingQuantity.value = null
            _errorMessage.value = null
        }
    }

    private fun handleError(error: Exception) {
        if (error is CancellationException) throw error

        val appException = error as? AppException
        if (appException?.error is AppError.UnauthorizedError) {
            _unauthorized.value = true
            return
        }

        _errorMessage.value = appException?.error?.userMessage
            ?: error.message
            ?: "Unexpected error occurred."
    }
}
