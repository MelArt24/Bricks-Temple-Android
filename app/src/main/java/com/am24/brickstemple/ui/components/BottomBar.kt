package com.am24.brickstemple.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.am24.brickstemple.ui.navigation.Screen
import androidx.compose.ui.graphics.Color


@Composable
fun BottomBar(navController: NavController) {

    val items = listOf(
        Screen.ProductList,
        Screen.Cart,
        Screen.Wishlist,
        Screen.Profile
    )

    val navBackStackEntry = navController.currentBackStackEntryAsState().value

    val currentRoute = navBackStackEntry
        ?.destination
        ?.route
        ?.substringBefore("/")


    NavigationBar {
        items.forEach { screen ->

            val selected = currentRoute == screen.route.substringBefore("/")

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (screen == Screen.ProductList) {
                        val popped = navController.popBackStack(
                            Screen.ProductList.route,
                            inclusive = false
                        )

                        if (!popped) {
                            navController.navigate(Screen.ProductList.route) {
                                launchSingleTop = true
                            }
                        }
                    } else {
                        navController.navigate(screen.route) {
                            launchSingleTop = true
                            restoreState = true

                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = when (screen) {
                            Screen.ProductList -> Icons.Default.Home
                            Screen.Cart -> Icons.Default.ShoppingCart
                            Screen.Wishlist -> Icons.Default.Favorite
                            Screen.Profile -> Icons.Default.Person
                            else -> Icons.Default.Home
                        },
                        contentDescription = screen.route,
                        tint = if (selected) Color.Red else Color.Gray
                    )
                },
                label = {
                    Text(
                        text = when (screen) {
                            Screen.ProductList -> "Home"
                            Screen.Cart -> "Cart"
                            Screen.Wishlist -> "Wishlist"
                            Screen.Profile -> "Profile"
                            else -> ""
                        },
                        color = if (selected) Color.Red else Color.Gray
                    )
                }
            )
        }
    }
}
