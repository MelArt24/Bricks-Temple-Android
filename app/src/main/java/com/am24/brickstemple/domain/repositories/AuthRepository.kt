package com.am24.brickstemple.domain.repositories

import com.am24.brickstemple.data.remote.auth.UserMeResponse

interface AuthRepository {

    suspend fun login(email: String, password: String): String

    suspend fun register(
        username: String,
        email: String,
        password: String
    ): Long

    suspend fun logout()

    suspend fun getCurrentUser(): UserMeResponse
}
