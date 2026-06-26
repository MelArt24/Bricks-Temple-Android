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

open class WishlistRepositoryImpl(
    private val api: WishlistApiService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : WishlistRepository {

    private val scope = CoroutineScope(dispatcher + SupervisorJob())
    private val coordinator = WishlistMutationCoordinator()

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

    private val pendingAddProductIds = mutableSetOf<Int>()
    private val pendingRemoveProductIds = mutableSetOf<Int>()
    private val pendingQuantityUpdates = mutableMapOf<Int, Int>()
    private var reconciliationJob: Job? = null

    override suspend fun refresh() = withContext(dispatcher) {
        refreshFromRemote(showLoading = true)
    }

    private suspend fun refreshFromRemote(showLoading: Boolean) {
        if (showLoading) _isLoading.value = true
        try {
            val response = api.getWishlist()

            coordinator.withLocalStateLock {
                applyRemoteWishlist(response.items.map { it.toDomain() })
                _isLoaded.value = true
            }
        } catch (e: Exception) {
            throw e.toAppException("Failed to refresh wishlist.")
        } finally {
            if (showLoading) _isLoading.value = false
        }
    }

    override suspend fun removeCompletely(productId: Int) = withContext(dispatcher) {
        coordinator.runIndividualMutation {
            coordinator.withProductLock(productId) {
                val itemId = coordinator.withLocalStateLock {
                    _wishlist.value[productId]
                } ?: return@withProductLock
                if (itemId == PENDING_ITEM_ID) return@withProductLock

                coordinator.withItemLock(itemId) {
                    val snapshot = coordinator.withLocalStateLock {
                        val currentItemId = _wishlist.value[productId] ?: return@withLocalStateLock null
                        if (currentItemId != itemId) return@withLocalStateLock null

                        val previousWishlist = _wishlist.value
                        val previousItems = _items.value

                        pendingRemoveProductIds += productId
                        _wishlist.value = previousWishlist - productId
                        _items.value = previousItems.filterNot { it.productId == productId }

                        MutationSnapshot(
                            itemId = itemId,
                            wishlist = previousWishlist,
                            items = previousItems
                        )
                    } ?: return@withItemLock

                    try {
                        api.removeItem(snapshot.itemId)
                    } catch (e: Exception) {
                        coordinator.withLocalStateLock {
                            _wishlist.value = snapshot.wishlist
                            _items.value = snapshot.items
                        }
                        throw e.toAppException("Failed to remove wishlist item.")
                    } finally {
                        coordinator.withLocalStateLock {
                            pendingRemoveProductIds -= productId
                        }
                    }
                }
            }
        }
    }

    override suspend fun removeOne(productId: Int) = withContext(dispatcher) {
        coordinator.runIndividualMutation {
            coordinator.withProductLock(productId) {
                val itemId = coordinator.withLocalStateLock {
                    _wishlist.value[productId]
                } ?: return@withProductLock
                if (itemId == PENDING_ITEM_ID) return@withProductLock

                coordinator.withItemLock(itemId) {
                    val snapshot = coordinator.withLocalStateLock {
                        val currentItemId = _wishlist.value[productId] ?: return@withLocalStateLock null
                        if (currentItemId != itemId) return@withLocalStateLock null

                        val item = _items.value.firstOrNull { it.productId == productId }
                            ?: return@withLocalStateLock MutationSnapshot(
                                itemId = itemId,
                                wishlist = _wishlist.value,
                                items = _items.value,
                                requiresRefresh = true
                            )

                        val previousWishlist = _wishlist.value
                        val previousItems = _items.value

                        if (item.quantity > 1) {
                            _items.value = previousItems.map {
                                if (it.productId == productId) it.copy(quantity = it.quantity - 1) else it
                            }
                        } else {
                            pendingRemoveProductIds += productId
                            _wishlist.value = previousWishlist - productId
                            _items.value = previousItems.filterNot { it.productId == productId }
                        }

                        MutationSnapshot(
                            itemId = itemId,
                            wishlist = previousWishlist,
                            items = previousItems,
                            removedProductId = if (item.quantity <= 1) productId else null
                        )
                    } ?: return@withItemLock

                    try {
                        api.removeOneItem(snapshot.itemId)
                        if (snapshot.requiresRefresh) {
                            refreshFromRemote(showLoading = false)
                        }
                    } catch (e: Exception) {
                        coordinator.withLocalStateLock {
                            _wishlist.value = snapshot.wishlist
                            _items.value = snapshot.items
                        }
                        throw e.toAppException("Failed to update wishlist item.")
                    } finally {
                        snapshot.removedProductId?.let { removedProductId ->
                            coordinator.withLocalStateLock {
                                pendingRemoveProductIds -= removedProductId
                            }
                        }
                    }
                }
            }
        }
    }

    override fun toggle(productId: Int) {
        pendingJobs[productId]?.cancel()
        _isUpdating.value += productId

        lateinit var job: Job
        job = scope.launch {
            try {
                delay(200)

                performToggle(productId)
            } catch (e: CancellationException) {
                if (pendingJobs[productId] === job) {
                    _isUpdating.value -= productId
                }
                throw e
            } catch (_: Exception) {
                _isUpdating.value -= productId
            }

            if (pendingJobs[productId] === job) {
                pendingJobs.remove(productId)
            }
        }

        pendingJobs[productId] = job
    }

    private suspend fun performToggle(productId: Int) = withContext(dispatcher) {
        coordinator.runIndividualMutation {
            coordinator.withProductLock(productId) {
                val snapshot = coordinator.withLocalStateLock {
                    val current = _wishlist.value
                    val previousItems = _items.value

                    if (productId in current.keys) {
                        val itemId = current[productId]!!
                        if (itemId == PENDING_ITEM_ID) return@withLocalStateLock null

                        pendingRemoveProductIds += productId
                        _wishlist.value = current - productId
                        _items.value = previousItems.filterNot { it.productId == productId }

                        ToggleSnapshot(
                            itemId = itemId,
                            wishlist = current,
                            items = previousItems,
                            wasFavorite = true
                        )
                    } else {
                        pendingAddProductIds += productId
                        _wishlist.value = current + (productId to PENDING_ITEM_ID)
                        _items.value = previousItems + WishlistItem(
                            id = null,
                            wishlistId = PENDING_ITEM_ID,
                            productId = productId,
                            quantity = 1
                        )

                        ToggleSnapshot(
                            itemId = null,
                            wishlist = current,
                            items = previousItems,
                            wasFavorite = false
                        )
                    }
                } ?: return@withProductLock

                if (snapshot.wasFavorite) {
                    try {
                        api.removeItem(snapshot.itemId!!)
                    } catch (e: Exception) {
                        coordinator.withLocalStateLock {
                            _wishlist.value = snapshot.wishlist
                            _items.value = snapshot.items
                        }
                        throw e.toAppException("Failed to remove wishlist item.")
                    } finally {
                        coordinator.withLocalStateLock {
                            pendingRemoveProductIds -= productId
                        }
                        _isUpdating.value -= productId
                    }
                } else {
                    try {
                        api.addItem(productId)
                        scheduleReconciliation()
                    } catch (e: Exception) {
                        coordinator.withLocalStateLock {
                            pendingAddProductIds -= productId
                            _wishlist.value = snapshot.wishlist
                            _items.value = snapshot.items
                        }
                        _isUpdating.value -= productId
                        throw e.toAppException("Failed to add wishlist item.")
                    }
                }
            }
        }
    }

    override fun lastFetchedItem(productId: Int): WishlistItem? =
        _items.value.firstOrNull { it.productId == productId }

    override suspend fun updateQuantity(itemId: Int, newQuantity: Int) = withContext(dispatcher) {
        coordinator.runIndividualMutation {
            coordinator.withItemLock(itemId) {
                val snapshot = coordinator.withLocalStateLock {
                    val previousItems = _items.value
                    if (previousItems.none { it.id == itemId }) {
                        return@withLocalStateLock MutationSnapshot(
                            itemId = itemId,
                            wishlist = _wishlist.value,
                            items = previousItems,
                            requiresRefresh = true
                        )
                    }

                    pendingQuantityUpdates[itemId] = newQuantity
                    _items.value = previousItems.map {
                        if (it.id == itemId) it.copy(quantity = newQuantity) else it
                    }

                    MutationSnapshot(
                        itemId = itemId,
                        wishlist = _wishlist.value,
                        items = previousItems
                    )
                }

                try {
                    api.updateQuantity(itemId, newQuantity)
                    if (snapshot.requiresRefresh) {
                        refreshFromRemote(showLoading = false)
                    }
                } catch (e: Exception) {
                    coordinator.withLocalStateLock {
                        _items.value = snapshot.items
                    }
                    throw e.toAppException("Failed to update wishlist quantity.")
                } finally {
                    coordinator.withLocalStateLock {
                        pendingQuantityUpdates.remove(itemId)
                    }
                }
            }
        }
    }

    override fun clearLocal() {
        pendingJobs.values.forEach { it.cancel() }
        reconciliationJob?.cancel()
        _wishlist.value = emptyMap()
        _items.value = emptyList()
        _isUpdating.value = emptySet()
        _isClearing.value = false
        _isLoaded.value = false
        pendingAddProductIds.clear()
        pendingRemoveProductIds.clear()
        pendingQuantityUpdates.clear()
    }


    override suspend fun clearWishlist() = withContext(dispatcher) {
        coordinator.beginClear()
        val previousWishlist = _wishlist.value
        val previousItems = _items.value

        _isClearing.value = true
        pendingJobs.values.forEach { it.cancel() }
        pendingJobs.clear()
        reconciliationJob?.cancel()

        try {
            api.clearWishlist()
            coordinator.withLocalStateLock {
                pendingAddProductIds.clear()
                pendingRemoveProductIds.clear()
                pendingQuantityUpdates.clear()
                _isUpdating.value = emptySet()
                _wishlist.value = emptyMap()
                _items.value = emptyList()
            }
        } catch (e: Exception) {
            coordinator.withLocalStateLock {
                _wishlist.value = previousWishlist
                _items.value = previousItems
            }
            throw e.toAppException("Failed to clear wishlist.")
        } finally {
            _isClearing.value = false
            coordinator.endClear()
        }
    }

    private fun scheduleReconciliation() {
        reconciliationJob?.cancel()
        reconciliationJob = scope.launch {
            delay(RECONCILIATION_DELAY_MS)
            try {
                refreshFromRemote(showLoading = false)
            } catch (_: Exception) {
                val unresolved = coordinator.withLocalStateLock { pendingAddProductIds.toSet() }
                _isUpdating.value -= unresolved
            }
        }
    }

    private fun applyRemoteWishlist(remoteItems: List<WishlistItem>) {
        val pendingAdds = pendingAddProductIds.toSet()
        val pendingRemoves = pendingRemoveProductIds.toSet()
        val localItemsByProduct = _items.value.associateBy { it.productId }

        val mergedItems = remoteItems
            .filterNot { it.productId in pendingRemoves }
            .map { remoteItem ->
                val pendingQuantity = remoteItem.id?.let { pendingQuantityUpdates[it] }
                if (pendingQuantity != null) remoteItem.copy(quantity = pendingQuantity) else remoteItem
            }
            .toMutableList()

        pendingAdds.forEach { productId ->
            val remoteItem = mergedItems.firstOrNull { it.productId == productId }
            if (remoteItem != null && remoteItem.id != null) {
                pendingAddProductIds -= productId
                _isUpdating.value -= productId
            } else {
                localItemsByProduct[productId]?.let { localItem ->
                    if (mergedItems.none { it.productId == productId }) {
                        mergedItems += localItem
                    }
                }
            }
        }

        _items.value = mergedItems
        _wishlist.value = mergedItems
            .filter { it.id != null }
            .associate { it.productId to it.id!! } +
                pendingAddProductIds.associateWith { PENDING_ITEM_ID }
    }

    private data class MutationSnapshot(
        val itemId: Int,
        val wishlist: Map<Int, Int>,
        val items: List<WishlistItem>,
        val requiresRefresh: Boolean = false,
        val removedProductId: Int? = null
    )

    private data class ToggleSnapshot(
        val itemId: Int?,
        val wishlist: Map<Int, Int>,
        val items: List<WishlistItem>,
        val wasFavorite: Boolean
    )

    private companion object {
        const val PENDING_ITEM_ID = -1
        const val RECONCILIATION_DELAY_MS = 250L
    }

}
