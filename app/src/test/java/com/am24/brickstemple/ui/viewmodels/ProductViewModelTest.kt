package com.am24.brickstemple.ui.viewmodels

import com.am24.brickstemple.MainDispatcherRule
import com.am24.brickstemple.data.fakes.FakeProductApiService
import com.am24.brickstemple.data.fakes.FakeProductDao
import com.am24.brickstemple.data.fakes.FakeProductRepository
import com.am24.brickstemple.data.repositories.ProductRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var dao: FakeProductDao
    private lateinit var api: FakeProductApiService
    private lateinit var repo: ProductRepository
    private lateinit var vm: ProductViewModel

    @Before
    fun setup() {
        api = FakeProductApiService(HttpClient(MockEngine) {
            engine {
                addHandler { error("no calls should happen") }
            }
        })

        dao = FakeProductDao()
        repo = ProductRepository(api, dao, mainDispatcherRule.dispatcher)
        vm = ProductViewModel(repo)
    }

    @Test
    fun `initial load fills category lists`() = runTest {
        advanceUntilIdle()

        assertTrue(vm.sets.value.products.isNotEmpty())
        assertTrue(vm.minifigs.value.products.isNotEmpty())
    }

    @Test
    fun `search finds matching products`() = runTest {
        advanceUntilIdle()

        vm.search("Falcon")
        advanceUntilIdle()

        val result = vm.searchResult.value.products
        assertEquals(1, result.size)
        assertEquals("Millennium Falcon", result.first().name)
    }

    @Test
    fun `loadById loads product correctly`() = runTest {
        vm.loadById(1)
        advanceUntilIdle()

        val result = vm.productById.value.products
        assertEquals(1, result.first().id)
    }

    @Test
    fun `loadLocalCache loads sets correctly`() = runTest {
        advanceUntilIdle()

        val sets = vm.sets.value.products
        assertEquals(2, sets.count { it.type == "set" })
    }

    @Test
    fun `loadById success updates productById with product`() = runTest {
        val repo = FakeProductRepository()
        val vm = ProductViewModel(repo)

        vm.loadById(1)
        advanceUntilIdle()

        val state = vm.productById.value

        assertFalse(state.isLoading)
        assertNotNull(state.products.firstOrNull())
        assertEquals(1, state.products.first().id)
        assertNull(state.error)
    }

    @Test
    fun `loadById failure updates productById with error`() = runTest {
        val repo = FakeProductRepository()
        repo.shouldThrow = true

        val vm = ProductViewModel(repo)

        vm.loadById(1)
        advanceUntilIdle()

        val state = vm.productById.value

        assertTrue(state.products.isEmpty())
        assertNotNull(state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun `setSearchQuery updates searchQuery flow`() = runTest {
        assertEquals("", vm.searchQuery.value)

        vm.setSearchQuery("ninja")
        assertEquals("ninja", vm.searchQuery.value)

        vm.setSearchQuery("star wars")
        assertEquals("star wars", vm.searchQuery.value)
    }

    @Test
    fun `matchesQuery finds product by name`() = runTest {
        advanceUntilIdle()

        val falcon = vm.sets.value.products.first { it.id == 1 }

        assertTrue(vm.matchesQuery(falcon, "falcon"))
        assertTrue(vm.matchesQuery(falcon, "MILLENNIUM"))
        assertTrue(vm.matchesQuery(falcon, "millennium falcon"))
    }

    @Test
    fun `matchesQuery finds product by partial name`() = runTest {
        advanceUntilIdle()

        val police = vm.sets.value.products.first { it.id == 3 }

        assertTrue(vm.matchesQuery(police, "pol"))
        assertTrue(vm.matchesQuery(police, "station"))
        assertTrue(vm.matchesQuery(police, "lice"))
    }

    @Test
    fun `matchesQuery returns false for unrelated query`() = runTest {
        advanceUntilIdle()

        val falcon = vm.sets.value.products.first { it.id == 1 }

        assertFalse(vm.matchesQuery(falcon, "technic"))
        assertFalse(vm.matchesQuery(falcon, "creator"))
        assertFalse(vm.matchesQuery(falcon, "bionicle"))
    }

    @Test
    fun `matchesQuery returns true for blank query`() = runTest {
        advanceUntilIdle()

        val pilot = vm.minifigs.value.products.first { it.id == 2 }

        assertTrue(vm.matchesQuery(pilot, ""))
        assertTrue(vm.matchesQuery(pilot, "   "))
    }

    @Test
    fun `matchesQuery finds product by number`() = runTest {
        advanceUntilIdle()

        val falcon = vm.sets.value.products.first { it.id == 1 }

        assertTrue(vm.matchesQuery(falcon, "75192"))
    }
}
