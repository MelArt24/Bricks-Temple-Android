package com.am24.brickstemple.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateOrderItemDto(
    val productId: Int,
    val quantity: Int
)

@Serializable
data class CreateOrderRequestDto(
    val items: List<CreateOrderItemDto>,
    val totalPrice: Double
)

@Serializable
data class CreatedResponse(
    val message: String,
    val id: Int
)
