package com.am24.brickstemple.ui.screens.settings

import androidx.lifecycle.ViewModel
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
}
