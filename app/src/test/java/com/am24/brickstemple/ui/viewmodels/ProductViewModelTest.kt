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

}
