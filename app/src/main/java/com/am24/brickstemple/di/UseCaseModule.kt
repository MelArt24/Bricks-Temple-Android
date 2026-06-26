package com.am24.brickstemple.di

import com.am24.brickstemple.domain.usecase.order.LoadOrderDetailsUseCase
import org.koin.dsl.module

val useCaseModule = module {
    factory {
        LoadOrderDetailsUseCase(
            orderRepository = get(),
            productRepository = get()
        )
    }
}
