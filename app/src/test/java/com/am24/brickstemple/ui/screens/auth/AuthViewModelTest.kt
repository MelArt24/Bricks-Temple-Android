package com.am24.brickstemple.ui.screens.auth

import com.am24.brickstemple.data.auth.AuthSession
import com.am24.brickstemple.domain.error.AppError
import com.am24.brickstemple.domain.error.AppException
import com.am24.brickstemple.domain.model.UpdateUser
import com.am24.brickstemple.domain.model.User
import com.am24.brickstemple.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FakeAuthRepository : AuthRepository {

    var shouldFail = false
    var loginError: AppException? = null
    var registerError: AppException? = null
    var currentUserError: AppException? = null
    var updateUserError: AppException? = null

    override suspend fun login(email: String, password: String): String {
        loginError?.let { throw it }
        if (shouldFail) throw AppException(AppError.UnknownError("Invalid password"))
        return "token123"
    }

    override suspend fun register(username: String, email: String, password: String): Long {
        registerError?.let { throw it }
        if (shouldFail) throw AppException(AppError.UnknownError("User exists"))
        return 99
    }

    override suspend fun logout() {}

    override suspend fun getCurrentUser(): User {
        currentUserError?.let { throw it }
        return User(
            id = 1,
            username = "TestUser",
            email = "a@mail.com",
            message = null
        )
    }

    override suspend fun updateUser(
        id: Int,
        user: UpdateUser
    ) {
        updateUserError?.let { throw it }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private lateinit var repo: FakeAuthRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        AuthSession.clear()
        AuthSession.updateUserId(null)
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
    fun `login network failure exposes user-facing message`() = runTest {
        repo.loginError = AppException(AppError.NetworkError())

        viewModel.onLoginEmailChange("a@mail.com")
        viewModel.onLoginPasswordChange("123456")

        viewModel.login()
        advanceUntilIdle()

        assertEquals("No internet connection.", viewModel.loginState.value.errorMessage)
        assertFalse(viewModel.loginState.value.isLoading)
    }

    @Test
    fun `unauthorized login exposes existing auth error message`() = runTest {
        repo.loginError = AppException(AppError.UnauthorizedError("Incorrect password."))

        viewModel.onLoginEmailChange("a@mail.com")
        viewModel.onLoginPasswordChange("123456")

        viewModel.login()
        advanceUntilIdle()

        assertEquals("Incorrect password.", viewModel.loginState.value.errorMessage)
        assertFalse(viewModel.loginState.value.isSuccess)
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

    @Test
    fun `password change failure exposes error callback message`() = runTest {
        AuthSession.updateToken("token")
        AuthSession.updateUserId(1)
        AuthSession.updateUsername("TestUser")
        AuthSession.updateEmail("a@mail.com")
        repo.updateUserError = AppException(AppError.ServerError(userMessage = "Failed to update password"))

        var error: String? = null

        viewModel.changePassword(
            newPassword = "123456",
            onSuccess = { fail("Success should not be called") },
            onError = { error = it }
        )
        advanceUntilIdle()

        assertEquals("Failed to update password", error)
    }

    @Test
    fun `profile load success updates profile state and auth session`() = runTest {
        AuthSession.updateToken("token")

        viewModel.loadCurrentUser()
        advanceUntilIdle()

        assertEquals("TestUser", viewModel.profileState.value.username)
        assertEquals("a@mail.com", viewModel.profileState.value.email)
        assertTrue(viewModel.profileState.value.isLoggedIn)
        assertFalse(viewModel.profileState.value.isLoading)
        assertNull(viewModel.profileState.value.errorMessage)
        assertEquals(1, AuthSession.userId)
        assertEquals("TestUser", AuthSession.username)
        assertEquals("a@mail.com", AuthSession.email)
    }

    @Test
    fun `profile load failure exposes profile state error`() = runTest {
        AuthSession.updateToken("token")
        repo.currentUserError = AppException(AppError.ServerError(userMessage = "Failed to load profile"))

        viewModel.loadCurrentUser()
        advanceUntilIdle()

        assertEquals("Failed to load profile", viewModel.profileState.value.errorMessage)
        assertFalse(viewModel.profileState.value.isLoading)
    }

    @Test
    fun `loadCurrentUser when logged out sets profile state logged out`() = runTest {
        viewModel.loadCurrentUser()
        advanceUntilIdle()

        assertFalse(viewModel.profileState.value.isLoggedIn)
        assertFalse(viewModel.profileState.value.isLoading)
        assertNull(viewModel.profileState.value.errorMessage)
    }

    @Test
    fun `logout clears auth session and resets auth states`() = runTest {
        AuthSession.updateToken("token")
        AuthSession.updateUserId(1)
        AuthSession.updateUsername("TestUser")
        AuthSession.updateEmail("a@mail.com")

        viewModel.onLoginEmailChange("a@mail.com")
        viewModel.onLoginPasswordChange("123456")
        viewModel.login()
        advanceUntilIdle()

        viewModel.onRegisterUsernameChange("Artem")
        viewModel.onRegisterEmailChange("artem@mail.com")
        viewModel.onRegisterPasswordChange("123456")

        viewModel.logout()

        assertNull(AuthSession.token)
        assertNull(AuthSession.username)
        assertNull(AuthSession.email)
        assertEquals(LoginUiState(), viewModel.loginState.value)
        assertEquals(RegisterUiState(), viewModel.registerState.value)
        assertEquals(ProfileUiState(), viewModel.profileState.value)
    }
}
