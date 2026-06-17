package com.am24.brickstemple.domain.repository

import com.am24.brickstemple.domain.model.WishlistItem
import kotlinx.coroutines.flow.StateFlow

interface WishlistRepository {
    val wishlist: StateFlow<Map<Int, Int>>
    val items: StateFlow<List<WishlistItem>>
    val isUpdating: StateFlow<Set<Int>>
    val isClearing: StateFlow<Boolean>
    val isLoading: StateFlow<Boolean>
    val isLoaded: StateFlow<Boolean>

    suspend fun refresh()
    suspend fun removeCompletely(productId: Int)
    suspend fun removeOne(productId: Int)
    fun toggle(productId: Int)
    fun lastFetchedItem(productId: Int): WishlistItem?
    suspend fun updateQuantity(itemId: Int, newQuantity: Int)
    fun clearLocal()
    suspend fun clearWishlist()
}
