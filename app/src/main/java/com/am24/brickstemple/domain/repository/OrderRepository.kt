package com.am24.brickstemple.domain.repository

import com.am24.brickstemple.data.remote.OrderApiService

interface OrderRepository {
    suspend fun getMyOrders(): OrderApiService.PagedResponse<OrderApiService.OrderResponse>
    suspend fun getOrderDetails(id: Int): OrderApiService.OrderWithItemsResponse
}
