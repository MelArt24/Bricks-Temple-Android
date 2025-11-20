package com.am24.brickstemple.data.repositories

import android.content.Context
import com.am24.brickstemple.auth.AuthSession
import com.am24.brickstemple.data.remote.auth.AuthLoginResponse
import com.am24.brickstemple.data.remote.auth.AuthRegisterResponse
import com.am24.brickstemple.data.remote.auth.UserMeResponse
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryImplTest {

    private fun mockContext(): Context =
        mockk(relaxed = true)

    private fun mockClient(code: HttpStatusCode, json: String): HttpClient {
        return HttpClient(MockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }

            engine {
                addHandler { request ->
                    respond(
                        content = json,
                        status = code,
                        headers = headersOf("Content-Type", "application/json")
                    )
                }
            }
        }
    }

    @Test
    fun `login should save token`() = runBlocking {
        val responseJson = Json.encodeToString(
            AuthLoginResponse.serializer(),
            AuthLoginResponse("token123")
        )

        val client = mockClient(HttpStatusCode.OK, responseJson)
        val repo = AuthRepositoryImpl(client, mockContext())

        val token = repo.login("test@mail.com", "pass")

        assertEquals("token123", token)
        assertEquals("token123", AuthSession.token)
    }

    @Test
    fun `register should return id`() = runBlocking {
        val responseJson = Json.encodeToString(
            AuthRegisterResponse.serializer(),
            AuthRegisterResponse("ok", 42)
        )

        val repo = AuthRepositoryImpl(
            mockClient(HttpStatusCode.Created, responseJson),
            mockContext()
        )

        val id = repo.register("Artem", "test@mail.com", "123456")

        assertEquals(42, id)
    }

    @Test
    fun `getCurrentUser should return user data`() = runBlocking {
        val responseJson = Json.encodeToString(
            UserMeResponse.serializer(),
            UserMeResponse(5, "Artem", "test@mail.com")
        )

        val repo = AuthRepositoryImpl(
            mockClient(HttpStatusCode.OK, responseJson),
            mockContext()
        )

        val result = repo.getCurrentUser()

        assertEquals(5, result.id)
        assertEquals("Artem", result.username)
    }
}
