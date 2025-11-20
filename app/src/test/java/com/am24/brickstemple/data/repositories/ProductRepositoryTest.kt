package com.am24.brickstemple.data.repositories

import io.ktor.client.HttpClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductRepositoryTest {

    private lateinit var repository: ProductRepository
    private lateinit var fakeApi: FakeProductApiService

    @Before
    fun setup() {
        fakeApi = FakeProductApiService(HttpClient())
        repository = ProductRepository(fakeApi)
    }

    @Test
    fun `getAll should return all products`() = runTest {
        val result = repository.getAll()
        assertEquals(3, result.size)
    }

    @Test
    fun `getByType should filter by type`() = runTest {
        val result = repository.getByType("set")
        assertEquals(2, result.size)
    }

    @Test
    fun `getByCategory should filter by category`() = runTest {
        val result = repository.getByCategory("Star Wars")
        assertEquals(2, result.size)
        assertEquals("Millennium Falcon", result[0].name)
    }

    @Test
    fun `search should return matching products`() = runTest {
        val result = repository.search("falcon")
        assertEquals(1, result.size)
        assertEquals("Millennium Falcon", result[0].name)
    }

    @Test
    fun `getFiltered should mix filters correctly`() = runTest {
        val result = repository.getFiltered(
            type = "set",
            category = null,
            search = "l",
            minPrice = "100",
            maxPrice = "900",
            year = "2023"
        )

        assertEquals(1, result.size)
    }

    @Test
    fun `getById should return a product`() = runTest {
        val result = repository.getById(1)
        assertEquals("Millennium Falcon", result.name)
    }

    @Test
    fun `getPaged should return correct page`() = runTest {
        val result = repository.getPaged(page = 1, limit = 2)
        assertEquals(2, result.size)
    }

    @Test
    fun `getFiltered - no filters returns all`() = runTest {
        val result = repository.getFiltered()
        assertEquals(3, result.size)
    }

    @Test
    fun `getFiltered - filter by type`() = runTest {
        val result = repository.getFiltered(type = "set")
        assertEquals(2, result.size)
        assertTrue(result.all { it.type == "set" })
    }

    @Test
    fun `getFiltered - filter by category`() = runTest {
        val result = repository.getFiltered(category = "Star Wars")
        assertEquals(2, result.size) // Falcon + Pilot
        assertTrue(result.all { it.category == "Star Wars" })
    }

    @Test
    fun `getFiltered - filter by search text`() = runTest {
        val result = repository.getFiltered(search = "falcon")
        assertEquals(1, result.size)
        assertEquals("Millennium Falcon", result.first().name)
    }

    @Test
    fun `getFiltered - filter by minPrice`() = runTest {
        val result = repository.getFiltered(minPrice = "200")
        assertEquals(1, result.size)
        assertEquals("799.99", result.first().price)
    }

    @Test
    fun `getFiltered - filter by maxPrice`() = runTest {
        val result = repository.getFiltered(maxPrice = "50")
        assertEquals(1, result.size)
        assertEquals("19.99", result.first().price)
    }

    @Test
    fun `getFiltered - filter by year`() = runTest {
        val result = repository.getFiltered(year = "2023")
        assertEquals(1, result.size)
        assertEquals(1, result.first().id)
    }

    @Test
    fun `getFiltered - multiple filters applied together`() = runTest {
        val result = repository.getFiltered(
            type = "set",
            category = "Star Wars",
            minPrice = "500",
            year = "2023"
        )
        assertEquals(1, result.size)
        assertEquals("Millennium Falcon", result.first().name)
    }

}
