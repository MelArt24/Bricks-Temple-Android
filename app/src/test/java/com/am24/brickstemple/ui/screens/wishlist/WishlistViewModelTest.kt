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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
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
        val repo = FakeWishlistRepository(
            wishlistItems = mapOf(7 to 70),
            refreshError = AppException(AppError.NetworkError())
        )
        val productRepository = FakeProductRepository(products = listOf(existingProduct))
        val viewModel = WishlistViewModel(repo, productRepository)
        launchUiStateCollector(viewModel)

        viewModel.loadProducts()
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals("No internet connection.", viewModel.uiState.value.errorMessage)
        assertEquals(listOf(existingProduct), viewModel.uiState.value.products)
    }

    @Test
    fun `loadProducts failure stores error and preserves products`() = runTest {
        val existingProduct = product(7)
        val repo = FakeWishlistRepository(wishlistItems = mapOf(7 to 70))
        val productRepository = FakeProductRepository(products = listOf(existingProduct))
        val viewModel = WishlistViewModel(repo, productRepository)
        launchUiStateCollector(viewModel)

        viewModel.loadProducts()
        advanceUntilIdle()

        productRepository.cachedByIdsError = AppException(AppError.LocalDataError("Local data error. Please try again."))
        viewModel.loadProducts()
        advanceUntilIdle()

        assertEquals("Local data error. Please try again.", viewModel.uiState.value.errorMessage)
        assertEquals(listOf(existingProduct), viewModel.uiState.value.products)
    }

    @Test
    fun `updateQuantity failure clears updating quantity and stores error`() = runTest {
        val repo = FakeWishlistRepository(
            wishlistItems = mapOf(7 to 70),
            items = listOf(WishlistItem(id = 70, wishlistId = 1, productId = 7, quantity = 1)),
            updateQuantityError = AppException(AppError.ServerError(userMessage = "Failed to update wishlist quantity."))
        )
        val viewModel = WishlistViewModel(repo, FakeProductRepository())
        launchUiStateCollector(viewModel)

        viewModel.updateQuantity(7, +1)
        advanceUntilIdle()

        assertEquals("Failed to update wishlist quantity.", viewModel.uiState.value.errorMessage)
        assertEquals(emptySet<Int>(), viewModel.uiState.value.updatingQuantityIds)
    }

    @Test
    fun `updateQuantity tracks multiple updating products independently`() = runTest {
        val updateGate = CompletableDeferred<Unit>()
        val repo = FakeWishlistRepository(
            wishlistItems = mapOf(7 to 70, 8 to 80),
            items = listOf(
                WishlistItem(id = 70, wishlistId = 1, productId = 7, quantity = 1),
                WishlistItem(id = 80, wishlistId = 1, productId = 8, quantity = 1)
            ),
            updateQuantityGate = updateGate
        )
        val viewModel = WishlistViewModel(repo, FakeProductRepository())
        launchUiStateCollector(viewModel)

        viewModel.updateQuantity(7, +1)
        viewModel.updateQuantity(8, +1)
        advanceUntilIdle()

        assertEquals(setOf(7, 8), viewModel.uiState.value.updatingQuantityIds)

        updateGate.complete(Unit)
        advanceUntilIdle()

        assertEquals(emptySet<Int>(), viewModel.uiState.value.updatingQuantityIds)
    }

    @Test
    fun `updateQuantity ignores duplicate mutation for same product while running`() = runTest {
        val updateGate = CompletableDeferred<Unit>()
        val repo = FakeWishlistRepository(
            wishlistItems = mapOf(7 to 70),
            items = listOf(WishlistItem(id = 70, wishlistId = 1, productId = 7, quantity = 1)),
            updateQuantityGate = updateGate
        )
        val viewModel = WishlistViewModel(repo, FakeProductRepository())
        launchUiStateCollector(viewModel)

        viewModel.updateQuantity(7, +1)
        viewModel.updateQuantity(7, +1)
        advanceUntilIdle()

        assertEquals(listOf(70 to 2), repo.updatedQuantities)

        updateGate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `updateQuantity refreshes missing item once before retrying mutation`() = runTest {
        val product7 = product(7)
        val repo = FakeWishlistRepository(
            wishlistItems = mapOf(7 to 70),
            refreshedItems = listOf(WishlistItem(id = 70, wishlistId = 1, productId = 7, quantity = 1))
        )
        val productRepository = FakeProductRepository(products = listOf(product7))
        val viewModel = WishlistViewModel(repo, productRepository)
        launchUiStateCollector(viewModel)

        viewModel.updateQuantity(7, +1)
        advanceUntilIdle()

        assertEquals(1, repo.refreshCount)
        assertEquals(listOf(70 to 2), repo.updatedQuantities)
        assertEquals(listOf(product7), viewModel.uiState.value.products)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `updateQuantity stores error when item is still missing after refresh`() = runTest {
        val existingProduct = product(7)
        val repo = FakeWishlistRepository(wishlistItems = mapOf(7 to 70))
        val productRepository = FakeProductRepository(products = listOf(existingProduct))
        val viewModel = WishlistViewModel(repo, productRepository)
        launchUiStateCollector(viewModel)

        viewModel.loadProducts()
        advanceUntilIdle()

        viewModel.updateQuantity(7, +1)
        advanceUntilIdle()

        assertEquals("Failed to update wishlist item.", viewModel.uiState.value.errorMessage)
        assertEquals(emptySet<Int>(), viewModel.uiState.value.updatingQuantityIds)
        assertEquals(listOf(existingProduct), viewModel.uiState.value.products)
    }

    @Test
    fun `updateQuantity success reloads products`() = runTest {
        val product7 = product(7)
        val repo = FakeWishlistRepository(
            wishlistItems = mapOf(7 to 70),
            items = listOf(WishlistItem(id = 70, wishlistId = 1, productId = 7, quantity = 1))
        )
        val productRepository = FakeProductRepository(products = listOf(product7))
        val viewModel = WishlistViewModel(repo, productRepository)
        launchUiStateCollector(viewModel)

        viewModel.updateQuantity(7, +1)
        advanceUntilIdle()

        assertEquals(listOf(product7), viewModel.uiState.value.products)
    }

    @Test
    fun `removeCompletely failure stores error`() = runTest {
        val repo = FakeWishlistRepository(
            removeCompletelyError = AppException(AppError.ServerError(userMessage = "Failed to remove wishlist item."))
        )
        val viewModel = WishlistViewModel(repo, FakeProductRepository())
        launchUiStateCollector(viewModel)

        viewModel.removeCompletely(7)
        advanceUntilIdle()

        assertEquals("Failed to remove wishlist item.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `removeCompletely success refreshes products from current wishlist`() = runTest {
        val product7 = product(7)
        val product8 = product(8)
        val repo = FakeWishlistRepository(wishlistItems = mapOf(7 to 70, 8 to 80))
        val productRepository = FakeProductRepository(products = listOf(product7, product8))
        val viewModel = WishlistViewModel(repo, productRepository)
        launchUiStateCollector(viewModel)

        viewModel.loadProducts()
        advanceUntilIdle()

        assertEquals(listOf(product7, product8), viewModel.uiState.value.products)

        viewModel.removeCompletely(7)
        advanceUntilIdle()

        assertEquals(listOf(product8), viewModel.uiState.value.products)
    }

    @Test
    fun `removeCompletely last item emits empty wishlist state without error`() = runTest {
        val product7 = product(7)
        val repo = FakeWishlistRepository(
            wishlistItems = mapOf(7 to 70),
            items = listOf(WishlistItem(id = 70, wishlistId = 1, productId = 7, quantity = 1))
        )
        val productRepository = FakeProductRepository(products = listOf(product7))
        val viewModel = WishlistViewModel(repo, productRepository)
        launchUiStateCollector(viewModel)

        viewModel.loadProducts()
        advanceUntilIdle()

        assertEquals(listOf(product7), viewModel.uiState.value.products)

        viewModel.removeCompletely(7)
        advanceUntilIdle()

        assertEquals(emptyMap<Int, Int>(), viewModel.uiState.value.wishlist)
        assertEquals(emptyList<WishlistItem>(), viewModel.uiState.value.items)
        assertEquals(emptyList<Product>(), viewModel.uiState.value.products)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `removeCompletely failure preserves products`() = runTest {
        val existingProduct = product(7)
        val repo = FakeWishlistRepository(
            wishlistItems = mapOf(7 to 70),
            removeCompletelyError = AppException(AppError.ServerError(userMessage = "Failed to remove wishlist item."))
        )
        val productRepository = FakeProductRepository(products = listOf(existingProduct))
        val viewModel = WishlistViewModel(repo, productRepository)
        launchUiStateCollector(viewModel)

        viewModel.loadProducts()
        advanceUntilIdle()

        viewModel.removeCompletely(7)
        advanceUntilIdle()

        assertEquals(listOf(existingProduct), viewModel.uiState.value.products)
        assertEquals("Failed to remove wishlist item.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `removeCompletely tracks removing product and clears after success`() = runTest {
        val removeGate = CompletableDeferred<Unit>()
        val repo = FakeWishlistRepository(removeCompletelyGate = removeGate)
        val viewModel = WishlistViewModel(repo, FakeProductRepository())
        launchUiStateCollector(viewModel)

        viewModel.removeCompletely(7)
        advanceUntilIdle()

        assertEquals(setOf(7), viewModel.uiState.value.removingProductIds)

        removeGate.complete(Unit)
        advanceUntilIdle()

        assertEquals(emptySet<Int>(), viewModel.uiState.value.removingProductIds)
    }

    @Test
    fun `removeCompletely clears removing product after failure`() = runTest {
        val repo = FakeWishlistRepository(
            removeCompletelyError = AppException(AppError.ServerError(userMessage = "Failed to remove wishlist item."))
        )
        val viewModel = WishlistViewModel(repo, FakeProductRepository())
        launchUiStateCollector(viewModel)

        viewModel.removeCompletely(7)
        advanceUntilIdle()

        assertEquals(emptySet<Int>(), viewModel.uiState.value.removingProductIds)
        assertEquals("Failed to remove wishlist item.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `reset clears removing products`() = runTest {
        val removeGate = CompletableDeferred<Unit>()
        val repo = FakeWishlistRepository(removeCompletelyGate = removeGate)
        val viewModel = WishlistViewModel(repo, FakeProductRepository())
        launchUiStateCollector(viewModel)

        viewModel.removeCompletely(7)
        advanceUntilIdle()

        assertEquals(setOf(7), viewModel.uiState.value.removingProductIds)

        viewModel.reset()
        advanceUntilIdle()

        assertEquals(emptySet<Int>(), viewModel.uiState.value.removingProductIds)
        removeGate.complete(Unit)
    }

    @Test
    fun `reset clears updating quantity ids`() = runTest {
        val updateGate = CompletableDeferred<Unit>()
        val repo = FakeWishlistRepository(
            wishlistItems = mapOf(7 to 70),
            items = listOf(WishlistItem(id = 70, wishlistId = 1, productId = 7, quantity = 1)),
            updateQuantityGate = updateGate
        )
        val viewModel = WishlistViewModel(repo, FakeProductRepository())
        launchUiStateCollector(viewModel)

        viewModel.updateQuantity(7, +1)
        advanceUntilIdle()

        assertEquals(setOf(7), viewModel.uiState.value.updatingQuantityIds)

        viewModel.reset()
        advanceUntilIdle()

        assertEquals(emptySet<Int>(), viewModel.uiState.value.updatingQuantityIds)
        updateGate.complete(Unit)
    }

    @Test
    fun `clearWishlist failure stores error and keeps clearing reset`() = runTest {
        val repo = FakeWishlistRepository(
            clearWishlistError = AppException(AppError.ServerError(userMessage = "Failed to clear wishlist."))
        )
        val viewModel = WishlistViewModel(repo, FakeProductRepository())
        launchUiStateCollector(viewModel)

        viewModel.clearWishlist()
        advanceUntilIdle()

        assertEquals("Failed to clear wishlist.", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isClearing)
    }

    @Test
    fun `clearError clears stored error`() = runTest {
        val viewModel = WishlistViewModel(
            FakeWishlistRepository(refreshError = AppException(AppError.NetworkError())),
            FakeProductRepository()
        )
        launchUiStateCollector(viewModel)

        viewModel.refresh()
        advanceUntilIdle()
        viewModel.clearError()

        assertNull(viewModel.uiState.value.errorMessage)
    }

    private fun TestScope.launchUiStateCollector(viewModel: WishlistViewModel) {
        backgroundScope.launch {
            viewModel.uiState.collect()
        }
    }

    private class FakeWishlistRepository(
        wishlistItems: Map<Int, Int> = emptyMap(),
        items: List<WishlistItem> = emptyList(),
        private val refreshError: Exception? = null,
        private val removeCompletelyError: Exception? = null,
        private val removeCompletelyGate: CompletableDeferred<Unit>? = null,
        private val updateQuantityGate: CompletableDeferred<Unit>? = null,
        private val refreshedItems: List<WishlistItem>? = null,
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
        var refreshCount = 0
            private set
        val updatedQuantities = mutableListOf<Pair<Int, Int>>()

        override suspend fun refresh() {
            refreshCount++
            refreshError?.let { throw it }
            refreshedItems?.let { items ->
                _wishlist.value = items.associate { it.productId to it.id!! }
                _items.value = items
            }
        }

        override suspend fun removeCompletely(productId: Int) {
            removeCompletelyGate?.await()
            removeCompletelyError?.let { throw it }
            _wishlist.value = _wishlist.value - productId
            _items.value = _items.value.filterNot { it.productId == productId }
        }

        override suspend fun removeOne(productId: Int) {
            updateQuantityError?.let { throw it }
            _wishlist.value = _wishlist.value - productId
            _items.value = _items.value.filterNot { it.productId == productId }
        }

        override fun toggle(productId: Int) = Unit

        override fun lastFetchedItem(productId: Int): WishlistItem? =
            _items.value.firstOrNull { it.productId == productId }

        override suspend fun updateQuantity(itemId: Int, newQuantity: Int) {
            updatedQuantities += itemId to newQuantity
            updateQuantityGate?.await()
            updateQuantityError?.let { throw it }
            _items.value = _items.value.map {
                if (it.id == itemId) it.copy(quantity = newQuantity) else it
            }
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
            return products.filter { it.id in ids }
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
