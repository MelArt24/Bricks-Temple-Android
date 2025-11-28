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
                updating = updating,
                wishlistViewModel = wishlistViewModel,
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
    updating: Set<Int>,
    wishlistViewModel: WishlistViewModel,
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
                inCart = false,
                onClick = {
                    navController.navigate(Screen.ProductDetails.pass(p.id))
                },
                onAddToCartClick = {
                    // TODO cart later
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
