package com.am24.brickstemple.ui.screens.product

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.am24.brickstemple.data.remote.dto.ProductDto
import com.am24.brickstemple.ui.components.ProductItemCard
import com.am24.brickstemple.ui.navigation.Screen
import com.am24.brickstemple.ui.viewmodels.CartViewModel
import com.am24.brickstemple.ui.viewmodels.ProductViewModel
import com.am24.brickstemple.ui.viewmodels.WishlistViewModel
import com.am24.brickstemple.utils.PriceFormatter
import com.am24.brickstemple.utils.requireLogin

@Composable
fun ProductCategoryScreen(
    category: String?,
    navController: NavController,
    productViewModel: ProductViewModel,
    wishlistViewModel: WishlistViewModel,
    cartViewModel: CartViewModel,
    paddingValues: PaddingValues
) {
    if (category == null) {
        Text("Invalid category", modifier = Modifier.padding(paddingValues))
        return
    }

    val type = when (category) {
        "sets" -> "set"
        "minifigures" -> "minifigure"
        "details" -> "detail"
        "polybags" -> "polybag"
        else -> "other"
    }

    val state by when (type) {
        "set" -> productViewModel.sets.collectAsState()
        "minifigure" -> productViewModel.minifigs.collectAsState()
        "detail" -> productViewModel.details.collectAsState()
        "polybag" -> productViewModel.polybags.collectAsState()
        else -> productViewModel.others.collectAsState()
    }

    val wishlist = wishlistViewModel.wishlist.collectAsState().value
    val updating = wishlistViewModel.isUpdating.collectAsState().value
    val cart = cartViewModel.cart.collectAsState().value

    when {
        state.isLoading && state.products.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        state.error != null && state.products.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Error: ${state.error}")
            }
        }

        else -> {
            CategoryContent(
                products = state.products,
                navController = navController,
                wishlist = wishlist.keys.toList(),
                cart = cart,
                updating = updating,
                wishlistViewModel = wishlistViewModel,
                cartViewModel = cartViewModel,
                paddingValues = paddingValues
            )
        }
    }
}



@Composable
fun CategoryContent(
    products: List<ProductDto>,
    navController: NavController,
    wishlist: List<Int>,
    cart: Map<Int, Int>,
    updating: Set<Int>,
    wishlistViewModel: WishlistViewModel,
    cartViewModel: CartViewModel,
    paddingValues: PaddingValues
) {
    val wishlistLoaded = !wishlistViewModel.isLoading.collectAsState().value

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(products.size) { index ->
            val p = products[index]

            val isFavorite = if (wishlistLoaded) {
                p.id in wishlist
            } else null

            val favoriteLoading = !wishlistLoaded || updating.contains(p.id)

            ProductItemCard(
                name = p.name,
                price = PriceFormatter.format(p.price) + "₴",
                imageUrl = p.image ?: "",
                isFavorite = isFavorite == true,
                inCart = cart.containsKey(p.id),
                onClick = {
                    navController.navigate(Screen.ProductDetails.pass(p.id))
                },
                onAddToCartClick = {
                    requireLogin(navController) {
                        cartViewModel.toggle(p.id)
                    }
                },
                onFavoriteClick = {
                    requireLogin(navController) {
                        wishlistViewModel.toggle(p.id)
                    }
                },
                favoriteLoading = favoriteLoading
            )
        }
    }
}
