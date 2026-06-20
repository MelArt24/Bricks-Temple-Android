package com.am24.brickstemple.data.remote

import com.am24.brickstemple.domain.error.AppError
import com.am24.brickstemple.domain.error.AppException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import java.io.IOException
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderApiServiceTest {

    @Test
    fun `getMyOrders returns decoded paged orders`() = runBlocking {
        val service = serviceWithResponse(
            """
            {
              "page": 1,
              "limit": 10,
              "total": 1,
              "data": [
                {
                  "id": 7,
                  "userId": 3,
                  "status": "PENDING",
                  "totalPrice": 120.5,
                  "createdAt": "2026-06-20T10:00:00Z"
                }
              ]
            }
            """.trimIndent()
        )

        val result = service.getMyOrders()

        assertEquals(1, result.page)
        assertEquals(1, result.data.size)
        assertEquals(7, result.data.first().id)
    }

    @Test
    fun `getOrderDetails returns decoded order details`() = runBlocking {
        val service = serviceWithResponse(orderDetailsJson())

        val result = service.getOrderDetails(7)

        assertEquals(7, result.order.id)
        assertEquals(1, result.items.size)
        assertEquals(22, result.items.first().productId)
    }

    @Test
    fun `getMyOrders maps unauthorized to app exception`() = runBlocking {
        val service = serviceWithStatus(HttpStatusCode.Unauthorized, "Unauthorized")

        try {
            service.getMyOrders()
            throw AssertionError("Expected AppException")
        } catch (e: AppException) {
            assertTrue(e.error is AppError.UnauthorizedError)
            assertEquals("Unauthorized", e.message)
        }
    }

    @Test
    fun `getOrderDetails maps not found to app exception`() = runBlocking {
        val service = serviceWithStatus(HttpStatusCode.NotFound, "Order not found")

        try {
            service.getOrderDetails(404)
            throw AssertionError("Expected AppException")
        } catch (e: AppException) {
            assertTrue(e.error is AppError.NotFoundError)
            assertEquals("Order not found", e.message)
        }
    }

    @Test
    fun `checkout maps 499 to network error`() = runBlocking {
        val service = serviceWithStatus(HttpStatusCode(499, "Client Closed Request"), "Offline")

        try {
            service.checkout(emptyList(), 0.0)
            throw AssertionError("Expected AppException")
        } catch (e: AppException) {
            assertTrue(e.error is AppError.NetworkError)
            assertEquals("Offline", e.message)
        }
    }

    @Test
    fun `checkout maps server error to app exception`() = runBlocking {
        val service = serviceWithStatus(HttpStatusCode.InternalServerError, "Server failed")

        try {
            service.checkout(emptyList(), 0.0)
            throw AssertionError("Expected AppException")
        } catch (e: AppException) {
            val error = e.error
            assertTrue(error is AppError.ServerError)
            assertEquals(500, (error as AppError.ServerError).statusCode)
            assertEquals("Server failed", e.message)
        }
    }

    @Test
    fun `network exception maps to network error`() = runBlocking {
        val client = HttpClient(MockEngine { throw IOException("No route") }) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        val service = OrderApiService(client)

        try {
            service.getMyOrders()
            throw AssertionError("Expected AppException")
        } catch (e: AppException) {
            assertTrue(e.error is AppError.NetworkError)
            assertEquals("No internet connection.", e.message)
        }
    }

    @Test
    fun `cancellation exception is rethrown`() = runBlocking {
        val client = HttpClient(MockEngine { throw CancellationException("Cancelled") }) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        val service = OrderApiService(client)

        try {
            service.getMyOrders()
            throw AssertionError("Expected CancellationException")
        } catch (e: CancellationException) {
            assertEquals("Cancelled", e.message)
        }
    }

    private fun serviceWithResponse(
        content: String,
        status: HttpStatusCode = HttpStatusCode.OK
    ): OrderApiService {
        val client = HttpClient(MockEngine {
            respond(
                content = content,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        return OrderApiService(client)
    }

    private fun serviceWithStatus(
        status: HttpStatusCode,
        content: String
    ): OrderApiService {
        val client = HttpClient(MockEngine {
            respondError(
                status = status,
                content = content,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Text.Plain.toString())
            )
        }) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        return OrderApiService(client)
    }

    private fun orderDetailsJson(): String = """
        {
          "order": {
            "id": 7,
            "userId": 3,
            "status": "PENDING",
            "totalPrice": 120.5,
            "createdAt": "2026-06-20T10:00:00Z"
          },
          "items": [
            {
              "id": 9,
              "orderId": 7,
              "productId": 22,
              "quantity": 2,
              "priceAtPurchase": 60.25
            }
          ]
        }
    """.trimIndent()
}
