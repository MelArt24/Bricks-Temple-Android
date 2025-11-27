package com.am24.brickstemple.data.remote

import com.am24.brickstemple.auth.AuthSession
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.serializers.LocalDateTimeIso8601Serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

private const val BASE_URL = "https://bricks-temple-server.onrender.com"
private const val HEALTH_URL = "$BASE_URL/health"

enum class NetworkStatus {
    CONNECTED,
    CONNECTING,
    OFFLINE
}

object KtorClientProvider {

    private val _networkStatus = MutableStateFlow(NetworkStatus.CONNECTED)
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private val monitorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var isPinging = false

    private fun makeOkHttp(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .addInterceptor(offlineInterceptor)
            .build()
    }

    private val offlineInterceptor = Interceptor { chain ->
        try {
            val response = chain.proceed(chain.request())

            if (_networkStatus.value != NetworkStatus.CONNECTED) {
                _networkStatus.value = NetworkStatus.CONNECTED
            }

            response

        } catch (e: UnknownHostException) {
            handleNetworkError(chain, e)
        } catch (e: IOException) {
            handleNetworkError(chain, e)
        }
    }

    private fun handleNetworkError(
        chain: Interceptor.Chain,
        @Suppress("UNUSED_PARAMETER") e: Exception
    ): okhttp3.Response {
        if (_networkStatus.value != NetworkStatus.OFFLINE) {
            _networkStatus.value = NetworkStatus.OFFLINE
            startPingLoop()
        }
        return makeOfflineResponse(chain)
    }

    private fun makeOfflineResponse(chain: Interceptor.Chain): okhttp3.Response =
        okhttp3.Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(499) // our custom "offline"
            .message("Offline")
            .body("".toResponseBody(null))
            .build()

    val client: HttpClient by lazy {
        HttpClient(OkHttp) {
            engine {
                preconfigured = makeOkHttp()
            }

            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        prettyPrint = false
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
        }
    }

    fun syncWithSystemNetwork(isAvailable: Boolean) {
        if (!isAvailable) {
            if (_networkStatus.value != NetworkStatus.OFFLINE) {
                _networkStatus.value = NetworkStatus.OFFLINE
                startPingLoop()
            }
        }
    }

    fun startPingLoop() {
        if (isPinging) return
        isPinging = true

        monitorScope.launch {
            _networkStatus.value = NetworkStatus.CONNECTING

            val rawClient = OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS)
                .build()

            repeat(20) {
                try {
                    val request = Request.Builder().url(HEALTH_URL).build()
                    rawClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            _networkStatus.value = NetworkStatus.CONNECTING
                            delay(500)
                            _networkStatus.value = NetworkStatus.CONNECTED
                            isPinging = false
                            return@launch
                        }
                    }
                } catch (_: Exception) { }

                delay(1500)
            }

            _networkStatus.value = NetworkStatus.OFFLINE
            isPinging = false
        }
    }
}
