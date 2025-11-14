package com.am24.brickstemple.ui.screens.wishlist

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.am24.brickstemple.ui.navigation.Screen

@Composable
fun WishlistScreen(navController: NavController) {
    val items = emptyList<Any>() // TODO: реальні дані

    if (items.isEmpty()) {
        WishlistEmptyScreen(
            onGoToProductsClick = {
                navController.navigate(Screen.ProductList.route) {
                    launchSingleTop = true
                    restoreState = true
                    popUpTo(Screen.ProductList.route)
                }
            }
        )
    } else {
        Text("Wishlist with items")
    }
}
