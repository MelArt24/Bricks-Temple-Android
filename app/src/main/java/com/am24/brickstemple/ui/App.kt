package com.am24.brickstemple.ui

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.am24.brickstemple.ui.navigation.AppNavGraph
import com.am24.brickstemple.ui.components.BottomBar
import com.am24.brickstemple.ui.components.DrawerContent
import com.am24.brickstemple.ui.components.TopBar
import com.am24.brickstemple.ui.navigation.Screen
import com.am24.brickstemple.ui.navigation.shouldShowMenu
import com.am24.brickstemple.ui.theme.BricksTempleTheme
import com.am24.brickstemple.ui.viewmodels.UserViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    BricksTempleTheme {
        val navController = rememberNavController()
        val navBackStackEntry = navController.currentBackStackEntryAsState().value
        val currentRoute = navBackStackEntry?.destination?.route
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val userViewModel: UserViewModel = viewModel()
        val userState by userViewModel.uiState.collectAsState()

        LaunchedEffect(Unit) {
            userViewModel.loadUser()
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                DrawerContent(
                    userName = userState.name,
                    userEmail = userState.email,
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }
                        navController.navigate(route)
                    },
                    onLogout = {
                        scope.launch { drawerState.close() }
                        // TODO logout
                    },
                    onLogin = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Login.route)
                    }
                )
            }
        ) {
            Scaffold(
                topBar = {
                    TopBar(
                        showMenu = shouldShowMenu(currentRoute),
                        onMenuClick = { scope.launch { drawerState.open() } },
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
}
