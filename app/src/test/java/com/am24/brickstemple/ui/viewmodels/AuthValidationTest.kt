package com.am24.brickstemple.ui.viewmodels

import org.junit.Assert
import org.junit.Test

class AuthValidationTest {

    private val vm = AuthViewModel(FakeAuthRepository())

    @Test
    fun `empty email returns error`() {
        Assert.assertEquals(
            "Email and password cannot be empty",
            vm.validateLogin("", "")
        )
    }

    @Test
    fun `invalid email format returns error`() {
        Assert.assertEquals(
            "Invalid email format",
            vm.validateLogin("test.com", "123")
        )
    }

    @Test
    fun `short register password gives error`() {
        Assert.assertEquals(
            "Password must be at least 6 characters",
            vm.validateRegister("Artem", "a@mail.com", "12")
        )
    }
}