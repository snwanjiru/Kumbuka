package com.example.kumbuka.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kumbuka.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// AuthState — every possible state any auth screen can be in
// ─────────────────────────────────────────────────────────────────────────────

sealed class AuthState {
    object Idle    : AuthState()   // nothing happening — default state
    object Loading : AuthState()   // network call in progress — show spinner
    object Success : AuthState()   // operation completed — trigger navigation
    object OtpSent : AuthState()   // OTP dispatched — show code input field
    data class Error(val message: String) : AuthState()   // something went wrong
}

// ─────────────────────────────────────────────────────────────────────────────
// AuthViewModel
//
// Shared by: SplashScreen (isAlreadyLoggedIn check in NavGraph),
//            SignUpScreen, LoginScreen, ForgotPasswordScreen, HomeScreen (logout)
//
// This ViewModel has NO knowledge of:
//   • Firebase (removed — Spring Boot handles auth)
//   • Retrofit (that lives in AuthRepositoryImpl)
//   • DataStore (that lives in TokenManager)
//
// It only knows about AuthRepository (the interface) and AuthState.
// This makes it fully testable with a fake repository — no network needed.
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // ── Session check (used by NavGraph at Splash) ────────────────────────────
    // With Spring Boot auth, this checks whether a JWT token exists in DataStore.
    // Works offline — reads from local storage, no network call.
    fun isAlreadyLoggedIn(): Boolean = authRepository.isLoggedIn()

    // ── Sign Up ───────────────────────────────────────────────────────────────
    // CHANGED: now accepts name and phone in addition to email and password.
    //
    // WHY: Firebase Auth only needed email + password to create an account.
    // Spring Boot receives ONE registration request containing all four fields.
    // The backend creates both the auth record and the user profile in one step.
    //
    // SignUpScreen already collects all four fields — they just weren't being
    // passed through. This change connects them to the actual API call.
    fun signUp(
        name: String,
        email:    String,
        phone:    String,
        password: String,
        confirmPassword: String
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepository.signUp(name, email, phone, password, confirmPassword)
                .onSuccess { _authState.value = AuthState.Success }
                .onFailure { _authState.value = AuthState.Error(it.message ?: "Sign up failed.") }
        }
    }

    // ── Login (password) ──────────────────────────────────────────────────────
    // Spring Boot verifies credentials and returns a JWT token.
    // TokenManager stores the token — AuthRepositoryImpl handles this detail.
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepository.login(email, password)
                .onSuccess { _authState.value = AuthState.Success }
                .onFailure { _authState.value = AuthState.Error(it.message ?: "Login failed.") }
        }
    }

    // ── Send OTP (passwordless tab) ───────────────────────────────────────────
    // Asks the Spring Boot backend to send a one-time code to the user's email.
    // On success → AuthState.OtpSent → LoginScreen reveals the code input field.
    fun sendOtp(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepository.sendOtp(email)
                .onSuccess { _authState.value = AuthState.OtpSent }
                .onFailure { _authState.value = AuthState.Error(it.message ?: "Could not send OTP.") }
        }
    }

    // ── Verify OTP (passwordless tab) ─────────────────────────────────────────
    // Verifies the code sent to the user's email.
    // On success → AuthState.Success → LoginScreen navigates to Home.
    fun verifyOtp(email: String, otp: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepository.verifyOtp(email, otp)
                .onSuccess { _authState.value = AuthState.Success }
                .onFailure { _authState.value = AuthState.Error(it.message ?: "Invalid or expired code.") }
        }
    }

    // ── Forgot Password ───────────────────────────────────────────────────────
    // Asks Spring Boot to send a password reset email.
    // ForgotPasswordScreen shows the success dialog on AuthState.Success.
    fun forgotPassword(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepository.forgotPassword(email)
                .onSuccess { _authState.value = AuthState.Success }
                .onFailure { _authState.value = AuthState.Error(it.message ?: "Could not send reset email.") }
        }
    }

    // ── Sign Out ──────────────────────────────────────────────────────────────
    // Clears the JWT token from DataStore (handled by repository).
    // NavGraph navigates to SignUp after this.
    fun signOut() {
        authRepository.signOut()
        _authState.value = AuthState.Idle
    }

    // ── Reset state ───────────────────────────────────────────────────────────
    // Called via LaunchedEffect(Unit) in each screen on first composition.
    // Prevents a stale Success or Error from a previous session bleeding into
    // a freshly-composed screen.
    fun resetState() { _authState.value = AuthState.Idle }
}