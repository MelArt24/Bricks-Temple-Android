package com.am24.brickstemple.ui.navigation

import org.junit.Assert.*
import org.junit.Test

class NavigationUtilsTest {

    @Test
    fun `top bar shown when route is null`() {
        assertTrue(shouldShowTopBar(null))
    }

    @Test
    fun `top bar hidden on Login screen`() {
        assertFalse(shouldShowTopBar(Screen.Login.route))
    }

    @Test
    fun `top bar hidden on Register screen`() {
        assertFalse(shouldShowTopBar(Screen.Register.route))
    }

    @Test
    fun `top bar shown on any other screen`() {
        assertTrue(shouldShowTopBar("home"))
        assertTrue(shouldShowTopBar("products"))
        assertTrue(shouldShowTopBar("settings"))
    }

    @Test
    fun `back arrow hidden when route is null`() {
        assertFalse(shouldShowBackArrow(null))
    }

    @Test
    fun `back arrow shown on ProductDetails`() {
        assertTrue(shouldShowBackArrow(Screen.ProductDetails.route))
    }

    @Test
    fun `back arrow shown on ProductCategory`() {
        assertTrue(shouldShowBackArrow(Screen.ProductCategory.route))
    }

    @Test
    fun `back arrow shown on Settings`() {
        assertTrue(shouldShowBackArrow(Screen.Settings.route))
    }

    @Test
    fun `back arrow hidden on unrelated screen`() {
        assertFalse(shouldShowBackArrow("home"))
        assertFalse(shouldShowBackArrow("products"))
    }

    @Test
    fun `bottom bar shown when route is null`() {
        assertTrue(shouldShowBottomBar(null))
    }

    @Test
    fun `bottom bar hidden on ProductDetails`() {
        assertFalse(shouldShowBottomBar(Screen.ProductDetails.route))
    }

    @Test
    fun `bottom bar shown on any other screen`() {
        assertTrue(shouldShowBottomBar("home"))
        assertTrue(shouldShowBottomBar("cart"))
        assertTrue(shouldShowBottomBar(Screen.Settings.route))
    }
}
