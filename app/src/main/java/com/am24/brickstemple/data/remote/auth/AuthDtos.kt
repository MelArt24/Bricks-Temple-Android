package com.am24.brickstemple.data.remote.auth

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

@Serializable
data class AuthLoginResponse(
    val token: String
)

@Serializable
data class AuthRegisterResponse(
    val message: String,
    val id: Long
)

@Serializable
data class UserMeResponse(
    val id: Int,
    val username: String,
    val email: String,
    val message: String? = null
)