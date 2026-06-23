package com.am24.brickstemple.data.local.db

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun get(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "bricks_temple.db"
            )
                .addMigrations(*DatabaseMigrations.ALL)
                .build().also { INSTANCE = it }
        }
    }
}
