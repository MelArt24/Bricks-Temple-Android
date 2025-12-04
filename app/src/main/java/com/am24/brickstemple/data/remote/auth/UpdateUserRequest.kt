package com.am24.brickstemple.data.remote.auth

import kotlinx.serialization.Serializable

@Serializable
data class UpdateUserRequest(
    val username: String,
    val email: String,
    val password: String
)