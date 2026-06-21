package com.am24.brickstemple.ui.screens.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.am24.brickstemple.domain.error.AppException
import com.am24.brickstemple.domain.model.Product
import com.am24.brickstemple.domain.repository.ProductRepository
import com.am24.brickstemple.domain.repository.WishlistRepository
import kotlinx.coroutines.CancellationException
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

    private val _updatingQuantityIds = MutableStateFlow<Set<Int>>(emptySet())
    val updatingQuantityIds = _updatingQuantityIds.asStateFlow()

    private val _removingProductIds = MutableStateFlow<Set<Int>>(emptySet())
    val removingProductIds = _removingProductIds.asStateFlow()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products = _products.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            try {
                _errorMessage.value = null
                repo.refresh()
                loadProductsForCurrentWishlist()
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun loadProducts() {
        viewModelScope.launch {
            try {
                loadProductsForCurrentWishlist()
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


    fun updateQuantity(productId: Int, delta: Int) {
        viewModelScope.launch {
            if (productId in _updatingQuantityIds.value) return@launch

            _updatingQuantityIds.value += productId

            try {
                var item = repo.lastFetchedItem(productId)
                if (item == null) {
                    repo.refresh()
                    loadProductsForCurrentWishlist()
                    item = repo.lastFetchedItem(productId)
                }

                if (item == null) {
                    _errorMessage.value = "Failed to update wishlist item."
                    return@launch
                }

                val newQty = item.quantity + delta

                when {
                    newQty <= 0 -> repo.removeCompletely(productId)
                    delta == -1 -> repo.removeOne(productId)
                    else -> repo.updateQuantity(item.id!!, newQty)
                }
                loadProductsForCurrentWishlist()
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _updatingQuantityIds.value -= productId
            }
        }
    }

    fun removeCompletely(productId: Int) {
        viewModelScope.launch {
            _removingProductIds.value += productId

            try {
                repo.removeCompletely(productId)
                loadProductsForCurrentWishlist()
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _removingProductIds.value -= productId
            }
        }
    }

    fun reset() {
        viewModelScope.launch {
            repo.clearLocal()
            _updatingQuantityIds.value = emptySet()
            _removingProductIds.value = emptySet()
            _errorMessage.value = null
        }
    }


    fun clearWishlist() {
        viewModelScope.launch {
            try {
                _errorMessage.value = null
                repo.clearWishlist()
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private suspend fun loadProductsForCurrentWishlist() {
        val productIds = repo.wishlist.value.keys.toList()
        _products.value = productRepository.getCachedByIds(productIds)
    }

    private fun handleError(error: Exception) {
        if (error is CancellationException) throw error
        _errorMessage.value = (error as? AppException)?.error?.userMessage
            ?: error.message
            ?: "Unexpected error occurred."
    }
}
