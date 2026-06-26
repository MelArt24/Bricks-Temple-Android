package com.am24.brickstemple.data.repository

import com.am24.brickstemple.data.fakes.FakeProductApiService
import com.am24.brickstemple.data.fakes.FakeProductDao
import com.am24.brickstemple.data.mapper.toEntity
import com.am24.brickstemple.domain.error.AppException
import io.ktor.client.HttpClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductRepositoryTest {

    private lateinit var api: FakeProductApiService
    private lateinit var dao: FakeProductDao
    private lateinit var repo: ProductRepositoryImpl

    @Before
    fun setup() {
        api = FakeProductApiService(HttpClient())
        dao = FakeProductDao()
        repo = ProductRepositoryImpl(api, dao)
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

        api.failProductByIdRequests = true

        val product = repo.getById(1)

        assertEquals("Millennium Falcon", product.name)
    }

    @Test
    fun `syncByType throws app exception when remote request fails`() = runTest {
        api.failTypeRequests = true

        try {
            repo.syncByType("set")
            assertTrue("Expected AppException", false)
        } catch (e: AppException) {
            assertTrue(e.message?.isNotBlank() == true)
        }
    }

    @Test
    fun `getById throws app exception when remote and cache fail`() = runTest {
        api.failProductByIdRequests = true

        try {
            repo.getById(1)
            assertTrue("Expected AppException", false)
        } catch (e: AppException) {
            assertEquals("Product not found locally or remotely.", e.message)
        }
    }

}
