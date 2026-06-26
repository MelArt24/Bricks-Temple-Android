package com.am24.brickstemple.domain.usecase.cart

import com.am24.brickstemple.domain.error.AppError
import com.am24.brickstemple.domain.error.AppException
import com.am24.brickstemple.domain.repository.CartRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class UpdateCartQuantityUseCaseTest {

    @Test
    fun `invoke with positive resulting quantity updates cart quantity`() = runTest {
        val productId = 5
        val repository = cartRepository(cart = mapOf(productId to 2))
        val useCase = UpdateCartQuantityUseCase(repository)

        useCase(productId = productId, delta = +1)

        coVerify(exactly = 1) { repository.updateQuantity(productId, 3) }
        coVerify(exactly = 0) { repository.removeCompletely(any()) }
    }

    @Test
    fun `invoke with non-positive resulting quantity removes product`() = runTest {
        val productId = 5
        val repository = cartRepository(cart = mapOf(productId to 1))
        val useCase = UpdateCartQuantityUseCase(repository)

        useCase(productId = productId, delta = -1)

        coVerify(exactly = 1) { repository.removeCompletely(productId) }
        coVerify(exactly = 0) { repository.updateQuantity(any(), any()) }
    }

    @Test
    fun `invoke treats missing product as zero quantity`() = runTest {
        val productId = 5
        val repository = cartRepository(cart = emptyMap())
        val useCase = UpdateCartQuantityUseCase(repository)

        useCase(productId = productId, delta = +1)

        coVerify(exactly = 1) { repository.updateQuantity(productId, 1) }
        coVerify(exactly = 0) { repository.removeCompletely(any()) }
    }

    @Test
    fun `invoke propagates repository errors`() = runTest {
        val productId = 5
        val error = AppException(AppError.ServerError(userMessage = "Failed to update cart quantity."))
        val repository = cartRepository(cart = mapOf(productId to 2))
        coEvery { repository.updateQuantity(productId, 3) } throws error
        val useCase = UpdateCartQuantityUseCase(repository)

        try {
            useCase(productId = productId, delta = +1)
            fail("Expected AppException")
        } catch (e: AppException) {
            assertEquals(error, e)
        }
    }

    private fun cartRepository(cart: Map<Int, Int>): CartRepository {
        return mockk(relaxed = true) {
            every { this@mockk.cart } returns MutableStateFlow(cart)
            coEvery { updateQuantity(any(), any()) } returns Unit
            coEvery { removeCompletely(any()) } returns Unit
        }
    }
}
