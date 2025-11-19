package com.am24.brickstemple.ui.viewmodels

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test


@OptIn(ExperimentalCoroutinesApi::class)
class ProductDetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repo: FakeProductRepository

    @Before
    fun setup() {
        repo = FakeProductRepository()
    }

    @Test
    fun `loadProduct success updates uiState with product`() = runTest {
        val vm = ProductDetailsViewModel(1, repo)

        advanceUntilIdle()

        val state = vm.uiState.value

        assertFalse(state.isLoading)
        assertNotNull(state.product)
        assertEquals(1, state.product!!.id)
        assertNull(state.error)
    }

    @Test
    fun `loadProduct failure updates uiState with error`() = runTest {
        repo.shouldThrow = true

        val vm = ProductDetailsViewModel(1, repo)

        advanceUntilIdle()

        val state = vm.uiState.value

        assertNull(state.product)
        assertNotNull(state.error)
        assertFalse(state.isLoading)
    }
}

