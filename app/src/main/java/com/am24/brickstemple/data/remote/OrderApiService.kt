package com.am24.brickstemple.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
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

    @Serializable
    data class PagedResponse<T>(
        val page: Int,
        val limit: Int,
        val total: Long,
        val data: List<T>
    )

    @Serializable
    data class OrderResponse(
        val id: Int,
        val userId: Int,
        val status: String,
        val totalPrice: Double,
        val createdAt: String
    )

    @Serializable
    data class OrderItemResponse(
        val id: Int,
        val orderId: Int,
        val productId: Int,
        val quantity: Int,
        val priceAtPurchase: Double
    )

    @Serializable
    data class OrderWithItemsResponse(
        val order: OrderResponse,
        val items: List<OrderItemResponse>
    )

    suspend fun getMyOrders(): PagedResponse<OrderResponse> {
        val response = client.get("$BASE_URL/me")
        return response.body()
    }

    suspend fun getOrderDetails(id: Int): OrderWithItemsResponse {
        val response = client.get("$BASE_URL/$id")
        return response.body()
    }

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
