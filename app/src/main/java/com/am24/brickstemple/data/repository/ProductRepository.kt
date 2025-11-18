package com.am24.brickstemple.data.repository

import com.am24.brickstemple.data.remote.ProductApiService
import com.am24.brickstemple.data.remote.dto.ProductDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProductRepository(
    private val api: ProductApiService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun getAll(): List<ProductDto> =
        withContext(dispatcher) { api.getAll() }

    suspend fun getByType(type: String): List<ProductDto> =
        withContext(dispatcher) { api.getByType(type) }

    suspend fun getByCategory(category: String): List<ProductDto> =
        withContext(dispatcher) { api.getByCategory(category) }

    suspend fun search(query: String): List<ProductDto> =
        withContext(dispatcher) { api.search(query) }

    suspend fun getFiltered(
        type: String? = null,
        category: String? = null,
        search: String? = null,
        minPrice: String? = null,
        maxPrice: String? = null,
        year: String? = null
    ): List<ProductDto> =
        withContext(dispatcher) {
            api.getFiltered(type, category, search, minPrice, maxPrice, year)
        }

    suspend fun getById(id: Int): ProductDto =
        withContext(dispatcher) { api.getProductById(id) }

    suspend fun getPaged(page: Int, limit: Int): List<ProductDto> =
        withContext(dispatcher) { api.getPaged(page, limit) }
}
