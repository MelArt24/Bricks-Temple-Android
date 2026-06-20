package com.am24.brickstemple.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.am24.brickstemple.data.auth.AuthSession
import com.am24.brickstemple.data.auth.AuthStorage
import com.am24.brickstemple.ui.screens.auth.LogoutManager
import com.am24.brickstemple.data.remote.KtorClientProvider
import com.am24.brickstemple.data.remote.NetworkObserver
import com.am24.brickstemple.data.remote.NetworkStatus
import com.am24.brickstemple.ui.navigation.AppNavGraph
import com.am24.brickstemple.ui.components.BottomBar
import com.am24.brickstemple.ui.components.DrawerContent
import com.am24.brickstemple.ui.components.TopBar
import com.am24.brickstemple.ui.navigation.AppNavGraphCallbacks
import com.am24.brickstemple.ui.navigation.Screen
import com.am24.brickstemple.ui.navigation.shouldShowBackArrow
import com.am24.brickstemple.ui.navigation.shouldShowBottomBar
import com.am24.brickstemple.ui.navigation.shouldShowTopBar
import com.am24.brickstemple.ui.theme.BricksTempleTheme
import com.am24.brickstemple.ui.screens.auth.AuthViewModel
import com.am24.brickstemple.ui.screens.cart.CartViewModel
import com.am24.brickstemple.ui.screens.orders.OrderViewModel
import com.am24.brickstemple.ui.screens.product.ProductViewModel
import com.am24.brickstemple.ui.screens.settings.ThemeViewModel
import com.am24.brickstemple.ui.screens.wishlist.WishlistViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val context = LocalContext.current

    val themeViewModel: ThemeViewModel = koinViewModel()
    val isDarkTheme by themeViewModel.isDarkMode.collectAsState()

    BricksTempleTheme(darkTheme = isDarkTheme) {
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            AuthStorage.load(context)
        }

        val systemOnline by NetworkObserver.isNetworkAvailable.collectAsState()

        LaunchedEffect(systemOnline) {
            KtorClientProvider.syncWithSystemNetwork(systemOnline)
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

        val authViewModel: AuthViewModel = koinViewModel()
        val wishlistViewModel: WishlistViewModel = koinViewModel()
        val productViewModel: ProductViewModel = koinViewModel()
        val cartViewModel: CartViewModel = koinViewModel()
        val orderViewModel: OrderViewModel = koinViewModel()

        LaunchedEffect(AuthSession.token) {
            if (AuthSession.token.isNullOrBlank()) {
                authViewModel.logout()
            }
        }

        val networkState by KtorClientProvider.networkStatus.collectAsState()

        LaunchedEffect(networkState) {
            if (networkState == NetworkStatus.CONNECTED) {

                delay(300)

                try {
                    wishlistViewModel.refresh()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                }

                if (AuthSession.isLoggedIn()) {
                    try {
                        authViewModel.loadCurrentUser()
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                    }
                }
            }
        }


        val isCategory = currentRoute?.startsWith(Screen.ProductCategory.route) == true

        val topBarActions: @Composable RowScope.() -> Unit = if (isCategory) {
            {
                IconButton(onClick = { AppNavGraphCallbacks.openSort?.invoke() }) {
                    Icon(Icons.Default.Sort, "Sort")
                }
                IconButton(onClick = { AppNavGraphCallbacks.openFilters?.invoke() }) {
                    Icon(Icons.Default.FilterList, "Filters")
                }
            }
        } else { {} }

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
                            enableSearch = isCategory,
                            title = "",
                            searchText = productViewModel.searchQuery.collectAsState().value,
                            onMenuClick = { scope.launch { drawerState.open() } },
                            onBackClick = { navController.popBackStack() },
                            onSearchChange = { productViewModel.setSearchQuery(it) },
                            actions = topBarActions
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
                    wishlistViewModel = wishlistViewModel,
                    cartViewModel = cartViewModel,
                    orderViewModel = orderViewModel,
                    openSort = { AppNavGraphCallbacks.openSort?.invoke() },
                    openFilters = { AppNavGraphCallbacks.openFilters?.invoke() }
                )
            }
        }
    }
}
