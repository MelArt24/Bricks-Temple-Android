package com.am24.brickstemple.utils

import androidx.navigation.NavController
import com.am24.brickstemple.auth.AuthSession
import com.am24.brickstemple.ui.navigation.Screen

fun requireLogin(navController: NavController, block: () -> Unit) {
    if (!AuthSession.isLoggedIn()) {
        navController.navigate(Screen.Login.route)
    } else {
        block()
    }
}
