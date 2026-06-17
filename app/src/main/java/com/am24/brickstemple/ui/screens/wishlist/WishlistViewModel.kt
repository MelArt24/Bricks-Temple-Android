package com.am24.brickstemple.ui.screens.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.am24.brickstemple.domain.model.Product
import com.am24.brickstemple.domain.repository.ProductRepository
import com.am24.brickstemple.domain.repository.WishlistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WishlistViewModel(
    private val repo: WishlistRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    val wishlist = repo.wishlist
    val items = repo.items
    val isUpdating = repo.isUpdating
    val isClearing = repo.isClearing
    val isLoading = repo.isLoading
    val loaded = repo.isLoaded

    private val _updatingQuantity = MutableStateFlow<Int?>(null)
    val updatingQuantity = _updatingQuantity.asStateFlow()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products = _products.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            repo.refresh()
            loadProducts()
        }
    }

    fun loadProducts() {
        viewModelScope.launch {
            val productIds = repo.wishlist.value.keys.toList()
            _products.value = productRepository.getCachedByIds(productIds)
        }
    }

    fun toggle(productId: Int) {
        viewModelScope.launch {
            repo.toggle(productId)
        }
    }


    fun updateQuantity(productId: Int, delta: Int) {
        viewModelScope.launch {
            val item = repo.lastFetchedItem(productId) ?: return@launch
            val newQty = item.quantity + delta

            _updatingQuantity.value = productId

            try {
                when {
                    newQty <= 0 -> repo.removeCompletely(productId)
                    delta == -1 -> repo.removeOne(productId)
                    else -> repo.updateQuantity(item.id!!, newQty)
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

    fun reset() {
        viewModelScope.launch {
            repo.clearLocal()
            _updatingQuantity.value = null
        }
    }


    fun clearWishlist() {
        viewModelScope.launch {
            repo.clearWishlist()
        }
    }

    class Factory(
        private val repo: WishlistRepository,
        private val productRepository: ProductRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WishlistViewModel(repo, productRepository) as T
        }
    }
}
