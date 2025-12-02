package com.am24.brickstemple.ui.screens.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.am24.brickstemple.data.local.dao.ProductDao
import com.am24.brickstemple.data.mappers.toDto
import com.am24.brickstemple.data.remote.dto.ProductDto
import com.am24.brickstemple.ui.navigation.Screen
import com.am24.brickstemple.ui.viewmodels.CartViewModel
import com.am24.brickstemple.utils.PriceFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    navController: NavController,
    viewModel: CartViewModel,
    productDao: ProductDao,
    paddingValues: PaddingValues
) {
    val cartMap = viewModel.cart.collectAsState().value
    val isUpdating = viewModel.isUpdating.collectAsState().value
    val updatingQty = viewModel.updatingQuantity.collectAsState().value
    val isClearing = viewModel.isClearing.collectAsState().value
    val isLoading = viewModel.isLoading.collectAsState().value

    val checkoutInProgress = viewModel.checkoutInProgress.collectAsState().value
    val checkoutResult = viewModel.checkoutResult.collectAsState().value

    var products by remember { mutableStateOf(emptyList<com.am24.brickstemple.data.remote.dto.ProductDto>()) }
    val productIds = cartMap.keys.toList()

    val unauthorized by viewModel.unauthorized.collectAsState()

    LaunchedEffect(productIds) {
        products =
            if (productIds.isEmpty()) emptyList()
            else productDao.getByIds(productIds).map { it.toDto() }
    }

    LaunchedEffect(unauthorized) {
        if (unauthorized) {
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.Cart.route) { inclusive = true }
            }
            viewModel.clearUnauthorized()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {

        Box(modifier = Modifier.fillMaxSize()) {

            when {
                isLoading && products.isEmpty() -> LoadingView()

                products.isEmpty() -> CartEmptyScreen(
                    onGoToProductsClick = {
                        navController.navigate(Screen.ProductList.route) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(Screen.ProductList.route)
                        }
                    }
                )

                else -> CartContent(
                    products = products,
                    cartMap = cartMap,
                    updatingQty = updatingQty,
                    updatingIds = isUpdating,
                    checkoutInProgress = checkoutInProgress,
                    viewModel = viewModel,
                    navController = navController
                )
            }

            if (isClearing) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    if (checkoutResult != null) {
        AlertDialog(
            onDismissRequest = {
                viewModel.clearCheckoutResult()
            },
            title = { Text("Order Completed") },
            text = { Text("Your order has been successfully created!") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCheckoutResult()
                        navController.popBackStack()
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CartContent(
    products: List<ProductDto>,
    cartMap: Map<Int, Int>,
    updatingQty: Int?,
    updatingIds: Set<Int>,
    checkoutInProgress: Boolean,
    viewModel: CartViewModel,
    navController: NavController
) {
    val subtotal = products.sumOf { p ->
        val qty = cartMap[p.id] ?: 0
        p.price * qty
    }

    Column(Modifier.fillMaxSize()) {

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(products, key = { it.id }) { p ->
                val quantity = cartMap[p.id] ?: 1
                val spin = updatingQty == p.id
                val updating = p.id in updatingIds

                CartItemRow(
                    name = p.name,
                    image = p.image ?: "",
                    price = p.price,
                    quantity = quantity,
                    spin = spin,
                    updating = updating,
                    onIncrease = { viewModel.updateQuantity(p.id, +1) },
                    onDecrease = {
                        if (quantity > 1) viewModel.updateQuantity(p.id, -1)
                        else viewModel.removeCompletely(p.id)
                    },
                    onRemove = { viewModel.removeCompletely(p.id) },
                    onClick = {
                        navController.navigate(Screen.ProductDetails.pass(p.id))
                    }
                )
            }
        }

        CartSummarySection(
            subtotal = subtotal,
            delivery = 5.0,
            isCheckoutInProgress = checkoutInProgress,
            onCheckoutClick = { viewModel.checkout() }
        )
    }
}

@Composable
private fun CartItemRow(
    name: String,
    image: String,
    price: Double,
    quantity: Int,
    spin: Boolean,
    updating: Boolean,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            AsyncImage(
                model = image,
                contentDescription = name,
                modifier = Modifier.size(80.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text("Price: ${PriceFormatter.format(price)}₴")
                Text("Total: ${PriceFormatter.format(price * quantity)}₴")
            }

            if (spin) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDecrease, enabled = !updating) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                        }

                        Text(quantity.toString())

                        IconButton(onClick = onIncrease, enabled = !updating) {
                            Icon(Icons.Default.Add, contentDescription = "Increase")
                        }
                    }

                    IconButton(onClick = onRemove, enabled = !updating) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove")
                    }
                }
            }
        }
    }
}

@Composable
private fun CartSummarySection(
    subtotal: Double,
    delivery: Double,
    isCheckoutInProgress: Boolean,
    onCheckoutClick: () -> Unit
) {
    val total = subtotal + delivery

    Surface(
        tonalElevation = 4.dp,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {

            Text("Subtotal: ${PriceFormatter.format(subtotal)}₴")
            Text("Delivery: ${PriceFormatter.format(delivery)}₴")

            Divider(Modifier.padding(vertical = 8.dp))

            Text(
                "Total: ${PriceFormatter.format(total)}₴",
                style = MaterialTheme.typography.titleMedium
            )


            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onCheckoutClick,
                enabled = total > 0 && !isCheckoutInProgress,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isCheckoutInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Processing…")
                } else {
                    Text("Checkout")
                }
            }
        }
    }
}
