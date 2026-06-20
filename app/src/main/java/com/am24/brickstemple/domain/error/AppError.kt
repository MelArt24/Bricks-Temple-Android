package com.am24.brickstemple.domain.error

sealed class AppError(open val userMessage: String) {
    data class NetworkError(
        override val userMessage: String = "No internet connection."
    ) : AppError(userMessage)

    data class UnauthorizedError(
        override val userMessage: String = "Please log in to continue."
    ) : AppError(userMessage)

    data class ServerError(
        val statusCode: Int? = null,
        override val userMessage: String = "Server error. Please try again later."
    ) : AppError(userMessage)

    data class LocalDataError(
        override val userMessage: String = "Local data error. Please try again."
    ) : AppError(userMessage)

    data class NotFoundError(
        override val userMessage: String = "Requested item was not found."
    ) : AppError(userMessage)

    data class UnknownError(
        override val userMessage: String = "Unexpected error occurred."
    ) : AppError(userMessage)
}

class AppException(
    val error: AppError,
    cause: Throwable? = null
) : Exception(error.userMessage, cause)
