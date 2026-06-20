package com.am24.brickstemple.data.repository

import android.content.Context
import com.am24.brickstemple.data.error.toAppException
import com.am24.brickstemple.data.auth.AuthSession
import com.am24.brickstemple.data.auth.AuthStorage
import com.am24.brickstemple.data.mapper.toDomain
import com.am24.brickstemple.data.mapper.toRequest
import com.am24.brickstemple.data.remote.auth.AuthLoginResponse
import com.am24.brickstemple.data.remote.auth.AuthRegisterResponse
import com.am24.brickstemple.data.remote.auth.LoginRequest
import com.am24.brickstemple.data.remote.auth.RegisterRequest
import com.am24.brickstemple.data.remote.auth.UserMeResponse
import com.am24.brickstemple.data.remote.util.NetworkConstants
import com.am24.brickstemple.domain.error.AppError
import com.am24.brickstemple.domain.error.AppException
import com.am24.brickstemple.domain.model.UpdateUser
import com.am24.brickstemple.domain.repository.AuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AuthRepositoryImpl(
    private val client: HttpClient,
    private val appContext: Context? = null
) : AuthRepository {

    override suspend fun login(email: String, password: String): String {
        try {
            val response: HttpResponse = client.post("${NetworkConstants.AUTH_URL}/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(email, password))
            }

            if (!response.status.isSuccess()) {
                throw response.toAuthException()
            }

            val json = response.bodyAsText()
            val data = Json.decodeFromString<AuthLoginResponse>(json)

            AuthSession.updateToken(data.token)
            AuthSession.updateEmail(email)

            val user = getCurrentUser()
            AuthSession.updateUsername(user.username)
            AuthSession.updateUserId(user.id)

            appContext?.let {
                AuthStorage.save(it, data.token, email, user.username)
            }

            return data.token
        } catch (e: Exception) {
            throw e.toAppException("Failed to log in.")
        }
    }

    override suspend fun register(username: String, email: String, password: String): Long {
        try {
            val response: HttpResponse = client.post("${NetworkConstants.AUTH_URL}/register") {
                contentType(ContentType.Application.Json)
                setBody(RegisterRequest(username, email, password))
            }

            if (!response.status.isSuccess()) {
                throw response.toAuthException()
            }

            val json = response.bodyAsText()
            val data = Json.decodeFromString<AuthRegisterResponse>(json)

            AuthSession.updateEmail(email)
            AuthSession.updateUsername(username)

            return data.id
        } catch (e: Exception) {
            throw e.toAppException("Failed to register.")
        }
    }

    override suspend fun logout() {
        AuthSession.clear()
    }

    private suspend fun parseError(response: HttpResponse): String {

        val body = response.bodyAsText()

        try {
            val element = Json.parseToJsonElement(body).jsonObject

            element["error"]?.jsonPrimitive?.content?.let { return it }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        }

        return when (response.status) {
            HttpStatusCode.NotFound -> "User not found."
            HttpStatusCode.Unauthorized -> "Incorrect password."
            HttpStatusCode.Conflict -> "User with this email already exists."
            HttpStatusCode.BadRequest -> "Invalid input data."
            else -> "Server error (${response.status.value})"
        }
    }

    private fun HttpStatusCode.isSuccess(): Boolean =
        this.value in 200..299

    private suspend fun HttpResponse.toAuthException(): AppException {
        val message = parseError(this)

        return when (status) {
            HttpStatusCode.Unauthorized,
            HttpStatusCode.Forbidden -> AppException(AppError.UnauthorizedError(message))
            HttpStatusCode.NotFound -> AppException(AppError.NotFoundError(message))
            HttpStatusCode.InternalServerError,
            HttpStatusCode.BadGateway,
            HttpStatusCode.ServiceUnavailable,
            HttpStatusCode.GatewayTimeout -> AppException(AppError.ServerError(status.value, message))
            else -> AppException(AppError.UnknownError(message))
        }
    }

    override suspend fun getCurrentUser() = getCurrentUserResponse().toDomain()

    private suspend fun getCurrentUserResponse(): UserMeResponse {
        try {
            val response = client.get("${NetworkConstants.USERS_URL}/me")

            if (!response.status.isSuccess()) {
                throw response.toAuthException()
            }

            val body = response.bodyAsText()
            return Json.decodeFromString<UserMeResponse>(body)
        } catch (e: Exception) {
            throw e.toAppException("Failed to load current user.")
        }
    }

    override suspend fun updateUser(id: Int, user: UpdateUser) {
        try {
            val response: HttpResponse = client.put("${NetworkConstants.USERS_URL}/$id") {
                contentType(ContentType.Application.Json)
                setBody(user.toRequest())
            }

            if (!response.status.isSuccess()) {
                throw response.toAuthException()
            }
        } catch (e: Exception) {
            throw e.toAppException("Failed to update user.")
        }
    }


}
