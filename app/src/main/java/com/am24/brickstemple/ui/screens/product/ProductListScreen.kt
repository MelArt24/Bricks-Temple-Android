package com.am24.brickstemple.ui.screens.product

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.am24.brickstemple.data.remote.dto.ProductDto
import com.am24.brickstemple.ui.components.CategorySection
import com.am24.brickstemple.ui.components.ProductDemo
import com.am24.brickstemple.ui.navigation.Screen
import com.am24.brickstemple.ui.viewmodels.ProductViewModel
import com.am24.brickstemple.ui.viewmodels.WishlistViewModel
import com.am24.brickstemple.utils.requireLogin

@Composable
fun ProductListScreen(
    navController: NavController,
    paddingValues: PaddingValues,
    productViewModel: ProductViewModel,
    wishlistViewModel: WishlistViewModel
) {
    val sections = productViewModel.sections.collectAsState().value
    val wishlist = wishlistViewModel.wishlist.collectAsState().value

    val setsState = sections["set"]
    val minifigsState = sections["minifigure"]
    val detailsState = sections["detail"]
    val polybagsState = sections["polybag"]
    val othersState = sections["other"]

    fun toggleCart(product: ProductDemo) {
        // TODO
    }

    fun mapDtoToDemo(dto: ProductDto): ProductDemo {
        val id = dto.id

        val isFavorite = id in wishlist

        return ProductDemo(
            id = dto.id,
            name = dto.name,
            price = "${dto.price}₴",
            image = dto.image ?: "",
            isFavorite = isFavorite,
            inCart = false
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
                onFavoriteClick = { product ->
                    requireLogin(navController) {
                        wishlistViewModel.toggle(product.id)
                    }
                }

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
                    navController.navigate(Screen.ProductCategory.pass("minifigures"))
                },
                onAddToCartClick = { product -> toggleCart(product) },
                onFavoriteClick = { product ->
                    requireLogin(navController) {
                        wishlistViewModel.toggle(product.id)
                    }
                }

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
                    navController.navigate(Screen.ProductCategory.pass("details"))
                },
                onAddToCartClick = { product -> toggleCart(product) },
                onFavoriteClick = { product ->
                    requireLogin(navController) {
                        wishlistViewModel.toggle(product.id)
                    }
                }

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
                    navController.navigate(Screen.ProductCategory.pass("polybags"))
                },
                onAddToCartClick = { product -> toggleCart(product) },
                onFavoriteClick = { product ->
                    requireLogin(navController) {
                        wishlistViewModel.toggle(product.id)
                    }
                }

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
                    navController.navigate(Screen.ProductCategory.pass("other"))
                },
                onAddToCartClick = { product -> toggleCart(product) },
                onFavoriteClick = { product ->
                    requireLogin(navController) {
                        wishlistViewModel.toggle(product.id)
                    }
                }

            )
        }
    }

}
