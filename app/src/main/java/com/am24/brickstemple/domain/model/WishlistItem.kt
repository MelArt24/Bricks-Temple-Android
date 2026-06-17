package com.am24.brickstemple.domain.model

import kotlinx.datetime.LocalDateTime

data class WishlistItem(
    val id: Int? = null,
    val wishlistId: Int,
    val productId: Int,
    val quantity: Int,
    val addedAt: LocalDateTime? = null
)
