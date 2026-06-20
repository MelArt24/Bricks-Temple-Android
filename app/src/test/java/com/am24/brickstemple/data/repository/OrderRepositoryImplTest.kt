package com.am24.brickstemple.data.repository

import com.am24.brickstemple.data.remote.OrderApiService
import com.am24.brickstemple.domain.error.AppError
import com.am24.brickstemple.domain.error.AppException
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderRepositoryImplTest {

    private val api: OrderApiService = mockk()
    private val repository = OrderRepositoryImpl(api)

    @Test
    fun `getMyOrders maps response to domain`() = runBlocking {
        coEvery { api.getMyOrders() } returns OrderApiService.PagedResponse(
            page = 1,
            limit = 10,
            total = 1,
            data = listOf(orderResponse())
        )

        val result = repository.getMyOrders()

        assertEquals(1, result.page)
        assertEquals(1, result.data.size)
        assertEquals(7, result.data.first().id)
        assertEquals("PENDING", result.data.first().status)
    }

    @Test
    fun `getOrderDetails maps response to domain`() = runBlocking {
        coEvery { api.getOrderDetails(7) } returns OrderApiService.OrderWithItemsResponse(
            order = orderResponse(),
            items = listOf(orderItemResponse())
        )

        val result = repository.getOrderDetails(7)

        assertEquals(7, result.order.id)
        assertEquals(1, result.items.size)
        assertEquals(22, result.items.first().productId)
    }

    @Test
    fun `getMyOrders maps raw failure to app exception`() = runBlocking {
        coEvery { api.getMyOrders() } throws RuntimeException("Broken")

        try {
            repository.getMyOrders()
            throw AssertionError("Expected AppException")
        } catch (e: AppException) {
            assertTrue(e.error is AppError.UnknownError)
            assertEquals("Broken", e.message)
        }
    }

    @Test
    fun `getOrderDetails preserves existing app exception`() = runBlocking {
        val expected = AppException(AppError.NotFoundError("Order missing"))
        coEvery { api.getOrderDetails(7) } throws expected

        try {
            repository.getOrderDetails(7)
            throw AssertionError("Expected AppException")
        } catch (e: AppException) {
            assertSame(expected, e)
        }
    }

    @Test
    fun `getMyOrders rethrows cancellation exception`() = runBlocking {
        coEvery { api.getMyOrders() } throws CancellationException("Cancelled")

        try {
            repository.getMyOrders()
            throw AssertionError("Expected CancellationException")
        } catch (e: CancellationException) {
            assertEquals("Cancelled", e.message)
        }
    }

    private fun orderResponse() = OrderApiService.OrderResponse(
        id = 7,
        userId = 3,
        status = "PENDING",
        totalPrice = 120.5,
        createdAt = "2026-06-20T10:00:00Z"
    )

    private fun orderItemResponse() = OrderApiService.OrderItemResponse(
        id = 9,
        orderId = 7,
        productId = 22,
        quantity = 2,
        priceAtPurchase = 60.25
    )
}
