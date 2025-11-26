package com.am24.brickstemple.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.am24.brickstemple.auth.AuthSession
import com.am24.brickstemple.auth.LogoutManager
import com.am24.brickstemple.ui.navigation.Screen
import com.am24.brickstemple.ui.viewmodels.AuthViewModel
import com.am24.brickstemple.ui.viewmodels.WishlistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: AuthViewModel,
    wishlistViewModel: WishlistViewModel,
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (AuthSession.token.isNullOrBlank()) {
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.Profile.route) { inclusive = true }
            }
            return@LaunchedEffect
        }

        if (AuthSession.username == null || AuthSession.email == null) {
            viewModel.loadCurrentUser()
        }
    }

    val email = AuthSession.email ?: "Unknown"
    val username = AuthSession.username ?: "User"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {

            Text(
                text = "Account Information",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text("Email:", style = MaterialTheme.typography.labelLarge)
            Text(email, style = MaterialTheme.typography.bodyLarge)

            Spacer(modifier = Modifier.height(12.dp))

            Text("Username:", style = MaterialTheme.typography.labelLarge)
            Text(username, style = MaterialTheme.typography.bodyLarge)

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    LogoutManager.performLogout(
                        context = context,
                        navController = navController,
                        wishlistViewModel = wishlistViewModel,
                        authViewModel = viewModel
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Logout")
            }
        }
    }
}
