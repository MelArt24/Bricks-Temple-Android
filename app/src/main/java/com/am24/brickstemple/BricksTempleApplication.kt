package com.am24.brickstemple

import android.app.Application
import com.am24.brickstemple.di.databaseModule
import com.am24.brickstemple.di.networkModule
import com.am24.brickstemple.di.repositoryModule
import com.am24.brickstemple.di.useCaseModule
import com.am24.brickstemple.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class BricksTempleApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@BricksTempleApplication)
            modules(
                databaseModule,
                networkModule,
                repositoryModule,
                useCaseModule,
                viewModelModule
            )
        }
    }
}
