package com.am24.brickstemple.ui.screens.product

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.am24.brickstemple.data.remote.dto.ProductDto
import com.am24.brickstemple.ui.components.CategorySection
import com.am24.brickstemple.ui.components.ProductDemo
import com.am24.brickstemple.ui.navigation.Screen
import com.am24.brickstemple.ui.viewmodels.ProductViewModel

@Composable
fun ProductListScreen(
    navController: NavController,
    paddingValues: PaddingValues,
    productViewModel: ProductViewModel
) {
    val sections = productViewModel.sections.collectAsState().value

    val setsState = sections["set"]
    val minifigsState = sections["minifigure"]
    val detailsState = sections["detail"]
    val polybagsState = sections["polybag"]
    val othersState = sections["other"]

    val favoriteState = remember { mutableStateMapOf<Int, Boolean>() }
    val cartState = remember { mutableStateMapOf<Int, Boolean>() }

    fun toggleFavorite(product: ProductDemo) {
        val id = product.id
        val current = favoriteState[id] ?: product.isFavorite
        favoriteState[id] = !current
    }

    fun toggleCart(product: ProductDemo) {
        val id = product.id
        val current = cartState[id] ?: product.inCart
        cartState[id] = !current
    }

    fun mapDtoToDemo(dto: ProductDto): ProductDemo {
        val id = dto.id
        val fav = favoriteState[id] ?: false
        val inCart = cartState[id] ?: false

        return ProductDemo(
            id = dto.id,
            name = dto.name,
            price = "${dto.price}₴",
            image = dto.image ?: "",
            isFavorite = fav,
            inCart = inCart
        )
    }

    LazyColumn(
        modifier = Modifier.padding(paddingValues),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            val items = setsState?.products
                ?.map { mapDtoToDemo(it) }
                ?.take(5) ?: emptyList()

            CategorySection(
                title = "Sets",
                items = items,
                onItemClick = { product ->
                    navController.navigate(Screen.ProductDetails.pass(product.id))
                },
                onMoreClick = {
                    navController.navigate(Screen.ProductCategory.pass("sets"))
                },
                onAddToCartClick = { product -> toggleCart(product) },
                onFavoriteClick = { product -> toggleFavorite(product) }
            )
        }

        item {
            val items = minifigsState?.products
                ?.map { mapDtoToDemo(it) }
                ?.take(5) ?: emptyList()

            CategorySection(
                title = "Minifigures",
                items = items,
                onItemClick = { product ->
                    navController.navigate(Screen.ProductDetails.pass(product.id))
                },
                onMoreClick = {
                    navController.navigate(Screen.ProductCategory.pass("sets"))
                },
                onAddToCartClick = { product -> toggleCart(product) },
                onFavoriteClick = { product -> toggleFavorite(product) }
            )
        }

        item {
            val items = detailsState?.products
                ?.map { mapDtoToDemo(it) }
                ?.take(5) ?: emptyList()

            CategorySection(
                title = "Details",
                items = items,
                onItemClick = { product ->
                    navController.navigate(Screen.ProductDetails.pass(product.id))
                },
                onMoreClick = {
                    navController.navigate(Screen.ProductCategory.pass("sets"))
                },
                onAddToCartClick = { product -> toggleCart(product) },
                onFavoriteClick = { product -> toggleFavorite(product) }
            )
        }

        item {
            val items = polybagsState?.products
                ?.map { mapDtoToDemo(it) }
                ?.take(5) ?: emptyList()

            CategorySection(
                title = "Polybags",
                items = items,
                onItemClick = { product ->
                    navController.navigate(Screen.ProductDetails.pass(product.id))
                },
                onMoreClick = {
                    navController.navigate(Screen.ProductCategory.pass("sets"))
                },
                onAddToCartClick = { product -> toggleCart(product) },
                onFavoriteClick = { product -> toggleFavorite(product) }
            )
        }

        item {
            val items = othersState?.products
                ?.map { mapDtoToDemo(it) }
                ?.take(5) ?: emptyList()

            CategorySection(
                title = "Other",
                items = items,
                onItemClick = { product ->
                    navController.navigate(Screen.ProductDetails.pass(product.id))
                },
                onMoreClick = {
                    navController.navigate(Screen.ProductCategory.pass("sets"))
                },
                onAddToCartClick = { product -> toggleCart(product) },
                onFavoriteClick = { product -> toggleFavorite(product) }
            )
        }
    }

}
