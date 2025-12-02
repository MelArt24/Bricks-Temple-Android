package com.am24.brickstemple.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.am24.brickstemple.data.remote.KtorClientProvider
import com.am24.brickstemple.data.remote.NetworkStatus
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    showMenu: Boolean,
    title: String = "",
    enableSearch: Boolean = false,
    searchText: String = "",
    onMenuClick: () -> Unit,
    onBackClick: () -> Unit,
    onSearchChange: (String) -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    var internalSearchText by remember { mutableStateOf(searchText) }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        NetworkBanner()

        TopAppBar(
            title = {
                if (enableSearch) {
                    TextField(
                        value = internalSearchText,
                        onValueChange = {
                            internalSearchText = it
                            onSearchChange(it)
                        },
                        placeholder = { Text("Search…") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                        )
                    )
                } else {
                    Text(title)
                }
            },
            navigationIcon = {
                if (showMenu) {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            actions = actions
        )
    }
}

@Composable
fun NetworkBanner() {
    val state by KtorClientProvider.networkStatus.collectAsState()

    var showBackOnline by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (state == NetworkStatus.CONNECTED) {
            showBackOnline = true
            delay(2000)
            showBackOnline = false
        } else {
            showBackOnline = false
        }
    }

    val visible = state == NetworkStatus.OFFLINE ||
            state == NetworkStatus.CONNECTING ||
            showBackOnline

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val (text, color) = when {
            state == NetworkStatus.OFFLINE ->
                "Offline — no internet" to Color(0xFFD32F2F)

            state == NetworkStatus.CONNECTING ->
                "Reconnecting…" to Color(0xFFFFA000)

            showBackOnline ->
                "Back online" to Color(0xFF388E3C)

            else ->
                "" to Color.Transparent
        }

        if (text.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
