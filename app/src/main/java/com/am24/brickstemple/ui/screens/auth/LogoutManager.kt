package com.am24.brickstemple.ui.screens.auth

import android.content.Context
import androidx.navigation.NavController
import com.am24.brickstemple.data.auth.AuthStorage
import com.am24.brickstemple.ui.navigation.Screen
import com.am24.brickstemple.ui.screens.wishlist.WishlistViewModel

object LogoutManager {

    fun performLogout(
        context: Context,
        navController: NavController,
        wishlistViewModel: WishlistViewModel,
        authViewModel: AuthViewModel
    ) {

        authViewModel.logout()

        AuthStorage.clear(context)

        wishlistViewModel.reset()

        navController.navigate(Screen.Login.route) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }
}
