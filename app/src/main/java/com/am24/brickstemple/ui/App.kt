package com.am24.brickstemple.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.am24.brickstemple.auth.AuthSession
import com.am24.brickstemple.auth.AuthStorage
import com.am24.brickstemple.data.remote.KtorClientProvider
import com.am24.brickstemple.data.remote.ProductApiService
import com.am24.brickstemple.data.repositories.ProductRepository
import com.am24.brickstemple.ui.navigation.AppNavGraph
import com.am24.brickstemple.ui.components.BottomBar
import com.am24.brickstemple.ui.components.DrawerContent
import com.am24.brickstemple.ui.components.TopBar
import com.am24.brickstemple.ui.navigation.Screen
import com.am24.brickstemple.ui.navigation.shouldShowBackArrow
import com.am24.brickstemple.ui.navigation.shouldShowBottomBar
import com.am24.brickstemple.ui.navigation.shouldShowTopBar
import com.am24.brickstemple.ui.theme.BricksTempleTheme
import com.am24.brickstemple.ui.viewmodels.AuthViewModel
import com.am24.brickstemple.ui.viewmodels.ProductViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    BricksTempleTheme {
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            AuthStorage.load(context)
        }

        if (!AuthSession.isLoaded) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@BricksTempleTheme
        }

        val navController = rememberNavController()
        val navBackStackEntry = navController.currentBackStackEntryAsState().value
        val currentRoute = navBackStackEntry?.destination?.route
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        val authViewModel: AuthViewModel = viewModel(
            factory = AuthViewModel.AuthViewModelFactory(context)
        )

        LaunchedEffect(AuthSession.isLoaded, AuthSession.token) {
            if (AuthSession.isLoaded && AuthSession.isLoggedIn()) {
                authViewModel.loadCurrentUser()
            }
        }

        val uiAuth = authViewModel.uiState.collectAsState().value

        val productRepository = remember {
            ProductRepository(
                ProductApiService(
                    KtorClientProvider.client
                )
            )
        }

        val productViewModel: ProductViewModel =
            viewModel(factory = ProductViewModel.Factory(productRepository))

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                DrawerContent(
                    userName = when {
                        uiAuth.isLoading -> "Loading..."
                        uiAuth.username.isNotBlank() -> uiAuth.username
                        else -> null
                    },
                    userEmail = uiAuth.email.takeIf { it.isNotBlank() },
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
                    if (shouldShowTopBar(currentRoute)) {
                        TopBar(
                            showMenu = !shouldShowBackArrow(currentRoute),
                            onMenuClick = { scope.launch { drawerState.open() } },
                            onBackClick = { navController.popBackStack() },
                            onSearchChange = { }
                        )
                    }
                },
                bottomBar = {
                    if (shouldShowBottomBar(currentRoute) && shouldShowTopBar(currentRoute)) {
                        BottomBar(navController)
                    }
                }

            ) { innerPadding ->
                AppNavGraph(
                    navController = navController,
                    paddingValues = innerPadding,
                    productViewModel = productViewModel,
                    authViewModel = authViewModel
                )
            }
        }
    }
}
