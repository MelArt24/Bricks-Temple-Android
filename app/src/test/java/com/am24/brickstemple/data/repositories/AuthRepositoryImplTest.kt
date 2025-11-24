package com.am24.brickstemple.data.repositories

import android.content.Context
import com.am24.brickstemple.auth.AuthSession
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
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
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

    fun mockClientSequence(responses: List<Pair<HttpStatusCode, String>>): HttpClient {
        var index = 0

        return HttpClient(MockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }

            engine {
                addHandler { request ->
                    val (status, body) = responses[index]
                    index++

                    respond(
                        content = body,
                        status = status,
                        headers = headersOf("Content-Type" to listOf("application/json"))
                    )
                }
            }
        }
    }



    @Test
    fun `login should save token`() = runTest {
        val loginJson = """{"token":"token123"}"""
        val meJson = """
        {
            "id": 1,
            "username": "TestUser",
            "email": "test@mail.com"
        }
    """.trimIndent()

        val client = mockClientSequence(
            listOf(
                HttpStatusCode.OK to loginJson,
                HttpStatusCode.OK to meJson
            )
        )

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

    @Test
    fun `register success returns id and updates session`() = runTest {
        val responseJson = """{"id": 55, "message": "ok"}"""

        val client = mockClient(
            HttpStatusCode.OK,
            responseJson
        )

        val repo = AuthRepositoryImpl(client, mockContext())

        val id = repo.register("Artem", "a@mail.com", "pass123")

        assertEquals(55, id)
        assertEquals("Artem", AuthSession.username)
        assertEquals("a@mail.com", AuthSession.email)
    }

    @Test
    fun `register 404 returns user not found error`() = runTest {
        val client = mockClient(HttpStatusCode.NotFound, """{"error":"User not found."}""")
        val repo = AuthRepositoryImpl(client, mockContext())

        try {
            repo.register("A", "b@mail.com", "123")
            fail("Exception expected")
        } catch (e: Exception) {
            assertEquals("User not found.", e.message)
        }
    }

    @Test
    fun `register 401 returns incorrect password`() = runTest {
        val client = mockClient(HttpStatusCode.Unauthorized, """{"error":"Incorrect password."}""")
        val repo = AuthRepositoryImpl(client, mockContext())

        try {
            repo.register("A", "b@mail.com", "123")
            fail("Exception expected")
        } catch (e: Exception) {
            assertEquals("Incorrect password.", e.message)
        }
    }

    @Test
    fun `register 409 returns user exists`() = runTest {
        val client = mockClient(HttpStatusCode.Conflict, """{"error":"User with this email already exists"}""")
        val repo = AuthRepositoryImpl(client, mockContext())

        try {
            repo.register("A", "b@mail.com", "123")
            fail("Exception expected")
        } catch (e: Exception) {
            assertEquals("User with this email already exists", e.message)
        }
    }

    @Test
    fun `register 400 returns invalid input`() = runTest {
        val client = mockClient(HttpStatusCode.BadRequest, """{"error":"Invalid"}""")
        val repo = AuthRepositoryImpl(client, mockContext())

        try {
            repo.register("A", "b@mail.com", "123")
            fail("Exception expected")
        } catch (e: Exception) {
            assertEquals("Invalid", e.message)
        }
    }

    @Test
    fun `register unknown status returns server error`() = runTest {
        val client = mockClient(HttpStatusCode.InternalServerError, "{}")
        val repo = AuthRepositoryImpl(client, mockContext())

        try {
            repo.register("A", "b@mail.com", "123")
            fail("Exception expected")
        } catch (e: Exception) {
            assertEquals("Server error (500)", e.message)
        }
    }

    @Test
    fun `logout clears session`() = runTest {
        AuthSession.updateToken("abc")
        AuthSession.updateEmail("mail@mail.com")
        AuthSession.updateUsername("Artem")

        val client = mockClient(HttpStatusCode.OK, "{}")
        val repo = AuthRepositoryImpl(client, mockContext())

        repo.logout()

        assertEquals(null, AuthSession.token)
        assertEquals(null, AuthSession.email)
        assertEquals(null, AuthSession.username)
    }

}
