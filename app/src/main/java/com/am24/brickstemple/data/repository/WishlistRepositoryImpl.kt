package com.am24.brickstemple.data.repository

import com.am24.brickstemple.data.error.toAppException
import com.am24.brickstemple.data.mapper.toDomain
import com.am24.brickstemple.data.remote.WishlistApiService
import com.am24.brickstemple.domain.model.WishlistItem
import com.am24.brickstemple.domain.repository.WishlistRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

open class WishlistRepositoryImpl(
    private val api: WishlistApiService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : WishlistRepository {

    private val scope = CoroutineScope(dispatcher + SupervisorJob())
    private val mutationMutex = Mutex()

    val _wishlist = MutableStateFlow<Map<Int, Int>>(emptyMap())
    override val wishlist: StateFlow<Map<Int, Int>> = _wishlist.asStateFlow()

    val _items = MutableStateFlow<List<WishlistItem>>(emptyList())
    override val items: StateFlow<List<WishlistItem>> = _items.asStateFlow()

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
        mutationMutex.withLock {
            refreshFromRemote()
        }
    }

    private suspend fun refreshFromRemote() {
        _isLoading.value = true
        try {
            val response = api.getWishlist()

            _wishlist.value = response.items.associate { it.productId to it.id!! }
            _items.value = response.items.map { it.toDomain() }

            _isLoaded.value = true
        } catch (e: Exception) {
            throw e.toAppException("Failed to refresh wishlist.")
        } finally {
            _isLoading.value = false
        }
    }

    override suspend fun removeCompletely(productId: Int) = withContext(dispatcher) {
        mutationMutex.withLock {
            val id = _wishlist.value[productId] ?: return@withLock
            try {
                api.removeItem(id)
                refreshFromRemote()
            } catch (e: Exception) {
                throw e.toAppException("Failed to remove wishlist item.")
            }
        }
    }

    override suspend fun removeOne(productId: Int) = withContext(dispatcher) {
        mutationMutex.withLock {
            val id = _wishlist.value[productId] ?: return@withLock
            try {
                val item = _items.value.firstOrNull { it.productId == productId }
                api.removeOneItem(id)
                if (item != null && item.quantity > 1) {
                    _items.value = _items.value.map {
                        if (it.productId == productId) it.copy(quantity = it.quantity - 1) else it
                    }
                } else {
                    refreshFromRemote()
                }
            } catch (e: Exception) {
                throw e.toAppException("Failed to update wishlist item.")
            }
        }
    }

    override fun toggle(productId: Int) {
        pendingJobs[productId]?.cancel()

        val job = scope.launch {
            try {
                delay(200)

                performToggle(productId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _isUpdating.value -= productId
            }

            pendingJobs.remove(productId)

// I let this code to be commented and not deleted
//            if (pendingJobs.isEmpty()) {
//                refresh()
//            }
        }

        pendingJobs[productId] = job
    }

    private suspend fun performToggle(productId: Int) = withContext(dispatcher) {
        mutationMutex.withLock {
            _isUpdating.value += productId

            try {
                val current = _wishlist.value

                if (productId in current.keys) {
                    val itemId = current[productId]!!
                    _wishlist.value = current - productId
                    try {
                        api.removeItem(itemId)
                        refreshFromRemote()
                    } catch (e: Exception) {
                        _wishlist.value = current
                        throw e.toAppException("Failed to remove wishlist item.")
                    }
                } else {
                    _wishlist.value = current + (productId to -1)
                    try {
                        api.addItem(productId)
                        refreshFromRemote()
                    } catch (e: Exception) {
                        _wishlist.value = current
                        throw e.toAppException("Failed to add wishlist item.")
                    }
                }

            } finally {
                _isUpdating.value -= productId
            }
        }
    }

    override fun lastFetchedItem(productId: Int): WishlistItem? =
        _items.value.firstOrNull { it.productId == productId }

    override suspend fun updateQuantity(itemId: Int, newQuantity: Int) = withContext(dispatcher) {
        mutationMutex.withLock {
            try {
                api.updateQuantity(itemId, newQuantity)
                if (_items.value.any { it.id == itemId }) {
                    _items.value = _items.value.map {
                        if (it.id == itemId) it.copy(quantity = newQuantity) else it
                    }
                } else {
                    refreshFromRemote()
                }
            } catch (e: Exception) {
                throw e.toAppException("Failed to update wishlist quantity.")
            }
        }
    }

    override fun clearLocal() {
        _wishlist.value = emptyMap()
        _items.value = emptyList()
        _isUpdating.value = emptySet()
        _isClearing.value = false
        _isLoaded.value = false
    }


    override suspend fun clearWishlist() = withContext(dispatcher) {
        mutationMutex.withLock {
            _isClearing.value = true

            try {
                api.clearWishlist()
                _wishlist.value = emptyMap()
                _items.value = emptyList()
            } catch (e: Exception) {
                throw e.toAppException("Failed to clear wishlist.")
            } finally {
                _isClearing.value = false
            }
        }
    }

}
