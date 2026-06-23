package com.am24.brickstemple.ui.screens.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.am24.brickstemple.domain.error.AppException
import com.am24.brickstemple.domain.model.Product
import com.am24.brickstemple.domain.repository.ProductRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProductResultUiState(
    val isLoading: Boolean = false,
    val products: List<Product> = emptyList(),
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

data class ProductUiState(
    val sets: ProductResultUiState = ProductResultUiState(),
    val minifigs: ProductResultUiState = ProductResultUiState(),
    val details: ProductResultUiState = ProductResultUiState(),
    val polybags: ProductResultUiState = ProductResultUiState(),
    val others: ProductResultUiState = ProductResultUiState(),
    val searchResult: ProductResultUiState = ProductResultUiState(),
    val productById: ProductResultUiState = ProductResultUiState(),
    val filteredProducts: ProductResultUiState = ProductResultUiState(),
    val filters: FilterState = FilterState(),
    val sortOrder: SortOrder = SortOrder.NONE,
    val searchQuery: String = "",
    val isLoading: Boolean = true
)


class ProductViewModel(
    private val repo: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductUiState())
    val uiState: StateFlow<ProductUiState> = _uiState.asStateFlow()

    val searchQuery: StateFlow<String> = _uiState
        .map { it.searchQuery }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = _uiState.value.searchQuery
        )

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            loadLocalCacheSafely()

            try {
                repo.refreshAllTypesParallel()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            } finally {
                loadLocalCacheSafely()
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun matchesQuery(product: Product, query: String): Boolean {
        if (query.isBlank()) return true

        val q = query.lowercase()

        return product.name.lowercase().contains(q)
                || (product.description ?: "").lowercase().contains(q)
                || (product.keywords ?: "").lowercase().contains(q)
                || product.number?.contains(q) == true
    }

    private suspend fun loadLocalCacheSafely() {
        loadCategorySafely("set")
        loadCategorySafely("minifigure")
        loadCategorySafely("detail")
        loadCategorySafely("polybag")
        loadCategorySafely("other")
    }

    private suspend fun loadCategorySafely(type: String) {
        try {
            setCategoryState(type, ProductResultUiState(products = repo.getCachedByType(type)))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val current = categoryState(type)
            setCategoryState(
                type,
                current.copy(
                    isLoading = false,
                    error = e.toUserMessage()
                )
            )
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _uiState.update {
                    it.copy(searchResult = ProductResultUiState(products = emptyList()))
                }
                return@launch
            }

            _uiState.update {
                it.copy(searchResult = ProductResultUiState(isLoading = true))
            }

            try {
                val result = repo.searchLocal(query)
                _uiState.update {
                    it.copy(searchResult = ProductResultUiState(products = result))
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(searchResult = ProductResultUiState(error = e.toUserMessage()))
                }
            }
        }
    }

    fun loadById(id: Int) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(productById = ProductResultUiState(isLoading = true))
            }

            try {
                val local = repo.getLocalById(id)
                if (local != null)
                    _uiState.update {
                        it.copy(productById = ProductResultUiState(products = listOf(local)))
                    }

                val updated = repo.getById(id)
                _uiState.update {
                    it.copy(productById = ProductResultUiState(products = listOf(updated)))
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(productById = ProductResultUiState(error = e.toUserMessage()))
                }
            }
        }
    }

    private fun applySorting(list: List<Product>, order: SortOrder): List<Product> {
        return when (order) {
            SortOrder.PRICE_ASC -> list.sortedBy { it.price }
            SortOrder.PRICE_DESC -> list.sortedByDescending { it.price }
            SortOrder.YEAR_ASC -> list.sortedBy { it.year }
            SortOrder.YEAR_DESC -> list.sortedByDescending { it.year }
            SortOrder.NONE -> list
        }
    }

    fun setSort(order: SortOrder) {
        _uiState.update { it.copy(sortOrder = order) }

        val state = _uiState.value
        val f = state.filters
        val type = fTypeFromFilters()

        if (f.minPrice != null || f.maxPrice != null || f.year != null) {
            applyFilters(type, f.minPrice, f.maxPrice, f.year)
            return
        }

        val list = when (type) {
            "set" -> state.sets.products
            "minifigure" -> state.minifigs.products
            "detail" -> state.details.products
            "polybag" -> state.polybags.products
            else -> state.others.products
        }

        val sorted = applySorting(list, order)
        _uiState.update {
            it.copy(filteredProducts = ProductResultUiState(products = sorted))
        }
    }

    private fun fTypeFromFilters(): String {
        return when (_uiState.value.filters) {
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
            _uiState.update {
                it.copy(filteredProducts = ProductResultUiState(isLoading = true))
            }

            _uiState.update {
                it.copy(filters = FilterState(minPrice, maxPrice, year))
            }

            try {
                val result = repo.getFiltered(
                    type = type,
                    minPrice = minPrice,
                    maxPrice = maxPrice,
                    year = year
                )
                val sorted = applySorting(result, _uiState.value.sortOrder)

                _uiState.update {
                    it.copy(filteredProducts = ProductResultUiState(products = sorted))
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(filteredProducts = ProductResultUiState(error = e.toUserMessage()))
                }
            }
        }
    }

    private fun categoryState(type: String): ProductResultUiState {
        return when (type) {
            "set" -> _uiState.value.sets
            "minifigure" -> _uiState.value.minifigs
            "detail" -> _uiState.value.details
            "polybag" -> _uiState.value.polybags
            else -> _uiState.value.others
        }
    }

    private fun setCategoryState(type: String, state: ProductResultUiState) {
        _uiState.update {
            when (type) {
                "set" -> it.copy(sets = state)
                "minifigure" -> it.copy(minifigs = state)
                "detail" -> it.copy(details = state)
                "polybag" -> it.copy(polybags = state)
                else -> it.copy(others = state)
            }
        }
    }

    private fun Exception.toUserMessage(): String {
        if (this is CancellationException) throw this
        return (this as? AppException)?.error?.userMessage
            ?: "Unexpected error occurred."
    }
}
