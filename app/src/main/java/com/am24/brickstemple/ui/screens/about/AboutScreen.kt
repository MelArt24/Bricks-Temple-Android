package com.am24.brickstemple.ui.screens.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.am24.brickstemple.R

@Composable
fun AboutScreen(paddingValues: PaddingValues) {

    val context = LocalContext.current
    val packageInfo = remember {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }

    val versionName = packageInfo.versionName

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Image(
            painter = painterResource(id = R.drawable.bricks_temple_logo),
            contentDescription = "App Logo",
            modifier = Modifier
                .size(150.dp)
                .padding(bottom = 20.dp)
        )

        Text(
            text = "Bricks Temple",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Version $versionName",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Bricks Temple is a modern application for browsing LEGO sets, minifigures, parts, polybags and other stuff. " +
                    "The application is designed to make the user's interaction with the catalog as fast and pleasant as possible.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "The project was created as an educational mobile application that combines " +
                    "Android Jetpack Compose, Ktor backend and PostgreSQL.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
