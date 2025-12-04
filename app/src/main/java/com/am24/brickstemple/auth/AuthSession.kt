package com.am24.brickstemple.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object AuthSession {

    private var _token by mutableStateOf<String?>(null)
    private var _email by mutableStateOf<String?>(null)
    private var _username by mutableStateOf<String?>(null)
    private var _userId by mutableStateOf<Int?>(null)

    val userId: Int? get() = _userId
    val token: String? get() = _token
    val email: String? get() = _email
    val username: String? get() = _username

    var isLoaded by mutableStateOf(false)
        private set

    fun updateUserId(value: Int?) {
        _userId = value
    }

    fun updateToken(value: String?) {
        _token = value
    }

    fun updateEmail(value: String?) {
        _email = value
    }

    fun updateUsername(value: String?) {
        _username = value
    }

    fun markLoaded() {
        isLoaded = true
    }

    fun clear() {
        _token = null
        _email = null
        _username = null
        isLoaded = true
    }

    fun isLoggedIn(): Boolean = !_token.isNullOrBlank()
}
