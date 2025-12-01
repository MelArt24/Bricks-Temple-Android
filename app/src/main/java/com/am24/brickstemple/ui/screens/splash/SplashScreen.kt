package com.am24.brickstemple.ui.screens.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.am24.brickstemple.R
import com.am24.brickstemple.auth.AuthSession
import kotlinx.coroutines.delay
import com.am24.brickstemple.ui.navigation.Screen

@Composable
fun SplashScreen(navController: NavController) {

    LaunchedEffect(Unit) {
        delay(1200)

        if (AuthSession.isLoggedIn()) {
            navController.navigate(Screen.ProductList.route) {
                popUpTo(0)
            }
        } else {
            navController.navigate(Screen.Login.route) {
                popUpTo(0)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.bricks_temple_transparent_logo),
            contentDescription = "Logo",
            modifier = Modifier.size(140.dp)
        )
    }
}
