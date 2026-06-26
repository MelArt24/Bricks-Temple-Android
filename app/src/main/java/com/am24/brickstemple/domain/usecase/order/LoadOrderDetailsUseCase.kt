package com.am24.brickstemple.domain.usecase.order

import com.am24.brickstemple.domain.model.OrderDetails
import com.am24.brickstemple.domain.repository.OrderRepository
import com.am24.brickstemple.domain.repository.ProductRepository

class LoadOrderDetailsUseCase(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(orderId: Int): OrderDetailsWithProducts {
        val details = orderRepository.getOrderDetails(orderId)
        return OrderDetailsWithProducts(
            details = details,
            fullItems = hydrateItems(details)
        )
    }

    suspend fun hydrateItems(details: OrderDetails): List<FullOrderItem> {
        return details.items.map { item ->
            FullOrderItem(
                item = item,
                product = productRepository.getLocalById(item.productId)
            )
        }
    }
}
