package com.am24.brickstemple.data.local.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.am24.brickstemple.data.local.dao.ProductDao
import com.am24.brickstemple.data.local.entities.CartItemEntity
import com.am24.brickstemple.data.local.entities.ProductEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun setUp() {
        context.deleteDatabase(TEST_DB)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(TEST_DB)
    }

    @Test
    fun migrationFrom1To2PreservesProductsAndCreatesCartItems() = runBlocking {
        val versionOneDb = Room.databaseBuilder(
            context,
            VersionOneDatabase::class.java,
            TEST_DB
        ).build()

        versionOneDb.productDao().insert(productEntity)
        versionOneDb.close()

        val migratedDb = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            TEST_DB
        )
            .addMigrations(DatabaseMigrations.MIGRATION_1_2)
            .build()

        val migratedProduct = migratedDb.productDao().getById(productEntity.id)
        assertEquals(productEntity, migratedProduct)

        migratedDb.cartDao().insert(
            CartItemEntity(
                productId = productEntity.id,
                quantity = 2
            )
        )
        assertEquals(1, migratedDb.cartDao().getAll().size)

        migratedDb.openHelper.readableDatabase.query(
            "PRAGMA index_list(`cart_items`)"
        ).use { cursor ->
            var hasProductIdIndex = false
            while (cursor.moveToNext()) {
                if (cursor.getString(cursor.getColumnIndexOrThrow("name")) == "index_cart_items_productId") {
                    hasProductIdIndex = true
                }
            }
            assertTrue(hasProductIdIndex)
        }

        migratedDb.close()
    }

    @Database(
        entities = [ProductEntity::class],
        version = 1,
        exportSchema = false
    )
    abstract class VersionOneDatabase : RoomDatabase() {
        abstract fun productDao(): ProductDao
    }

    private companion object {
        const val TEST_DB = "migration-test.db"

        val productEntity = ProductEntity(
            id = 1,
            name = "Millennium Falcon",
            category = "Star Wars",
            number = "75192",
            details = 7541,
            minifigures = 8,
            age = "16+",
            year = "2017",
            size = "Large",
            condition = "New",
            price = 849.99,
            createdAt = "2024-01-01",
            image = "falcon.png",
            description = "Large collector set",
            type = "set",
            keywords = "falcon star wars",
            isAvailable = true
        )
    }
}
