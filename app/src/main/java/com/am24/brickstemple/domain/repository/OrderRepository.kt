package com.am24.brickstemple.domain.repository

import com.am24.brickstemple.domain.model.Order
import com.am24.brickstemple.domain.model.OrderDetails
import com.am24.brickstemple.domain.model.PagedResult

interface OrderRepository {
    suspend fun getMyOrders(): PagedResult<Order>
    suspend fun getOrderDetails(id: Int): OrderDetails
}
