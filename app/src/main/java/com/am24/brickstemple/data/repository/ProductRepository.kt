package com.am24.brickstemple.data.repository

import com.am24.brickstemple.data.remote.ProductApiService
import com.am24.brickstemple.data.remote.dto.ProductDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProductRepository(
    private val api: ProductApiService
) {

    suspend fun getAll(): List<ProductDto> =
        withContext(Dispatchers.IO) {
            api.getAll()
        }

    suspend fun getByType(type: String): List<ProductDto> =
        withContext(Dispatchers.IO) {
            api.getByType(type)
        }

    suspend fun getByCategory(category: String): List<ProductDto> =
        withContext(Dispatchers.IO) {
            api.getByCategory(category)
        }

    suspend fun search(query: String): List<ProductDto> =
        withContext(Dispatchers.IO) {
            api.search(query)
        }

    suspend fun getFiltered(
        type: String? = null,
        category: String? = null,
        search: String? = null,
        minPrice: String? = null,
        maxPrice: String? = null,
        year: String? = null
    ): List<ProductDto> =
        withContext(Dispatchers.IO) {
            api.getFiltered(type, category, search, minPrice, maxPrice, year)
        }

    suspend fun getById(id: Int): ProductDto =
        withContext(Dispatchers.IO) {
            api.getProductById(id)
        }

    suspend fun getPaged(page: Int, limit: Int): List<ProductDto> =
        withContext(Dispatchers.IO) {
            api.getPaged(page, limit)
        }
}
