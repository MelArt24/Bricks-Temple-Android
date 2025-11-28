package com.am24.brickstemple.data.fakes

import com.am24.brickstemple.data.local.dao.ProductDao
import com.am24.brickstemple.data.local.entities.ProductEntity

class FakeProductDao : ProductDao {

    private val storage = mutableListOf<ProductEntity>()

    override suspend fun getAll(): List<ProductEntity> =
        storage.toList()

    override suspend fun getByType(type: String): List<ProductEntity> =
        storage.filter { it.type == type }

    override suspend fun getById(id: Int): ProductEntity? =
        storage.firstOrNull { it.id == id }

    override suspend fun insertAll(items: List<ProductEntity>) {
        items.forEach { insert(it) }
    }

    override suspend fun insert(item: ProductEntity) {
        storage.removeAll { it.id == item.id }
        storage += item
    }

    override suspend fun clear() {
        storage.clear()
    }

    override suspend fun getByIds(ids: List<Int>): List<ProductEntity> =
        storage.filter { it.id in ids }
}