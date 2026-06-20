package com.am24.brickstemple.data.fakes

import com.am24.brickstemple.domain.model.Product
import com.am24.brickstemple.domain.repository.ProductRepository

class FakeProductRepository : ProductRepository {
    var searchLocalError: Exception? = null
    var getByIdError: Exception? = null
    var getFilteredError: Exception? = null

    override suspend fun getCachedByType(type: String): List<Product> = emptyList()
    override suspend fun getCachedByIds(ids: List<Int>): List<Product> = emptyList()
    override suspend fun searchLocal(query: String): List<Product> {
        searchLocalError?.let { throw it }
        return emptyList()
    }

    override suspend fun getLocalById(id: Int): Product? = null
    override suspend fun refreshAllTypesParallel(): List<Product> = emptyList()
    override suspend fun syncByType(type: String): List<Product> = emptyList()

    override suspend fun getFiltered(
        type: String?,
        category: String?,
        search: String?,
        minPrice: String?,
        maxPrice: String?,
        year: String?
    ): List<Product> {
        getFilteredError?.let { throw it }
        return emptyList()
    }

    var shouldThrow = false

    private val product = Product(
        id = 1,
        name = "Falcon",
        category = "Star Wars",
        type = "set",
        price = 799.00,
        year = "2023",
        image = "",
        description = ""
    )

    override suspend fun getById(id: Int): Product {
        getByIdError?.let { throw it }
        if (shouldThrow) throw RuntimeException("Error loading")

        return product
    }
}
