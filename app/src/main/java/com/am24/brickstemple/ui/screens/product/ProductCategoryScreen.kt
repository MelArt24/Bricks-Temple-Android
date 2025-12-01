package com.am24.brickstemple.ui.screens.product

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.am24.brickstemple.data.remote.dto.ProductDto
import com.am24.brickstemple.ui.components.ProductItemCard
import com.am24.brickstemple.ui.navigation.Screen
import com.am24.brickstemple.ui.viewmodels.CartViewModel
import com.am24.brickstemple.ui.viewmodels.ProductViewModel
import com.am24.brickstemple.ui.viewmodels.SortOrder
import com.am24.brickstemple.ui.viewmodels.WishlistViewModel
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

    val state by when (type) {
        "set" -> productViewModel.sets.collectAsState()
        "minifigure" -> productViewModel.minifigs.collectAsState()
        "detail" -> productViewModel.details.collectAsState()
        "polybag" -> productViewModel.polybags.collectAsState()
        else -> productViewModel.others.collectAsState()
    }

    val filters by productViewModel.filters.collectAsState()
    val filteredState by productViewModel.filteredProducts.collectAsState()
    val sortOrder by productViewModel.sortOrder.collectAsState()

    val hasFilters = filters.minPrice != null ||
            filters.maxPrice != null ||
            filters.year != null

    val wishlist = wishlistViewModel.wishlist.collectAsState().value
    val updating = wishlistViewModel.isUpdating.collectAsState().value
    val cart = cartViewModel.cart.collectAsState().value

    val baseProducts =
        if (hasFilters) filteredState.products else state.products

    val productsToShow = remember(baseProducts, sortOrder) {

        val newestFirst = baseProducts.sortedByDescending { it.createdAt ?: "" }

        when (sortOrder) {
            SortOrder.PRICE_ASC -> newestFirst.sortedBy { it.price }
            SortOrder.PRICE_DESC -> newestFirst.sortedByDescending { it.price }
            SortOrder.YEAR_ASC -> newestFirst.sortedBy { it.year }
            SortOrder.YEAR_DESC -> newestFirst.sortedByDescending { it.year }
            else -> newestFirst
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {

        TopAppBar(
            title = { Text(category.uppercase()) },
            actions = {
                IconButton(onClick = { showSort = true }) {
                    Icon(Icons.Default.Sort, "Sort")
                }
                IconButton(onClick = { showFilters = true }) {
                    Icon(Icons.Default.FilterList, "Filters")
                }
            }
        )

        when {
            (state.isLoading || filteredState.isLoading) && productsToShow.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            (state.error != null || filteredState.error != null) && productsToShow.isEmpty() -> {
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
            onApply = { min, max, year ->
                productViewModel.applyFilters(type, min, max, year)
                showFilters = false
            },
            onReset = {
                productViewModel.applyFilters(type, null, null, null)
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

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Reset filters") }

            Spacer(Modifier.height(16.dp))
        }
    }
}



@Composable
fun CategoryContent(
    products: List<ProductDto>,
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
