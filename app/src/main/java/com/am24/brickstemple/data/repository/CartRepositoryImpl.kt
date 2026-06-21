package com.am24.brickstemple.data.repository

import com.am24.brickstemple.data.error.toAppException
import com.am24.brickstemple.data.local.dao.CartDao
import com.am24.brickstemple.data.local.dao.ProductDao
import com.am24.brickstemple.data.local.entities.CartItemEntity
import com.am24.brickstemple.data.remote.OrderApiService
import com.am24.brickstemple.domain.repository.CartRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

open class CartRepositoryImpl(
    private val cartDao: CartDao,
    private val productDao: ProductDao,
    private val orderApi: OrderApiService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : CartRepository {

    private val _cart = MutableStateFlow<Map<Int, Int>>(emptyMap())
    override val cart: StateFlow<Map<Int, Int>> = _cart.asStateFlow()

    private val _isUpdating = MutableStateFlow<Set<Int>>(emptySet())
    override val isUpdating = _isUpdating.asStateFlow()

    private val _isClearing = MutableStateFlow(false)
    override val isClearing: StateFlow<Boolean> = _isClearing.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading = _isLoading.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    override val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    private val pendingJobs = mutableMapOf<Int, Job>()

    override suspend fun checkout(): Int? = withContext(dispatcher) {
        try {
            val itemsDb = cartDao.getAll()
            if (itemsDb.isEmpty()) return@withContext null

            val apiItems = itemsDb.map {
                OrderApiService.CreateOrderItemRequest(
                    productId = it.productId,
                    quantity = it.quantity
                )
            }

            val totalPrice = itemsDb.sumOf { item ->
                val product = productDao.getById(item.productId)
                    ?: return@withContext null
                product.price * item.quantity
            }

            val response = orderApi.checkout(
                items = apiItems,
                totalPrice = totalPrice
            )

            cartDao.clear()
            _cart.value = emptyMap()

            return@withContext response.id
        } catch (e: Exception) {
            throw e.toAppException("Failed to checkout cart.")
        }
    }

    override suspend fun refresh() = withContext(dispatcher) {
        _isLoading.value = true
        try {
            val list = cartDao.getAll()
            _cart.value = list.associate { it.productId to it.quantity }
            _isLoaded.value = true
        } catch (e: Exception) {
            throw e.toAppException("Failed to load cart.")
        } finally {
            _isLoading.value = false
        }
    }

    private suspend fun getCurrentItem(productId: Int) = try {
        cartDao.getByProductId(productId)
    } catch (e: Exception) {
        throw e.toAppException("Failed to load cart item.")
    }

    private suspend fun updateCartState() {
        try {
            val list = cartDao.getAll()
            _cart.value = list.associate { it.productId to it.quantity }
        } catch (e: Exception) {
            throw e.toAppException("Failed to update cart state.")
        }
    }

    private suspend fun withUpdatingFlag(productId: Int, block: suspend () -> Unit) {
        _isUpdating.value += productId
        try {
            block()
        } finally {
            _isUpdating.value -= productId
        }
    }

    override suspend fun add(productId: Int) = withContext(dispatcher) {
        pendingJobs[productId]?.cancel()

        val job = currentCoroutineContext().job
        pendingJobs[productId] = job

        try {
            delay(200)
            performAdd(productId)
        } finally {
            if (pendingJobs[productId] == job) {
                pendingJobs.remove(productId)
            }
        }
    }

    suspend fun performAdd(productId: Int) = withContext(dispatcher) {
        withUpdatingFlag(productId) {
            try {
                val current = getCurrentItem(productId)

                if (current == null) {
                    cartDao.insert(CartItemEntity(productId = productId, quantity = 1))
                } else {
                    cartDao.updateQuantity(current.id, current.quantity + 1)
                }

                updateCartState()
            } catch (e: Exception) {
                throw e.toAppException("Failed to add cart item.")
            }
        }
    }

    override suspend fun toggle(productId: Int) = withContext(dispatcher) {
        withUpdatingFlag(productId) {
            try {
                val current = getCurrentItem(productId)

                if (current == null) {
                    cartDao.insert(CartItemEntity(productId = productId, quantity = 1))
                } else {
                    cartDao.deleteById(current.id)
                }

                updateCartState()
            } catch (e: Exception) {
                throw e.toAppException("Failed to update cart item.")
            }
        }
    }


    override suspend fun updateQuantity(productId: Int, newQuantity: Int) =
        withContext(dispatcher) {

            _isUpdating.value += productId

            try {
                val entity = cartDao.getByProductId(productId) ?: return@withContext

                if (newQuantity <= 0) {
                    cartDao.deleteById(entity.id)
                } else {
                    cartDao.updateQuantity(entity.id, newQuantity)
                }

                val list = cartDao.getAll()
                _cart.value = list.associate { it.productId to it.quantity }

            } catch (e: Exception) {
                throw e.toAppException("Failed to update cart quantity.")
            } finally {
                _isUpdating.value -= productId
            }
        }

    override suspend fun removeCompletely(productId: Int) = withContext(dispatcher) {
        _isUpdating.value += productId

        try {
            val entity = cartDao.getByProductId(productId) ?: return@withContext

            cartDao.deleteById(entity.id)

            val list = cartDao.getAll()
            _cart.value = list.associate { it.productId to it.quantity }

        } catch (e: Exception) {
            throw e.toAppException("Failed to remove cart item.")
        } finally {
            _isUpdating.value -= productId
        }
    }

    override suspend fun clearCart() = withContext(dispatcher) {
        _isClearing.value = true
        try {
            cartDao.clear()
            _cart.value = emptyMap()
        } catch (e: Exception) {
            throw e.toAppException("Failed to clear cart.")
        } finally {
            _isClearing.value = false
        }
    }

    override fun clearLocal() {
        _cart.value = emptyMap()
        _isUpdating.value = emptySet()
        _isClearing.value = false
        _isLoading.value = false
        _isLoaded.value = false
    }
}
