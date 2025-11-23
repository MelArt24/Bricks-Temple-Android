package com.am24.brickstemple.ui.screens.wishlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.am24.brickstemple.ui.components.ProductItemCard
import com.am24.brickstemple.ui.navigation.Screen
import com.am24.brickstemple.ui.viewmodels.ProductViewModel
import com.am24.brickstemple.ui.viewmodels.WishlistViewModel



@Composable
fun WishlistScreen(
    navController: NavController,
    wishlistViewModel: WishlistViewModel,
    productViewModel: ProductViewModel,
    paddingValues: PaddingValues
) {
    val wishlist = wishlistViewModel.wishlist.collectAsState().value

    val sections = productViewModel.sections.collectAsState().value
    val allProducts = sections.values.flatMap { it.products }

    val items = allProducts.filter { it.id in wishlist }

    val updating: Set<Int> =
        wishlistViewModel.isUpdating.collectAsState().value


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
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items.size) { index ->
                val p = items[index]

                ProductItemCard(
                    name = p.name,
                    price = "${p.price}₴",
                    imageUrl = p.image ?: "",
                    isFavorite = true,
                    inCart = false,
                    onClick = {
                        navController.navigate(Screen.ProductDetails.pass(p.id))
                    },
                    onAddToCartClick = { /* TODO */ },
                    onFavoriteClick = {
                        wishlistViewModel.toggle(p.id)
                    },
                    favoriteDisabled = updating.any { it == p.id }
                )
            }
        }
    }
}
