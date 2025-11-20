package com.am24.brickstemple.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.am24.brickstemple.data.remote.dto.ProductDto
import com.am24.brickstemple.data.repositories.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProductDetailsUiState(
    val isLoading: Boolean = false,
    val product: ProductDto? = null,
    val error: String? = null
)

class ProductDetailsViewModel(
    private val productId: Int,
    private val repo: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductDetailsUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    init {
        loadProduct()
    }

    private fun loadProduct() {
        viewModelScope.launch {
            try {
                val result = repo.getById(productId)
                _uiState.value = ProductDetailsUiState(product = result)
            } catch (e: Exception) {
                _uiState.value = ProductDetailsUiState(error = e.message)
            }
        }
    }

    class Factory(
        private val id: Int,
        private val repo: ProductRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProductDetailsViewModel(id, repo) as T
        }
    }
}
