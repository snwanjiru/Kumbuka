package com.example.kumbuka.repository

import com.example.kumbuka.data.TokenManager
import com.example.kumbuka.network.AuthApiService
import com.example.kumbuka.network.ForgotPasswordRequest
import com.example.kumbuka.network.LoginRequest
import com.example.kumbuka.network.RegisterRequest
import com.example.kumbuka.network.SendOtpRequest
import com.example.kumbuka.network.VerifyOtpRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
// AuthRepositoryImpl
//
// The ONLY class in the app that knows about:
//   • Retrofit / Spring Boot HTTP calls (via AuthApiService)
//   • JWT token storage (via TokenManager)
//
// The ViewModel sees only AuthRepository (the interface).
// Tests inject FakeAuthRepository instead of this class.
//
// Complete rewrite — all Firebase Auth code removed.
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api:          AuthApiService,
    private val tokenManager: TokenManager
) : AuthRepository {

    // A dedicated IO scope for fire-and-forget operations like signOut().
    // SupervisorJob means a failure in one child does not cancel the others.
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ── Session check ─────────────────────────────────────────────────────────
    // Non-suspend — called from a LaunchedEffect in NavGraph (already a coroutine
    // context) but the interface contract is non-suspend to keep NavGraph simple.
    // runBlocking is justified here: DataStore reads from an in-memory cache
    // after the first access, making this effectively instant. It is also only
    // called once per app start inside the 2.2s splash delay.
    override fun isLoggedIn(): Boolean = runBlocking {
        tokenManager.isTokenValid()
    }

    // ── Register ──────────────────────────────────────────────────────────────
    // Spring Boot creates the user and returns a JWT immediately — the user is
    // logged in straight after sign-up, no separate login step needed.
    override suspend fun signUp(
        name: String,
        email:    String,
        phone:    String,
        password: String,
        confirmPassword: String
    ): Result<Unit> = safeApiCall {
        val response = api.register(RegisterRequest(name, email, phone, password, confirmPassword))
        if (response.isSuccessful) {
            val body = response.body()!!
            // Store the JWT so the user is immediately considered logged in
            tokenManager.saveTokens(
                accessToken  = body.accessToken,
                refreshToken = body.refreshToken,
                expiresInMs  = body.expiresIn * 1_000L   // Spring Boot sends seconds
            )
            Result.success(Unit)
        } else {
            Result.failure(Exception(parseError(response, mapOf(
                409 to "An account with this email already exists. Try logging in.",
                400 to "Please check your details and try again."
            ))))
        }
    }

    // ── Login ─────────────────────────────────────────────────────────────────
    override suspend fun login(email: String, password: String): Result<Unit> = safeApiCall {
        val response = api.login(LoginRequest(email, password))
        if (response.isSuccessful) {
            val body = response.body()!!
            tokenManager.saveTokens(
                accessToken  = body.accessToken,
                refreshToken = body.refreshToken,
                expiresInMs  = body.expiresIn * 1_000L
            )
            Result.success(Unit)
        } else {
            Result.failure(Exception(parseError(response, mapOf(
                401 to "Incorrect email or password. Please try again.",
                404 to "No account found with this email. Try signing up."
            ))))
        }
    }

    // ── Send OTP ──────────────────────────────────────────────────────────────
    // Step 1 of passwordless login — dispatches the code, no token yet.
    override suspend fun sendOtp(email: String): Result<Unit> = safeApiCall {
        val response = api.sendOtp(SendOtpRequest(email))
        if (response.isSuccessful) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(parseError(response, mapOf(
                404 to "No account found with this email. Try signing up.",
                429 to "Too many attempts. Please wait a moment before trying again."
            ))))
        }
    }

    // ── Verify OTP ────────────────────────────────────────────────────────────
    // Step 2 of passwordless login — validates the code and stores the JWT.
    // This is what the "Log In" button on the OTP tab must call, NOT login().
    override suspend fun verifyOtp(email: String, otp: String): Result<Unit> = safeApiCall {
        val response = api.verifyOtp(VerifyOtpRequest(email, otp))
        if (response.isSuccessful) {
            val body = response.body()!!
            tokenManager.saveTokens(
                accessToken  = body.accessToken,
                refreshToken = body.refreshToken,
                expiresInMs  = body.expiresIn * 1_000L
            )
            Result.success(Unit)
        } else {
            Result.failure(Exception(parseError(response, mapOf(
                400 to "Invalid or expired code. Please request a new one.",
                401 to "Invalid or expired code. Please request a new one."
            ))))
        }
    }

    // ── Forgot password ───────────────────────────────────────────────────────
    // Spring Boot sends the reset email — no token stored.
    // Returns success even for unknown emails (security: prevents enumeration).
    override suspend fun forgotPassword(email: String): Result<Unit> = safeApiCall {
        val response = api.forgotPassword(ForgotPasswordRequest(email))
        // Always return success to the UI regardless of whether the email exists.
        // The backend should implement the same policy. If it does not, 404 is
        // still mapped to success here so the UI always shows the success dialog.
        if (response.isSuccessful || response.code() == 404) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(parseError(response, emptyMap())))
        }
    }

    // ── Sign out ──────────────────────────────────────────────────────────────
    // Non-suspend — navigation away from Home happens immediately. Token clearing
    // runs in the background on the IO dispatcher. If the clear fails (extremely
    // unlikely with DataStore), the expired token will be rejected by the backend
    // on the next launch anyway, which re-triggers the sign-out flow.
    override fun signOut() {
        ioScope.launch {
            tokenManager.clearTokens()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    // Wraps every API call in a try/catch so network errors (no internet,
    // DNS failure, timeout) are converted into Result.failure with a
    // human-readable message instead of crashing.
    private suspend fun <T> safeApiCall(block: suspend () -> Result<T>): Result<T> {
        return try {
            block()
        } catch (e: java.net.UnknownHostException) {
            Result.failure(Exception("No internet connection. Please check your network."))
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(Exception("Request timed out. Please try again."))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Something went wrong. Please try again."))
        }
    }

    // Parses the Spring Boot error response body.
    // Spring Boot typically returns: { "message": "Email already exists" }
    // Falls back to the status-code map, then to a generic message.
    private fun parseError(
        response:    Response<*>,
        statusMap:   Map<Int, String>
    ): String {
        // 1. Try to read a "message" field from the JSON error body
        try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrBlank()) {
                val json = JSONObject(errorBody)
                val msg  = json.optString("message", "")
                    .ifBlank { json.optString("error", "") }
                if (msg.isNotBlank()) return msg
            }
        } catch (_: Exception) { /* malformed JSON — fall through */ }

        // 2. Fall back to our own status-code message map
        statusMap[response.code()]?.let { return it }

        // 3. Last resort generic message
        return "Something went wrong (${response.code()}). Please try again."
    }
}