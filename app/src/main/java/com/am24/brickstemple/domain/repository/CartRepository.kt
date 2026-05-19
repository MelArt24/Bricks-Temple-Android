package com.am24.brickstemple.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface CartRepository {
    val cart: StateFlow<Map<Int, Int>>
    val isUpdating: StateFlow<Set<Int>>
    val isClearing: StateFlow<Boolean>
    val isLoading: StateFlow<Boolean>
    val isLoaded: StateFlow<Boolean>
    
    suspend fun checkout(): Int?
    suspend fun refresh()
    suspend fun add(productId: Int)
    suspend fun toggle(productId: Int)
    suspend fun updateQuantity(productId: Int, newQuantity: Int)
    suspend fun removeCompletely(productId: Int)
    suspend fun clearCart()
    fun clearLocal()
}
