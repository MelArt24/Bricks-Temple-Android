package com.am24.brickstemple.data.repository

import com.am24.brickstemple.data.mapper.mapData
import com.am24.brickstemple.data.mapper.toDomain
import com.am24.brickstemple.data.remote.OrderApiService
import com.am24.brickstemple.domain.repository.OrderRepository

class OrderRepositoryImpl(
    private val api: OrderApiService
) : OrderRepository {

    override suspend fun getMyOrders() = api.getMyOrders().mapData { it.toDomain() }

    override suspend fun getOrderDetails(id: Int) = api.getOrderDetails(id).toDomain()
}
