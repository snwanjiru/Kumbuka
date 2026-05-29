package com.example.kumbuka.di

import android.content.Context
import com.example.kumbuka.BuildConfig
import com.example.kumbuka.data.TokenManager
import com.example.kumbuka.network.AuthApiService
import com.example.kumbuka.repository.AuthRepository
import com.example.kumbuka.repository.AuthRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
// QUALIFIER annotations
//
// We have two OkHttp interceptors. Qualifiers let Hilt tell them apart when
// injecting — without these, Hilt sees two Interceptor bindings and crashes
// with "ambiguous bindings" at compile time.
// ─────────────────────────────────────────────────────────────────────────────

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthInterceptorQualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LoggingInterceptorQualifier

// ─────────────────────────────────────────────────────────────────────────────
// NetworkModule
//
// Provides the entire networking stack as singletons:
//   TokenManager → AuthInterceptor → LoggingInterceptor → OkHttpClient
//                                                       → Retrofit
//                                                       → AuthApiService
//
// FirebaseModule is REMOVED — no Firebase Auth dependency anywhere here.
// ─────────────────────────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // ── TokenManager ──────────────────────────────────────────────────────────
    // Manages JWT storage in DataStore. Provided here so Hilt can inject it
    // into both AuthRepositoryImpl and the AuthInterceptor.
    @Provides
    @Singleton
    fun provideTokenManager(
        @ApplicationContext context: Context
    ): TokenManager = TokenManager(context)

    // ── Auth interceptor ──────────────────────────────────────────────────────
    // Reads the stored JWT and attaches it to every outgoing request as:
    //   Authorization: Bearer <token>
    //
    // This runs BEFORE the request hits the network. If no token exists
    // (user not logged in), the request goes out without the header — the
    // backend will return 401 which the repository handles.
    //
    // runBlocking is justified here: OkHttp interceptors are synchronous by
    // design. The DataStore read hits the in-memory cache after first access
    // so it is effectively instant and will not block long enough to ANR.
    @Provides
    @Singleton
    @AuthInterceptorQualifier
    fun provideAuthInterceptor(tokenManager: TokenManager): Interceptor =
        Interceptor { chain ->
            val token = runBlocking { tokenManager.getAccessToken() }
            val request = if (!token.isNullOrBlank()) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }

    // ── Logging interceptor ───────────────────────────────────────────────────
    // Prints the full request URL, headers, and response body in Logcat.
    // BODY level is used in debug only — switch to NONE in release to avoid
    // leaking token values into device logs in production.
    @Provides
    @Singleton
    @LoggingInterceptorQualifier
    fun provideLoggingInterceptor(): Interceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

    // ── OkHttpClient ──────────────────────────────────────────────────────────
    // The HTTP engine that Retrofit sits on top of.
    // Interceptor ORDER matters:
    //   1. AuthInterceptor runs first — adds the token to the request
    //   2. LoggingInterceptor runs second — logs the final request WITH the token
    // Reversing the order would log requests before the token is attached.
    @Provides
    @Singleton
    fun provideOkHttpClient(
        @AuthInterceptorQualifier    authInterceptor:    Interceptor,
        @LoggingInterceptorQualifier loggingInterceptor: Interceptor
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS) // set back to 30 before release
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()

    // ── Retrofit ──────────────────────────────────────────────────────────────
    // The base_url comes from BuildConfig.API_BASE_URL which is injected at
    // build time from secrets.properties — no URL hardcoded in source code.
    // GsonConverterFactory handles JSON ↔ data class conversion automatically.
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    // ── AuthApiService ────────────────────────────────────────────────────────
    // Retrofit generates the implementation of our interface at runtime.
    // Injecting AuthApiService anywhere in the app gives you a fully working
    // HTTP client pointed at your Spring Boot backend.
    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService =
        retrofit.create(AuthApiService::class.java)
}

// ─────────────────────────────────────────────────────────────────────────────
// RepositoryModule
//
// Tells Hilt: "when AuthRepository is requested, provide AuthRepositoryImpl".
// Abstract @Binds is more efficient than @Provides for this pattern — Hilt
// generates the binding at compile time without creating a wrapper method.
// ─────────────────────────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}