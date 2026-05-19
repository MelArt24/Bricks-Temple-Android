package com.am24.brickstemple.data.repository

import com.am24.brickstemple.data.remote.OrderApiService
import com.am24.brickstemple.domain.repository.OrderRepository

class OrderRepositoryImpl(
    private val api: OrderApiService
) : OrderRepository {

    override suspend fun getMyOrders(): OrderApiService.PagedResponse<OrderApiService.OrderResponse> = api.getMyOrders()

    override suspend fun getOrderDetails(id: Int): OrderApiService.OrderWithItemsResponse = api.getOrderDetails(id)
}
