package com.example.kumbuka.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
// DataStore instance — one per app, created as a Kotlin extension property
// on Context so it is a true singleton regardless of how many times it is
// accessed. The file is named "kumbuka_auth" on disk.
// ─────────────────────────────────────────────────────────────────────────────
private val Context.authDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "kumbuka_auth")

// ─────────────────────────────────────────────────────────────────────────────
// TokenManager
//
// Single source of truth for the user's session on this device.
// Stores the JWT that Spring Boot returns after a successful login or sign-up.
//
// WHY DataStore instead of SharedPreferences:
//   • Async-first — no blocking the main thread on reads
//   • Coroutine-native — fits naturally into the suspend function pattern
//   • Type-safe — compiler errors instead of silent key-name typos
//   • Survives app restarts and process death (written to disk, not memory)
//
// WHY NOT EncryptedSharedPreferences:
//   • Requires Jetpack Security which has had reliability issues on some OEMs
//   • For a fintech app with a backend, the JWT being readable in an app
//     sandbox is acceptable — the token itself is short-lived and the backend
//     validates it on every call. Add encryption later if compliance requires it.
//
// This class is @Singleton — Hilt creates exactly one instance for the
// entire app lifetime. All callers share the same DataStore handle.
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // ─────────────────────────────────────────────────────────────────────────
    // WRITE — called by AuthRepositoryImpl after a successful login / sign-up
    // ─────────────────────────────────────────────────────────────────────────

    // Save the access token (and optionally a refresh token + expiry).
    // All three are written in a single atomic DataStore edit so there is
    // never a partial state where the token exists but the expiry does not.
    suspend fun saveTokens(
        accessToken:  String,
        refreshToken: String? = null,
        expiresInMs:  Long    = DEFAULT_EXPIRY_MS
    ) {
        context.authDataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN]  = accessToken
            prefs[KEY_TOKEN_EXPIRY]  = System.currentTimeMillis() + expiresInMs
            refreshToken?.let { prefs[KEY_REFRESH_TOKEN] = it }
        }
    }

    // Clear every stored value — called by AuthRepositoryImpl on sign-out
    // or when the backend returns a 401 (expired / revoked token).
    suspend fun clearTokens() {
        context.authDataStore.edit { prefs ->
            prefs.remove(KEY_ACCESS_TOKEN)
            prefs.remove(KEY_REFRESH_TOKEN)
            prefs.remove(KEY_TOKEN_EXPIRY)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ — called by AuthRepositoryImpl and the AuthInterceptor
    // ─────────────────────────────────────────────────────────────────────────

    // Returns the raw JWT string, or null if not stored.
    // Used by AuthInterceptor to attach "Authorization: Bearer <token>" to
    // every outgoing Retrofit request.
    suspend fun getAccessToken(): String? =
        context.authDataStore.data
            .map { prefs -> prefs[KEY_ACCESS_TOKEN] }
            .first()

    // Returns the refresh token, or null if none was issued by the backend.
    suspend fun getRefreshToken(): String? =
        context.authDataStore.data
            .map { prefs -> prefs[KEY_REFRESH_TOKEN] }
            .first()

    // ─────────────────────────────────────────────────────────────────────────
    // SESSION CHECK — called by AuthRepository.isLoggedIn()
    // ─────────────────────────────────────────────────────────────────────────

    // Returns true if an access token exists AND it has not yet expired.
    // This is what NavGraph uses at Splash to decide: Home or SignUp?
    //
    // Importantly this is a SYNCHRONOUS check (using .first() under the hood
    // via runBlocking in the repository) — the splash screen needs the answer
    // before it can navigate. The DataStore read is from disk cache so it is
    // fast enough not to cause a visible delay beyond the 2.2s splash timer.
    suspend fun isTokenValid(): Boolean {
        val prefs  = context.authDataStore.data.first()
        val token  = prefs[KEY_ACCESS_TOKEN]
        val expiry = prefs[KEY_TOKEN_EXPIRY] ?: 0L

        if (token.isNullOrBlank()) return false

        // Consider the token expired 60 seconds before its actual expiry.
        // This buffer prevents edge cases where the token expires mid-request.
        val bufferMs = 60_000L
        return System.currentTimeMillis() < (expiry - bufferMs)
    }

    // Observable flow of the access token — useful for reactive UI updates
    // if you ever need to react to token changes in a Composable.
    // Not used right now but costs nothing to expose.
    val accessTokenFlow: Flow<String?> =
        context.authDataStore.data.map { prefs -> prefs[KEY_ACCESS_TOKEN] }

    // ─────────────────────────────────────────────────────────────────────────
    // CONSTANTS AND KEYS
    // ─────────────────────────────────────────────────────────────────────────

    companion object {
        // The JWT access token returned by Spring Boot on login / sign-up
        private val KEY_ACCESS_TOKEN  = stringPreferencesKey("access_token")

        // Optional refresh token
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")

        // Unix timestamp (ms) of when the access token expires.
        private val KEY_TOKEN_EXPIRY  = longPreferencesKey("token_expiry_ms")

        // Default expiry used if the backend does not return one.
        // 24 hours — adjust to match whatever your Spring Boot config uses.
        const val DEFAULT_EXPIRY_MS = 24 * 60 * 60 * 1000L  // 24 hours in ms
    }
}
