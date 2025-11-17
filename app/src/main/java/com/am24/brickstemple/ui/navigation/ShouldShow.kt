package com.am24.brickstemple.ui.navigation

fun shouldShowTopBar(route: String?): Boolean {
    if (route == null) return true
    return !route.startsWith(Screen.Login.route) &&
            !route.startsWith(Screen.Register.route)
}

fun shouldShowBackArrow(route: String?): Boolean {
    if (route == null) return false
    return route.startsWith(Screen.ProductDetails.route) ||
            route.startsWith(Screen.ProductCategory.route) ||
            route.startsWith(Screen.Settings.route) ||
            route.startsWith(Screen.About.route) ||
            route.startsWith(Screen.ViewDetails.route) ||
            route.startsWith(Screen.OrderHistory.route)
}

fun shouldShowBottomBar(route: String?): Boolean {
    if (route == null) return true
    return !route.startsWith(Screen.ProductDetails.route)
}