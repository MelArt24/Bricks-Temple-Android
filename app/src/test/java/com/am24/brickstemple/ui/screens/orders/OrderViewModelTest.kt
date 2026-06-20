package com.am24.brickstemple.ui.screens.orders

import com.am24.brickstemple.MainDispatcherRule
import com.am24.brickstemple.domain.error.AppError
import com.am24.brickstemple.domain.error.AppException
import com.am24.brickstemple.domain.model.Order
import com.am24.brickstemple.domain.model.OrderDetails
import com.am24.brickstemple.domain.model.OrderItem
import com.am24.brickstemple.domain.model.PagedResult
import com.am24.brickstemple.domain.model.Product
import com.am24.brickstemple.domain.repository.OrderRepository
import com.am24.brickstemple.domain.repository.ProductRepository
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrderViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loadOrders sets orders and resets loading on success`() = runTest {
        val order = order()
        val viewModel = OrderViewModel(
            repo = FakeOrderRepository(orders = PagedResult(1, 10, 1, listOf(order))),
            productRepository = FakeProductRepository()
        )

        viewModel.loadOrders()
        advanceUntilIdle()

        assertEquals(listOf(order), viewModel.orders.value)
        assertFalse(viewModel.loading.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `loadOrders exposes error and keeps existing orders on failed refresh`() = runTest {
        val existingOrder = order(id = 1)
        val repo = FakeOrderRepository(orders = PagedResult(1, 10, 1, listOf(existingOrder)))
        val viewModel = OrderViewModel(repo, FakeProductRepository())

        viewModel.loadOrders()
        advanceUntilIdle()

        repo.ordersError = AppException(AppError.NetworkError("No internet connection."))

        viewModel.loadOrders()
        advanceUntilIdle()

        assertEquals(listOf(existingOrder), viewModel.orders.value)
        assertEquals("No internet connection.", viewModel.errorMessage.value)
        assertFalse(viewModel.loading.value)
    }

    @Test
    fun `loadOrderDetails sets details and full items on success`() = runTest {
        val item = orderItem(productId = 22)
        val details = OrderDetails(order = order(), items = listOf(item))
        val product = product(id = 22)
        val viewModel = OrderViewModel(
            repo = FakeOrderRepository(details = details),
            productRepository = FakeProductRepository(products = mapOf(22 to product))
        )

        viewModel.loadOrderDetails(7)
        advanceUntilIdle()

        assertEquals(details, viewModel.orderDetails.value)
        assertEquals(1, viewModel.orderDetailsFull.value.size)
        assertEquals(item, viewModel.orderDetailsFull.value.first().item)
        assertEquals(product, viewModel.orderDetailsFull.value.first().product)
        assertFalse(viewModel.loading.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `loadOrderDetails exposes error and keeps existing details on failed refresh`() = runTest {
        val existingDetails = OrderDetails(order = order(id = 1), items = listOf(orderItem()))
        val repo = FakeOrderRepository(details = existingDetails)
        val viewModel = OrderViewModel(repo, FakeProductRepository())

        viewModel.loadOrderDetails(1)
        advanceUntilIdle()

        repo.detailsError = AppException(AppError.ServerError(userMessage = "Failed to load order details."))

        viewModel.loadOrderDetails(1)
        advanceUntilIdle()

        assertEquals(existingDetails, viewModel.orderDetails.value)
        assertEquals("Failed to load order details.", viewModel.errorMessage.value)
        assertFalse(viewModel.loading.value)
    }

    @Test
    fun `loadOrders cancellation does not expose error message`() = runTest {
        val viewModel = OrderViewModel(
            repo = FakeOrderRepository(ordersError = CancellationException("Cancelled")),
            productRepository = FakeProductRepository()
        )

        viewModel.loadOrders()
        advanceUntilIdle()

        assertNull(viewModel.errorMessage.value)
        assertFalse(viewModel.loading.value)
    }

    @Test
    fun `loadOrderDetails cancellation does not expose error message`() = runTest {
        val viewModel = OrderViewModel(
            repo = FakeOrderRepository(detailsError = CancellationException("Cancelled")),
            productRepository = FakeProductRepository()
        )

        viewModel.loadOrderDetails(7)
        advanceUntilIdle()

        assertNull(viewModel.errorMessage.value)
        assertFalse(viewModel.loading.value)
    }

    private class FakeOrderRepository(
        var orders: PagedResult<Order> = PagedResult(1, 10, 0, emptyList()),
        var details: OrderDetails = OrderDetails(order(), emptyList()),
        var ordersError: Throwable? = null,
        var detailsError: Throwable? = null
    ) : OrderRepository {
        override suspend fun getMyOrders(): PagedResult<Order> {
            ordersError?.let { throw it }
            return orders
        }

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
            id = 9,
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
