package com.am24.brickstemple.data.repository

import com.am24.brickstemple.data.fakes.FakeWishlistApiService
import com.am24.brickstemple.data.remote.WishlistApiService
import com.am24.brickstemple.data.remote.dto.WishlistDto
import com.am24.brickstemple.data.remote.dto.WishlistItemDto
import com.am24.brickstemple.data.remote.dto.WishlistResponse
import com.am24.brickstemple.domain.error.AppException
import com.am24.brickstemple.domain.model.WishlistItem
import io.ktor.client.HttpClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WishlistRepositoryTest {

    private lateinit var api: FakeWishlistApiService
    private lateinit var repo: WishlistRepositoryImpl
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        api = FakeWishlistApiService().apply {
            serverItems = mutableListOf(
                Triple(10, 1, 1),
                Triple(20, 2, 1),
            )
        }

        repo = WishlistRepositoryImpl(api, dispatcher)
    }

    @Test
    fun `refresh should update local wishlist`() = runTest(dispatcher) {
        repo.refresh()
        advanceUntilIdle()

        assertEquals(
            mapOf(10 to 1, 20 to 2),
            repo.wishlist.value
        )
    }

    @Test
    fun `refresh maps remote failure to app exception and clears loading`() = runTest(dispatcher) {
        api.failGetWishlist = true

        try {
            repo.refresh()
            advanceUntilIdle()
            assertTrue("Expected AppException", false)
        } catch (e: AppException) {
            assertEquals("Remote wishlist request failed", e.message)
        }

        assertFalse(repo.isLoading.value)
        assertFalse(repo.isLoaded.value)
    }

    @Test
    fun `performToggle should add item when not exists`() = runTest(dispatcher) {
        repo.refresh()
        advanceUntilIdle()

        repo.toggle(30)
        advanceUntilIdle()

        assertTrue(api.added.contains(30))
    }

    @Test
    fun `toggle debounce merges multiple requests into a single refresh`() = runTest(dispatcher) {
        var refreshCount = 0

        val repoSpy = object : WishlistRepositoryImpl(api, dispatcher) {
            override suspend fun refresh() {
                refreshCount++
                super.refresh()
            }
        }

        repoSpy.toggle(10)
        repoSpy.toggle(10)
        repoSpy.toggle(10)

        advanceTimeBy(250)
        advanceUntilIdle()

        assertEquals(0, refreshCount)
    }

    @Test
    fun `performToggle should remove existing item`() = runTest(dispatcher) {
        repo.refresh()
        advanceUntilIdle()

        repo.toggle(10)
        advanceUntilIdle()

        assertTrue(api.removed.contains(1))
        assertEquals(mapOf(20 to 2), repo.wishlist.value)
    }


    @Test
    fun `toggle on multiple products runs refresh only once after all`() = runTest(dispatcher) {
        var count = 0

        val spy = object : WishlistRepositoryImpl(api, dispatcher) {
            override suspend fun refresh() {
                count++
                super.refresh()
            }
        }

        spy.toggle(10)
        spy.toggle(20)
        spy.toggle(30)

        advanceTimeBy(500)
        advanceUntilIdle()

        assertEquals(0, count)
    }

    @Test
    fun `clearLocal should clear wishlist`() = runTest(dispatcher) {
        repo.refresh()
        advanceUntilIdle()

        repo.clearLocal()

        assertEquals(emptyMap<Int, Int>(), repo.wishlist.value)
    }



    @Test
    fun `removeCompletely should remove item from API and refresh`() = runTest(dispatcher) {
        repo.refresh()
        advanceUntilIdle()

        repo.removeCompletely(10)
        advanceUntilIdle()

        assertTrue(api.removed.contains(1))

        assertEquals(
            mapOf(20 to 2),
            repo.wishlist.value
        )
    }

    @Test
    fun `removeCompletely should do nothing if product not found`() = runTest(dispatcher) {
        repo.refresh()
        advanceUntilIdle()

        repo.removeCompletely(999)
        advanceUntilIdle()

        assertTrue(api.removed.isEmpty())

        assertEquals(
            mapOf(10 to 1, 20 to 2),
            repo.wishlist.value
        )
    }

    @Test
    fun `concurrent removeCompletely serializes refreshes before reading next item id`() = runTest(dispatcher) {
        val api = ReassigningWishlistApiService(
            initialItems = listOf(
                Triple(10, 1, 1),
                Triple(20, 2, 1),
                Triple(30, 3, 1)
            )
        )
        val repo = WishlistRepositoryImpl(api, dispatcher)

        repo.refresh()
        advanceUntilIdle()

        listOf(
            launch { repo.removeCompletely(10) },
            launch { repo.removeCompletely(20) },
            launch { repo.removeCompletely(30) }
        ).joinAll()
        advanceUntilIdle()

        assertEquals(emptyMap<Int, Int>(), repo.wishlist.value)
        assertTrue(api.serverItems.isEmpty())
        assertEquals(3, api.removed.size)
    }

    @Test
    fun `concurrent toggle removals preserve all local removals`() = runTest(dispatcher) {
        val api = ReassigningWishlistApiService(
            initialItems = listOf(
                Triple(10, 1, 1),
                Triple(20, 2, 1),
                Triple(30, 3, 1)
            )
        )
        val repo = WishlistRepositoryImpl(api, dispatcher)

        repo.refresh()
        advanceUntilIdle()

        repo.toggle(10)
        repo.toggle(20)
        repo.toggle(30)
        advanceTimeBy(250)
        advanceUntilIdle()

        assertEquals(emptyMap<Int, Int>(), repo.wishlist.value)
        assertTrue(api.serverItems.isEmpty())
        assertEquals(3, api.removed.size)
    }

    @Test
    fun `removeOne should call removeOneItem and refresh`() = runTest(dispatcher) {
        repo.refresh()
        advanceUntilIdle()

        repo.removeOne(20)
        advanceUntilIdle()

        assertTrue(api.removedOne.contains(2))

        assertEquals(
            mapOf(10 to 1),
            repo.wishlist.value
        )
    }

    @Test
    fun `removeOne with remaining quantity updates local item without refresh`() = runTest(dispatcher) {
        val api = CountingWishlistApiService().apply {
            serverItems = mutableListOf(Triple(10, 1, 3))
        }
        val repo = WishlistRepositoryImpl(api, dispatcher)

        repo.refresh()
        advanceUntilIdle()

        assertEquals(1, api.getWishlistCount)

        repo.removeOne(10)
        advanceUntilIdle()

        assertEquals(1, api.getWishlistCount)
        assertEquals(mapOf(10 to 1), repo.wishlist.value)
        assertEquals(2, repo.lastFetchedItem(10)?.quantity)
    }

    @Test
    fun `removeOne with missing local item falls back to refresh`() = runTest(dispatcher) {
        val api = CountingWishlistApiService().apply {
            serverItems = mutableListOf(Triple(10, 1, 2))
        }
        val repo = WishlistRepositoryImpl(api, dispatcher)

        repo._wishlist.value = mapOf(10 to 1)

        repo.removeOne(10)
        advanceUntilIdle()

        assertEquals(1, api.getWishlistCount)
        assertEquals(1, repo.lastFetchedItem(10)?.quantity)
    }


    @Test
    fun `removeOne should ignore call when product not in wishlist`() = runTest(dispatcher) {
        repo.refresh()
        advanceUntilIdle()

        repo.removeOne(777)
        advanceUntilIdle()

        assertTrue(api.removed.isEmpty())
        assertEquals(
            mapOf(10 to 1, 20 to 2),
            repo.wishlist.value
        )
    }

    @Test
    fun `lastFetchedItem should return correct WishlistItemDto`() = runTest(dispatcher) {
        repo.refresh()
        advanceUntilIdle()

        val item = repo.lastFetchedItem(10)

        assertEquals(10, item?.productId)
        assertEquals(1, item?.id)
    }

    @Test
    fun `lastFetchedItem should return null when product not found`() = runTest(dispatcher) {
        repo.refresh()
        advanceUntilIdle()

        val item = repo.lastFetchedItem(999)

        assertEquals(null, item)
    }


    @Test
    fun `updateQuantity should call API and refresh`() = runTest(dispatcher) {
        repo.refresh()
        advanceUntilIdle()

        repo.updateQuantity(1, 5)
        advanceUntilIdle()

        assertTrue(api.updated.contains(1 to 5))

        val item = repo.lastFetchedItem(10)
        assertEquals(5, item?.quantity)
    }

    @Test
    fun `updateQuantity with local item updates local item without refresh`() = runTest(dispatcher) {
        val api = CountingWishlistApiService().apply {
            serverItems = mutableListOf(Triple(10, 1, 1))
        }
        val repo = WishlistRepositoryImpl(api, dispatcher)

        repo.refresh()
        advanceUntilIdle()

        assertEquals(1, api.getWishlistCount)

        repo.updateQuantity(1, 4)
        advanceUntilIdle()

        assertEquals(1, api.getWishlistCount)
        assertEquals(4, repo.lastFetchedItem(10)?.quantity)
        assertEquals(mapOf(10 to 1), repo.wishlist.value)
    }

    @Test
    fun `updateQuantity with missing local item falls back to refresh`() = runTest(dispatcher) {
        val api = CountingWishlistApiService().apply {
            serverItems = mutableListOf(Triple(10, 1, 1))
        }
        val repo = WishlistRepositoryImpl(api, dispatcher)

        repo._wishlist.value = mapOf(10 to 1)

        repo.updateQuantity(1, 6)
        advanceUntilIdle()

        assertEquals(1, api.getWishlistCount)
        assertEquals(6, repo.lastFetchedItem(10)?.quantity)
    }


    @Test
    fun `removeOne does nothing for unknown product`() = runTest(dispatcher) {
        repo.refresh()
        advanceUntilIdle()

        repo.removeOne(999)
        advanceUntilIdle()

        assertTrue(api.removedOne.isEmpty())
        assertEquals(
            mapOf(10 to 1, 20 to 2),
            repo.wishlist.value
        )
    }

    @Test
    fun `clearWishlist clears local state and calls API`() = runTest(dispatcher) {
        val api = FakeWishlistApiService()
        val repo = WishlistRepositoryImpl(api, dispatcher)

        repo._wishlist.value = mapOf(
            10 to 1,
            20 to 2
        )
        repo._items.value = listOf(
            WishlistItem(id = 1, wishlistId = 1, productId = 10, quantity = 1),
            WishlistItem(id = 2, wishlistId = 1, productId = 20, quantity = 2)
        )

        api.serverItems = mutableListOf(
            Triple(10, 1, 1),
            Triple(20, 2, 2)
        )

        assertFalse(repo.isClearing.value)

        repo.clearWishlist()
        advanceUntilIdle()

        assertTrue(api.serverItems.isEmpty())

        assertTrue(repo.wishlist.value.isEmpty())
        assertTrue(repo.items.value.isEmpty())

        assertFalse(repo.isClearing.value)
    }

    @Test
    fun `clearWishlist clears local state without refresh`() = runTest(dispatcher) {
        val api = CountingWishlistApiService().apply {
            serverItems = mutableListOf(Triple(10, 1, 1), Triple(20, 2, 1))
        }
        val repo = WishlistRepositoryImpl(api, dispatcher)

        repo.refresh()
        advanceUntilIdle()

        assertEquals(1, api.getWishlistCount)

        repo.clearWishlist()
        advanceUntilIdle()

        assertEquals(1, api.getWishlistCount)
        assertTrue(repo.wishlist.value.isEmpty())
        assertTrue(repo.items.value.isEmpty())
        assertTrue(api.serverItems.isEmpty())
    }

    @Test
    fun `clearWishlist preserves local state when remote clear fails`() = runTest(dispatcher) {
        val existingWishlist = mapOf(10 to 1, 20 to 2)
        val existingItems = listOf(
            WishlistItem(id = 1, wishlistId = 1, productId = 10, quantity = 1),
            WishlistItem(id = 2, wishlistId = 1, productId = 20, quantity = 2)
        )

        repo._wishlist.value = existingWishlist
        repo._items.value = existingItems
        api.failClearWishlist = true

        try {
            repo.clearWishlist()
            advanceUntilIdle()
            assertTrue("Expected AppException", false)
        } catch (e: AppException) {
            assertEquals("Remote clear wishlist request failed", e.message)
        }

        assertEquals(existingWishlist, repo.wishlist.value)
        assertEquals(existingItems, repo.items.value)
        assertFalse(repo.isClearing.value)
    }



    private class ReassigningWishlistApiService(
        initialItems: List<Triple<Int, Int, Int>>
    ) : WishlistApiService(HttpClient()) {
        var serverItems = initialItems.toMutableList()
        val removed = mutableListOf<Int>()
        private var nextItemId = 100

        override suspend fun getWishlist(): WishlistResponse =
            WishlistResponse(
                wishlist = WishlistDto(
                    id = 1,
                    userId = 1,
                    createdAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
                ),
                items = serverItems.map { (productId, itemId, quantity) ->
                    WishlistItemDto(
                        id = itemId,
                        wishlistId = 1,
                        productId = productId,
                        quantity = quantity,
                        addedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
                    )
                }
            )

        override suspend fun removeItem(itemId: Int) {
            delay(1)
            val index = serverItems.indexOfFirst { it.second == itemId }
            if (index == -1) error("""{ "error": "Item not found" }""")

            removed += itemId
            serverItems.removeAt(index)
            serverItems = serverItems.map { (productId, _, quantity) ->
                Triple(productId, nextItemId++, quantity)
            }.toMutableList()
        }
    }

    private class CountingWishlistApiService : WishlistApiService(HttpClient()) {
        var serverItems: MutableList<Triple<Int, Int, Int>> = mutableListOf()
        var getWishlistCount = 0
            private set

        override suspend fun getWishlist(): WishlistResponse {
            getWishlistCount++
            return WishlistResponse(
                wishlist = WishlistDto(
                    id = 1,
                    userId = 1,
                    createdAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
                ),
                items = serverItems.map { (productId, itemId, quantity) ->
                    WishlistItemDto(
                        id = itemId,
                        wishlistId = 1,
                        productId = productId,
                        quantity = quantity,
                        addedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
                    )
                }
            )
        }

        override suspend fun removeOneItem(itemId: Int) {
            val index = serverItems.indexOfFirst { it.second == itemId }
            if (index != -1) {
                val item = serverItems[index]
                val quantity = item.third - 1
                if (quantity <= 0) {
                    serverItems.removeAt(index)
                } else {
                    serverItems[index] = item.copy(third = quantity)
                }
            }
        }

        override suspend fun updateQuantity(itemId: Int, quantity: Int) {
            val index = serverItems.indexOfFirst { it.second == itemId }
            if (index != -1) {
                serverItems[index] = serverItems[index].copy(third = quantity)
            }
        }

        override suspend fun clearWishlist() {
            serverItems.clear()
        }
    }

}
