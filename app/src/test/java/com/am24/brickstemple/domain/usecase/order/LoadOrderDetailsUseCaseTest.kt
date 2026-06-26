package com.am24.brickstemple.domain.usecase.order

import com.am24.brickstemple.domain.error.AppError
import com.am24.brickstemple.domain.error.AppException
import com.am24.brickstemple.domain.model.Order
import com.am24.brickstemple.domain.model.OrderDetails
import com.am24.brickstemple.domain.model.OrderItem
import com.am24.brickstemple.domain.model.PagedResult
import com.am24.brickstemple.domain.model.Product
import com.am24.brickstemple.domain.repository.OrderRepository
import com.am24.brickstemple.domain.repository.ProductRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class LoadOrderDetailsUseCaseTest {

    @Test
    fun `invoke loads details and hydrates products`() = runTest {
        val item = orderItem(productId = 22)
        val details = OrderDetails(order = order(), items = listOf(item))
        val product = product(id = 22)
        val useCase = LoadOrderDetailsUseCase(
            orderRepository = FakeOrderRepository(details = details),
            productRepository = FakeProductRepository(products = mapOf(22 to product))
        )

        val result = useCase(orderId = 7)

        assertEquals(details, result.details)
        assertEquals(1, result.fullItems.size)
        assertEquals(item, result.fullItems.first().item)
        assertEquals(product, result.fullItems.first().product)
    }

    @Test
    fun `invoke preserves item when product is missing`() = runTest {
        val item = orderItem(productId = 404)
        val details = OrderDetails(order = order(), items = listOf(item))
        val useCase = LoadOrderDetailsUseCase(
            orderRepository = FakeOrderRepository(details = details),
            productRepository = FakeProductRepository()
        )

        val result = useCase(orderId = 7)

        assertEquals(item, result.fullItems.first().item)
        assertNull(result.fullItems.first().product)
    }

    @Test
    fun `invoke propagates order repository errors`() = runTest {
        val error = AppException(AppError.ServerError(userMessage = "Failed to load order details."))
        val useCase = LoadOrderDetailsUseCase(
            orderRepository = FakeOrderRepository(detailsError = error),
            productRepository = FakeProductRepository()
        )

        try {
            useCase(orderId = 7)
            fail("Expected AppException")
        } catch (e: AppException) {
            assertEquals(error, e)
        }
    }

    @Test
    fun `hydrateItems loads products for existing details`() = runTest {
        val first = orderItem(productId = 22)
        val second = orderItem(productId = 33)
        val details = OrderDetails(order = order(), items = listOf(first, second))
        val product = product(id = 33)
        val useCase = LoadOrderDetailsUseCase(
            orderRepository = FakeOrderRepository(),
            productRepository = FakeProductRepository(products = mapOf(33 to product))
        )

        val result = useCase.hydrateItems(details)

        assertNull(result.first().product)
        assertEquals(product, result.last().product)
    }

    private class FakeOrderRepository(
        private val details: OrderDetails = OrderDetails(order(), emptyList()),
        private val detailsError: Throwable? = null
    ) : OrderRepository {
        override suspend fun getMyOrders(): PagedResult<Order> = PagedResult(1, 10, 0, emptyList())

        override suspend fun getOrderDetails(id: Int): OrderDetails {
            detailsError?.let { throw it }
            return details
        }
    }

    private class FakeProductRepository(
        private val products: Map<Int, Product?> = emptyMap()
    ) : ProductRepository {
        override suspend fun getCachedByType(type: String): List<Product> = emptyList()
        override suspend fun getCachedByIds(ids: List<Int>): List<Product> = emptyList()
        override suspend fun searchLocal(query: String): List<Product> = emptyList()
        override suspend fun getLocalById(id: Int): Product? = products[id]
        override suspend fun refreshAllTypesParallel(): List<Product> = emptyList()
        override suspend fun syncByType(type: String): List<Product> = emptyList()
        override suspend fun getById(id: Int): Product = product(id)
        override suspend fun getFiltered(
            type: String?,
            category: String?,
            search: String?,
            minPrice: String?,
            maxPrice: String?,
            year: String?
        ): List<Product> = emptyList()
    }

    private companion object {
        fun order(id: Int = 7) = Order(
            id = id,
            userId = 3,
            status = "PENDING",
            totalPrice = 120.5,
            createdAt = "2026-06-20T10:00:00Z"
        )

        fun orderItem(productId: Int = 22) = OrderItem(
            id = productId,
            orderId = 7,
            productId = productId,
            quantity = 2,
            priceAtPurchase = 60.25
        )

        fun product(id: Int) = Product(
            id = id,
            name = "Castle",
            price = 60.25,
            type = "set"
        )
    }
}
