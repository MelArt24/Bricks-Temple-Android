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
import com.am24.brickstemple.domain.model.Product
import com.am24.brickstemple.ui.components.ProductItemCard
import com.am24.brickstemple.ui.navigation.AppNavGraphCallbacks
import com.am24.brickstemple.ui.navigation.Screen
import com.am24.brickstemple.ui.screens.cart.CartViewModel
import com.am24.brickstemple.ui.screens.wishlist.WishlistViewModel
import com.am24.brickstemple.utils.PriceFormatter
import com.am24.brickstemple.utils.requireLogin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductCategoryScreen(
    category: String?,
    navController: NavController,
    productViewModel: ProductViewModel,
    wishlistViewModel: WishlistViewModel,
    cartViewModel: CartViewModel,
    paddingValues: PaddingValues
) {
    if (category == null) {
        Text("Invalid category", modifier = Modifier.padding(paddingValues))
        return
    }

    var showFilters by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }

    val type = when (category) {
        "sets" -> "set"
        "minifigures" -> "minifigure"
        "details" -> "detail"
        "polybags" -> "polybag"
        else -> "other"
    }

    val productState = productViewModel.uiState.collectAsState().value
    val state = when (type) {
        "set" -> productState.sets
        "minifigure" -> productState.minifigs
        "detail" -> productState.details
        "polybag" -> productState.polybags
        else -> productState.others
    }

    val filteredState = productState.filteredProducts
    val sortOrder = productState.sortOrder
    val hasFiltersForType = productViewModel.hasActiveFiltersFor(type)

    val wishlist = wishlistViewModel.wishlist.collectAsState().value
    val updating = wishlistViewModel.isUpdating.collectAsState().value
    val cart = cartViewModel.cart.collectAsState().value

    val baseProducts = productViewModel.productsForCategory(type).products

    val searchQuery = productState.searchQuery

    val productsToShow = remember(baseProducts, sortOrder, searchQuery) {

        val newestFirst = baseProducts.sortedByDescending { it.createdAt ?: "" }

        val sorted = when (sortOrder) {
            SortOrder.PRICE_ASC -> newestFirst.sortedBy { it.price }
            SortOrder.PRICE_DESC -> newestFirst.sortedByDescending { it.price }
            SortOrder.YEAR_ASC -> newestFirst.sortedBy { it.year }
            SortOrder.YEAR_DESC -> newestFirst.sortedByDescending { it.year }
            else -> newestFirst
        }

        sorted.filter { product ->
            productViewModel.matchesQuery(product, searchQuery)
        }
    }

    LaunchedEffect(Unit) {
        AppNavGraphCallbacks.openSort = { showSort = true }
        AppNavGraphCallbacks.openFilters = { showFilters = true }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        if (hasFiltersForType) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = productViewModel::resetFilters) {
                    Text("Clear filters")
                }
            }
        }

        when {
            (state.isLoading || (hasFiltersForType && filteredState.isLoading)) && productsToShow.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            (state.error != null || (hasFiltersForType && filteredState.error != null)) && productsToShow.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Error loading products")
                }
            }

            else -> {
                CategoryContent(
                    products = productsToShow,
                    navController = navController,
                    wishlist = wishlist.keys.toList(),
                    cart = cart,
                    updating = updating,
                    wishlistViewModel = wishlistViewModel,
                    cartViewModel = cartViewModel
                )
            }
        }
    }

    if (showFilters) {
        FilterBottomSheet(
            type = type,
            hasActiveFilters = hasFiltersForType,
            onApply = { min, max, year ->
                productViewModel.applyFilters(type, min, max, year)
                showFilters = false
            },
            onReset = {
                productViewModel.resetFilters()
                showFilters = false
            },
            onDismiss = { showFilters = false }
        )
    }

    if (showSort) {
        SortBottomSheet(
            current = sortOrder,
            onSelect = {
                productViewModel.setSort(it)
                showSort = false
            },
            onDismiss = { showSort = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    current: SortOrder,
    onSelect: (SortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {

        Column(Modifier.padding(16.dp)) {

            Text("Sort", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))

            @Composable
            fun sortButton(order: SortOrder, label: String) {
                FilledTonalButton(
                    onClick = { onSelect(order) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = if (current == order)
                        ButtonDefaults.filledTonalButtonColors(MaterialTheme.colorScheme.primaryContainer)
                    else
                        ButtonDefaults.filledTonalButtonColors()
                ) {
                    Text(label)
                }
            }

            sortButton(SortOrder.PRICE_ASC, "Price ↑")
            sortButton(SortOrder.PRICE_DESC, "Price ↓")
            sortButton(SortOrder.YEAR_ASC, "Year ↑")
            sortButton(SortOrder.YEAR_DESC, "Year ↓")
            sortButton(SortOrder.NONE, "No sort")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    type: String,
    hasActiveFilters: Boolean,
    onApply: (String?, String?, String?) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {

        var minPrice by remember { mutableStateOf("") }
        var maxPrice by remember { mutableStateOf("") }
        var year by remember { mutableStateOf("") }

        Column(Modifier.padding(16.dp)) {

            Text("Filters", style = MaterialTheme.typography.headlineSmall)

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = minPrice,
                onValueChange = { minPrice = it },
                label = { Text("Min price") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = maxPrice,
                onValueChange = { maxPrice = it },
                label = { Text("Max price") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = year,
                onValueChange = { year = it },
                label = { Text("Year") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    onApply(
                        minPrice.ifBlank { null },
                        maxPrice.ifBlank { null },
                        year.ifBlank { null }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Apply filters") }

            if (hasActiveFilters) {
                Spacer(Modifier.height(8.dp))

                TextButton(
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Reset filters") }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}



@Composable
fun CategoryContent(
    products: List<Product>,
    navController: NavController,
    wishlist: List<Int>,
    cart: Map<Int, Int>,
    updating: Set<Int>,
    wishlistViewModel: WishlistViewModel,
    cartViewModel: CartViewModel,
) {
    val wishlistLoaded = !wishlistViewModel.isLoading.collectAsState().value

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize(),
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
                inCart = cart.containsKey(p.id),
                onClick = {
                    navController.navigate(Screen.ProductDetails.pass(p.id))
                },
                onAddToCartClick = {
                    requireLogin(navController) {
                        cartViewModel.toggle(p.id)
                    }
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
