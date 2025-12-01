package com.am24.brickstemple.data.fakes

import com.am24.brickstemple.data.mappers.toEntity
import com.am24.brickstemple.data.remote.ProductApiService
import com.am24.brickstemple.data.remote.dto.ProductDto
import com.am24.brickstemple.data.repositories.ProductRepository
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers

class FakeProductRepository : ProductRepository(
    api = FakeApiService(),
    dao = FakeProductDao(),
    dispatcher = Dispatchers.Unconfined
) {

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

        productDao.insert(product.toEntity())

        return product
    }
}

class FakeApiService : ProductApiService(HttpClient()) {

    override suspend fun getProductById(id: Int): ProductDto {
        error("Should not be called")
    }
}