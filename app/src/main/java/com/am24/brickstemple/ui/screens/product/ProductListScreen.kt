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
import com.am24.brickstemple.utils.PriceFormatter
import com.am24.brickstemple.utils.requireLogin
import androidx.compose.foundation.layout.Box
import com.am24.brickstemple.auth.AuthSession
import com.am24.brickstemple.ui.viewmodels.CartViewModel

@Composable
fun ProductListScreen(
    navController: NavController,
    paddingValues: PaddingValues,
    productViewModel: ProductViewModel,
    wishlistViewModel: WishlistViewModel,
    cartViewModel: CartViewModel
) {
    val setsState = productViewModel.sets.collectAsState().value
    val minifigsState = productViewModel.minifigs.collectAsState().value
    val detailsState = productViewModel.details.collectAsState().value
    val polybagsState = productViewModel.polybags.collectAsState().value
    val othersState = productViewModel.others.collectAsState().value

    val wishlist = wishlistViewModel.wishlist.collectAsState().value
    val updating = wishlistViewModel.isUpdating.collectAsState().value

    val wishlistLoading = wishlistViewModel.isLoading.collectAsState().value
    val productsLoading = productViewModel.loading.collectAsState().value
    val userIsLoggedIn = AuthSession.isLoggedIn()

    val cart = cartViewModel.cart.collectAsState().value

    fun mapDtoToDemo(dto: ProductDto): ProductDemo {
        val id = dto.id

        if (!userIsLoggedIn) {
            return ProductDemo(
                id = id,
                name = dto.name,
                price = PriceFormatter.format(dto.price) + "₴",
                image = dto.image ?: "",
                isFavorite = false,
                favoriteLoading = false,
                inCart = false,
                isLoading = false
            )
        }

        if (wishlistLoading) {
            return ProductDemo(
                id = id,
                name = dto.name,
                price = PriceFormatter.format(dto.price) + "₴",
                image = dto.image ?: "",
                isFavorite = null,
                favoriteLoading = true,
                inCart = false,
                isLoading = false
            )
        }

        val isFav = id in wishlist

        return ProductDemo(
            id = id,
            name = dto.name,
            price = PriceFormatter.format(dto.price) + "₴",
            image = dto.image ?: "",
            isFavorite = isFav,
            favoriteLoading = updating.contains(id),
            inCart = cart.containsKey(id),
            isLoading = updating.contains(id)
        )
    }

    data class CategoryBlock(
        val title: String,
        val route: String,
        val items: List<ProductDto>
    )

    val blocks = listOf(
        CategoryBlock("Sets", "sets", setsState.products),
        CategoryBlock("Minifigures", "minifigures", minifigsState.products),
        CategoryBlock("Details", "details", detailsState.products),
        CategoryBlock("Polybags", "polybags", polybagsState.products),
        CategoryBlock("Other", "other", othersState.products)
    )

    Box(Modifier.padding(paddingValues)) {
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            blocks.forEach { block ->
                item {
                    CategorySection(
                        title = block.title,
                        items = block.items.map { mapDtoToDemo(it) }.take(5),
                        onItemClick = {
                            navController.navigate(Screen.ProductDetails.pass(it.id))
                        },
                        onMoreClick = {
                            navController.navigate(Screen.ProductCategory.pass(block.route))
                        },
                        onAddToCartClick = {
                            requireLogin(navController) {
                                cartViewModel.toggle(it.id)
                            }
                        },
                        onFavoriteClick = {
                            requireLogin(navController) { wishlistViewModel.toggle(it.id) }
                        }
                    )
                }
            }
        }
    }

}
