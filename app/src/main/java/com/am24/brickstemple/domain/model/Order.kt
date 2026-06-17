package com.am24.brickstemple.domain.model

data class PagedResult<T>(
    val page: Int,
    val limit: Int,
    val total: Long,
    val data: List<T>
)

data class Order(
    val id: Int,
    val userId: Int,
    val status: String,
    val totalPrice: Double,
    val createdAt: String
)

data class OrderItem(
    val id: Int,
    val orderId: Int,
    val productId: Int,
    val quantity: Int,
    val priceAtPurchase: Double
)

data class OrderDetails(
    val order: Order,
    val items: List<OrderItem>
)
