package com.am24.brickstemple.ui.screens.wishlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.am24.brickstemple.ui.components.ProductItemCard
import com.am24.brickstemple.ui.navigation.Screen
import com.am24.brickstemple.ui.viewmodels.ProductViewModel
import com.am24.brickstemple.ui.viewmodels.WishlistViewModel

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WishlistScreen(
    navController: NavController,
    wishlistViewModel: WishlistViewModel,
    productViewModel: ProductViewModel,
    paddingValues: PaddingValues
) {
    val wishlist = wishlistViewModel.wishlist.collectAsState().value
    val updating = wishlistViewModel.isUpdating.collectAsState().value

    val sections = productViewModel.sections.collectAsState().value
    val allProducts = sections.values.flatMap { it.products }
    val items = allProducts.filter { it.id in wishlist }

    var refreshing by remember { mutableStateOf(false) }

    LaunchedEffect(refreshing) {
        if (refreshing) {
            wishlistViewModel.refresh()
            refreshing = false
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = { refreshing = true }
    )

    Box(
        modifier = Modifier
            .padding(paddingValues)
            .pullRefresh(pullRefreshState)
            .fillMaxSize()
    ) {

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
                modifier = Modifier.fillMaxSize(),
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
                        favoriteDisabled = p.id in updating
                    )
                }
            }
        }

        PullRefreshIndicator(
            refreshing = refreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        if (updating.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
