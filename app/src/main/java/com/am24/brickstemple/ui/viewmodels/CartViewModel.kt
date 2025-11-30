package com.am24.brickstemple.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.am24.brickstemple.data.repositories.CartRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CartViewModel(
    private val repo: CartRepository
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

    init {
        viewModelScope.launch {
            repo.refresh()
        }
    }

    fun checkout() {
        viewModelScope.launch {
            _checkoutInProgress.value = true

            try {
                val orderId = repo.checkout()
                _checkoutResult.value = orderId
            } finally {
                _checkoutInProgress.value = false
            }
        }
    }


    fun refresh() {
        viewModelScope.launch {
            repo.refresh()
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
        private val repo: CartRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CartViewModel(repo) as T
        }
    }
}
