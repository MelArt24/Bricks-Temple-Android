package com.am24.brickstemple.ui.screens.product

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.am24.brickstemple.ui.components.CategorySection
import com.am24.brickstemple.ui.components.ProductDemo
import com.am24.brickstemple.ui.navigation.Screen

@Composable
fun ProductListScreen(
    navController: NavController,
    paddingValues: PaddingValues
) {

    // for demonstration
    val demoItems = remember {
        mutableStateListOf(
            ProductDemo(
                name = "LEGO Star Wars 75419 “Death Star”",
                price = "$100",
                image = "https://www.lego.com/cdn/cs/set/assets/blt725a94446f56dbe2/75419_Prod.png",
                isFavorite = false,
                inCart = false
            ),
            ProductDemo(
                name = "LEGO Star Wars 42112 Technic",
                price = "$150",
                image = "https://www.lego.com/cdn/cs/set/assets/blt725a94446f56dbe2/75419_Prod.png",
                isFavorite = true,
                inCart = true
            )
        )
    }

    fun toggleFavorite(product: ProductDemo) {
        val index = demoItems.indexOf(product)
        if (index != -1) {
            demoItems[index] = demoItems[index].copy(
                isFavorite = !demoItems[index].isFavorite
            )
        }
    }

    fun toggleCart(product: ProductDemo) {
        val index = demoItems.indexOf(product)
        if (index != -1) {
            demoItems[index] = demoItems[index].copy(
                inCart = !demoItems[index].inCart
            )
        }
    }

    LazyColumn(
        modifier = Modifier.padding(paddingValues),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            CategorySection(
                title = "Sets",
                items = demoItems,
                onItemClick = { product ->
                    navController.navigate(Screen.ProductDetails.pass(1))
                },
                onMoreClick = {
                    navController.navigate(Screen.ProductCategory.pass("sets"))
                },
                onAddToCartClick = { product -> toggleCart(product) },
                onFavoriteClick = { product -> toggleFavorite(product) }
            )
        }

        item {
            CategorySection(
                title = "Minifigures",
                items = demoItems,
                onItemClick = { },
                onMoreClick = { },
                onAddToCartClick = { toggleCart(it) },
                onFavoriteClick = { toggleFavorite(it) }
            )
        }

        item {
            CategorySection(
                title = "Details",
                items = demoItems,
                onItemClick = { },
                onMoreClick = { },
                onAddToCartClick = { toggleCart(it) },
                onFavoriteClick = { toggleFavorite(it) }
            )
        }

        item {
            CategorySection(
                title = "Polybags",
                items = demoItems,
                onItemClick = { },
                onMoreClick = { },
                onAddToCartClick = { toggleCart(it) },
                onFavoriteClick = { toggleFavorite(it) }
            )
        }

        item {
            CategorySection(
                title = "Other",
                items = demoItems,
                onItemClick = { },
                onMoreClick = { },
                onAddToCartClick = { toggleCart(it) },
                onFavoriteClick = { toggleFavorite(it) }
            )
        }
    }
}
