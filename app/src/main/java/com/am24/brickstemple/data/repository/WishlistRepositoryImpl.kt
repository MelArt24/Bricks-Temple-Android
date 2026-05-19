package com.am24.brickstemple.data.repository

import com.am24.brickstemple.data.remote.WishlistApiService
import com.am24.brickstemple.data.remote.dto.WishlistItemDto
import com.am24.brickstemple.domain.repository.WishlistRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

open class WishlistRepositoryImpl(
    private val api: WishlistApiService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : WishlistRepository {

    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    val _wishlist = MutableStateFlow<Map<Int, Int>>(emptyMap())
    override val wishlist: StateFlow<Map<Int, Int>> = _wishlist.asStateFlow()

    val _items = MutableStateFlow<List<WishlistItemDto>>(emptyList())
    override val items: StateFlow<List<WishlistItemDto>> = _items.asStateFlow()

    private val _isUpdating = MutableStateFlow<Set<Int>>(emptySet())
    override val isUpdating: StateFlow<Set<Int>> = _isUpdating.asStateFlow()

    private val _isClearing = MutableStateFlow(false)
    override val isClearing: StateFlow<Boolean> = _isClearing.asStateFlow()

    private val pendingJobs = mutableMapOf<Int, Job>()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    override val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    override suspend fun refresh() = withContext(dispatcher) {
        _isLoading.value = true
        try {
            val response = api.getWishlist()

            _wishlist.value = response?.items?.associate { it.productId to it.id!! } ?: emptyMap()
            _items.value = response?.items ?: emptyList()

            _isLoaded.value = true
        } finally {
            _isLoading.value = false
        }
    }

    override suspend fun removeCompletely(productId: Int) = withContext(dispatcher) {
        val id = _wishlist.value[productId] ?: return@withContext
        api.removeItem(id)
        refresh()
    }

    override suspend fun removeOne(productId: Int) = withContext(dispatcher) {
        val id = _wishlist.value[productId] ?: return@withContext
        api.removeOneItem(id)
        refresh()
    }

    override fun toggle(productId: Int) {
        pendingJobs[productId]?.cancel()

        val job = scope.launch {
            delay(200)

            performToggle(productId)

            pendingJobs.remove(productId)

// I let this code to be commented and not deleted
//            if (pendingJobs.isEmpty()) {
//                refresh()
//            }
        }

        pendingJobs[productId] = job
    }

    private suspend fun performToggle(productId: Int) = withContext(dispatcher) {
        _isUpdating.value += productId

        try {
            val current = _wishlist.value

            if (productId in current.keys) {
                val itemId = current[productId]!!
                _wishlist.value = current - productId
                api.removeItem(itemId)
            } else {
                _wishlist.value = current + (productId to -1)
                api.addItem(productId)
            }

        } finally {
            _isUpdating.value -= productId
        }
    }

    override fun lastFetchedItem(productId: Int): WishlistItemDto? =
        _items.value.firstOrNull { it.productId == productId }

    override suspend fun updateQuantity(itemId: Int, newQuantity: Int) = withContext(dispatcher) {
        api.updateQuantity(itemId, newQuantity)
        refresh()
    }

    override fun clearLocal() {
        _wishlist.value = emptyMap()
        _items.value = emptyList()
        _isUpdating.value = emptySet()
        _isClearing.value = false
        _isLoaded.value = false
    }


    override suspend fun clearWishlist() = withContext(dispatcher) {
        _isClearing.value = true

        try {
            try { api.clearWishlist() } catch (_: Exception) {}
            _wishlist.value = emptyMap()
            _items.value = emptyList()
        } finally {
            _isClearing.value = false
        }
    }

}
