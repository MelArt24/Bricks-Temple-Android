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
        Text("Invalid category")
        return
    }

    LaunchedEffect(category) {
        productViewModel.loadType(
            when (category) {
                "sets" -> "set"
                "minifigures" -> "minifigure"
                "details" -> "detail"
                "polybags" -> "polybag"
                else -> "other"
            }
        )
    }

    val wishlist = wishlistViewModel.wishlist.collectAsState().value
    val sections = productViewModel.sections.collectAsState().value

    val key = when (category) {
        "sets" -> "set"
        "minifigures" -> "minifigure"
        "details" -> "detail"
        "polybags" -> "polybag"
        else -> "other"
    }

    val state = sections[key]

    when {
        state == null || state.isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        }

        state.error != null -> {
            Text(
                text = "Error: ${state.error}",
                modifier = Modifier.padding(paddingValues)
            )
        }

        else -> {
            CategoryContent(
                products = state.products,
                navController = navController,
                wishlist = wishlist.keys.toList(),
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
    wishlistViewModel: WishlistViewModel,
    paddingValues: PaddingValues
) {
    val updating = wishlistViewModel.isUpdating.collectAsState().value

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
            val isFav = p.id in wishlist
            val isLoading = updating.contains(p.id)

            ProductItemCard(
                name = p.name,
                price = "${p.price}₴",
                imageUrl = p.image ?: "",
                isFavorite = isFav,
                inCart = false,
                onClick = {
                    navController.navigate(Screen.ProductDetails.pass(p.id))
                },
                onAddToCartClick = {
                // TODO
                },
                onFavoriteClick = {
                    requireLogin(navController) {
                        wishlistViewModel.toggle(p.id)
                    }
                },
                favoriteDisabled = isLoading
            )
        }
    }
}
