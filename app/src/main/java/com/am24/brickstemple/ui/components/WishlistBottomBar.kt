package com.am24.brickstemple.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WishlistBottomBar(
    onClear: () -> Unit,
    onCheckout: () -> Unit,
    enabled: Boolean
) {
    Surface(
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Button(
                onClick = onCheckout,
                modifier = Modifier.weight(1f),
                enabled = enabled
            ) {
                Text("Add all to cart")
            }

            OutlinedButton(
                onClick = onClear,
                modifier = Modifier.weight(1f),
                enabled = enabled
            ) {
                Text("Clear wishlist")
            }
        }
    }
}
