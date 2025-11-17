package com.am24.brickstemple.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PagedResponse<T>(
    val page: Int,
    val limit: Int,
    val total: Long,
    val data: List<T>
)
