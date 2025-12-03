package com.am24.brickstemple.ui.screens.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.am24.brickstemple.data.remote.OrderApiService
import com.am24.brickstemple.ui.navigation.Screen
import com.am24.brickstemple.ui.viewmodels.OrderViewModel
import com.am24.brickstemple.utils.DateFormatter
import com.am24.brickstemple.utils.PriceFormatter

@Composable
fun statusColor(status: String): Color = when (status.uppercase()) {
    "PENDING" -> Color(0xFFFF9800)
    "CONFIRMED" -> Color(0xFFFFEB3B)
    "DELIVERED" -> Color(0xFF4CAF50)
    "CANCELLED" -> Color(0xFFF44336)
    else -> MaterialTheme.colorScheme.primary
}


@Composable
fun OrderHistoryScreen(
    navController: NavController,
    viewModel: OrderViewModel,
    paddingValues: PaddingValues
) {
    val loading by viewModel.loading.collectAsState()
    val orders by viewModel.orders.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadOrders() }

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

            orders.isEmpty() -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Order history is empty", style = MaterialTheme.typography.titleMedium)
            }

            else -> {
                val sorted = orders.sortedByDescending { it.createdAt }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sorted) { order ->
                        OrderCard(order) {
                            navController.navigate(Screen.ViewDetails.pass(order.id))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderCard(
    order: OrderApiService.OrderResponse,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(16.dp)) {

            Text("Order #${order.id}", style = MaterialTheme.typography.titleMedium)

            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(statusColor(order.status), shape = CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text("Status: ${order.status}")
            }

            Spacer(Modifier.height(4.dp))
            Text("Total: ${PriceFormatter.format(order.totalPrice)}")

            Spacer(Modifier.height(4.dp))
            Text("Date: ${DateFormatter.formatDate(order.createdAt)}")
        }
    }
}