package com.am24.brickstemple.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.am24.brickstemple.data.local.ThemePreferenceDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(
    private val store: ThemePreferenceDataStore
) : ViewModel() {

    val isDarkMode = store.isDarkMode.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        false
    )

    fun toggleTheme(value: Boolean) {
        viewModelScope.launch {
            store.setDarkMode(value)
        }
    }

    class Factory(
        private val store: ThemePreferenceDataStore
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ThemeViewModel(store) as T
        }
    }
}
