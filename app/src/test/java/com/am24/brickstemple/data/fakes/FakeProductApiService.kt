package com.am24.brickstemple.data.fakes

import com.am24.brickstemple.data.remote.ProductApiService
import com.am24.brickstemple.data.remote.dto.ProductDto
import io.ktor.client.HttpClient

class FakeProductApiService(client: HttpClient) : ProductApiService(client) {

    val products = mutableListOf(
        ProductDto(
            id = 1,
            name = "Millennium Falcon",
            category = "Star Wars",
            type = "set",
            price = 799.99,
            year = "2023",
            image = "",
            description = ""
        ),
        ProductDto(
            id = 2,
            name = "TIE Fighter Pilot",
            category = "Star Wars",
            type = "minifigure",
            price = 19.99,
            year = "2020",
            image = "",
            description = ""
        ),
        ProductDto(
            id = 3,
            name = "Police Station",
            category = "City",
            type = "set",
            price = 199.99,
            year = "2021",
            image = "",
            description = ""
        )
    )

    override suspend fun getByType(type: String): List<ProductDto> =
        products.filter { it.type == type }

    override suspend fun getByCategory(category: String): List<ProductDto> =
        products.filter { it.category == category }

    override suspend fun getProductById(id: Int): ProductDto =
        products.first { it.id == id }


}