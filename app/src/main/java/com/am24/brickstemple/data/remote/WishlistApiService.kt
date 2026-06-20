package com.am24.brickstemple.data.remote

import com.am24.brickstemple.data.error.toAppException
import com.am24.brickstemple.data.error.toAppExceptionWithBody
import com.am24.brickstemple.data.remote.dto.WishlistResponse
import com.am24.brickstemple.data.remote.util.NetworkConstants
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import kotlinx.serialization.Serializable

open class WishlistApiService(
    private val client: HttpClient
) {

    private val BASE_URL = NetworkConstants.WISHLIST_URL

    open suspend fun getWishlist(): WishlistResponse {
        return try {
            val response = client.get(BASE_URL)
            response.ensureSuccess("Failed to load wishlist.")
            response.body()
        } catch (e: Exception) {
            throw e.toAppException("Failed to load wishlist.")
        }
    }

    open suspend fun addItem(productId: Int) {
        val response = client.post("$BASE_URL/add") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("productId" to productId))
        }
        response.ensureSuccess("Failed to add wishlist item.")
    }

    open suspend fun removeItem(itemId: Int) {
        client.delete("$BASE_URL/remove/$itemId")
            .ensureSuccess("Failed to remove wishlist item.")
    }

    open suspend fun removeOneItem(itemId: Int) {
        client.delete("$BASE_URL/removeOneItem/$itemId")
            .ensureSuccess("Failed to update wishlist item.")
    }

    open suspend fun clearWishlist() {
        client.delete("$BASE_URL/clear")
            .ensureSuccess("Failed to clear wishlist.")
    }

    open suspend fun updateQuantity(itemId: Int, quantity: Int) {
        val response = client.put("$BASE_URL/item/$itemId") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("quantity" to quantity))
        }
        response.ensureSuccess("Failed to update wishlist quantity.")
    }

    @Serializable
    data class CreatedResponse(
        val message: String,
        val id: Int
    )

    open suspend fun checkout(): CreatedResponse {
        val response = client.post("$BASE_URL/checkout")
        response.ensureSuccess("Failed to checkout wishlist.")
        return response.body()
    }

    private suspend fun HttpResponse.ensureSuccess(message: String) {
        if (!status.isSuccess()) {
            throw toAppExceptionWithBody(message)
        }
    }
}
