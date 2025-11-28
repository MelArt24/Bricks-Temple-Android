package com.am24.brickstemple.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class ProductDemo(
    val id: Int,
    val name: String,
    val price: String,
    val image: String,
    val inCart: Boolean = false,
    val isFavorite: Boolean? = false,
    val favoriteLoading: Boolean = false,
    val isLoading: Boolean,
    )

@Composable
fun CategorySection(
    title: String,
    items: List<ProductDemo>,
    onItemClick: (ProductDemo) -> Unit,
    onMoreClick: () -> Unit,
    onAddToCartClick: (ProductDemo) -> Unit,
    onFavoriteClick: (ProductDemo) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )

            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "See more",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }


        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp)
        ) {
            items.forEach { product ->
                ProductItemCard(
                    name = product.name,
                    price = product.price,
                    imageUrl = product.image,
                    isFavorite = product.isFavorite,
                    inCart = product.inCart,
                    onClick = { onItemClick(product) },
                    onAddToCartClick = { onAddToCartClick(product) },
                    onFavoriteClick = { onFavoriteClick(product) },
                    favoriteLoading = product.favoriteLoading
                )

                Spacer(modifier = Modifier.width(12.dp))
            }
        }
    }
}
