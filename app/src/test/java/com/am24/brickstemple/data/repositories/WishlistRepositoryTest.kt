package com.am24.brickstemple.data.repositories

import com.am24.brickstemple.data.fakes.FakeWishlistApiService
import com.am24.brickstemple.data.remote.dto.WishlistItemDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WishlistRepositoryTest {

    private lateinit var api: FakeWishlistApiService
    private lateinit var repo: WishlistRepository
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        api = FakeWishlistApiService().apply {
            serverItems = mutableListOf(
                Triple(10, 1, 1),
                Triple(20, 2, 1),
            )
        }

        repo = WishlistRepository(api, dispatcher)
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

        val repoSpy = object : WishlistRepository(api, dispatcher) {
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

        val spy = object : WishlistRepository(api, dispatcher) {
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
        val repo = WishlistRepository(api, dispatcher)

        repo._wishlist.value = mapOf(
            10 to 1,
            20 to 2
        )
        repo._items.value = listOf(
            WishlistItemDto(id = 1, wishlistId = 1, productId = 10, quantity = 1),
            WishlistItemDto(id = 2, wishlistId = 1, productId = 20, quantity = 2)
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



}
