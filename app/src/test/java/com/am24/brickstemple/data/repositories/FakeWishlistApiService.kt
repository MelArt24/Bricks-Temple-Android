package com.am24.brickstemple.data.repositories

import com.am24.brickstemple.data.remote.WishlistApiService
import com.am24.brickstemple.data.remote.dto.WishlistItemDto
import com.am24.brickstemple.data.remote.dto.WishlistDto
import com.am24.brickstemple.data.remote.dto.WishlistResponse
import io.ktor.client.HttpClient
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class FakeWishlistApiService : WishlistApiService(
    client = HttpClient()
) {

    var serverItems: MutableList<Pair<Int, Int>> = mutableListOf()

    val added = mutableListOf<Int>()
    val removed = mutableListOf<Int>()

    override suspend fun getWishlist(): WishlistResponse {
        return WishlistResponse(
            wishlist = WishlistDto(
                id = 1,
                userId = 1,
                createdAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            ),
            items = serverItems.map { (productId, itemId) ->
                WishlistItemDto(
                    id = itemId,
                    wishlistId = 1,
                    productId = productId,
                    quantity = 1,
                    addedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
                )
            }
        )
    }

    override suspend fun addItem(productId: Int) {
        added += productId
        val newId = (serverItems.maxOfOrNull { it.second } ?: 0) + 1
        serverItems += (productId to newId)
    }

    override suspend fun removeItem(itemId: Int) {
        removed += itemId
        serverItems.removeIf { it.second == itemId }
    }

    override suspend fun clearWishlist() {
        serverItems.clear()
    }

    override suspend fun updateQuantity(itemId: Int, quantity: Int) {

    }

    override suspend fun checkout() =
        WishlistApiService.CreatedResponse("ok", 1)
}
