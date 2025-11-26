package com.am24.brickstemple.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.am24.brickstemple.auth.AuthSession
import com.am24.brickstemple.ui.navigation.Screen

@Composable
fun DrawerContent(
    isLoggedIn: Boolean,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    onLogin: () -> Unit
) {
    val username = AuthSession.username
    val email = AuthSession.email

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(260.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {

        Text(
            text = when {
                isLoggedIn -> username ?: "User"
                else -> "Guest User"
            },
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = when {
                isLoggedIn -> email ?: ""
                else -> "Not logged in"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoggedIn) {
            NavigationDrawerItem(
                label = { Text("Order history") },
                selected = false,
                onClick = { onNavigate(Screen.OrderHistory.route) }
            )
        }

        NavigationDrawerItem(
            label = { Text("Settings") },
            selected = false,
            onClick = { onNavigate(Screen.Settings.route) }
        )

        NavigationDrawerItem(
            label = { Text("About") },
            selected = false,
            onClick = { onNavigate(Screen.About.route) }
        )

        Spacer(modifier = Modifier.weight(1f))

        if (isLoggedIn) {
            NavigationDrawerItem(
                label = { Text("Logout", color = MaterialTheme.colorScheme.error) },
                selected = false,
                onClick = onLogout
            )
        } else {
            NavigationDrawerItem(
                label = { Text("Login") },
                selected = false,
                onClick = onLogin
            )
        }
    }
}
