package com.am24.brickstemple.data.repository

import com.am24.brickstemple.data.error.toAppException
import com.am24.brickstemple.data.mapper.mapData
import com.am24.brickstemple.data.mapper.toDomain
import com.am24.brickstemple.data.remote.OrderApiService
import com.am24.brickstemple.domain.repository.OrderRepository

class OrderRepositoryImpl(
    private val api: OrderApiService
) : OrderRepository {

    override suspend fun getMyOrders() = try {
        api.getMyOrders().mapData { it.toDomain() }
    } catch (e: Throwable) {
        throw e.toAppException("Failed to load orders.")
    }

    override suspend fun getOrderDetails(id: Int) = try {
        api.getOrderDetails(id).toDomain()
    } catch (e: Throwable) {
        throw e.toAppException("Failed to load order details.")
    }
}
