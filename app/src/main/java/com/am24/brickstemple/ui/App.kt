package com.am24.brickstemple.ui

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.am24.brickstemple.ui.navigation.AppNavGraph
import com.am24.brickstemple.ui.components.BottomBar
import com.am24.brickstemple.ui.components.TopBar
import com.am24.brickstemple.ui.navigation.shouldShowMenu
import com.am24.brickstemple.ui.theme.BricksTempleTheme

@Composable
fun App() {
    BricksTempleTheme {
        val navController = rememberNavController()
        val navBackStackEntry = navController.currentBackStackEntryAsState().value
        val currentRoute = navBackStackEntry?.destination?.route

        Scaffold(
            topBar = {
                TopBar(
                    showMenu = shouldShowMenu(currentRoute),
                    onMenuClick = { /* TODO Open Drawer */ },
                    onBackClick = { navController.popBackStack() },
                    onSearchChange = { query -> /* TODO handle search */ }
                )
            },
            bottomBar = {
                BottomBar(navController)
            }
        ) { innerPadding ->
            AppNavGraph(
                navController = navController,
                paddingValues = innerPadding
            )
        }
    }
}
