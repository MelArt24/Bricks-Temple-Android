package com.am24.brickstemple.data.repositories

import com.am24.brickstemple.data.remote.OrderApiService

class OrderRepository(
    private val api: OrderApiService
) {

    suspend fun getMyOrders() = api.getMyOrders()

    suspend fun getOrderDetails(id: Int) = api.getOrderDetails(id)
}
