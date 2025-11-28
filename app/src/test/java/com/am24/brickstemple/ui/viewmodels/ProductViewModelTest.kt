package com.am24.brickstemple.ui.viewmodels

import com.am24.brickstemple.data.fakes.FakeProductApiService
import com.am24.brickstemple.data.fakes.FakeProductDao
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
        val api = FakeProductApiService(HttpClient(MockEngine) {
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
}
