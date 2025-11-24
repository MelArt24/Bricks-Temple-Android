package com.am24.brickstemple.ui.screens.product

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.am24.brickstemple.data.repositories.ProductRepository
import com.am24.brickstemple.ui.viewmodels.ProductDetailsViewModel
import com.am24.brickstemple.data.remote.ProductApiService
import com.am24.brickstemple.data.remote.KtorClientProvider
import com.am24.brickstemple.ui.components.FavoriteButton
import com.am24.brickstemple.ui.viewmodels.WishlistViewModel
import com.am24.brickstemple.utils.requireLogin

@Composable
fun ProductDetailsScreen(
    id: Int?,
    navController: NavController,
    paddingValues: PaddingValues,
    wishlistViewModel: WishlistViewModel
) {
    if (id == null) {
        Text("Invalid product ID", modifier = Modifier.padding(paddingValues))
        return
    }

    val repo = ProductRepository(ProductApiService(KtorClientProvider.client))

    val viewModel: ProductDetailsViewModel =
        viewModel(factory = ProductDetailsViewModel.Factory(id, repo))

    val state = viewModel.uiState.collectAsState().value
    val wishlist = wishlistViewModel.wishlist.collectAsState().value

    val updating = wishlistViewModel.isUpdating.collectAsState().value

    val isFavorite = id in wishlist
    val isLoading = updating.contains(id)

    var inCart by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
    ) {

        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Error: ${state.error}")
                }
            }

            state.product != null -> {
                val p = state.product

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 90.dp)
                ) {

                    AsyncImage(
                        model = p.image ?: "",
                        contentDescription = p.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = p.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp)
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "${p.price}₴",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 16.dp)
                    )

                    Spacer(Modifier.height(24.dp))

                    val details = listOfNotNull(
                        p.category?.let { "Category" to it },
                        p.number?.let { "Number" to it },
                        p.details?.let { "Details" to it.toString() },
                        p.minifigures?.let { "Minifigures" to it.toString() },
                        p.age?.let { "Age" to it },
                        p.year?.let { "Year" to it },
                        p.size?.let { "Size" to it },
                        p.condition?.let { "Condition" to it },
                        p.isAvailable?.let {
                            if (it) "Available" to "In stock" else "Available" to "Out of stock"
                        }
                    )

                    if (details.isNotEmpty()) {
                        Text(
                            text = "Product Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
                        )

                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            tonalElevation = 3.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                details.forEach { (label, value) ->
                                    DetailRow(label, value)
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                        }
                    }

                    if (!p.description.isNullOrBlank()) {
                        Spacer(Modifier.height(24.dp))

                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            tonalElevation = 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = p.description,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(40.dp))
                }

                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FavoriteButton(
                            isFavorite = isFavorite,
                            isLoading = isLoading,
                            onClick = {
                                requireLogin(navController) {
                                    wishlistViewModel.toggle(p.id)
                                }
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .weight(1f)
                        )

                        Button(
                            onClick = { inCart = !inCart },
                            modifier = Modifier.weight(4f)
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (inCart) "In Cart" else "Add to Cart")
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
