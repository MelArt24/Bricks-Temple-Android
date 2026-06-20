package com.am24.brickstemple.di

import com.am24.brickstemple.data.remote.KtorClientProvider
import com.am24.brickstemple.data.remote.OrderApiService
import com.am24.brickstemple.data.remote.ProductApiService
import com.am24.brickstemple.data.remote.WishlistApiService
import io.ktor.client.HttpClient
import org.koin.dsl.module

val networkModule = module {
    single<HttpClient> { KtorClientProvider.client }
    single { ProductApiService(get()) }
    single { OrderApiService(get()) }
    single { WishlistApiService(get()) }
}
