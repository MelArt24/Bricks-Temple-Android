package com.am24.brickstemple.data.remote

import com.am24.brickstemple.data.error.toAppException
import com.am24.brickstemple.data.error.toAppExceptionWithBody
import com.am24.brickstemple.data.remote.util.NetworkConstants
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable


class OrderApiService(
    private val client: HttpClient
) {

    private val BASE_URL = NetworkConstants.ORDERS_URL

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

    private suspend inline fun <reified T> safeRequest(
        defaultMessage: String,
        crossinline block: suspend () -> HttpResponse
    ): T {
        try {
            val response = block()
            if (response.status.value !in 200..299) {
                throw response.toAppExceptionWithBody(defaultMessage)
            }
            return response.body()
        } catch (e: Exception) {
            throw e.toAppException(defaultMessage)
        }
    }

    suspend fun getMyOrders(): PagedResponse<OrderResponse> {
        return safeRequest("Failed to load orders.") {
            client.get("$BASE_URL/me")
        }
    }

    suspend fun getOrderDetails(id: Int): OrderWithItemsResponse {
        return safeRequest("Failed to load order details.") {
            client.get("$BASE_URL/$id")
        }
    }

    suspend fun checkout(
        items: List<CreateOrderItemRequest>,
        totalPrice: Double
    ): CreatedOrderResponse {
        return safeRequest("Failed to checkout cart.") {
            client.post(BASE_URL) {
                contentType(ContentType.Application.Json)
                setBody(CreateOrderRequest(items, totalPrice))
            }
        }
    }
}
