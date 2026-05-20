package com.example.kumbuka.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.ActionCodeSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ─────────────────────────────────────────────────────────────────────────────
// AuthState — the four states any auth screen can be in
// ─────────────────────────────────────────────────────────────────────────────

sealed class AuthState {
    object Idle    : AuthState()
    object Loading : AuthState()
    object Success : AuthState()

    //AuthState gets a new OtpSent state
    object OtpSent : AuthState()
    data class Error(val message: String) : AuthState()
}

// ─────────────────────────────────────────────────────────────────────────────
// AuthViewModel
// Shared by: SignUpScreen, LoginScreen, ForgotPasswordScreen, NavGraph (Splash)
// Now also handles email‑link sign‑in completion.
// ─────────────────────────────────────────────────────────────────────────────

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // ── Already logged in check (used by NavGraph at Splash) ──────────────────
    fun isAlreadyLoggedIn(): Boolean = auth.currentUser != null

    // ── Sign Up ───────────────────────────────────────────────────────────────
    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                auth.createUserWithEmailAndPassword(email, password).await()
                _authState.value = AuthState.Success
            } catch (e: FirebaseAuthWeakPasswordException) {
                _authState.value = AuthState.Error(
                    "Password is too weak — use at least 8 characters."
                )
            } catch (e: FirebaseAuthUserCollisionException) {
                _authState.value = AuthState.Error(
                    "An account with this email already exists. Try logging in."
                )
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                _authState.value = AuthState.Error(
                    "The email address is badly formatted."
                )
            } catch (e: Exception) {
                _authState.value = AuthState.Error(
                    e.message ?: "Sign up failed. Please try again."
                )
            }
        }
    }

    // ── Login (password) ──────────────────────────────────────────────────────
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                _authState.value = AuthState.Success
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                _authState.value = AuthState.Error(
                    "Incorrect email or password. Please try again."
                )
            } catch (e: FirebaseAuthInvalidUserException) {
                _authState.value = AuthState.Error(
                    "No account found with this email. Try signing up."
                )
            } catch (e: Exception) {
                _authState.value = AuthState.Error(
                    e.message ?: "Login failed. Please try again."
                )
            }
        }
    }

    // ── Send email link (passwordless sign‑in) ────────────────────────────────
    // Call this from your LoginScreen (pass the context and the email)
    fun sendOtp(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val settings = ActionCodeSettings.newBuilder()
                    .setUrl("https://kumbuka-ea3ac.web.app/finishSignUp")
                    .setHandleCodeInApp(true)
                    .setAndroidPackageName("com.example.kumbuka", true, null)
                    .build()

                auth.sendSignInLinkToEmail(email, settings).await()
                _authState.value = AuthState.OtpSent
            } catch (e: Exception) {
                _authState.value = AuthState.Error(
                    e.message ?: "Failed to send sign‑in link. Please try again."
                )
            }
        }
    }

    fun sendSignInLinkToEmail(email: String, context: Context) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // Store email to complete sign‑in later
                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                prefs.edit().putString("email_for_sign_in", email).apply()

                val settings = ActionCodeSettings.newBuilder()
                    // ⚠️ REPLACE this URL with your own whitelisted domain
                    .setUrl("https://kumbuka-ea3ac.web.app/finishSignUp")
                    .setHandleCodeInApp(true)
                    .setAndroidPackageName(
                        "com.example.kumbuka",   // your app’s package name
                        true,                    // install if not available
                        null                     // minimum version
                    )
                    .build()

                auth.sendSignInLinkToEmail(email, settings).await()
                _authState.value = AuthState.Success   // indicates “link sent”
            } catch (e: Exception) {
                _authState.value = AuthState.Error(
                    e.message ?: "Failed to send sign‑in link. Please try again."
                )
            }
        }
    }

    // ── Complete sign‑in with email link (called from MainActivity) ───────────
    fun signInWithEmailLink(email: String, link: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                auth.signInWithEmailLink(email, link).await()
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(
                    e.message ?: "Invalid or expired sign‑in link."
                )
            }
        }
    }

    // ── Forgot Password ───────────────────────────────────────────────────────
    fun forgotPassword(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                auth.sendPasswordResetEmail(email).await()
                _authState.value = AuthState.Success
            } catch (e: FirebaseAuthInvalidUserException) {
                // For security, show generic success
                _authState.value = AuthState.Success
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                _authState.value = AuthState.Error(
                    "The email address is not valid. Please check and try again."
                )
            } catch (e: Exception) {
                _authState.value = AuthState.Error(
                    e.message ?: "Could not send the reset email. Please try again."
                )
            }
        }
    }

    // ── Sign Out ──────────────────────────────────────────────────────────────
    fun signOut() {
        auth.signOut()
        _authState.value = AuthState.Idle
    }

    // ── Reset state (call on LaunchedEffect(Unit) in each screen) ─────────────
    fun resetState() {
        _authState.value = AuthState.Idle
    }
}