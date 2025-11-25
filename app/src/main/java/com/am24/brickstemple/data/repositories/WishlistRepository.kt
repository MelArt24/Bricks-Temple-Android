package com.am24.brickstemple.data.repositories

import com.am24.brickstemple.data.remote.WishlistApiService
import com.am24.brickstemple.data.remote.dto.WishlistItemDto
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

open class WishlistRepository(
    private val api: WishlistApiService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    private val _wishlist = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val wishlist = _wishlist.asStateFlow()

    private val _items = MutableStateFlow<List<WishlistItemDto>>(emptyList())
    val items = _items.asStateFlow()

    private val _isUpdating = MutableStateFlow<Set<Int>>(emptySet())
    val isUpdating = _isUpdating.asStateFlow()

    private val pendingJobs = mutableMapOf<Int, Job>()

    open suspend fun refresh() = withContext(dispatcher) {
        val response = api.getWishlist()

        _wishlist.value = response?.items?.associate { it.productId to it.id!! } ?: emptyMap()
        _items.value = response?.items ?: emptyList()
    }

    suspend fun removeCompletely(productId: Int) = withContext(dispatcher) {
        val id = _wishlist.value[productId] ?: return@withContext
        api.removeItem(id)
        refresh()
    }

    suspend fun removeOne(productId: Int) = withContext(dispatcher) {
        val id = _wishlist.value[productId] ?: return@withContext
        api.removeOneItem(id)
        refresh()
    }

    fun toggle(productId: Int) {
        pendingJobs[productId]?.cancel()

        val job = scope.launch {
            delay(200)

            performToggle(productId)

            pendingJobs.remove(productId)

            if (pendingJobs.isEmpty()) {
                refresh()
            }
        }

        pendingJobs[productId] = job
    }

    private suspend fun performToggle(productId: Int) = withContext(dispatcher) {
        _isUpdating.value += productId

        try {
            val current = _wishlist.value

            if (productId in current.keys) {
                val itemId = current[productId]!!
                api.removeItem(itemId)
                _wishlist.value = current - productId
            } else {
                api.addItem(productId)
            }

        } finally {
            _isUpdating.value -= productId
        }
    }

    fun lastFetchedItem(productId: Int): WishlistItemDto? =
        _items.value.firstOrNull { it.productId == productId }

    open suspend fun updateQuantity(itemId: Int, newQuantity: Int) = withContext(dispatcher) {
        api.updateQuantity(itemId, newQuantity)
        refresh()
    }

    fun clearLocal() {
        _wishlist.value = emptyMap()
    }
}
