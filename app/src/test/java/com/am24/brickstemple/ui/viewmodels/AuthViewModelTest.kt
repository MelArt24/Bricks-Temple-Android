package com.am24.brickstemple.ui.viewmodels

import com.am24.brickstemple.data.remote.auth.UpdateUserRequest
import com.am24.brickstemple.data.remote.auth.UserMeResponse
import com.am24.brickstemple.domain.repositories.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FakeAuthRepository : AuthRepository {

    var shouldFail = false

    override suspend fun login(email: String, password: String): String {
        if (shouldFail) throw Exception("Invalid password")
        return "token123"
    }

    override suspend fun register(username: String, email: String, password: String): Long {
        if (shouldFail) throw Exception("User exists")
        return 99
    }

    override suspend fun logout() {}

    override suspend fun getCurrentUser() =
        UserMeResponse(
            id = 1,
            username = "TestUser",
            email = "a@mail.com",
            message = null
        )

    override suspend fun updateUser(
        id: Int,
        req: UpdateUserRequest
    ) {
        TODO("Not yet implemented")
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private lateinit var repo: FakeAuthRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        repo = FakeAuthRepository()
        viewModel = AuthViewModel(repo)
    }

    @Test
    fun `login successful updates state to success`() = runTest {
        viewModel.onLoginEmailChange("a@mail.com")
        viewModel.onLoginPasswordChange("123456")

        viewModel.login()
        advanceUntilIdle()

        assertTrue(viewModel.loginState.value.isSuccess)
    }

    @Test
    fun `login error updates error message`() = runTest {
        repo.shouldFail = true

        viewModel.onLoginEmailChange("a@mail.com")
        viewModel.onLoginPasswordChange("123456")

        viewModel.login()
        advanceUntilIdle()

        assertEquals("Invalid password", viewModel.loginState.value.errorMessage)
    }

    @Test
    fun `register successful updates isSuccess`() = runTest {
        viewModel.onRegisterUsernameChange("Artem")
        viewModel.onRegisterEmailChange("a@mail.com")
        viewModel.onRegisterPasswordChange("123456")

        viewModel.register()
        advanceUntilIdle()

        assertTrue(viewModel.registerState.value.isSuccess)
    }

    @Test
    fun `register error updates errorMessage`() = runTest {
        repo.shouldFail = true

        viewModel.onRegisterUsernameChange("Artem")
        viewModel.onRegisterEmailChange("a@mail.com")
        viewModel.onRegisterPasswordChange("123456")

        viewModel.register()
        advanceUntilIdle()

        assertEquals("User exists", viewModel.registerState.value.errorMessage)
    }

}
