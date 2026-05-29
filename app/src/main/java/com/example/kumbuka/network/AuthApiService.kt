package com.example.kumbuka.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

// ─────────────────────────────────────────────────────────────────────────────
// REQUEST bodies — what Android sends TO Spring Boot
// ─────────────────────────────────────────────────────────────────────────────

data class RegisterRequest(
    val name: String,
    val email:    String,
    val phone:    String,
    val password: String,
    val confirmPassword: String
)

data class LoginRequest(
    val email:    String,
    val password: String
)

data class SendOtpRequest(
    val email: String
)

data class VerifyOtpRequest(
    val email: String,
    val otp:   String
)

data class ForgotPasswordRequest(
    val email: String
)

// ─────────────────────────────────────────────────────────────────────────────
// RESPONSE bodies — what Spring Boot sends BACK to Android
// ─────────────────────────────────────────────────────────────────────────────

// Returned by /login, /register, and /otp/verify.
// expiresIn is in SECONDS — TokenManager converts to ms when storing.
// refreshToken is nullable — include it only if your Spring Boot config issues one.
// ""
data class AuthResponse(
    val accessToken:  String,
    val refreshToken: String? = null,
    val expiresIn:    Long    = 86400L   // default 24 hours if backend omits it
)

// Generic response for operations that return a message but no token.
// Used by /otp/send and /forgot-password.
data class MessageResponse(
    val message: String
)

// ─────────────────────────────────────────────────────────────────────────────
// AuthApiService — the Retrofit interface
//
// Each function maps to one Spring Boot endpoint.
// Using Response<T> as the return type (not just T) gives us access to the
// HTTP status code and error body in the repository — essential for mapping
// 400/401/409 errors into user-friendly messages.
//
// The paths here ("/api/auth/...") are relative to the BASE_URL in AppModule.
// Confirm the exact paths with your Spring Boot developer — these are the
// conventional REST naming for an auth controller.
// ─────────────────────────────────────────────────────────────────────────────

interface AuthApiService {

    // POST /api/auth/register
    // Creates a new user account. Spring Boot returns the JWT immediately so
    // the user is logged in straight after sign-up — no separate login needed.
    @POST("kumbukaa/api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    // POST /api/auth/login
    // Verifies email + password. Returns JWT on success.
    @POST("kumbukaa/api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    // POST /api/auth/otp/send
    // Asks Spring Boot to generate and email a one-time code.
    // Returns a message confirming dispatch — no token yet.
    @POST("api/auth/otp/send")
    suspend fun sendOtp(@Body request: SendOtpRequest): Response<MessageResponse>

    // POST /api/auth/otp/verify
    // Submits the code the user received. Spring Boot validates it and returns
    // a JWT if correct — same AuthResponse as a normal login.
    @POST("api/auth/otp/verify")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<AuthResponse>

    // POST /api/auth/forgot-password
    // Triggers a password reset email from Spring Boot.
    // Returns a message — no token (user is not yet authenticated).
    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<MessageResponse>
}