package com.am24.brickstemple.data.repository

import com.am24.brickstemple.data.error.toAppException
import com.am24.brickstemple.data.local.dao.ProductDao
import com.am24.brickstemple.data.mapper.toDomain
import com.am24.brickstemple.data.remote.ProductApiService
import com.am24.brickstemple.data.remote.dto.ProductDto
import com.am24.brickstemple.domain.error.AppError
import com.am24.brickstemple.domain.error.AppException
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
        try {
            dao.getByType(type).map { it.toDomain() }
        } catch (e: Exception) {
            throw e.toLocalDataException("Failed to load cached products.")
        }
    }

    override suspend fun getCachedByIds(ids: List<Int>) = withContext(dispatcher) {
        try {
            if (ids.isEmpty()) emptyList() else dao.getByIds(ids).map { it.toDomain() }
        } catch (e: Exception) {
            throw e.toLocalDataException("Failed to load cached products.")
        }
    }

    override suspend fun searchLocal(query: String) = withContext(dispatcher) {
        try {
            dao.getAll()
                .filter { it.name.contains(query, ignoreCase = true) }
                .map { it.toDomain() }
        } catch (e: Exception) {
            throw e.toLocalDataException("Failed to search cached products.")
        }
    }

    override suspend fun getLocalById(id: Int) = withContext(dispatcher) {
        try {
            dao.getById(id)?.toDomain()
        } catch (e: Exception) {
            throw e.toLocalDataException("Failed to load cached product.")
        }
    }

    private suspend fun fetchTypeWithRetry(type: String): List<ProductDto> {
        var lastError: Exception? = null

        repeat(3) { attempt ->
            try {
                return api.getByType(type)
            } catch (e: Exception) {
                lastError = e
            }

            delay(300L)
        }

        throw lastError?.toAppException("Failed to load $type products.")
            ?: AppException(AppError.UnknownError("Failed to load $type products."))
    }

    override suspend fun refreshAllTypesParallel() = withContext(dispatcher) {
        val types = listOf("set", "minifigure", "detail", "polybag", "other")

        val allRemote = mutableListOf<ProductDto>()

        for (type in types) {
            val remote = fetchTypeWithRetry(type)

            if (remote.isNotEmpty()) {
                try {
                    dao.insertAll(remote.map { it.toEntity() })
                } catch (e: Exception) {
                    throw e.toLocalDataException("Failed to cache remote products.")
                }
                allRemote += remote
            }
        }

        return@withContext allRemote.map { it.toDomain() }
    }

    override suspend fun syncByType(type: String) =
        withContext(dispatcher) {
            val remote = fetchTypeWithRetry(type)
            if (remote.isNotEmpty()) {
                try {
                    dao.insertAll(remote.map { it.toEntity() })
                } catch (e: Exception) {
                    throw e.toLocalDataException("Failed to cache remote products.")
                }
            }
            remote.map { it.toDomain() }
        }

    override suspend fun getById(id: Int) =
        withContext(dispatcher) {
            val remote = try {
                api.getProductById(id)
            } catch (e: Exception) {
                val cached = try {
                    dao.getById(id)?.toDomain()
                } catch (localError: Exception) {
                    throw localError.toLocalDataException("Failed to load cached product fallback.")
                }

                return@withContext cached ?: throw e.toProductNotAvailableException()
            }

            try {
                dao.insert(remote.toEntity())
            } catch (e: Exception) {
                throw e.toLocalDataException("Failed to cache remote product.")
            }

            remote.toDomain()
        }

    override suspend fun getFiltered(
        type: String?,
        category: String?,
        search: String?,
        minPrice: String?,
        maxPrice: String?,
        year: String?
    ) = withContext(dispatcher) {
        try {
            api.getFiltered(type, category, search, minPrice, maxPrice, year).map { it.toDomain() }
        } catch (e: Exception) {
            throw e.toAppException("Failed to filter products.")
        }
    }

    private fun Exception.toLocalDataException(message: String): AppException {
        if (this is AppException) return this
        return AppException(AppError.LocalDataError(message), this)
    }

    private fun Exception.toProductNotAvailableException(): AppException {
        val appException = toAppException("Product not found locally or remotely.")
        return when (appException.error) {
            is AppError.UnauthorizedError,
            is AppError.NetworkError,
            is AppError.ServerError -> appException
            else -> AppException(
                AppError.NotFoundError("Product not found locally or remotely."),
                appException
            )
        }
    }
}
