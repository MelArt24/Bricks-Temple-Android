package com.am24.brickstemple.ui.viewmodels

import com.am24.brickstemple.MainDispatcherRule
import com.am24.brickstemple.data.repositories.CartRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CartViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repo: CartRepository
    private lateinit var viewModel: CartViewModel

    @Before
    fun setup() {
        repo = mockk(relaxed = true)

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

        viewModel = CartViewModel(repo)
    }

    @Test
    fun `checkout calls repo and sets result`() {
        coEvery { repo.checkout() } returns 123

        viewModel.checkout()

        coVerify(exactly = 1) { repo.checkout() }
        assertEquals(123, viewModel.checkoutResult.value)
        assertFalse(viewModel.checkoutInProgress.value)
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
    fun `updateQuantity with positive delta calls repo updateQuantity`() {
        val productId = 5

        every { repo.cart } returns MutableStateFlow(mapOf(productId to 2))

        viewModel.updateQuantity(productId, delta = +1)

        coVerify(exactly = 1) { repo.updateQuantity(productId, 3) }

        assertNull(viewModel.updatingQuantity.value)
    }

    @Test
    fun `updateQuantity with non-positive result removes item completely`() {
        val productId = 5

        every { repo.cart } returns MutableStateFlow(mapOf(productId to 1))

        viewModel.updateQuantity(productId, delta = -1)

        coVerify(exactly = 1) { repo.removeCompletely(productId) }
        coVerify(exactly = 0) { repo.updateQuantity(any(), any()) }
        assertNull(viewModel.updatingQuantity.value)
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
    fun `clearCheckoutResult sets result to null`() {
        val orderId = 99

        val field = CartViewModel::class.java.getDeclaredField("_checkoutResult")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val state = field.get(viewModel) as MutableStateFlow<Int?>
        state.value = orderId

        viewModel.clearCheckoutResult()

        assertNull(viewModel.checkoutResult.value)
    }

    @Test
    fun `reset calls clearLocal and clears updatingQuantity`() {
        val field = CartViewModel::class.java.getDeclaredField("_updatingQuantity")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val state = field.get(viewModel) as MutableStateFlow<Int?>
        state.value = 123

        viewModel.reset()

        coVerify(exactly = 1) { repo.clearLocal() }
        assertNull(viewModel.updatingQuantity.value)
    }
}
