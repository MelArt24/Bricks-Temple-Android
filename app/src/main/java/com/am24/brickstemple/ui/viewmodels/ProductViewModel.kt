package com.am24.brickstemple.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.am24.brickstemple.data.remote.dto.ProductDto
import com.am24.brickstemple.data.repositories.ProductRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProductUiState(
    val isLoading: Boolean = false,
    val products: List<ProductDto> = emptyList(),
    val error: String? = null
)

data class FilterState(
    val minPrice: String? = null,
    val maxPrice: String? = null,
    val year: String? = null
)

enum class SortOrder {
    PRICE_ASC,
    PRICE_DESC,
    YEAR_ASC,
    YEAR_DESC,
    NONE
}


class ProductViewModel(
    val repo: ProductRepository
) : ViewModel() {

    private val _sets = MutableStateFlow(ProductUiState())
    val sets = _sets.asStateFlow()

    private val _minifigs = MutableStateFlow(ProductUiState())
    val minifigs = _minifigs.asStateFlow()

    private val _details = MutableStateFlow(ProductUiState())
    val details = _details.asStateFlow()

    private val _polybags = MutableStateFlow(ProductUiState())
    val polybags = _polybags.asStateFlow()

    private val _others = MutableStateFlow(ProductUiState())
    val others = _others.asStateFlow()

    private val _searchResult = MutableStateFlow(ProductUiState())
    val searchResult = _searchResult.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    private val _filters = MutableStateFlow(FilterState())
    val filters = _filters.asStateFlow()

    private val _filteredProducts = MutableStateFlow(ProductUiState())
    val filteredProducts = _filteredProducts.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.NONE)
    val sortOrder = _sortOrder.asStateFlow()


    init {
        viewModelScope.launch {
            _loading.value = true

            loadLocalCache()

            try {
                repo.refreshAllTypesParallel()
            } catch (_: Exception) {

            } finally {
                loadLocalCache()
                _loading.value = false
            }
        }
    }



    private suspend fun loadLocalCache() {
        _sets.value = ProductUiState(products = repo.getCachedByType("set"))
        _minifigs.value = ProductUiState(products = repo.getCachedByType("minifigure"))
        _details.value = ProductUiState(products = repo.getCachedByType("detail"))
        _polybags.value = ProductUiState(products = repo.getCachedByType("polybag"))
        _others.value = ProductUiState(products = repo.getCachedByType("other"))
    }

    fun search(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _searchResult.value = ProductUiState(products = emptyList())
                return@launch
            }

            _searchResult.value = ProductUiState(isLoading = true)

            try {
                val result = repo.searchLocal(query)
                _searchResult.value = ProductUiState(products = result)
            } catch (e: Exception) {
                _searchResult.value = ProductUiState(error = e.message)
            }
        }
    }

    private val _productById = MutableStateFlow(ProductUiState())
    val productById = _productById.asStateFlow()

    fun loadById(id: Int) {
        viewModelScope.launch {
            _productById.value = ProductUiState(isLoading = true)

            try {
                val local = repo.getLocalById(id)
                if (local != null)
                    _productById.value = ProductUiState(products = listOf(local))

                val updated = repo.getById(id)
                _productById.value = ProductUiState(products = listOf(updated))

            } catch (e: Exception) {
                _productById.value = ProductUiState(error = e.message)
            }
        }
    }

    private fun applySorting(list: List<ProductDto>, order: SortOrder): List<ProductDto> {
        return when (order) {
            SortOrder.PRICE_ASC -> list.sortedBy { it.price }
            SortOrder.PRICE_DESC -> list.sortedByDescending { it.price }
            SortOrder.YEAR_ASC -> list.sortedBy { it.year }
            SortOrder.YEAR_DESC -> list.sortedByDescending { it.year }
            SortOrder.NONE -> list
        }
    }

    fun setSort(order: SortOrder) {
        _sortOrder.value = order

        val f = _filters.value
        val type = fTypeFromFilters()

        if (f.minPrice != null || f.maxPrice != null || f.year != null) {
            applyFilters(type, f.minPrice, f.maxPrice, f.year)
            return
        }

        val list = when (type) {
            "set" -> sets.value.products
            "minifigure" -> minifigs.value.products
            "detail" -> details.value.products
            "polybag" -> polybags.value.products
            else -> others.value.products
        }

        val sorted = applySorting(list, order)
        _filteredProducts.value = ProductUiState(products = sorted)
    }

    private fun fTypeFromFilters(): String {
        return when (_filters.value) {
            else -> ""
        }
    }

    fun applyFilters(
        type: String,
        minPrice: String?,
        maxPrice: String?,
        year: String?
    ) {
        viewModelScope.launch {
            _filteredProducts.value = ProductUiState(isLoading = true)

            _filters.value = FilterState(minPrice, maxPrice, year)

            try {
                val result = repo.api.getFiltered(
                    type = type,
                    minPrice = minPrice,
                    maxPrice = maxPrice,
                    year = year
                )
                val sorted = applySorting(result, _sortOrder.value)

                _filteredProducts.value = ProductUiState(products = sorted)
            } catch (e: Exception) {
                _filteredProducts.value = ProductUiState(error = e.message)
            }
        }
    }


    class Factory(
        private val repo: ProductRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProductViewModel::class.java)) {
                return ProductViewModel(repo) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
