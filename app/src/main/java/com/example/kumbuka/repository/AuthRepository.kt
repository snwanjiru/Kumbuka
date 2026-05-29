package com.example.kumbuka.repository

// ─────────────────────────────────────────────────────────────────────────────
// AuthRepository
//
// The interface defines WHAT the auth system can do — with no knowledge of
// HOW it does it. The ViewModel only ever sees this interface.
//
// This separation means:
//   • Tests inject FakeAuthRepository — no network, no DataStore, instant
//   • Production uses AuthRepositoryImpl — real Retrofit calls to Spring Boot
//   • Swapping backends in future requires only a new impl, not touching the ViewModel
//
// Each Spring Boot endpoint maps to exactly one function here:
//   POST /api/auth/register        → signUp()
//   POST /api/auth/login           → login()
//   POST /api/auth/otp/send        → sendOtp()
//   POST /api/auth/otp/verify      → verifyOtp()
//   POST /api/auth/forgot-password → forgotPassword()
//   (local DataStore clear)        → signOut()
//   (local DataStore read)         → isLoggedIn()
// ─────────────────────────────────────────────────────────────────────────────

interface AuthRepository {

    // ── Session check ─────────────────────────────────────────────────────────
    // Returns true if a valid (non-expired) JWT token exists in local storage.
    // Called by NavGraph at Splash to decide: Home or SignUp?
    // Non-suspend because it is called from within a LaunchedEffect coroutine
    // in NavGraph — the implementation uses runBlocking internally for the
    // single DataStore read this requires.
    fun isLoggedIn(): Boolean

    // ── Registration ──────────────────────────────────────────────────────────
    // CHANGED: added name and phone vs the previous version.
    // Spring Boot creates the user account and profile in one request.
    // Returns Result<Unit> — success means the JWT was stored, failure means
    // the error message is ready to show in the error banner.
    suspend fun signUp(
        name: String,
        email:    String,
        phone:    String,
        password: String,
        confirmPassword: String
    ): Result<Unit>

    // ── Password login ────────────────────────────────────────────────────────
    // Spring Boot verifies credentials and returns a JWT.
    // AuthRepositoryImpl stores the token via TokenManager on success.
    suspend fun login(
        email:    String,
        password: String
    ): Result<Unit>

    // ── Passwordless login — step 1: send the code ────────────────────────────
    // Asks Spring Boot to generate a one-time code and email it to the user.
    // On success → AuthState.OtpSent → LoginScreen reveals the OTP input field.
    suspend fun sendOtp(email: String): Result<Unit>

    // ── Passwordless login — step 2: verify the code ──────────────────────────
    // NEW: was missing from the previous version — this caused a bug where
    // the "Log In" button on the OTP tab called login(email, password) with an
    // empty password field. This function is the correct call for that action.
    // Spring Boot validates the code and returns a JWT on success.
    suspend fun verifyOtp(
        email: String,
        otp:   String
    ): Result<Unit>

    // ── Forgot password ───────────────────────────────────────────────────────
    // Asks Spring Boot to send a password reset email.
    // Result<Unit> success means the email was dispatched — not that the
    // password was reset (that happens outside the app in the email client).
    suspend fun forgotPassword(email: String): Result<Unit>

    // ── Sign out ──────────────────────────────────────────────────────────────
    // Clears the JWT from DataStore (via TokenManager).
    // Non-suspend because it is called directly from a Button onClick in
    // HomeScreen — the implementation clears storage in a background coroutine
    // internally. Navigation away from Home happens immediately regardless.
    fun signOut()
}