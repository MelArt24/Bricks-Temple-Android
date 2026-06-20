package com.am24.brickstemple.di

import com.am24.brickstemple.ui.screens.auth.AuthViewModel
import com.am24.brickstemple.ui.screens.cart.CartViewModel
import com.am24.brickstemple.ui.screens.orders.OrderViewModel
import com.am24.brickstemple.ui.screens.product.ProductViewModel
import com.am24.brickstemple.ui.screens.settings.ThemeViewModel
import com.am24.brickstemple.ui.screens.wishlist.WishlistViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { ProductViewModel(repo = get()) }
    viewModel { CartViewModel(repo = get(), productRepository = get()) }
    viewModel { WishlistViewModel(repo = get(), productRepository = get()) }
    viewModel { OrderViewModel(repo = get(), productRepository = get()) }
    viewModel { AuthViewModel(authRepository = get()) }
    viewModel { ThemeViewModel(store = get()) }
}
