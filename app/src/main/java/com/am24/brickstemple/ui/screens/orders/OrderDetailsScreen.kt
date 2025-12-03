package com.am24.brickstemple.ui.screens.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.am24.brickstemple.data.remote.OrderApiService
import com.am24.brickstemple.ui.viewmodels.OrderViewModel
import com.am24.brickstemple.ui.viewmodels.ProductViewModel
import com.am24.brickstemple.utils.DateFormatter
import com.am24.brickstemple.utils.PriceFormatter

@Composable
fun OrderDetailsScreen(
    orderId: Int?,
    viewModel: OrderViewModel,
    productViewModel: ProductViewModel,
    paddingValues: PaddingValues
) {
    if (orderId == null) {
        Text("Invalid ID", modifier = Modifier.padding(paddingValues))
        return
    }

    val loading by viewModel.loading.collectAsState()
    val details by viewModel.orderDetails.collectAsState()

    val fullItems by viewModel.orderDetailsFull.collectAsState()

    LaunchedEffect(orderId) {
        viewModel.loadOrderDetails(orderId, productViewModel.repo)
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {

        when {
            loading -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            details == null -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No data")
            }

            else -> {
                val order = details!!.order

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {

                                Text(
                                    "Order #${order.id}",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(Modifier.height(6.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(statusColor(order.status), CircleShape)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Status: ${order.status}")
                                }

                                Spacer(Modifier.height(8.dp))
                                Text("Total: ${PriceFormatter.format(order.totalPrice)}")
                                Text("Date: ${DateFormatter.formatDate(order.createdAt)}")
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Text("Items", style = MaterialTheme.typography.titleMedium)
                    }

                    items(fullItems) { full ->
                        OrderItemCard(
                            name = full.product?.name ?: "Unknown product",
                            image = full.product?.image ?: "",
                            quantity = full.item.quantity,
                            price = full.item.priceAtPurchase
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OrderItemCard(
    name: String,
    image: String,
    quantity: Int,
    price: Double
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            AsyncImage(
                model = image,
                contentDescription = null,
                modifier = Modifier.size(60.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text("Qty: $quantity")
                Text("Price: ${PriceFormatter.format(price)}")
            }
        }
    }
}