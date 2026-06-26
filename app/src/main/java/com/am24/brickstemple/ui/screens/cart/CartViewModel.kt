package com.am24.brickstemple.ui.screens.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.am24.brickstemple.domain.error.AppError
import com.am24.brickstemple.domain.error.AppException
import com.am24.brickstemple.domain.model.Product
import com.am24.brickstemple.domain.repository.CartRepository
import com.am24.brickstemple.domain.repository.ProductRepository
import com.am24.brickstemple.domain.usecase.cart.UpdateCartQuantityUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CartUiState(
    val cart: Map<Int, Int> = emptyMap(),
    val products: List<Product> = emptyList(),
    val updatingIds: Set<Int> = emptySet(),
    val updatingQuantityProductId: Int? = null,
    val isClearing: Boolean = false,
    val isLoading: Boolean = false,
    val isLoaded: Boolean = false,
    val checkoutInProgress: Boolean = false,
    val checkoutResult: Int? = null,
    val unauthorized: Boolean = false,
    val errorMessage: String? = null
)

private data class CartRepositoryState(
    val cart: Map<Int, Int> = emptyMap(),
    val updatingIds: Set<Int> = emptySet(),
    val isClearing: Boolean = false,
    val isLoading: Boolean = false,
    val isLoaded: Boolean = false
)

class CartViewModel(
    private val cartRepository: CartRepository,
    private val productRepository: ProductRepository,
    private val updateCartQuantityUseCase: UpdateCartQuantityUseCase
) : ViewModel() {

    val cart = cartRepository.cart

    private val _cartUiState = MutableStateFlow(CartUiState())

    private val repositoryState = combine(
        cartRepository.cart,
        cartRepository.isUpdating,
        cartRepository.isClearing,
        cartRepository.isLoading,
        cartRepository.isLoaded
    ) { cart, updatingIds, isClearing, isLoading, isLoaded ->
        CartRepositoryState(
            cart = cart,
            updatingIds = updatingIds,
            isClearing = isClearing,
            isLoading = isLoading,
            isLoaded = isLoaded
        )
    }

    val uiState: StateFlow<CartUiState> = combine(
        repositoryState,
        _cartUiState
    ) { repositoryState, state ->
        state.copy(
            cart = repositoryState.cart,
            updatingIds = repositoryState.updatingIds,
            isClearing = repositoryState.isClearing,
            isLoading = repositoryState.isLoading,
            isLoaded = repositoryState.isLoaded
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CartUiState()
    )

    init {
        viewModelScope.launch {
            try {
                cartRepository.refresh()
                val productIds = cartRepository.cart.value.keys.toList()
                _cartUiState.update {
                    it.copy(products = productRepository.getCachedByIds(productIds))
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun clearUnauthorized() {
        _cartUiState.update { it.copy(unauthorized = false) }
    }

    fun clearError() {
        _cartUiState.update { it.copy(errorMessage = null) }
    }

    fun checkout() {
        viewModelScope.launch {
            _cartUiState.update {
                it.copy(checkoutInProgress = true, errorMessage = null)
            }

            try {
                val orderId = cartRepository.checkout()
                _cartUiState.update { it.copy(checkoutResult = orderId) }

            } catch (e: Exception) {
                handleError(e)
            } finally {
                _cartUiState.update { it.copy(checkoutInProgress = false) }
            }
        }
    }


    fun refresh() {
        viewModelScope.launch {
            try {
                cartRepository.refresh()
                val productIds = cartRepository.cart.value.keys.toList()
                _cartUiState.update {
                    it.copy(products = productRepository.getCachedByIds(productIds))
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun loadProducts() {
        viewModelScope.launch {
            try {
                val productIds = cartRepository.cart.value.keys.toList()
                _cartUiState.update {
                    it.copy(products = productRepository.getCachedByIds(productIds))
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun toggle(productId: Int) {
        viewModelScope.launch {
            try {
                cartRepository.toggle(productId)
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun addProduct(productId: Int) {
        viewModelScope.launch {
            try {
                cartRepository.add(productId)
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun updateQuantity(productId: Int, delta: Int) {
        viewModelScope.launch {
            _cartUiState.update { it.copy(updatingQuantityProductId = productId) }

            try {
                updateCartQuantityUseCase(productId, delta)
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _cartUiState.update { it.copy(updatingQuantityProductId = null) }
            }
        }
    }

    fun removeCompletely(productId: Int) {
        viewModelScope.launch {
            try {
                cartRepository.removeCompletely(productId)
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            try {
                cartRepository.clearCart()
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun clearCheckoutResult() {
        _cartUiState.update { it.copy(checkoutResult = null) }
    }


    fun reset() {
        viewModelScope.launch {
            cartRepository.clearLocal()
            _cartUiState.update {
                it.copy(
                    updatingQuantityProductId = null,
                    errorMessage = null
                )
            }
        }
    }

    private fun handleError(error: Exception) {
        if (error is CancellationException) throw error

        val appException = error as? AppException
        if (appException?.error is AppError.UnauthorizedError) {
            _cartUiState.update { it.copy(unauthorized = true) }
            return
        }

        _cartUiState.update {
            it.copy(
                errorMessage = appException?.error?.userMessage
                    ?: error.message
                    ?: "Unexpected error occurred."
            )
        }
    }
}
