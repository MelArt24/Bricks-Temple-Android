package com.am24.brickstemple.ui.screens.cart

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.am24.brickstemple.ui.navigation.Screen

@Composable
fun CartScreen(navController: NavController) {
    val items = emptyList<Any>() // TODO: замінити на реальні дані з ViewModel

    if (items.isEmpty()) {
        CartEmptyScreen(
            onGoToProductsClick = {
                navController.navigate(Screen.ProductList.route) {
                    launchSingleTop = true
                    restoreState = true
                    popUpTo(Screen.ProductList.route)
                }
            }

        )
    } else {
        Text("Cart with items")
    }
}
