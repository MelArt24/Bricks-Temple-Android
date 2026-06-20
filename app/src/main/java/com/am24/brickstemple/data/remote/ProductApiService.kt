package com.am24.brickstemple.data.remote

import com.am24.brickstemple.data.error.toAppException
import com.am24.brickstemple.data.error.toAppExceptionWithBody
import com.am24.brickstemple.data.remote.dto.ProductDto
import com.am24.brickstemple.data.remote.util.NetworkConstants
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.delay

open class ProductApiService(
    private val client: HttpClient
) {
    private val BASE_URL = NetworkConstants.PRODUCTS_URL

    private suspend inline fun <reified T> safeRequest(
        crossinline block: suspend () -> HttpResponse
    ): T {
        var lastError: Exception? = null

        repeat(3) { attempt ->
            try {
                val response = block()
                if (response.status.value !in 200..299) {
                    throw response.toAppExceptionWithBody("Failed to load products.")
                }
                return response.body()
            } catch (e: Exception) {
                lastError = e
                if (attempt == 2) {
                    throw e.toAppException("Failed to load products.")
                }
                delay(400L)
            }
        }

        throw lastError?.toAppException("Failed to load products.")
            ?: error("Product request failed without an exception.")
    }

    open suspend fun getAll(): List<ProductDto> =
        safeRequest {
            client.get(BASE_URL)
        }

    open suspend fun getByType(type: String): List<ProductDto> =
        safeRequest {
            client.get(BASE_URL) { parameter("type", type) }
        }

    open suspend fun getByCategory(category: String): List<ProductDto> =
        safeRequest {
            client.get(BASE_URL) { parameter("category", category) }
        }

    open suspend fun search(query: String): List<ProductDto> =
        safeRequest {
            client.get(BASE_URL) { parameter("search", query) }
        }

    open suspend fun getFiltered(
        type: String? = null,
        category: String? = null,
        search: String? = null,
        minPrice: String? = null,
        maxPrice: String? = null,
        year: String? = null
    ): List<ProductDto> =
        safeRequest {
            client.get(BASE_URL) {
                type?.let { parameter("type", it) }
                category?.let { parameter("category", it) }
                search?.let { parameter("search", it) }
                minPrice?.let { parameter("minPrice", it) }
                maxPrice?.let { parameter("maxPrice", it) }
                year?.let { parameter("year", it) }
            }
        }

    open suspend fun getProductById(id: Int): ProductDto =
        safeRequest {
            client.get("$BASE_URL/$id")
        }

    open suspend fun getPaged(page: Int, limit: Int): List<ProductDto> =
        safeRequest {
            client.get(BASE_URL) {
                parameter("page", page)
                parameter("limit", limit)
            }
        }
}
