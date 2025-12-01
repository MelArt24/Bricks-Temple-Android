package com.am24.brickstemple.data.repositories

import com.am24.brickstemple.data.local.dao.ProductDao
import com.am24.brickstemple.data.remote.ProductApiService
import com.am24.brickstemple.data.remote.dto.ProductDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.am24.brickstemple.data.mappers.toDto
import com.am24.brickstemple.data.mappers.toEntity
import kotlinx.coroutines.delay

open class ProductRepository(
    val api: ProductApiService,
    private val dao: ProductDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    val productDao: ProductDao
        get() = dao

    suspend fun getCachedByType(type: String): List<ProductDto> =
        dao.getByType(type).map { it.toDto() }

    suspend fun searchLocal(query: String): List<ProductDto> =
        dao.getAll()
            .filter { it.name.contains(query, ignoreCase = true) }
            .map { it.toDto() }

    suspend fun getLocalById(id: Int): ProductDto? =
        withContext(dispatcher) {
            dao.getById(id)?.toDto()
        }

    private suspend fun safeFetchType(type: String): List<ProductDto> {
        repeat(3) { attempt ->
            try {
                val result = api.getByType(type)
                if (result.isNotEmpty()) return result
            } catch (_: Exception) {
            }

            delay(300L)
        }
        return emptyList()
    }

    suspend fun refreshAllTypesParallel() = withContext(dispatcher) {
        val types = listOf("set", "minifigure", "detail", "polybag", "other")

        val allRemote = mutableListOf<ProductDto>()

        for (type in types) {
            val remote = safeFetchType(type)

            if (remote.isNotEmpty()) {
                dao.insertAll(remote.map { it.toEntity() })
                allRemote += remote
            }
        }

        return@withContext allRemote
    }

    suspend fun syncByType(type: String): List<ProductDto> =
        withContext(dispatcher) {
            val remote = safeFetchType(type)
            if (remote.isNotEmpty()) {
                dao.insertAll(remote.map { it.toEntity() })
            }
            remote
        }

    open suspend fun getById(id: Int): ProductDto =
        withContext(dispatcher) {
            try {
                val remote = api.getProductById(id)
                dao.insert(remote.toEntity())
                remote
            } catch (_: Exception) {
                dao.getById(id)?.toDto() ?: throw Exception("Product not found locally or remotely")
            }
        }
}

//    suspend fun getAll(): List<ProductDto> =
//        withContext(dispatcher) { api.getAll() }
//
//    suspend fun getByCategory(category: String): List<ProductDto> =
//        withContext(dispatcher) { api.getByCategory(category) }
//
//    suspend fun search(query: String): List<ProductDto> =
//        withContext(dispatcher) { api.search(query) }
//
//    suspend fun getFiltered(
//        type: String? = null,
//        category: String? = null,
//        search: String? = null,
//        minPrice: String? = null,
//        maxPrice: String? = null,
//        year: String? = null
//    ): List<ProductDto> =
//        withContext(dispatcher) {
//            api.getFiltered(type, category, search, minPrice, maxPrice, year)
//        }
//
//
//    suspend fun getPaged(page: Int, limit: Int): List<ProductDto> =
//        withContext(dispatcher) { api.getPaged(page, limit) }
