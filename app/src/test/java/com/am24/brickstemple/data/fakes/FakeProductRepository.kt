package com.am24.brickstemple.data.fakes

import com.am24.brickstemple.data.mapper.toEntity
import com.am24.brickstemple.data.remote.ProductApiService
import com.am24.brickstemple.data.local.dao.ProductDao
import com.am24.brickstemple.data.remote.dto.ProductDto
import com.am24.brickstemple.domain.repository.ProductRepository
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers

class FakeProductRepository : ProductRepository {
    override val productDao: ProductDao
        get() = FakeProductDao()

    override suspend fun getCachedByType(type: String): List<ProductDto> = emptyList()
    override suspend fun searchLocal(query: String): List<ProductDto> = emptyList()
    override suspend fun getLocalById(id: Int): ProductDto? = null
    override suspend fun refreshAllTypesParallel(): List<ProductDto> = emptyList()
    override suspend fun syncByType(type: String): List<ProductDto> = emptyList()

    override suspend fun getFiltered(
        type: String?,
        category: String?,
        search: String?,
        minPrice: String?,
        maxPrice: String?,
        year: String?
    ): List<ProductDto> = emptyList()

    var shouldThrow = false

    private val product = ProductDto(
        id = 1,
        name = "Falcon",
        category = "Star Wars",
        type = "set",
        price = 799.00,
        year = "2023",
        image = "",
        description = ""
    )

    override suspend fun getById(id: Int): ProductDto {
        if (shouldThrow) throw RuntimeException("Error loading")

        return product
    }
}

class FakeApiService : ProductApiService(HttpClient()) {

    override suspend fun getProductById(id: Int): ProductDto {
        error("Should not be called")
    }
}