package com.am24.brickstemple.data.remote

import com.am24.brickstemple.auth.AuthSession
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.datetime.serializers.LocalDateTimeIso8601Serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

object KtorClientProvider {

    val client: HttpClient by lazy {
        HttpClient(CIO) {

            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        android.util.Log.d("KtorLogger", message)
                    }
                }
                level = LogLevel.ALL
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 25000
                connectTimeoutMillis = 25000
                socketTimeoutMillis = 25000
            }


            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        prettyPrint = false
                        isLenient = true

                        serializersModule = SerializersModule {
                            contextual(LocalDateTimeIso8601Serializer)
                        }
                    }
                )
            }

            install(DefaultRequest) {
                val token = AuthSession.token
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

                header(HttpHeaders.ContentType, ContentType.Application.Json)
            }

            engine {
                requestTimeout = 30_000
            }
        }
    }
}
