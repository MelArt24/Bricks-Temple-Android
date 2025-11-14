package com.am24.brickstemple.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.am24.brickstemple.ui.screens.about.AboutScreen
import com.am24.brickstemple.ui.screens.cart.CartScreen
import com.am24.brickstemple.ui.screens.login.LoginScreen
import com.am24.brickstemple.ui.screens.orders.OrderHistoryScreen
import com.am24.brickstemple.ui.screens.product.ProductListScreen
import com.am24.brickstemple.ui.screens.wishlist.WishlistScreen
import com.am24.brickstemple.ui.screens.profile.ProfileScreen
import com.am24.brickstemple.ui.screens.settings.SettingsScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    paddingValues: PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = Screen.ProductList.route
    ) {
        composable(Screen.ProductList.route) { ProductListScreen() }
        composable(Screen.Cart.route) { CartScreen() }
        composable(Screen.Wishlist.route) { WishlistScreen() }
        composable(Screen.Profile.route) { ProfileScreen() }
        composable(Screen.OrderHistory.route) { OrderHistoryScreen() }
        composable(Screen.Settings.route) { SettingsScreen() }
        composable(Screen.About.route) { AboutScreen() }
        composable(Screen.Login.route) { LoginScreen() }
    }
}
