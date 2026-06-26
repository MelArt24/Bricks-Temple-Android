package com.am24.brickstemple.domain.usecase.cart

import com.am24.brickstemple.domain.repository.CartRepository

class UpdateCartQuantityUseCase(
    private val cartRepository: CartRepository
) {

    suspend operator fun invoke(productId: Int, delta: Int) {
        val currentQty = cartRepository.cart.value[productId] ?: 0
        val newQty = currentQty + delta

        when {
            newQty <= 0 -> cartRepository.removeCompletely(productId)
            else -> cartRepository.updateQuantity(productId, newQty)
        }
    }
}
