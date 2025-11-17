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

@Composable
fun ProductCategoryScreen(
    category: String?,
    navController: NavController,
    productViewModel: ProductViewModel,
    paddingValues: PaddingValues
) {
    if (category == null) {
        Text("Invalid category")
        return
    }

    LaunchedEffect(category) {
        when (category) {
            "sets" -> productViewModel.loadType("set")
            "minifigures" -> productViewModel.loadType("minifigure")
            "details" -> productViewModel.loadType("detail")
            "polybags" -> productViewModel.loadType("polybag")
            "other" -> productViewModel.loadType("other")
        }
    }

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
                paddingValues = paddingValues
            )
        }
    }
}

@Composable
fun CategoryContent(
    products: List<ProductDto>,
    navController: NavController,
    paddingValues: PaddingValues
) {
    val favoriteState = remember { mutableStateMapOf<Int, Boolean>() }
    val cartState = remember { mutableStateMapOf<Int, Boolean>() }

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

            val isFav = favoriteState[p.id] ?: false
            val inCart = cartState[p.id] ?: false

            ProductItemCard(
                name = p.name,
                price = "${p.price}₴",
                imageUrl = p.image ?: "",
                isFavorite = isFav,
                inCart = inCart,
                onClick = {
                    navController.navigate(Screen.ProductDetails.pass(p.id))
                },
                onAddToCartClick = {
                    cartState[p.id] = !inCart
                },
                onFavoriteClick = {
                    favoriteState[p.id] = !isFav
                }
            )
        }
    }
}
