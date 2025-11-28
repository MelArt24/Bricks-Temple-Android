package com.am24.brickstemple.data.repositories

import com.am24.brickstemple.data.fakes.FakeProductApiService
import com.am24.brickstemple.data.fakes.FakeProductDao
import com.am24.brickstemple.data.mappers.toEntity
import io.ktor.client.HttpClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductRepositoryTest {

    private lateinit var api: FakeProductApiService
    private lateinit var dao: FakeProductDao
    private lateinit var repo: ProductRepository

    @Before
    fun setup() {
        api = FakeProductApiService(HttpClient())
        dao = FakeProductDao()
        repo = ProductRepository(api, dao)
    }

    @Test
    fun `getCachedByType returns only products from local DB`() = runTest {
        dao.insertAll(api.products.map { it.toEntity() })

        val result = repo.getCachedByType("set")

        assertEquals(2, result.size)
        assertEquals("Millennium Falcon", result.first().name)
    }

    @Test
    fun `searchLocal filters products by name`() = runTest {
        dao.insertAll(api.products.map { it.toEntity() })

        val result = repo.searchLocal("falcon")

        assertEquals(1, result.size)
        assertEquals("Millennium Falcon", result.first().name)
    }

    @Test
    fun `getLocalById returns product if exists`() = runTest {
        dao.insert(api.products.first().toEntity())

        val result = repo.getLocalById(1)

        assertNotNull(result)
        assertEquals("Millennium Falcon", result!!.name)
    }

    @Test
    fun `getLocalById returns null if not exists`() = runTest {
        val result = repo.getLocalById(999)
        assertNull(result)
    }

    @Test
    fun `refreshAllTypesParallel saves all products into local DB`() = runTest {
        repo.refreshAllTypesParallel()

        val saved = dao.getAll()

        assertEquals(api.products.size, saved.size)
    }

    @Test
    fun `syncByType stores only specific type in DB`() = runTest {
        repo.syncByType("set")

        val saved = dao.getAll()

        assertEquals(2, saved.size)
        assertEquals("set", saved.first().type)
    }

    @Test
    fun `getById fetches from API and caches locally`() = runTest {
        val product = repo.getById(1)

        assertEquals("Millennium Falcon", product.name)

        val cached = dao.getById(1)
        assertNotNull(cached)
    }

    @Test
    fun `getById returns cached product when API fails`() = runTest {
        dao.insert(api.products.first().toEntity())

        // break API
        api.products.clear() // ❗ only works if products = mutableList

        val product = repo.getById(1)

        assertEquals("Millennium Falcon", product.name)
    }

//    @Test
//    fun `getAll should return all products`() = runTest {
//        val result = repository.getAll()
//        assertEquals(3, result.size)
//    }
//
//    @Test
//    fun `getByType should filter by type`() = runTest {
//        val result = repository.getByType("set")
//        assertEquals(2, result.size)
//    }
//
//    @Test
//    fun `getByCategory should filter by category`() = runTest {
//        val result = repository.getByCategory("Star Wars")
//        assertEquals(2, result.size)
//        assertEquals("Millennium Falcon", result[0].name)
//    }
//
//    @Test
//    fun `search should return matching products`() = runTest {
//        val result = repository.search("falcon")
//        assertEquals(1, result.size)
//        assertEquals("Millennium Falcon", result[0].name)
//    }
//
//    @Test
//    fun `getFiltered should mix filters correctly`() = runTest {
//        val result = repository.getFiltered(
//            type = "set",
//            category = null,
//            search = "l",
//            minPrice = "100",
//            maxPrice = "900",
//            year = "2023"
//        )
//
//        assertEquals(1, result.size)
//    }
//
//    @Test
//    fun `getById should return a product`() = runTest {
//        val result = repository.getById(1)
//        assertEquals("Millennium Falcon", result.name)
//    }
//
//    @Test
//    fun `getPaged should return correct page`() = runTest {
//        val result = repository.getPaged(page = 1, limit = 2)
//        assertEquals(2, result.size)
//    }
//
//    @Test
//    fun `getFiltered - no filters returns all`() = runTest {
//        val result = repository.getFiltered()
//        assertEquals(3, result.size)
//    }
//
//    @Test
//    fun `getFiltered - filter by type`() = runTest {
//        val result = repository.getFiltered(type = "set")
//        assertEquals(2, result.size)
//        assertTrue(result.all { it.type == "set" })
//    }
//
//    @Test
//    fun `getFiltered - filter by category`() = runTest {
//        val result = repository.getFiltered(category = "Star Wars")
//        assertEquals(2, result.size) // Falcon + Pilot
//        assertTrue(result.all { it.category == "Star Wars" })
//    }
//
//    @Test
//    fun `getFiltered - filter by search text`() = runTest {
//        val result = repository.getFiltered(search = "falcon")
//        assertEquals(1, result.size)
//        assertEquals("Millennium Falcon", result.first().name)
//    }
//
//    @Test
//    fun `getFiltered - filter by minPrice`() = runTest {
//        val result = repository.getFiltered(minPrice = "200")
//        assertEquals(1, result.size)
//        assertEquals("799.99", result.first().price)
//    }
//
//    @Test
//    fun `getFiltered - filter by maxPrice`() = runTest {
//        val result = repository.getFiltered(maxPrice = "50")
//        assertEquals(1, result.size)
//        assertEquals("19.99", result.first().price)
//    }
//
//    @Test
//    fun `getFiltered - filter by year`() = runTest {
//        val result = repository.getFiltered(year = "2023")
//        assertEquals(1, result.size)
//        assertEquals(1, result.first().id)
//    }
//
//    @Test
//    fun `getFiltered - multiple filters applied together`() = runTest {
//        val result = repository.getFiltered(
//            type = "set",
//            category = "Star Wars",
//            minPrice = "500",
//            year = "2023"
//        )
//        assertEquals(1, result.size)
//        assertEquals("Millennium Falcon", result.first().name)
//    }

}
