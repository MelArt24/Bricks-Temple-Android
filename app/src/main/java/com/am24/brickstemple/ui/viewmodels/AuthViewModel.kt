package com.am24.brickstemple.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.am24.brickstemple.auth.AuthSession
import com.am24.brickstemple.data.remote.KtorClientProvider
import com.am24.brickstemple.data.repositories.AuthRepositoryImpl
import com.am24.brickstemple.domain.repositories.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

data class AuthFormState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow(AuthFormState())
    val loginState: StateFlow<AuthFormState> = _loginState

    private val _registerState = MutableStateFlow(AuthFormState())
    val registerState: StateFlow<AuthFormState> = _registerState

    fun onLoginEmailChange(value: String) {
        _loginState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onLoginPasswordChange(value: String) {
        _loginState.update { it.copy(password = value, errorMessage = null) }
    }

    fun login() {
        val state = _loginState.value

        val validationError = validateLogin(state.email, state.password)
        if (validationError != null) {
            _loginState.update { it.copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            _loginState.update { it.copy(isLoading = true) }

            try {
                authRepository.login(state.email, state.password)
                _loginState.update { it.copy(isSuccess = true, isLoading = false) }

            } catch (e: Exception) {
                _loginState.update {
                    it.copy(errorMessage = mapError(e), isLoading = false)
                }
            }
        }
    }

    fun onRegisterUsernameChange(v: String) {
        _registerState.update { it.copy(username = v, errorMessage = null) }
    }

    fun onRegisterEmailChange(v: String) {
        _registerState.update { it.copy(email = v, errorMessage = null) }
    }

    fun onRegisterPasswordChange(v: String) {
        _registerState.update { it.copy(password = v, errorMessage = null) }
    }

    fun register() {
        val state = _registerState.value

        val validationError = validateRegister(state.username, state.email, state.password)
        if (validationError != null) {
            _registerState.update { it.copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            _registerState.update { it.copy(isLoading = true) }

            try {
                authRepository.register(state.username, state.email, state.password)
                _registerState.update { it.copy(isSuccess = true, isLoading = false) }

            } catch (e: Exception) {
                _registerState.update {
                    it.copy(errorMessage = mapError(e), isLoading = false)
                }
            }
        }
    }

    private fun validateLogin(email: String, password: String): String? {
        if (email.isBlank() || password.isBlank())
            return "Email and password cannot be empty"
        if (!email.contains("@"))
            return "Invalid email format"
        return null
    }

    private fun validateRegister(username: String, email: String, password: String): String? {
        if (username.isBlank() || email.isBlank() || password.isBlank())
            return "All fields are required"
        if (username.length < 2)
            return "Username must be more than 2 characters"
        if (!email.contains("@"))
            return "Invalid email format"
        if (password.length < 6)
            return "Password must be at least 6 characters"
        return null
    }

    private fun mapError(e: Exception): String {
        println("EXCEPTION = ${e::class}")
        println("MESSAGE = ${e.message}")

        return when (e) {
            is IOException -> "No internet connection."
            else -> e.message ?: "Unexpected error occurred."
        }
    }


    fun resetLoginSuccess() {
        _loginState.update { it.copy(isSuccess = false) }
    }

    fun resetRegisterSuccess() {
        _registerState.update { it.copy(isSuccess = false) }
    }

    fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                val user = authRepository.getCurrentUser()
                AuthSession.updateEmail(user.email)
                AuthSession.updateUsername(user.username)
            } catch (e: Exception) {
                println("Failed to load user info: ${e.message}")
            }
        }
    }


    class AuthViewModelFactory(
        private val context: Context
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                val repo = AuthRepositoryImpl(
                    client = KtorClientProvider.client,
                    appContext = context
                )
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel(repo) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
