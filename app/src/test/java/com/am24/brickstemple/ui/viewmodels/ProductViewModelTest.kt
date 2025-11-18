package com.am24.brickstemple.ui.viewmodels

import com.am24.brickstemple.data.repository.FakeProductApiService
import com.am24.brickstemple.data.repository.ProductRepository
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

    private lateinit var vm: ProductViewModel

    @Before
    fun setup() {
        val api = FakeProductApiService(HttpClient(MockEngine) {
            engine {
                addHandler { error("no calls should happen") }
            }
        })

        val repo = ProductRepository(api, mainDispatcherRule.dispatcher)

        vm = ProductViewModel(repo)
    }


    @Test
    fun `init loads default sections`() = runTest {
        advanceUntilIdle()

        val sections = vm.sections.value

        assertTrue(sections.containsKey("set"))
        assertTrue(sections.containsKey("minifigure"))
        assertTrue(sections.containsKey("detail"))
        assertTrue(sections.containsKey("polybag"))
        assertTrue(sections.containsKey("other"))
    }

    @Test
    fun `loadType loads correct items`() = runTest {
        vm.loadType("set")
        advanceUntilIdle()

        val state = vm.sections.value["set"]!!
        assertEquals(2, state.products.size)
    }

    @Test
    fun `loadCategory loads correct items`() = runTest {
        vm.loadCategory("Star Wars")
        advanceUntilIdle()

        val state = vm.sections.value["category:Star Wars"]!!
        assertEquals(2, state.products.size)
    }

    @Test
    fun `search loads search results`() = runTest {
        vm.search("Falcon")
        advanceUntilIdle()

        val state = vm.sections.value["search:Falcon"]!!
        assertEquals(1, state.products.size)
        assertEquals("Millennium Falcon", state.products.first().name)
    }

    @Test
    fun `loadFiltered loads products`() = runTest {
        vm.loadFiltered(type = "set")
        advanceUntilIdle()

        val state = vm.sections.value["filtered"]!!
        assertEquals(2, state.products.size)
    }

    @Test
    fun `loadPaged loads correct items`() = runTest {
        vm.loadPaged(page = 1, limit = 2)
        advanceUntilIdle()

        val state = vm.sections.value["paged:1"]!!
        assertEquals(2, state.products.size)
    }

    @Test
    fun `loadById loads single product`() = runTest {
        vm.loadById(1)
        advanceUntilIdle()

        val state = vm.sections.value["id:1"]!!
        assertEquals(1, state.products.size)
        assertEquals(1, state.products.first().id)
    }
}
