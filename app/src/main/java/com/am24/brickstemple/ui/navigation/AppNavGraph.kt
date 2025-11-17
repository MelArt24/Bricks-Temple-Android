package com.am24.brickstemple.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.am24.brickstemple.ui.screens.about.AboutScreen
import com.am24.brickstemple.ui.screens.cart.CartScreen
import com.am24.brickstemple.ui.screens.auth.LoginScreen
import com.am24.brickstemple.ui.screens.auth.RegisterScreen
import com.am24.brickstemple.ui.screens.orders.OrderHistoryScreen
import com.am24.brickstemple.ui.screens.orders.ViewDetailsScreen
import com.am24.brickstemple.ui.screens.product.ProductCategoryScreen
import com.am24.brickstemple.ui.screens.product.ProductDetailsScreen
import com.am24.brickstemple.ui.screens.product.ProductListScreen
import com.am24.brickstemple.ui.screens.wishlist.WishlistScreen
import com.am24.brickstemple.ui.screens.profile.ProfileScreen
import com.am24.brickstemple.ui.screens.settings.SettingsScreen
import com.am24.brickstemple.ui.viewmodels.ProductViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    paddingValues: PaddingValues,
    productViewModel: ProductViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.ProductList.route
    ) {
        composable(Screen.ProductList.route) {
            ProductListScreen(
                navController = navController,
                paddingValues = paddingValues,
                productViewModel = productViewModel
            )
        }
        composable(Screen.Cart.route) { CartScreen(navController) }
        composable(Screen.Wishlist.route) { WishlistScreen(navController) }
        composable(Screen.Profile.route) { ProfileScreen() }
        composable(Screen.OrderHistory.route) { OrderHistoryScreen() }
        composable(Screen.Settings.route) { SettingsScreen() }
        composable(Screen.About.route) { AboutScreen() }
        composable(Screen.Login.route) { LoginScreen() }

        composable(Screen.ProductDetails.route) { backStack ->
            val id = backStack.arguments?.getString("id")?.toInt()
            ProductDetailsScreen(
                id = id,
                paddingValues = paddingValues
            )
        }

        composable(Screen.ProductCategory.route) { backStack ->
            val category = backStack.arguments?.getString("category")
            ProductCategoryScreen(
                category,
                navController = navController,
                productViewModel = productViewModel,
                paddingValues = paddingValues
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen()
        }

        composable(Screen.ViewDetails.route) { backStack ->
            val id = backStack.arguments?.getString("id")?.toInt()
            ViewDetailsScreen(id)
        }

    }
}
