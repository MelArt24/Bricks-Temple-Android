package com.am24.brickstemple.ui.navigation

sealed class Screen(val route: String) {
    data object ProductList : Screen("product_list")
    data object Cart : Screen("cart")
    data object Wishlist : Screen("wishlist")
    data object Profile : Screen("profile")
}
