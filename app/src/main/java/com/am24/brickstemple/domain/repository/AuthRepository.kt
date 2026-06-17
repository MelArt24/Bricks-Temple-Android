package com.am24.brickstemple.domain.repository

import com.am24.brickstemple.domain.model.UpdateUser
import com.am24.brickstemple.domain.model.User

interface AuthRepository {

    suspend fun login(email: String, password: String): String

    suspend fun register(
        username: String,
        email: String,
        password: String
    ): Long

    suspend fun logout()

    suspend fun getCurrentUser(): User

    suspend fun updateUser(id: Int, user: UpdateUser)

}
