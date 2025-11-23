package com.am24.brickstemple.data.repositories

import com.am24.brickstemple.auth.AuthSession
import com.am24.brickstemple.data.remote.WishlistApiService
import com.am24.brickstemple.data.remote.dto.WishlistResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WishlistRepository(
    private val api: WishlistApiService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val _wishlist = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val wishlist = _wishlist.asStateFlow()

    private var refreshJob: Job? = null

    private val _isUpdating = MutableStateFlow<Set<Int>>(emptySet())
    val isUpdating = _isUpdating.asStateFlow()

    suspend fun refresh() = withContext(dispatcher) {
        val response = api.getWishlist()
        if (response == null) {
            _wishlist.value = emptyMap()
        } else {
            _wishlist.value = response.items.associate { it.productId to it.id!! }
        }
    }

    suspend fun toggle(productId: Int) = withContext(dispatcher) {

        _isUpdating.value += productId

        val current = _wishlist.value

        try {
            if (productId in current.keys) {
                val itemId = current[productId]!!
                api.removeItem(itemId)
                _wishlist.value = current - productId
            } else {
                api.addItem(productId)
                refresh()
            }
        } finally {
            _isUpdating.value -= productId
        }
    }

    private suspend fun removeByProductId(productId: Int) = withContext(dispatcher) {
        val response: WishlistResponse? = api.getWishlist() ?: return@withContext

        val item = response?.items?.find { it.productId == productId } ?: return@withContext

        api.removeItem(item.id!!)
    }

    fun clearLocal() {
        _wishlist.value = emptyMap()
    }
}
