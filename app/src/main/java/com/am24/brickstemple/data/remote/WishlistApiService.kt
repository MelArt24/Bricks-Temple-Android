package com.am24.brickstemple.data.remote

import com.am24.brickstemple.data.remote.dto.WishlistResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

class WishlistApiService(
    private val client: HttpClient
) {

    private val BASE_URL = "https://bricks-temple-server.onrender.com/wishlist"

    suspend fun getWishlist(): WishlistResponse? {
        val response = client.get(BASE_URL)

        if (!response.status.isSuccess()) {
            return null
        }

        return try {
            response.body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun addItem(productId: Int) {
        client.post("$BASE_URL/add") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("productId" to productId))
        }
    }

    suspend fun removeItem(itemId: Int) {
        client.delete("$BASE_URL/remove/$itemId")
    }

    suspend fun clearWishlist() {
        client.delete("$BASE_URL/clear")
    }

    suspend fun updateQuantity(itemId: Int, quantity: Int) {
        client.put("$BASE_URL/item/$itemId") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("quantity" to quantity))
        }
    }

    @Serializable
    data class CreatedResponse(
        val message: String,
        val id: Int
    )

    suspend fun checkout(): CreatedResponse {
        val response = client.post("$BASE_URL/checkout")
        return response.body()
    }
}
