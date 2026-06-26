package com.am24.brickstemple.ui.screens.cart

import com.am24.brickstemple.MainDispatcherRule
import com.am24.brickstemple.domain.error.AppError
import com.am24.brickstemple.domain.error.AppException
import com.am24.brickstemple.domain.repository.CartRepository
import com.am24.brickstemple.domain.repository.ProductRepository
import com.am24.brickstemple.domain.usecase.cart.UpdateCartQuantityUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CartViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repo: CartRepository
    private lateinit var productRepository: ProductRepository
    private lateinit var viewModel: CartViewModel

    @Before
    fun setup() {
        repo = mockk(relaxed = true)
        productRepository = mockk(relaxed = true)

        every { repo.cart } returns MutableStateFlow(emptyMap())
        every { repo.isUpdating } returns MutableStateFlow(emptySet())
        every { repo.isClearing } returns MutableStateFlow(false)
        every { repo.isLoading } returns MutableStateFlow(false)
        every { repo.isLoaded } returns MutableStateFlow(false)

        coEvery { repo.refresh() } returns Unit
        coEvery { repo.checkout() } returns null
        coEvery { repo.toggle(any()) } returns Unit
        coEvery { repo.add(any()) } returns Unit
        coEvery { repo.updateQuantity(any(), any()) } returns Unit
        coEvery { repo.removeCompletely(any()) } returns Unit
        coEvery { repo.clearCart() } returns Unit
        coEvery { productRepository.getCachedByIds(any()) } returns emptyList()

        viewModel = createViewModel()
    }

    @Test
    fun `checkout calls repo and sets result`() = runTest {
        launchUiStateCollector()
        coEvery { repo.checkout() } returns 123

        viewModel.checkout()
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.checkout() }
        assertEquals(123, viewModel.uiState.value.checkoutResult)
        assertFalse(viewModel.uiState.value.checkoutInProgress)
    }

    @Test
    fun `checkout failure exposes error message and resets progress`() = runTest {
        launchUiStateCollector()
        coEvery { repo.checkout() } throws AppException(
            AppError.ServerError(userMessage = "Checkout failed")
        )

        viewModel.checkout()
        advanceUntilIdle()

        assertEquals("Checkout failed", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.checkoutInProgress)
        assertNull(viewModel.uiState.value.checkoutResult)
    }

    @Test
    fun `checkout unauthorized sets unauthorized without error message`() = runTest {
        launchUiStateCollector()
        coEvery { repo.checkout() } throws AppException(AppError.UnauthorizedError())

        viewModel.checkout()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.unauthorized)
        assertNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.checkoutInProgress)
    }

    @Test
    fun `checkout failure does not clear cart state`() = runTest {
        launchUiStateCollector()
        val cartState = MutableStateFlow(mapOf(5 to 2))
        every { repo.cart } returns cartState

        viewModel = createViewModel()
        launchUiStateCollector()

        coEvery { repo.checkout() } throws AppException(
            AppError.NetworkError("No internet connection.")
        )

        viewModel.checkout()
        advanceUntilIdle()

        assertEquals(mapOf(5 to 2), viewModel.cart.value)
    }

    @Test
    fun `toggle delegates to repository`() {
        val productId = 7

        viewModel.toggle(productId)

        coVerify(exactly = 1) { repo.toggle(productId) }
    }

    @Test
    fun `addProduct delegates to repository`() {
        val productId = 10

        viewModel.addProduct(productId)

        coVerify(exactly = 1) { repo.add(productId) }
    }

    @Test
    fun `addProduct failure exposes error message`() = runTest {
        launchUiStateCollector()
        val productId = 10
        coEvery { repo.add(productId) } throws AppException(
            AppError.ServerError(userMessage = "Failed to add cart item.")
        )

        viewModel.addProduct(productId)
        advanceUntilIdle()

        assertEquals("Failed to add cart item.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `updateQuantity with positive delta calls repo updateQuantity`() = runTest {
        launchUiStateCollector()
        val productId = 5

        every { repo.cart } returns MutableStateFlow(mapOf(productId to 2))

        viewModel.updateQuantity(productId, delta = +1)
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.updateQuantity(productId, 3) }

        assertNull(viewModel.uiState.value.updatingQuantityProductId)
    }

    @Test
    fun `updateQuantity with non-positive result removes item completely`() = runTest {
        launchUiStateCollector()
        val productId = 5

        every { repo.cart } returns MutableStateFlow(mapOf(productId to 1))

        viewModel.updateQuantity(productId, delta = -1)
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.removeCompletely(productId) }
        coVerify(exactly = 0) { repo.updateQuantity(any(), any()) }
        assertNull(viewModel.uiState.value.updatingQuantityProductId)
    }

    @Test
    fun `removeCompletely delegates to repository`() {
        val productId = 42

        viewModel.removeCompletely(productId)

        coVerify(exactly = 1) { repo.removeCompletely(productId) }
    }

    @Test
    fun `clearCart delegates to repository`() {
        viewModel.clearCart()

        coVerify(exactly = 1) { repo.clearCart() }
    }

    @Test
    fun `clearCheckoutResult sets result to null`() = runTest {
        launchUiStateCollector()
        coEvery { repo.checkout() } returns 99

        viewModel.checkout()
        advanceUntilIdle()

        viewModel.clearCheckoutResult()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.checkoutResult)
    }

    @Test
    fun `reset calls clearLocal and clears updatingQuantity`() = runTest {
        launchUiStateCollector()

        viewModel.reset()
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.clearLocal() }
        assertNull(viewModel.uiState.value.updatingQuantityProductId)
    }

    private fun TestScope.launchUiStateCollector() {
        backgroundScope.launch {
            viewModel.uiState.collect()
        }
    }

    private fun createViewModel(): CartViewModel {
        return CartViewModel(
            cartRepository = repo,
            productRepository = productRepository,
            updateCartQuantityUseCase = UpdateCartQuantityUseCase(repo)
        )
    }
}
