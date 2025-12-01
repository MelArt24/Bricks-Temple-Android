package com.am24.brickstemple.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProductDto(
    val id: Int,
    val name: String,
    val category: String? = null,
    val number: String? = null,
    val details: Int? = null,
    val minifigures: Int? = null,
    val age: String? = null,
    val year: String? = null,
    val size: String? = null,
    val condition: String? = null,
    val price: Double,
    val createdAt: String? = null,
    val image: String? = null,
    val description: String? = null,
    val type: String,
    val keywords: String? = null,
    val isAvailable: Boolean = true
)
