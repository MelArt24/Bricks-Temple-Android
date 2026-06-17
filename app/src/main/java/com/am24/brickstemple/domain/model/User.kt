package com.am24.brickstemple.domain.model

data class User(
    val id: Int,
    val username: String,
    val email: String,
    val message: String? = null
)
