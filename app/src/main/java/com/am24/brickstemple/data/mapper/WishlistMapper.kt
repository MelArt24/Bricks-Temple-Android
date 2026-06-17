package com.am24.brickstemple.data.mapper

import com.am24.brickstemple.data.remote.dto.WishlistItemDto
import com.am24.brickstemple.domain.model.WishlistItem

fun WishlistItemDto.toDomain() = WishlistItem(
    id = id,
    wishlistId = wishlistId,
    productId = productId,
    quantity = quantity,
    addedAt = addedAt
)
