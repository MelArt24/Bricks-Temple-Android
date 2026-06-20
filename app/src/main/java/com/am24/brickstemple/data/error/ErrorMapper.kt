package com.am24.brickstemple.data.error

import com.am24.brickstemple.domain.error.AppError
import com.am24.brickstemple.domain.error.AppException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun Throwable.toAppException(defaultMessage: String = "Unexpected error occurred."): AppException {
    if (this is AppException) return this
    if (this is CancellationException) throw this

    return when (this) {
        is UnknownHostException,
        is SocketTimeoutException,
        is ConnectException,
        is IOException -> AppException(AppError.NetworkError(), this)

        is ClientRequestException -> this.response.toAppException(defaultMessage, this)
        is ServerResponseException -> this.response.toAppException(defaultMessage, this)
        is ResponseException -> this.response.toAppException(defaultMessage, this)

        else -> AppException(
            AppError.UnknownError(message ?: defaultMessage),
            this
        )
    }
}

suspend fun HttpResponse.toAppExceptionWithBody(
    defaultMessage: String = "Request failed.",
    cause: Throwable? = null
): AppException {
    val statusCode = status.value
    val bodyMessage = runCatching { bodyAsText().takeIf { it.isNotBlank() } }.getOrNull()
    val message = bodyMessage ?: defaultMessage

    return toAppException(message, cause, statusCode)
}

fun HttpResponse.toAppException(
    defaultMessage: String = "Request failed.",
    cause: Throwable? = null
): AppException = toAppException(defaultMessage, cause, status.value)

private fun HttpResponse.toAppException(
    message: String,
    cause: Throwable?,
    statusCode: Int
): AppException = when (statusCode) {
    401, 403 -> AppException(AppError.UnauthorizedError(message), cause)
    404 -> AppException(AppError.NotFoundError(message), cause)
    408, 429, 499 -> AppException(AppError.NetworkError(message), cause)
    in 500..599 -> AppException(AppError.ServerError(statusCode, message), cause)
    else -> AppException(AppError.UnknownError(message), cause)
}
