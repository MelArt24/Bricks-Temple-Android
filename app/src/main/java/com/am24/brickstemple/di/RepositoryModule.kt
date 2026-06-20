package com.am24.brickstemple.di

import com.am24.brickstemple.data.repository.AuthRepositoryImpl
import com.am24.brickstemple.data.repository.CartRepositoryImpl
import com.am24.brickstemple.data.repository.OrderRepositoryImpl
import com.am24.brickstemple.data.repository.ProductRepositoryImpl
import com.am24.brickstemple.data.repository.WishlistRepositoryImpl
import com.am24.brickstemple.domain.repository.AuthRepository
import com.am24.brickstemple.domain.repository.CartRepository
import com.am24.brickstemple.domain.repository.OrderRepository
import com.am24.brickstemple.domain.repository.ProductRepository
import com.am24.brickstemple.domain.repository.WishlistRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val repositoryModule = module {
    single<ProductRepository> { ProductRepositoryImpl(api = get(), dao = get()) }
    single<WishlistRepository> { WishlistRepositoryImpl(api = get()) }
    single<CartRepository> {
        CartRepositoryImpl(
            cartDao = get(),
            productDao = get(),
            orderApi = get()
        )
    }
    single<OrderRepository> { OrderRepositoryImpl(api = get()) }
    single<AuthRepository> {
        AuthRepositoryImpl(
            client = get(),
            appContext = androidContext()
        )
    }
}
