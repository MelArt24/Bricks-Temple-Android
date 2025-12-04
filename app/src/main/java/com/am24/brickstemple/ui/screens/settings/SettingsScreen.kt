package com.am24.brickstemple.ui.screens.settings

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.am24.brickstemple.data.local.ThemePreferenceDataStore
import com.am24.brickstemple.ui.navigation.Screen
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    paddingValues: PaddingValues = PaddingValues(),
    navController: NavController? = null,
) {
    val context = LocalContext.current

    val themeStore = remember { ThemePreferenceDataStore(context) }
    val isDark by themeStore.isDarkMode.collectAsState(initial = false)

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    navController?.navigate(Screen.ChangePassword.route)
                },
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Text("Change Password", style = MaterialTheme.typography.titleMedium)
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    Icons.Default.DarkMode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text("Appearance", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (isDark) "Dark mode" else "Light mode",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Crossfade(targetState = isDark) { dark ->
                        Icon(
                            if (dark) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Switch(
                        checked = isDark,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                themeStore.setDarkMode(enabled)
                            }
                        }
                    )
                }
            }
        }
    }
}
