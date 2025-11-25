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

    var serverItems: MutableList<Triple<Int, Int, Int>> = mutableListOf()

    val added = mutableListOf<Int>()
    val removed = mutableListOf<Int>()
    val removedOne = mutableListOf<Int>()
    val updated = mutableListOf<Pair<Int, Int>>()

    override suspend fun getWishlist(): WishlistResponse {
        return WishlistResponse(
            wishlist = WishlistDto(
                id = 1,
                userId = 1,
                createdAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            ),
            items = serverItems.map { (productId, itemId, quantity) ->
                WishlistItemDto(
                    id = itemId,
                    wishlistId = 1,
                    productId = productId,
                    quantity = quantity,
                    addedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
                )
            }
        )
    }

    override suspend fun addItem(productId: Int) {
        added += productId
        val newItemId = (serverItems.maxOfOrNull { it.second } ?: 0) + 1
        serverItems += Triple(productId, newItemId, 1)
    }

    override suspend fun removeItem(itemId: Int) {
        removed += itemId
        serverItems.removeIf { it.second == itemId }
    }

    override suspend fun removeOneItem(itemId: Int) {
        removedOne += itemId
        val index = serverItems.indexOfFirst { it.second == itemId }
        if (index != -1) {
            val triple = serverItems[index]
            val newQty = triple.third - 1
            if (newQty <= 0) {
                serverItems.removeAt(index)
            } else {
                serverItems[index] = triple.copy(third = newQty)
            }
        }
    }

    override suspend fun clearWishlist() {
        serverItems.clear()
    }

    override suspend fun updateQuantity(itemId: Int, quantity: Int) {
        updated += (itemId to quantity)

        val index = serverItems.indexOfFirst { it.second == itemId }
        if (index != -1) {
            val triple = serverItems[index]
            serverItems[index] = triple.copy(third = quantity)
        }
    }


    override suspend fun checkout() =
        WishlistApiService.CreatedResponse("ok", 1)
}
