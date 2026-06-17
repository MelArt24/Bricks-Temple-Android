package com.am24.brickstemple.domain.repository

import com.am24.brickstemple.domain.model.Product

interface ProductRepository {
    suspend fun getCachedByType(type: String): List<Product>
    suspend fun getCachedByIds(ids: List<Int>): List<Product>
    suspend fun searchLocal(query: String): List<Product>
    suspend fun getLocalById(id: Int): Product?
    suspend fun refreshAllTypesParallel(): List<Product>
    suspend fun syncByType(type: String): List<Product>
    suspend fun getById(id: Int): Product
    suspend fun getFiltered(
        type: String? = null,
        category: String? = null,
        search: String? = null,
        minPrice: String? = null,
        maxPrice: String? = null,
        year: String? = null
    ): List<Product>
}
