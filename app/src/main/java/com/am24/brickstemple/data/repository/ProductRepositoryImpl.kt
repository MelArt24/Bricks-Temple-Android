package com.am24.brickstemple.data.repository

import com.am24.brickstemple.data.local.dao.ProductDao
import com.am24.brickstemple.data.mapper.toDomain
import com.am24.brickstemple.data.remote.ProductApiService
import com.am24.brickstemple.data.remote.dto.ProductDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.am24.brickstemple.data.mapper.toEntity
import com.am24.brickstemple.domain.repository.ProductRepository
import kotlinx.coroutines.delay

open class ProductRepositoryImpl(
    val api: ProductApiService,
    private val dao: ProductDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ProductRepository {

    override suspend fun getCachedByType(type: String) = withContext(dispatcher) {
        dao.getByType(type).map { it.toDomain() }
    }

    override suspend fun getCachedByIds(ids: List<Int>) = withContext(dispatcher) {
        if (ids.isEmpty()) emptyList() else dao.getByIds(ids).map { it.toDomain() }
    }

    override suspend fun searchLocal(query: String) = withContext(dispatcher) {
        dao.getAll()
            .filter { it.name.contains(query, ignoreCase = true) }
            .map { it.toDomain() }
    }

    override suspend fun getLocalById(id: Int) = withContext(dispatcher) {
            dao.getById(id)?.toDomain()
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

    override suspend fun refreshAllTypesParallel() = withContext(dispatcher) {
        val types = listOf("set", "minifigure", "detail", "polybag", "other")

        val allRemote = mutableListOf<ProductDto>()

        for (type in types) {
            val remote = safeFetchType(type)

            if (remote.isNotEmpty()) {
                dao.insertAll(remote.map { it.toEntity() })
                allRemote += remote
            }
        }

        return@withContext allRemote.map { it.toDomain() }
    }

    override suspend fun syncByType(type: String) =
        withContext(dispatcher) {
            val remote = safeFetchType(type)
            if (remote.isNotEmpty()) {
                dao.insertAll(remote.map { it.toEntity() })
            }
            remote.map { it.toDomain() }
        }

    override suspend fun getById(id: Int) =
        withContext(dispatcher) {
            try {
                val remote = api.getProductById(id)
                dao.insert(remote.toEntity())
                remote.toDomain()
            } catch (_: Exception) {
                dao.getById(id)?.toDomain() ?: throw Exception("Product not found locally or remotely")
            }
        }

    override suspend fun getFiltered(
        type: String?,
        category: String?,
        search: String?,
        minPrice: String?,
        maxPrice: String?,
        year: String?
    ) = withContext(dispatcher) {
        api.getFiltered(type, category, search, minPrice, maxPrice, year).map { it.toDomain() }
    }
}
