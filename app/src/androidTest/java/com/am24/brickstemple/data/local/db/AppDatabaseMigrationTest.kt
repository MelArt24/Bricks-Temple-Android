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
import org.junit.Assert.assertFalse
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
            .addMigrations(*DatabaseMigrations.ALL)
            .build()

        val migratedProduct = migratedDb.productDao().getById(productEntity.id)
        assertEquals(productEntity, migratedProduct)

        assertCartItemsTableExists(migratedDb)
        assertCartItemsColumnsMatchVersionTwoSchema(migratedDb)
        assertCartItemsProductIdIndexMatchesVersionTwoSchema(migratedDb)

        migratedDb.cartDao().insert(
            CartItemEntity(
                productId = productEntity.id,
                quantity = 2
            )
        )
        assertEquals(1, migratedDb.cartDao().getAll().size)

        migratedDb.close()
    }

    @Test
    fun currentVersionDatabaseOpensSuccessfully() = runBlocking {
        val currentDb = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            TEST_DB
        )
            .addMigrations(*DatabaseMigrations.ALL)
            .build()

        currentDb.productDao().insert(productEntity)
        assertEquals(productEntity, currentDb.productDao().getById(productEntity.id))

        currentDb.close()
    }

    private fun assertCartItemsTableExists(db: AppDatabase) {
        db.openHelper.readableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'cart_items'"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("cart_items", cursor.getString(0))
        }
    }

    private fun assertCartItemsColumnsMatchVersionTwoSchema(db: AppDatabase) {
        val columns = mutableMapOf<String, TableColumn>()
        db.openHelper.readableDatabase.query("PRAGMA table_info(`cart_items`)").use { cursor ->
            while (cursor.moveToNext()) {
                columns[cursor.getString(cursor.getColumnIndexOrThrow("name"))] = TableColumn(
                    type = cursor.getString(cursor.getColumnIndexOrThrow("type")),
                    notNull = cursor.getInt(cursor.getColumnIndexOrThrow("notnull")) == 1,
                    primaryKeyPosition = cursor.getInt(cursor.getColumnIndexOrThrow("pk"))
                )
            }
        }

        assertEquals(
            mapOf(
                "id" to TableColumn(type = "INTEGER", notNull = true, primaryKeyPosition = 1),
                "productId" to TableColumn(type = "INTEGER", notNull = true, primaryKeyPosition = 0),
                "quantity" to TableColumn(type = "INTEGER", notNull = true, primaryKeyPosition = 0)
            ),
            columns
        )

        db.openHelper.readableDatabase.query("SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'cart_items'")
            .use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertTrue(cursor.getString(0).contains("AUTOINCREMENT"))
            }
    }

    private fun assertCartItemsProductIdIndexMatchesVersionTwoSchema(db: AppDatabase) {
        var indexFound = false
        var indexIsUnique = true

        db.openHelper.readableDatabase.query("PRAGMA index_list(`cart_items`)").use { cursor ->
            while (cursor.moveToNext()) {
                if (cursor.getString(cursor.getColumnIndexOrThrow("name")) == "index_cart_items_productId") {
                    indexFound = true
                    indexIsUnique = cursor.getInt(cursor.getColumnIndexOrThrow("unique")) == 1
                }
            }
        }

        assertTrue(indexFound)
        assertFalse(indexIsUnique)

        db.openHelper.readableDatabase.query("PRAGMA index_info(`index_cart_items_productId`)").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("productId", cursor.getString(cursor.getColumnIndexOrThrow("name")))
            assertFalse(cursor.moveToNext())
        }
    }

    @Database(
        entities = [ProductEntity::class],
        version = 1,
        exportSchema = false
    )
    abstract class VersionOneDatabase : RoomDatabase() {
        abstract fun productDao(): ProductDao
    }

    private data class TableColumn(
        val type: String,
        val notNull: Boolean,
        val primaryKeyPosition: Int
    )

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
