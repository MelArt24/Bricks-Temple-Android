package com.am24.brickstemple.ui.navigation

sealed class Screen(val route: String) {
    data object ProductList : Screen("product_list")
    data object Cart : Screen("cart")
    data object Wishlist : Screen("wishlist")
    data object Profile : Screen("profile")

    data object OrderHistory : Screen("order_history")
    data object About : Screen("about")
    data object Settings : Screen("settings")
    data object Login : Screen("login")

    data object ProductDetails : Screen("product_details/{id}") {
        fun pass(id: Int) = "product_details/$id"
    }

    data object ProductCategory : Screen("product_category/{category}") {
        fun pass(category: String) = "product_category/$category"
    }

    data object Register : Screen("register")

    data object ViewDetails : Screen("order_details/{id}") {
        fun pass(id: Int) = "order_details/$id"
    }

    object Splash : Screen("splash")

}
