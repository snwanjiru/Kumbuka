package com.example.kumbuka.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.example.kumbuka.repository.FakeAuthRepository
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// ─────────────────────────────────────────────────────────────────────────────
// AuthViewModelTest
//
// Pure JVM unit tests — no device, no emulator, no network, no Firebase.
// Run with:  ./gradlew test
// Or:        right-click file in Android Studio → Run 'AuthViewModelTest'
//
// Location: app/src/test/java/com/example/kumbuka/viewmodel/
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    // StandardTestDispatcher makes coroutines NOT run automatically —
    // we control exactly when they execute using advanceUntilIdle().
    // This lets us assert the Loading state before the coroutine completes.
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeRepo:  FakeAuthRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo  = FakeAuthRepository()
        viewModel = AuthViewModel(fakeRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial state is Idle`() {
        assertEquals(AuthState.Idle, viewModel.authState.value)
    }

    // ── isAlreadyLoggedIn ─────────────────────────────────────────────────────

    @Test
    fun `isAlreadyLoggedIn returns true when repo says logged in`() {
        fakeRepo.loggedIn = true
        assertTrue(viewModel.isAlreadyLoggedIn())
    }

    @Test
    fun `isAlreadyLoggedIn returns false when repo says not logged in`() {
        fakeRepo.loggedIn = false
        assertFalse(viewModel.isAlreadyLoggedIn())
    }

    // ── signUp ────────────────────────────────────────────────────────────────

    @Test
    fun `signUp sets Loading while in progress`() = runTest {
        viewModel.signUp("Jane Doe", "jane@test.com", "+254700000000", "password123", "password123")
        // Start the coroutine and reach the suspension point in the repository
        runCurrent()
        assertEquals(AuthState.Loading, viewModel.authState.value)
    }

    @Test
    fun `signUp success sets state to Success`() = runTest {
        fakeRepo.signUpShouldSucceed = true
        viewModel.signUp("Jane Doe", "jane@test.com", "+254700000000", "password123", "password123")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(AuthState.Success, viewModel.authState.value)
    }

    @Test
    fun `signUp failure sets state to Error with message`() = runTest {
        fakeRepo.signUpShouldSucceed = false
        fakeRepo.signUpError         = "An account with this email already exists."
        viewModel.signUp("Jane Doe", "jane@test.com", "+254700000000", "password123", "password123")
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.authState.value
        assertTrue(state is AuthState.Error)
        assertEquals(
            "An account with this email already exists.",
            (state as AuthState.Error).message
        )
    }

    @Test
    fun `signUp passes all four fields to repository`() = runTest {
        viewModel.signUp("Jane Doe", "jane@test.com", "+254700000000", "password123", "password123")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(fakeRepo.signUpCalled)
        assertEquals("Jane Doe",        fakeRepo.lastSignUpname)
        assertEquals("jane@test.com",   fakeRepo.lastSignUpEmail)
        assertEquals("+254700000000",   fakeRepo.lastSignUpPhone)
        assertEquals("password123",     fakeRepo.lastSignUpPassword)
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    fun `login success sets state to Success`() = runTest {
        fakeRepo.loginShouldSucceed = true
        viewModel.login("user@test.com", "password123")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(AuthState.Success, viewModel.authState.value)
    }

    @Test
    fun `login failure sets Error with correct message`() = runTest {
        fakeRepo.loginShouldSucceed = false
        fakeRepo.loginError         = "Incorrect email or password."
        viewModel.login("user@test.com", "wrongpass")
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.authState.value
        assertTrue(state is AuthState.Error)
        assertEquals("Incorrect email or password.", (state as AuthState.Error).message)
    }

    @Test
    fun `login passes email and password to repository`() = runTest {
        viewModel.login("user@test.com", "password123")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(fakeRepo.loginCalled)
        assertEquals("user@test.com", fakeRepo.lastLoginEmail)
        assertEquals("password123",   fakeRepo.lastLoginPassword)
    }

    // ── sendOtp ───────────────────────────────────────────────────────────────

    @Test
    fun `sendOtp success sets state to OtpSent`() = runTest {
        fakeRepo.sendOtpShouldSucceed = true
        viewModel.sendOtp("user@test.com")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(AuthState.OtpSent, viewModel.authState.value)
    }

    @Test
    fun `sendOtp failure sets state to Error`() = runTest {
        fakeRepo.sendOtpShouldSucceed = false
        fakeRepo.sendOtpError         = "No account found with this email."
        viewModel.sendOtp("unknown@test.com")
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.authState.value
        assertTrue(state is AuthState.Error)
        assertEquals("No account found with this email.", (state as AuthState.Error).message)
    }

    // ── verifyOtp ─────────────────────────────────────────────────────────────

    @Test
    fun `verifyOtp success sets state to Success`() = runTest {
        fakeRepo.verifyOtpShouldSucceed = true
        viewModel.verifyOtp("user@test.com", "123456")
        advanceUntilIdle()
        assertEquals(AuthState.Success, viewModel.authState.value)
    }

    @Test
    fun `verifyOtp failure sets state to Error`() = runTest {
        fakeRepo.verifyOtpShouldSucceed = false
        fakeRepo.verifyOtpError         = "Invalid or expired code."
        viewModel.verifyOtp("user@test.com", "000000")
        advanceUntilIdle()
        val state = viewModel.authState.value
        assertTrue(state is AuthState.Error)
        assertEquals("Invalid or expired code.", (state as AuthState.Error).message)
    }

    @Test
    fun `verifyOtp passes email and otp to repository`() = runTest {
        viewModel.verifyOtp("user@test.com", "123456")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(fakeRepo.verifyOtpCalled)
        assertEquals("user@test.com", fakeRepo.lastVerifyEmail)
        assertEquals("123456",        fakeRepo.lastVerifyOtp)
    }

    // ── forgotPassword ────────────────────────────────────────────────────────

    @Test
    fun `forgotPassword success sets state to Success`() = runTest {
        fakeRepo.forgotPasswordShouldSucceed = true
        viewModel.forgotPassword("user@test.com")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(AuthState.Success, viewModel.authState.value)
    }

    @Test
    fun `forgotPassword failure sets state to Error`() = runTest {
        fakeRepo.forgotPasswordShouldSucceed = false
        fakeRepo.forgotPasswordError         = "No internet connection."
        viewModel.forgotPassword("user@test.com")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.authState.value is AuthState.Error)
    }

    // ── signOut ───────────────────────────────────────────────────────────────

    @Test
    fun `signOut resets state to Idle`() = runTest {
        // Put ViewModel in a non-idle state first
        fakeRepo.loginShouldSucceed = true
        viewModel.login("user@test.com", "pass")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(AuthState.Success, viewModel.authState.value)

        viewModel.signOut()
        assertEquals(AuthState.Idle, viewModel.authState.value)
    }

    @Test
    fun `signOut calls repository signOut`() = runTest {
        viewModel.signOut()
        assertTrue(fakeRepo.signOutCalled)
    }

    // ── resetState ────────────────────────────────────────────────────────────

    @Test
    fun `resetState always returns to Idle from any state`() = runTest {
        // From Error state
        fakeRepo.loginShouldSucceed = false
        viewModel.login("x@x.com", "wrong")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.authState.value is AuthState.Error)

        viewModel.resetState()
        assertEquals(AuthState.Idle, viewModel.authState.value)
    }
}