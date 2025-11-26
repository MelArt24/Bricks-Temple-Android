package com.am24.brickstemple.ui.screens.wishlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.am24.brickstemple.ui.navigation.Screen
import com.am24.brickstemple.ui.viewmodels.ProductViewModel
import com.am24.brickstemple.ui.viewmodels.WishlistViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import com.am24.brickstemple.ui.components.WishlistBottomBar

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
    val updatingQuantity = wishlistViewModel.updatingQuantity.collectAsState().value

    val sections = productViewModel.sections.collectAsState().value
    val allProducts = sections.values.flatMap { it.products }
    val items = allProducts.filter { it.id in wishlist }

    val itemDtos = wishlistViewModel.items.collectAsState().value
    val itemMap = itemDtos.associateBy { it.productId }

    val isClearing = wishlistViewModel.isClearing.collectAsState().value

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
            Column(modifier = Modifier.fillMaxSize()) {

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items, key = { it.id }) { p ->
                        val dto = itemMap[p.id]
                        val quantity = dto?.quantity ?: 1
                        val spinQty = (updatingQuantity == p.id)
                        val isUpdatingItem = p.id in updating

                        WishlistItemRow(
                            name = p.name,
                            priceText = "${p.price}₴",
                            imageUrl = p.image ?: "",
                            quantity = quantity,
                            spinQuantity = spinQty,
                            isUpdating = isUpdatingItem,
                            onClick = { navController.navigate(Screen.ProductDetails.pass(p.id)) },
                            onIncrease = {
                                if (!isUpdatingItem) wishlistViewModel.updateQuantity(p.id, +1)
                            },
                            onDecrease = {
                                if (!isUpdatingItem && !spinQty && quantity > 0)
                                    wishlistViewModel.updateQuantity(p.id, -1)
                            },
                            onRemove = { wishlistViewModel.toggle(p.id) },
                            onAddToCart = { /* TODO */ }
                        )
                    }
                }

                WishlistBottomBar(
                    onClear = { wishlistViewModel.clearWishlist() },
                    onCheckout = { /* TODO: checkout flow */ },
                    enabled = updating.isEmpty() && !refreshing
                )
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

        if (isClearing) {
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

@Composable
private fun WishlistItemRow(
    name: String,
    priceText: String,
    imageUrl: String,
    quantity: Int,
    spinQuantity: Boolean,
    isUpdating: Boolean,
    onClick: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    onAddToCart: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isUpdating, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(imageUrl),
                contentDescription = name,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = priceText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDecrease,
                        enabled = !isUpdating && quantity > 0,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease quantity"
                        )
                    }

                    if (spinQuantity) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = quantity.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = onIncrease,
                        enabled = !isUpdating,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase quantity"
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxHeight()
            ) {
                IconButton(
                    onClick = onRemove,
                    enabled = !isUpdating,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector =Icons.Default.Delete,
                        contentDescription = "Remove from wishlist",
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(Modifier.height(8.dp))

                IconButton(
                    onClick = onAddToCart,
                    enabled = !isUpdating,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Add to cart"
                    )
                }
            }
        }
    }
}
