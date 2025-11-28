package com.am24.brickstemple.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val category: String?,
    val number: String?,
    val details: Int?,
    val minifigures: Int?,
    val age: String?,
    val year: String?,
    val size: String?,
    val condition: String?,
    val price: Double,
    val createdAt: String?,
    val image: String?,
    val description: String?,
    val type: String,
    val keywords: String?,
    val isAvailable: Boolean
)