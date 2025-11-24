package com.am24.brickstemple.utils

import androidx.navigation.NavController
import com.am24.brickstemple.auth.AuthSession
import com.am24.brickstemple.ui.navigation.Screen
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*


class NavExtensionsTest {

    private lateinit var nav: NavController

    @Before
    fun setup() {
        nav = mock()
        AuthSession.clear()
    }

    interface NavigationWrapper {
        fun navigate(route: String)
    }

    fun requireLogin(nav: NavigationWrapper, block: () -> Unit) {
        if (!AuthSession.isLoggedIn()) {
            nav.navigate(Screen.Login.route)
        } else {
            block()
        }
    }

    @Test
    fun `requireLogin should navigate to login when not logged in`() {
        AuthSession.clear()

        requireLogin(nav) {}

        verify(nav).navigate(Screen.Login.route)
    }

    @Test
    fun `requireLogin should run block when logged in`() {
        AuthSession.updateToken("abc")
        val nav = mockk<NavigationWrapper>(relaxed = true)

        var executed = false

        requireLogin(nav) { executed = true }

        verify(exactly = 0) { nav.navigate(any()) }
        assertTrue(executed)
    }

}
