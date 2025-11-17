package com.am24.brickstemple.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.am24.brickstemple.data.remote.dto.ProductDto
import com.am24.brickstemple.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProductUiState(
    val isLoading: Boolean = false,
    val products: List<ProductDto> = emptyList(),
    val error: String? = null,
)

class ProductViewModel(
    private val repo: ProductRepository
) : ViewModel() {

    private val _sections = MutableStateFlow<Map<String, ProductUiState>>(emptyMap())
    val sections = _sections.asStateFlow()

    init {
        loadType("set")
        loadType("minifigure")
        loadType("detail")
        loadType("polybag")
        loadType("other")
    }

    private fun load(type: String, block: suspend () -> List<ProductDto>) {
        viewModelScope.launch {

            _sections.value += (type to ProductUiState(isLoading = true))

            try {
                val result = block()
                _sections.value += (type to ProductUiState(products = result))

            } catch (e: Exception) {
                _sections.value += (type to ProductUiState(error = e.message))
            }
        }
    }

    fun loadType(type: String) = load(type) {
        repo.getByType(type)
    }

    fun loadCategory(category: String) = load("category:$category") {
        repo.getByCategory(category)
    }

    fun search(query: String) = load("search:$query") {
        repo.search(query)
    }

    fun loadFiltered(
        type: String? = null,
        category: String? = null,
        search: String? = null,
        minPrice: String? = null,
        maxPrice: String? = null,
        year: String? = null
    ) = load("filtered") {
        repo.getFiltered(type, category, search, minPrice, maxPrice, year)
    }

    fun loadPaged(page: Int, limit: Int) = load("paged:$page") {
        repo.getPaged(page, limit)
    }

    fun loadById(id: Int) = load("id:$id") {
        listOf(repo.getById(id))
    }

    class Factory(
        private val repository: ProductRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProductViewModel::class.java)) {
                return ProductViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
