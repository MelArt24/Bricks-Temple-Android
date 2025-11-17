package com.am24.brickstemple.data.remote

import com.am24.brickstemple.data.remote.dto.ProductDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class ProductApiService(
    private val client: HttpClient
) {
    private val BASE_URL = "https://bricks-temple-server.onrender.com/products"

    suspend fun getAll(): List<ProductDto> =
        client.get(BASE_URL).body()

    suspend fun getByType(type: String): List<ProductDto> =
        client.get(BASE_URL) {
            parameter("type", type)
        }.body()

    suspend fun getByCategory(category: String): List<ProductDto> =
        client.get(BASE_URL) {
            parameter("category", category)
        }.body<List<ProductDto>>()

    suspend fun search(query: String): List<ProductDto> =
        client.get(BASE_URL) {
            parameter("search", query)
        }.body()

    suspend fun getFiltered(
        type: String? = null,
        category: String? = null,
        search: String? = null,
        minPrice: String? = null,
        maxPrice: String? = null,
        year: String? = null
    ): List<ProductDto> =
        client.get(BASE_URL) {
            type?.let { parameter("type", it) }
            category?.let { parameter("category", it) }
            search?.let { parameter("search", it) }
            minPrice?.let { parameter("minPrice", it) }
            maxPrice?.let { parameter("maxPrice", it) }
            year?.let { parameter("year", it) }
        }.body()

    suspend fun getProductById(id: Int): ProductDto =
        client.get("$BASE_URL/$id").body()

    suspend fun getPaged(page: Int, limit: Int): List<ProductDto> =
        client.get(BASE_URL) {
            parameter("page", page)
            parameter("limit", limit)
        }.body()
}
