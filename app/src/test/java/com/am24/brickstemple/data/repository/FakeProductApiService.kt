package com.am24.brickstemple.data.repository

import com.am24.brickstemple.data.remote.ProductApiService
import com.am24.brickstemple.data.remote.dto.ProductDto
import io.ktor.client.HttpClient

class FakeProductApiService(client: HttpClient) : ProductApiService(client) {

    private val products = listOf(
        ProductDto(
            id = 1,
            name = "Millennium Falcon",
            category = "Star Wars",
            type = "set",
            price = "799.99",
            year = "2023",
            image = "",
            description = ""
        ),
        ProductDto(
            id = 2,
            name = "TIE Fighter Pilot",
            category = "Star Wars",
            type = "minifigure",
            price = "19.99",
            year = "2020",
            image = "",
            description = ""
        ),
        ProductDto(
            id = 3,
            name = "Police Station",
            category = "City",
            type = "set",
            price = "199.99",
            year = "2021",
            image = "",
            description = ""
        )
    )

    override suspend fun getAll(): List<ProductDto> = products

    override suspend fun getByType(type: String): List<ProductDto> =
        products.filter { it.type == type }

    override suspend fun getByCategory(category: String): List<ProductDto> =
        products.filter { it.category == category }

    override suspend fun search(query: String): List<ProductDto> =
        products.filter { it.name.contains(query, ignoreCase = true) }

    override suspend fun getFiltered(
        type: String?,
        category: String?,
        search: String?,
        minPrice: String?,
        maxPrice: String?,
        year: String?
    ): List<ProductDto> =
        products.filter { p ->
            (type == null || p.type == type) &&
                    (category == null || p.category == category) &&
                    (search == null || p.name.contains(search, ignoreCase = true)) &&
                    (minPrice == null || p.price.toDouble() >= minPrice.toDouble()) &&
                    (maxPrice == null || p.price.toDouble() <= maxPrice.toDouble()) &&
                    (year == null || p.year.toString() == year)
        }

    override suspend fun getProductById(id: Int): ProductDto =
        products.first { it.id == id }

    override suspend fun getPaged(page: Int, limit: Int): List<ProductDto> {
        val from = (page - 1) * limit
        val to = minOf(from + limit, products.size)
        return products.subList(from, to)
    }
}
