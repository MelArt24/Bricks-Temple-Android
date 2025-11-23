package com.am24.brickstemple.auth

import android.content.Context
import androidx.navigation.NavController
import com.am24.brickstemple.ui.navigation.Screen
import com.am24.brickstemple.ui.viewmodels.AuthViewModel
import com.am24.brickstemple.ui.viewmodels.WishlistViewModel

object LogoutManager {

    fun performLogout(
        context: Context,
        navController: NavController,
        wishlistViewModel: WishlistViewModel,
        authViewModel: AuthViewModel
    ) {

        AuthSession.clear()

        AuthStorage.clear(context)

        wishlistViewModel.reset()

        authViewModel.logout()

        navController.navigate(Screen.Login.route) {
            popUpTo(Screen.ProductList.route) { inclusive = true }
            launchSingleTop = true
        }
    }
}
