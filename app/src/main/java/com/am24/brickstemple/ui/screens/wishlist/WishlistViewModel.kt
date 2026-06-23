package com.am24.brickstemple.ui.screens.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.am24.brickstemple.domain.error.AppException
import com.am24.brickstemple.domain.model.Product
import com.am24.brickstemple.domain.model.WishlistItem
import com.am24.brickstemple.domain.repository.ProductRepository
import com.am24.brickstemple.domain.repository.WishlistRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WishlistUiState(
    val wishlist: Map<Int, Int> = emptyMap(),
    val items: List<WishlistItem> = emptyList(),
    val products: List<Product> = emptyList(),
    val updatingIds: Set<Int> = emptySet(),
    val updatingQuantityIds: Set<Int> = emptySet(),
    val removingProductIds: Set<Int> = emptySet(),
    val isClearing: Boolean = false,
    val isLoading: Boolean = false,
    val isLoaded: Boolean = false,
    val errorMessage: String? = null
)

class WishlistViewModel(
    private val repo: WishlistRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    val wishlist = repo.wishlist
    val isUpdating = repo.isUpdating
    val isLoading = repo.isLoading

    private val _uiState = MutableStateFlow(WishlistUiState())

    val uiState: StateFlow<WishlistUiState> = combine(
        repo.wishlist,
        repo.items,
        repo.isUpdating,
        repo.isClearing,
        repo.isLoading,
        repo.isLoaded,
        _uiState
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val wishlist = values[0] as Map<Int, Int>
        @Suppress("UNCHECKED_CAST")
        val items = values[1] as List<WishlistItem>
        @Suppress("UNCHECKED_CAST")
        val updatingIds = values[2] as Set<Int>
        val isClearing = values[3] as Boolean
        val isLoading = values[4] as Boolean
        val isLoaded = values[5] as Boolean
        val state = values[6] as WishlistUiState

        state.copy(
            wishlist = wishlist,
            items = items,
            updatingIds = updatingIds,
            isClearing = isClearing,
            isLoading = isLoading,
            isLoaded = isLoaded
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WishlistUiState()
    )

    fun refresh() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(errorMessage = null) }
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
            if (productId in _uiState.value.updatingQuantityIds) return@launch

            _uiState.update {
                it.copy(updatingQuantityIds = it.updatingQuantityIds + productId)
            }

            try {
                var item = repo.lastFetchedItem(productId)
                if (item == null) {
                    repo.refresh()
                    loadProductsForCurrentWishlist()
                    item = repo.lastFetchedItem(productId)
                }

                if (item == null) {
                    _uiState.update { it.copy(errorMessage = "Failed to update wishlist item.") }
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
                _uiState.update {
                    it.copy(updatingQuantityIds = it.updatingQuantityIds - productId)
                }
            }
        }
    }

    fun removeCompletely(productId: Int) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(removingProductIds = it.removingProductIds + productId)
            }

            try {
                repo.removeCompletely(productId)
                loadProductsForCurrentWishlist()
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _uiState.update {
                    it.copy(removingProductIds = it.removingProductIds - productId)
                }
            }
        }
    }

    fun reset() {
        viewModelScope.launch {
            repo.clearLocal()
            _uiState.update {
                it.copy(
                    updatingQuantityIds = emptySet(),
                    removingProductIds = emptySet(),
                    errorMessage = null
                )
            }
        }
    }


    fun clearWishlist() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(errorMessage = null) }
                repo.clearWishlist()
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private suspend fun loadProductsForCurrentWishlist() {
        val productIds = repo.wishlist.value.keys.toList()
        _uiState.update {
            it.copy(products = productRepository.getCachedByIds(productIds))
        }
    }

    private fun handleError(error: Exception) {
        if (error is CancellationException) throw error
        _uiState.update {
            it.copy(
                errorMessage = (error as? AppException)?.error?.userMessage
                    ?: error.message
                    ?: "Unexpected error occurred."
            )
        }
    }
}
