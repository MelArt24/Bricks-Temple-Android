package com.am24.brickstemple.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.am24.brickstemple.data.local.dao.CartDao
import com.am24.brickstemple.data.local.dao.ProductDao
import com.am24.brickstemple.data.local.entities.ProductEntity
import com.am24.brickstemple.data.local.entities.CartItemEntity

@Database(
    entities = [
        ProductEntity::class,
        CartItemEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun cartDao(): CartDao
}
