package com.am24.brickstemple.ui.navigation

fun shouldShowMenu(route: String?): Boolean {
    return when (route) {
        Screen.ProductDetails.route -> false
        Screen.Login.route -> false
        Screen.Register.route -> false
        else -> true
    }
}
