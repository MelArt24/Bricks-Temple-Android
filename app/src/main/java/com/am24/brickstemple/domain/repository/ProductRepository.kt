package com.am24.brickstemple.domain.repository

import com.am24.brickstemple.data.local.dao.ProductDao
import com.am24.brickstemple.data.remote.dto.ProductDto

interface ProductRepository {
    val productDao: ProductDao
    suspend fun getCachedByType(type: String): List<ProductDto>
    suspend fun searchLocal(query: String): List<ProductDto>
    suspend fun getLocalById(id: Int): ProductDto?
    suspend fun refreshAllTypesParallel(): List<ProductDto>
    suspend fun syncByType(type: String): List<ProductDto>
    suspend fun getById(id: Int): ProductDto
    suspend fun getFiltered(
        type: String? = null,
        category: String? = null,
        search: String? = null,
        minPrice: String? = null,
        maxPrice: String? = null,
        year: String? = null
    ): List<ProductDto>
}
