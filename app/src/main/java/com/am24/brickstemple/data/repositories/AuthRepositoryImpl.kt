package com.am24.brickstemple.data.repositories

import android.content.Context
import com.am24.brickstemple.auth.AuthSession
import com.am24.brickstemple.auth.AuthStorage
import com.am24.brickstemple.data.remote.auth.AuthLoginResponse
import com.am24.brickstemple.data.remote.auth.AuthRegisterResponse
import com.am24.brickstemple.data.remote.auth.LoginRequest
import com.am24.brickstemple.data.remote.auth.RegisterRequest
import com.am24.brickstemple.data.remote.auth.UpdateUserRequest
import com.am24.brickstemple.data.remote.auth.UserMeResponse
import com.am24.brickstemple.domain.repositories.AuthRepository
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AuthRepositoryImpl(
    private val client: HttpClient,
    private val appContext: Context? = null
) : AuthRepository {

    private val BASE_URL = "https://bricks-temple-server.onrender.com/auth"

    override suspend fun login(email: String, password: String): String {

        val response: HttpResponse = client.post("$BASE_URL/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }

        if (!response.status.isSuccess()) {
            throw Exception(parseError(response))
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
    }

    override suspend fun register(username: String, email: String, password: String): Long {

        val response: HttpResponse = client.post("$BASE_URL/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username, email, password))
        }

        if (!response.status.isSuccess()) {
            throw Exception(parseError(response))
        }

        val json = response.bodyAsText()
        val data = Json.decodeFromString<AuthRegisterResponse>(json)

        AuthSession.updateEmail(email)
        AuthSession.updateUsername(username)

        return data.id
    }

    override suspend fun logout() {
        AuthSession.clear()
    }

    private suspend fun parseError(response: HttpResponse): String {

        val body = response.bodyAsText()

        try {
            val element = Json.parseToJsonElement(body).jsonObject

            element["error"]?.jsonPrimitive?.content?.let { return it }
        } catch (_: Exception) {
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


    override suspend fun getCurrentUser(): UserMeResponse {

        val response = client.get("https://bricks-temple-server.onrender.com/users/me")

        if (!response.status.isSuccess()) {
            throw Exception(parseError(response))
        }

        val body = response.bodyAsText()
        return Json.decodeFromString<UserMeResponse>(body)
    }

    override suspend fun updateUser(id: Int, req: UpdateUserRequest) {
        val response: HttpResponse = client.put("https://bricks-temple-server.onrender.com/users/$id") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }

        if (!response.status.isSuccess()) {
            throw Exception(parseError(response))
        }
    }


}
