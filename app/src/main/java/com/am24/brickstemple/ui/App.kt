package com.am24.brickstemple.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.am24.brickstemple.auth.AuthSession
import com.am24.brickstemple.auth.AuthStorage
import com.am24.brickstemple.auth.LogoutManager
import com.am24.brickstemple.data.remote.KtorClientProvider
import com.am24.brickstemple.data.remote.ProductApiService
import com.am24.brickstemple.data.remote.WishlistApiService
import com.am24.brickstemple.data.repositories.ProductRepository
import com.am24.brickstemple.data.repositories.WishlistRepository
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
import com.am24.brickstemple.ui.viewmodels.WishlistViewModel
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

        LaunchedEffect(AuthSession.token) {
            if (AuthSession.token.isNullOrBlank()) {
                authViewModel.logout()
            }
        }

        val productRepository = remember {
            ProductRepository(ProductApiService(KtorClientProvider.client))
        }

        val wishlistRepository = remember {
            WishlistRepository(WishlistApiService(KtorClientProvider.client))
        }

        val wishlistViewModel: WishlistViewModel =
            viewModel(factory = WishlistViewModel.Factory(wishlistRepository))

        val productViewModel: ProductViewModel =
            viewModel(factory = ProductViewModel.Factory(productRepository))

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                DrawerContent(
                    isLoggedIn = AuthSession.isLoggedIn(),
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }
                        navController.navigate(route)
                    },
                    onLogout = {
                        LogoutManager.performLogout(
                            context = context,
                            navController = navController,
                            wishlistViewModel = wishlistViewModel,
                            authViewModel = authViewModel
                        )
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
                    authViewModel = authViewModel,
                    wishlistViewModel = wishlistViewModel
                )
            }
        }
    }
}
