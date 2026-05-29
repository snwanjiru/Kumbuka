package com.example.kumbuka.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel          // ← CHANGED from viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.kumbuka.ui.screens.ForgotPasswordScreen
import com.example.kumbuka.ui.screens.HomeScreen
import com.example.kumbuka.ui.screens.LoginScreen
import com.example.kumbuka.ui.screens.SignUpScreen
import com.example.kumbuka.ui.screens.SplashScreen
import com.example.kumbuka.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// ROUTES
// ─────────────────────────────────────────────────────────────────────────────

object Routes {
    const val SPLASH          = "splash"
    const val SIGN_UP         = "sign_up"
    const val LOGIN           = "login"
    const val FORGOT_PASSWORD = "forgot_password"
    const val HOME            = "home"
}

// ─────────────────────────────────────────────────────────────────────────────
// SCENARIO COVERAGE
//
// Scenario  1 — New user, online          → Splash → SignUp → Home
// Scenario  2 — New user, offline         → Splash → SignUp (error banner in screen)
// Scenario  3 — Returning user, online    → Splash → SignUp → Login → Home
// Scenario  4 — Returning user, offline   → Splash → SignUp → Login (error banner)
// Scenario  5 — Already logged in, online → Splash → Home (skips auth)
// Scenario  6 — Already logged in, offline→ Splash → Home (JWT cached in DataStore)
// Scenario  7 — Forgot password, online   → Login → ForgotPassword → dialog → Login
// Scenario  8 — Forgot password, offline  → ForgotPassword (error banner in screen)
// Scenario  9 — Log out                   → Home → SignUp (stack cleared)
// Scenario 10 — Session expired / 401     → any screen → SignUp (wire in Stage 5)
// Scenario 11 — Duplicate email on signup → SignUp (error banner, no nav change)
// Scenario 12 — OTP login, online         → Login (two-step self-contained in screen)
// Scenario 13 — OTP login, offline        → Login (error banner in screen)
// Scenario 14 — Password reset link       → handled by Spring Boot email → browser
//
// BACK-STACK RULES
//   Back on SignUp         → exits the app  (nothing below it)
//   Back on Login          → returns to SignUp
//   Back on ForgotPassword → returns to Login
//   Back on Home           → exits the app  (auth stack cleared on login)
//   After logout           → lands on SignUp, back exits the app
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun KumbukaNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController    = navController,
        startDestination = Routes.SPLASH
    ) {

        // ── 1. SPLASH ──────────────────────────────────────────────────────────
        // Covers: Scenarios 1, 2, 3, 4, 5, 6
        //
        // WHY LaunchedEffect lives HERE and not inside SplashScreen:
        //   SplashScreen has its own internal LaunchedEffect that calls
        //   onNavigateToSignUp() after 2.2s. If we passed real lambdas to it
        //   AND had our own LaunchedEffect here, navigation would fire TWICE.
        //   Solution: NavGraph owns the routing decision entirely.
        //   SplashScreen receives empty lambdas {} — it just shows the UI.
        //
        // WHY isAlreadyLoggedIn() works offline (Scenario 6):
        //   TokenManager.isTokenValid() reads from DataStore which is written
        //   to disk on login. No network call is made — works fully offline
        //   as long as the JWT has not expired.
        // ──────────────────────────────────────────────────────────────────────
        composable(route = Routes.SPLASH) {
            // CHANGED: hiltViewModel() — Hilt creates and injects AuthRepository
            // automatically. viewModel() would fail because AuthViewModel now
            // requires constructor injection that only Hilt can provide.
            val viewModel: AuthViewModel = hiltViewModel()

            LaunchedEffect(Unit) {
                delay(2_200)
                if (viewModel.isAlreadyLoggedIn()) {
                    // Scenarios 5 & 6 — valid JWT in DataStore, go straight home
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                } else {
                    // Scenarios 1, 2, 3, 4 — no valid token, land on SignUp
                    navController.navigate(Routes.SIGN_UP) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            }

            // Empty lambdas — SplashScreen shows UI only.
            // NavGraph's LaunchedEffect above owns all navigation.
            SplashScreen(
                onNavigateToSignUp = {},
                onNavigateToHome   = {}
            )
        }

        // ── 2. SIGN UP ─────────────────────────────────────────────────────────
        // Covers: Scenarios 1, 2, 9, 11
        //
        // hiltViewModel() is called inside SignUpScreen itself — no ViewModel
        // needed at the NavGraph level for this destination.
        // ──────────────────────────────────────────────────────────────────────
        composable(route = Routes.SIGN_UP) {
            SignUpScreen(
                onSignUpSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SIGN_UP) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN)
                }
            )
        }

        // ── 3. LOGIN ───────────────────────────────────────────────────────────
        // Covers: Scenarios 3, 4, 7, 8, 12, 13
        //
        // hiltViewModel() is called inside LoginScreen itself.
        // ──────────────────────────────────────────────────────────────────────
        composable(route = Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SIGN_UP) { inclusive = true }
                    }
                },
                onForgotPassword = {
                    navController.navigate(Routes.FORGOT_PASSWORD)
                },
                onSignUp = {
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // ── 4. FORGOT PASSWORD ─────────────────────────────────────────────────
        // Covers: Scenarios 7, 8
        //
        // hiltViewModel() is called inside ForgotPasswordScreen itself.
        // ──────────────────────────────────────────────────────────────────────
        composable(route = Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                onNavigateBackToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.FORGOT_PASSWORD) { inclusive = true }
                    }
                }
            )
        }

        // ── 5. HOME ────────────────────────────────────────────────────────────
        // Covers: Scenarios 5, 6, 9, 10
        //
        // CHANGED: hiltViewModel() — needed here at the NavGraph level because
        // HomeScreen needs viewModel.signOut() wired to the logout button, and
        // Hilt must provide the injected AuthRepository to construct it.
        //
        // Scenario 10 (session expired / 401) will be handled here in Stage 5
        // when the AuthInterceptor detects a 401 and triggers signOut().
        // ──────────────────────────────────────────────────────────────────────
        composable(route = Routes.HOME) {
            // CHANGED: hiltViewModel() replaces viewModel()
            val viewModel: AuthViewModel = hiltViewModel()

            HomeScreen(
                onLogOut = {
                    // Clears JWT from DataStore, then NavGraph navigates away
                    viewModel.signOut()
                    navController.navigate(Routes.SIGN_UP) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }
    }
}