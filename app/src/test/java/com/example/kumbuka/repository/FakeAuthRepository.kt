package com.example.kumbuka.repository

class FakeAuthRepository : AuthRepository {

    // ── Control flags — set these in each test before calling ViewModel fns ───
    var loggedIn           = false     // controls isLoggedIn()
    var signUpShouldSucceed  = true
    var loginShouldSucceed   = true
    var sendOtpShouldSucceed = true
    var verifyOtpShouldSucceed = true
    var forgotPasswordShouldSucceed = true

    // ── Error messages returned on failure ────────────────────────────────────
    var signUpError          = "Sign up failed."
    var loginError           = "Login failed."
    var sendOtpError         = "Could not send OTP."
    var verifyOtpError       = "Invalid or expired code."
    var forgotPasswordError  = "Could not send reset email."

    // ── Call tracking — assert that functions were actually called ─────────────
    var signUpCalled          = false
    var loginCalled           = false
    var sendOtpCalled         = false
    var verifyOtpCalled       = false
    var forgotPasswordCalled  = false
    var signOutCalled         = false

    // ── Recorded arguments — verify the right values were passed ──────────────
    var lastSignUpname = ""
    var lastSignUpEmail    = ""
    var lastSignUpPhone    = ""
    var lastSignUpPassword = ""
    var lastLoginEmail     = ""
    var lastLoginPassword  = ""
    var lastOtpEmail       = ""
    var lastVerifyEmail    = ""
    var lastVerifyOtp      = ""
    var lastForgotEmail    = ""

    // ── AuthRepository implementation ─────────────────────────────────────────

    override fun isLoggedIn(): Boolean = loggedIn

    override suspend fun signUp(
        name: String,
        email:    String,
        phone:    String,
        password: String,
        confirmPassword: String
    ): Result<Unit> {
        kotlinx.coroutines.delay(1) // Force a suspension point to test Loading state
        signUpCalled      = true
        lastSignUpname    = name
        lastSignUpEmail    = email
        lastSignUpPhone    = phone
        lastSignUpPassword = password
        // Optionally track lastSignUpConfirmPassword if needed for tests
        return if (signUpShouldSucceed) Result.success(Unit)
        else Result.failure(Exception(signUpError))
    }

    override suspend fun login(email: String, password: String): Result<Unit> {
        kotlinx.coroutines.delay(1)
        loginCalled       = true
        lastLoginEmail    = email
        lastLoginPassword = password
        return if (loginShouldSucceed) Result.success(Unit)
        else Result.failure(Exception(loginError))
    }

    override suspend fun sendOtp(email: String): Result<Unit> {
        kotlinx.coroutines.delay(1)
        sendOtpCalled = true
        lastOtpEmail  = email
        return if (sendOtpShouldSucceed) Result.success(Unit)
        else Result.failure(Exception(sendOtpError))
    }

    override suspend fun verifyOtp(email: String, otp: String): Result<Unit> {
        kotlinx.coroutines.delay(1)
        verifyOtpCalled = true
        lastVerifyEmail = email
        lastVerifyOtp   = otp
        return if (verifyOtpShouldSucceed) Result.success(Unit)
        else Result.failure(Exception(verifyOtpError))
    }

    override suspend fun forgotPassword(email: String): Result<Unit> {
        kotlinx.coroutines.delay(1)
        forgotPasswordCalled = true
        lastForgotEmail      = email
        return if (forgotPasswordShouldSucceed) Result.success(Unit)
        else Result.failure(Exception(forgotPasswordError))
    }

    override fun signOut() {
        signOutCalled = true
        loggedIn      = false
    }
}