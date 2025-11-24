package com.am24.brickstemple.data.repositories

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
                10 to 1,
                20 to 2,
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

        assertEquals(1, refreshCount)
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

        assertEquals(1, count)
    }

    @Test
    fun `clearLocal should clear wishlist`() = runTest(dispatcher) {
        repo.refresh()
        advanceUntilIdle()

        repo.clearLocal()

        assertEquals(emptyMap<Int, Int>(), repo.wishlist.value)
    }
}
