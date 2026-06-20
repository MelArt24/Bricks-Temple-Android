package com.am24.brickstemple.di

import com.am24.brickstemple.data.local.ThemePreferenceDataStore
import com.am24.brickstemple.data.local.dao.CartDao
import com.am24.brickstemple.data.local.dao.ProductDao
import com.am24.brickstemple.data.local.db.AppDatabase
import com.am24.brickstemple.data.local.db.DatabaseProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single<AppDatabase> { DatabaseProvider.get(androidContext()) }
    single<ProductDao> { get<AppDatabase>().productDao() }
    single<CartDao> { get<AppDatabase>().cartDao() }
    single { ThemePreferenceDataStore(androidContext()) }
}
