package com.am24.brickstemple.data.repositories

import com.am24.brickstemple.data.local.dao.CartDao
import com.am24.brickstemple.data.local.dao.ProductDao
import com.am24.brickstemple.data.local.entities.CartItemEntity
import com.am24.brickstemple.data.remote.OrderApiService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

open class CartRepository(
    private val cartDao: CartDao,
    private val productDao: ProductDao,
    private val orderApi: OrderApiService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    private val _cart = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val cart = _cart.asStateFlow()

    private val _isUpdating = MutableStateFlow<Set<Int>>(emptySet())
    val isUpdating = _isUpdating.asStateFlow()

    private val _isClearing = MutableStateFlow(false)
    val isClearing = _isClearing.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded = _isLoaded.asStateFlow()

    private val pendingJobs = mutableMapOf<Int, Job>()

    open suspend fun checkout(): Int? = withContext(dispatcher) {

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
    }

    open suspend fun refresh() = withContext(dispatcher) {
        _isLoading.value = true
        try {
            val list = cartDao.getAll()
            _cart.value = list.associate { it.productId to it.quantity }
            _isLoaded.value = true
        } finally {
            _isLoading.value = false
        }
    }

    private suspend fun getCurrentItem(productId: Int) =
        cartDao.getByProductId(productId)

    private suspend fun updateCartState() {
        val list = cartDao.getAll()
        _cart.value = list.associate { it.productId to it.quantity }
    }

    private suspend fun withUpdatingFlag(productId: Int, block: suspend () -> Unit) {
        _isUpdating.value += productId
        try {
            block()
        } finally {
            _isUpdating.value -= productId
        }
    }

    fun add(productId: Int) {
        pendingJobs[productId]?.cancel()

        val job = scope.launch {
            delay(200)
            performAdd(productId)
            pendingJobs.remove(productId)
        }
        pendingJobs[productId] = job
    }

    suspend fun performAdd(productId: Int) = withContext(dispatcher) {
        withUpdatingFlag(productId) {
            val current = getCurrentItem(productId)

            if (current == null) {
                cartDao.insert(CartItemEntity(productId = productId, quantity = 1))
            } else {
                cartDao.updateQuantity(current.id, current.quantity + 1)
            }

            updateCartState()
        }
    }

    open suspend fun toggle(productId: Int) = withContext(dispatcher) {
        withUpdatingFlag(productId) {
            val current = getCurrentItem(productId)

            if (current == null) {
                cartDao.insert(CartItemEntity(productId = productId, quantity = 1))
            } else {
                cartDao.deleteById(current.id)
            }

            updateCartState()
        }
    }


    open suspend fun updateQuantity(productId: Int, newQuantity: Int) =
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

            } finally {
                _isUpdating.value -= productId
            }
        }

    open suspend fun removeCompletely(productId: Int) = withContext(dispatcher) {
        _isUpdating.value += productId

        try {
            val entity = cartDao.getByProductId(productId) ?: return@withContext

            cartDao.deleteById(entity.id)

            val list = cartDao.getAll()
            _cart.value = list.associate { it.productId to it.quantity }

        } finally {
            _isUpdating.value -= productId
        }
    }

    open suspend fun clearCart() = withContext(dispatcher) {
        _isClearing.value = true
        try {
            cartDao.clear()
            _cart.value = emptyMap()
        } finally {
            _isClearing.value = false
        }
    }

    fun clearLocal() {
        _cart.value = emptyMap()
        _isUpdating.value = emptySet()
        _isClearing.value = false
        _isLoading.value = false
        _isLoaded.value = false
    }
}
