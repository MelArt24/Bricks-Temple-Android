package com.am24.brickstemple.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UserUiState(
    val name: String? = null,
    val email: String? = null,
    val isLoading: Boolean = true
)

class UserViewModel(
    // UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserUiState())
    val uiState = _uiState.asStateFlow()

    fun loadUser() {
        viewModelScope.launch {
            // TODO: зробити запит на сервер
            _uiState.value = UserUiState(
                name = "John Doe",
                email = "john@example.com",
                isLoading = false
            )
        }
    }
}
