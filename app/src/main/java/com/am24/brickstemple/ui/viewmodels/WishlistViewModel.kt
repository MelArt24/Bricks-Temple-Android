package com.am24.brickstemple.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.am24.brickstemple.data.repositories.WishlistRepository
import kotlinx.coroutines.launch

class WishlistViewModel(
    private val repo: WishlistRepository
) : ViewModel() {

    val wishlist = repo.wishlist
    val isUpdating = repo.isUpdating

    fun refresh() {
        viewModelScope.launch {
            repo.refresh()
        }
    }

    init {
        viewModelScope.launch {
            repo.refresh()
        }
    }

    fun toggle(productId: Int) {
        viewModelScope.launch {
            repo.toggle(productId)
        }
    }

    fun reset() {
        repo.clearLocal()
    }

    class Factory(
        private val repo: WishlistRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WishlistViewModel(repo) as T
        }
    }
}
