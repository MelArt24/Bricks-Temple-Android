package com.am24.brickstemple.data.mappers

import com.am24.brickstemple.data.local.entities.ProductEntity
import com.am24.brickstemple.data.remote.dto.ProductDto

fun ProductDto.toEntity() = ProductEntity(
    id = id,
    name = name,
    category = category,
    number = number,
    details = details,
    minifigures = minifigures,
    age = age,
    year = year,
    size = size,
    condition = condition,
    price = price.toDoubleOrNull() ?: 0.0,
    createdAt = createdAt,
    image = image,
    description = description,
    type = type,
    keywords = keywords,
    isAvailable = isAvailable
)

fun ProductEntity.toDto() = ProductDto(
    id = id,
    name = name,
    category = category,
    number = number,
    details = details,
    minifigures = minifigures,
    age = age,
    year = year,
    size = size,
    condition = condition,
    price = price.toString(),
    createdAt = createdAt,
    image = image,
    description = description,
    type = type,
    keywords = keywords,
    isAvailable = isAvailable
)

val ProductDto.priceDouble: Double
    get() = price.toDoubleOrNull() ?: 0.0