package com.am24.brickstemple.data.repositories

import com.am24.brickstemple.data.local.dao.CartDao
import com.am24.brickstemple.data.local.dao.ProductDao
import com.am24.brickstemple.data.local.entities.CartItemEntity
import com.am24.brickstemple.data.local.entities.ProductEntity
import com.am24.brickstemple.data.remote.OrderApiService
import io.mockk.*
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class CartRepositoryTest {

    private lateinit var cartDao: CartDao
    private lateinit var productDao: ProductDao
    private lateinit var api: OrderApiService
    private lateinit var repo: CartRepository

    @Before
    fun setup() {
        cartDao = mockk(relaxed = true)
        productDao = mockk(relaxed = true)
        api = mockk(relaxed = true)

        repo = CartRepository(
            cartDao = cartDao,
            productDao = productDao,
            orderApi = api,
            dispatcher = Dispatchers.Unconfined
        )
    }

    @Test
    fun `refresh loads items`() = runBlocking {
        coEvery { cartDao.getAll() } returns listOf(
            CartItemEntity(id = 1, productId = 10, quantity = 2)
        )

        repo.refresh()

        assertEquals(mapOf(10 to 2), repo.cart.value)
    }

    @Test
    fun `add inserts new item if not exists`() = runBlocking {
        coEvery { cartDao.getByProductId(10) } returns null
        coEvery { cartDao.insert(any()) } returns 1

        coEvery { cartDao.getAll() } returns listOf(
            CartItemEntity(id = 1, productId = 10, quantity = 1)
        )

        repo.add(10)
        repo.refresh()

        assertEquals(mapOf(10 to 1), repo.cart.value)
    }

    @Test
    fun `add increments quantity if exists`() = runBlocking {
        coEvery { cartDao.getByProductId(10) } returns
                CartItemEntity(id = 1, productId = 10, quantity = 2)

        coEvery { cartDao.getAll() } returns listOf(
            CartItemEntity(id = 1, productId = 10, quantity = 3)
        )

        repo.performAdd(10)

        assertEquals(mapOf(10 to 3), repo.cart.value)
    }

    @Test
    fun `toggle inserts when not exists`() = runBlocking {
        coEvery { cartDao.getByProductId(5) } returns null
        coEvery { cartDao.getAll() } returns listOf(
            CartItemEntity(id = 1, productId = 5, quantity = 1)
        )

        repo.toggle(5)

        assertEquals(mapOf(5 to 1), repo.cart.value)
    }

    @Test
    fun `toggle deletes when exists`() = runBlocking {
        coEvery { cartDao.getByProductId(5) } returns
                CartItemEntity(id = 1, productId = 5, quantity = 1)

        coEvery { cartDao.getAll() } returns emptyList()

        repo.toggle(5)

        assertEquals(emptyMap<Int, Int>(), repo.cart.value)
    }

    @Test
    fun `updateQuantity reduces quantity`() = runBlocking {
        coEvery { cartDao.getByProductId(7) } returns
                CartItemEntity(id = 2, productId = 7, quantity = 5)

        coEvery { cartDao.getAll() } returns listOf(
            CartItemEntity(id = 2, productId = 7, quantity = 3)
        )

        repo.updateQuantity(7, 3)

        assertEquals(mapOf(7 to 3), repo.cart.value)
    }

    @Test
    fun `updateQuantity deletes when less than equal to 0`() = runBlocking {
        coEvery { cartDao.getByProductId(7) } returns
                CartItemEntity(id = 2, productId = 7, quantity = 1)

        coEvery { cartDao.getAll() } returns emptyList()

        repo.updateQuantity(7, 0)

        assertEquals(emptyMap<Int, Int>(), repo.cart.value)
    }

    @Test
    fun `removeCompletely deletes item`() = runBlocking {
        coEvery { cartDao.getByProductId(9) } returns
                CartItemEntity(id = 3, productId = 9, quantity = 5)

        coEvery { cartDao.getAll() } returns emptyList()

        repo.removeCompletely(9)

        assertEquals(emptyMap<Int, Int>(), repo.cart.value)
    }

    @Test
    fun `clearCart empties DB and state`() = runBlocking {
        coEvery { cartDao.getAll() } returns emptyList()

        repo.clearCart()

        assertEquals(emptyMap<Int, Int>(), repo.cart.value)
    }

    @Test
    fun `checkout returns null when cart is empty`() = runBlocking {
        coEvery { cartDao.getAll() } returns emptyList()

        val result = repo.checkout()

        assertEquals(null, result)
    }

    @Test
    fun `checkout returns null if product missing`() = runBlocking {
        coEvery { cartDao.getAll() } returns listOf(
            CartItemEntity(id = 1, productId = 5, quantity = 1)
        )

        coEvery { productDao.getById(5) } returns null

        val result = repo.checkout()

        assertEquals(null, result)
    }

    @Test
    fun `checkout sends request and clears cart`() = runBlocking {
        coEvery { cartDao.getAll() } returns listOf(
            CartItemEntity(id = 1, productId = 5, quantity = 2)
        )

        coEvery { productDao.getById(5) } returns ProductEntity(
            id = 5,
            name = "Test",
            category = null,
            number = null,
            details = null,
            minifigures = null,
            age = null,
            year = null,
            size = null,
            condition = null,
            price = 100.0,
            createdAt = null,
            image = null,
            description = null,
            type = "set",
            keywords = null,
            isAvailable = true
        )

        coEvery { api.checkout(any(), any()) } returns
                OrderApiService.CreatedOrderResponse("OK", 123)

        val result = repo.checkout()

        assertEquals(123, result)
        assertEquals(emptyMap<Int, Int>(), repo.cart.value)
    }
}
