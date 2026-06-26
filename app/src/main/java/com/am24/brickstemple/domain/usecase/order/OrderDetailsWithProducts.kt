package com.am24.brickstemple.domain.usecase.order

import com.am24.brickstemple.domain.model.OrderDetails
import com.am24.brickstemple.domain.model.OrderItem
import com.am24.brickstemple.domain.model.Product

data class FullOrderItem(
    val item: OrderItem,
    val product: Product?
)

data class OrderDetailsWithProducts(
    val details: OrderDetails,
    val fullItems: List<FullOrderItem>
)
