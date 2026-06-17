package com.am24.brickstemple.data.mapper

import com.am24.brickstemple.data.remote.OrderApiService
import com.am24.brickstemple.domain.model.Order
import com.am24.brickstemple.domain.model.OrderDetails
import com.am24.brickstemple.domain.model.OrderItem
import com.am24.brickstemple.domain.model.PagedResult

fun OrderApiService.OrderResponse.toDomain() = Order(
    id = id,
    userId = userId,
    status = status,
    totalPrice = totalPrice,
    createdAt = createdAt
)

fun OrderApiService.OrderItemResponse.toDomain() = OrderItem(
    id = id,
    orderId = orderId,
    productId = productId,
    quantity = quantity,
    priceAtPurchase = priceAtPurchase
)

fun OrderApiService.OrderWithItemsResponse.toDomain() = OrderDetails(
    order = order.toDomain(),
    items = items.map { it.toDomain() }
)

fun <T, R> OrderApiService.PagedResponse<T>.mapData(mapper: (T) -> R) = PagedResult(
    page = page,
    limit = limit,
    total = total,
    data = data.map(mapper)
)
