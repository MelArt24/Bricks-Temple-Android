package com.am24.brickstemple.ui.screens.wishlist

import com.am24.brickstemple.MainDispatcherRule
import com.am24.brickstemple.domain.error.AppError
import com.am24.brickstemple.domain.error.AppException
import com.am24.brickstemple.domain.model.Product
import com.am24.brickstemple.domain.model.WishlistItem
import com.am24.brickstemple.domain.repository.ProductRepository
import com.am24.brickstemple.domain.repository.WishlistRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WishlistViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `refresh failure stores error and preserves products`() = runTest {
        val existingProduct = product(7)
        val repo = FakeWishlistRepository(refreshError = AppException(AppError.NetworkError()))
        val productRepository = FakeProductRepository(products = listOf(existingProduct))
        val viewModel = WishlistViewModel(repo, productRepository)

        viewModel.loadProducts()
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals("No internet connection.", viewModel.errorMessage.value)
        assertEquals(listOf(existingProduct), viewModel.products.value)
    }

    @Test
    fun `loadProducts failure stores error and preserves products`() = runTest {
        val existingProduct = product(7)
        val repo = FakeWishlistRepository(wishlistItems = mapOf(7 to 70))
        val productRepository = FakeProductRepository(products = listOf(existingProduct))
        val viewModel = WishlistViewModel(repo, productRepository)

        viewModel.loadProducts()
        advanceUntilIdle()

        productRepository.cachedByIdsError = AppException(AppError.LocalDataError("Local data error. Please try again."))
        viewModel.loadProducts()
        advanceUntilIdle()

        assertEquals("Local data error. Please try again.", viewModel.errorMessage.value)
        assertEquals(listOf(existingProduct), viewModel.products.value)
    }

    @Test
    fun `updateQuantity failure clears updating quantity and stores error`() = runTest {
        val repo = FakeWishlistRepository(
            wishlistItems = mapOf(7 to 70),
            items = listOf(WishlistItem(id = 70, wishlistId = 1, productId = 7, quantity = 1)),
            updateQuantityError = AppException(AppError.ServerError(userMessage = "Failed to update wishlist quantity."))
        )
        val viewModel = WishlistViewModel(repo, FakeProductRepository())

        viewModel.updateQuantity(7, +1)
        advanceUntilIdle()

        assertEquals("Failed to update wishlist quantity.", viewModel.errorMessage.value)
        assertNull(viewModel.updatingQuantity.value)
    }

    @Test
    fun `removeCompletely failure stores error`() = runTest {
        val repo = FakeWishlistRepository(
            removeCompletelyError = AppException(AppError.ServerError(userMessage = "Failed to remove wishlist item."))
        )
        val viewModel = WishlistViewModel(repo, FakeProductRepository())

        viewModel.removeCompletely(7)
        advanceUntilIdle()

        assertEquals("Failed to remove wishlist item.", viewModel.errorMessage.value)
    }

    @Test
    fun `removeCompletely tracks removing product and clears after success`() = runTest {
        val removeGate = CompletableDeferred<Unit>()
        val repo = FakeWishlistRepository(removeCompletelyGate = removeGate)
        val viewModel = WishlistViewModel(repo, FakeProductRepository())

        viewModel.removeCompletely(7)
        advanceUntilIdle()

        assertEquals(setOf(7), viewModel.removingProductIds.value)

        removeGate.complete(Unit)
        advanceUntilIdle()

        assertEquals(emptySet<Int>(), viewModel.removingProductIds.value)
    }

    @Test
    fun `removeCompletely clears removing product after failure`() = runTest {
        val repo = FakeWishlistRepository(
            removeCompletelyError = AppException(AppError.ServerError(userMessage = "Failed to remove wishlist item."))
        )
        val viewModel = WishlistViewModel(repo, FakeProductRepository())

        viewModel.removeCompletely(7)
        advanceUntilIdle()

        assertEquals(emptySet<Int>(), viewModel.removingProductIds.value)
        assertEquals("Failed to remove wishlist item.", viewModel.errorMessage.value)
    }

    @Test
    fun `reset clears removing products`() = runTest {
        val removeGate = CompletableDeferred<Unit>()
        val repo = FakeWishlistRepository(removeCompletelyGate = removeGate)
        val viewModel = WishlistViewModel(repo, FakeProductRepository())

        viewModel.removeCompletely(7)
        advanceUntilIdle()

        assertEquals(setOf(7), viewModel.removingProductIds.value)

        viewModel.reset()
        advanceUntilIdle()

        assertEquals(emptySet<Int>(), viewModel.removingProductIds.value)
        removeGate.complete(Unit)
    }

    @Test
    fun `clearWishlist failure stores error and keeps clearing reset`() = runTest {
        val repo = FakeWishlistRepository(
            clearWishlistError = AppException(AppError.ServerError(userMessage = "Failed to clear wishlist."))
        )
        val viewModel = WishlistViewModel(repo, FakeProductRepository())

        viewModel.clearWishlist()
        advanceUntilIdle()

        assertEquals("Failed to clear wishlist.", viewModel.errorMessage.value)
        assertFalse(viewModel.isClearing.value)
    }

    @Test
    fun `clearError clears stored error`() = runTest {
        val viewModel = WishlistViewModel(
            FakeWishlistRepository(refreshError = AppException(AppError.NetworkError())),
            FakeProductRepository()
        )

        viewModel.refresh()
        advanceUntilIdle()
        viewModel.clearError()

        assertNull(viewModel.errorMessage.value)
    }

    private class FakeWishlistRepository(
        wishlistItems: Map<Int, Int> = emptyMap(),
        items: List<WishlistItem> = emptyList(),
        private val refreshError: Exception? = null,
        private val removeCompletelyError: Exception? = null,
        private val removeCompletelyGate: CompletableDeferred<Unit>? = null,
        private val updateQuantityError: Exception? = null,
        private val clearWishlistError: Exception? = null
    ) : WishlistRepository {
        private val _wishlist = MutableStateFlow(wishlistItems)
        override val wishlist: StateFlow<Map<Int, Int>> = _wishlist

        private val _items = MutableStateFlow(items)
        override val items: StateFlow<List<WishlistItem>> = _items

        private val _isClearing = MutableStateFlow(false)

        override val isUpdating: StateFlow<Set<Int>> = MutableStateFlow(emptySet())
        override val isClearing: StateFlow<Boolean> = _isClearing
        override val isLoading: StateFlow<Boolean> = MutableStateFlow(false)
        override val isLoaded: StateFlow<Boolean> = MutableStateFlow(false)

        override suspend fun refresh() {
            refreshError?.let { throw it }
        }

        override suspend fun removeCompletely(productId: Int) {
            removeCompletelyGate?.await()
            removeCompletelyError?.let { throw it }
        }

        override suspend fun removeOne(productId: Int) {
            updateQuantityError?.let { throw it }
        }

        override fun toggle(productId: Int) = Unit

        override fun lastFetchedItem(productId: Int): WishlistItem? =
            _items.value.firstOrNull { it.productId == productId }

        override suspend fun updateQuantity(itemId: Int, newQuantity: Int) {
            updateQuantityError?.let { throw it }
        }

        override fun clearLocal() {
            _wishlist.value = emptyMap()
            _items.value = emptyList()
        }

        override suspend fun clearWishlist() {
            try {
                clearWishlistError?.let { throw it }
                _wishlist.value = emptyMap()
                _items.value = emptyList()
            } finally {
                _isClearing.value = false
            }
        }
    }

    private class FakeProductRepository(
        private val products: List<Product> = emptyList(),
        var cachedByIdsError: Exception? = null
    ) : ProductRepository {
        override suspend fun getCachedByType(type: String): List<Product> = emptyList()

        override suspend fun getCachedByIds(ids: List<Int>): List<Product> {
            cachedByIdsError?.let { throw it }
            return products
        }

        override suspend fun searchLocal(query: String): List<Product> = emptyList()
        override suspend fun getLocalById(id: Int): Product? = null
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
        fun product(id: Int) = Product(
            id = id,
            name = "Castle",
            price = 60.25,
            type = "set"
        )
    }
}
