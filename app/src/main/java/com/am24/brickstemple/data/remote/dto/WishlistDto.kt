package com.am24.brickstemple.data.remote.dto

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDateTime

@Serializable
data class WishlistItemDto(
    val id: Int? = null,
    val wishlistId: Int,
    val productId: Int,
    val quantity: Int,
    @Contextual val addedAt: LocalDateTime? = null
)

@Serializable
data class WishlistDto(
    val id: Int,
    val userId: Int,
    @Contextual val createdAt: LocalDateTime
)

@Serializable
data class WishlistResponse(
    val wishlist: WishlistDto,
    val items: List<WishlistItemDto>
)
