package com.am24.brickstemple.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.testing.TestNavHostController
import com.am24.brickstemple.ui.navigation.Screen
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class BottomBarTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var navController: TestNavHostController

    @Before
    fun setup() {
        composeRule.setContent {
            navController = TestNavHostController(composeRule.activity)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            val graph = navController.createGraph(
                startDestination = Screen.ProductList.route,
                route = "root"
            ) {
                composable(Screen.ProductList.route) {}
                composable(Screen.Cart.route) {}
                composable(Screen.Wishlist.route) {}
                composable(Screen.Profile.route) {}
            }

            navController.graph = graph

            BottomBar(navController = navController)
        }
    }


    @Test
    fun bottomBar_showsAllItems() {
        composeRule.onNodeWithText("Home").assertExists()
        composeRule.onNodeWithText("Cart").assertExists()
        composeRule.onNodeWithText("Wishlist").assertExists()
        composeRule.onNodeWithText("Profile").assertExists()
    }

    @Test
    fun clickingHome_navigatesToProductList() {
        composeRule.onNodeWithText("Home").performClick()

        assert(navController.currentDestination?.route == Screen.ProductList.route)
    }

    @Test
    fun clickingCart_navigatesToCart() {
        composeRule.onNodeWithText("Cart").performClick()

        assert(navController.currentDestination?.route == Screen.Cart.route)
    }

    @Test
    fun clickingWishlist_navigatesToWishlist() {
        composeRule.onNodeWithText("Wishlist").performClick()

        assert(navController.currentDestination?.route == Screen.Wishlist.route)
    }

    @Test
    fun clickingProfile_navigatesToProfile() {
        composeRule.onNodeWithText("Profile").performClick()

        assert(navController.currentDestination?.route == Screen.Profile.route)
    }
}
