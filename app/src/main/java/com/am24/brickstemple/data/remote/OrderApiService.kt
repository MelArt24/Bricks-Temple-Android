package com.am24.brickstemple.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

class OrderApiService(
    private val client: HttpClient
) {

    private val BASE_URL = "https://bricks-temple-server.onrender.com/orders"

    @Serializable
    data class CreateOrderItemRequest(
        val productId: Int,
        val quantity: Int
    )

    @Serializable
    data class CreateOrderRequest(
        val items: List<CreateOrderItemRequest>,
        val totalPrice: Double
    )

    @Serializable
    data class CreatedOrderResponse(
        val message: String,
        val id: Int
    )

    suspend fun checkout(
        items: List<CreateOrderItemRequest>,
        totalPrice: Double
    ): CreatedOrderResponse {

        val response = client.post(BASE_URL) {
            contentType(ContentType.Application.Json)
            setBody(CreateOrderRequest(items, totalPrice))
        }

        return response.body()
    }
}
