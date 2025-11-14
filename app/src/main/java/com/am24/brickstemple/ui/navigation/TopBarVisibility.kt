package com.am24.brickstemple.ui.navigation

fun shouldShowMenu(route: String?): Boolean {
    return route == Screen.ProductList.route ||
            route == Screen.Cart.route ||
            route == Screen.Wishlist.route ||
            route == Screen.Profile.route           // ?
}
