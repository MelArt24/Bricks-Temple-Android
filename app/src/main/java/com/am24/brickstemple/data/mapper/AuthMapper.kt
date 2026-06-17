package com.am24.brickstemple.data.mapper

import com.am24.brickstemple.data.remote.auth.UpdateUserRequest
import com.am24.brickstemple.data.remote.auth.UserMeResponse
import com.am24.brickstemple.domain.model.UpdateUser
import com.am24.brickstemple.domain.model.User

fun UserMeResponse.toDomain() = User(
    id = id,
    username = username,
    email = email,
    message = message
)

fun UpdateUser.toRequest() = UpdateUserRequest(
    username = username,
    email = email,
    password = password
)
