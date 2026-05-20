package com.example.kumbuka.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
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
// Scenario  6 — Already logged in, offline→ Splash → Home (Firebase cached session)
// Scenario  7 — Forgot password, online   → Login → ForgotPassword → dialog → Login
// Scenario  8 — Forgot password, offline  → ForgotPassword (error banner in screen)
// Scenario  9 — Log out                   → Home → SignUp (stack cleared)
// Scenario 10 — Session expired / 401     → any screen → SignUp (wire in Stage 5)
// Scenario 11 — Duplicate email on signup → SignUp (error banner, no nav change)
// Scenario 12 — OTP login, online         → Login (two-step self-contained in screen)
// Scenario 13 — OTP login, offline        → Login (error banner in screen)
// Scenario 14 — Deep link password reset  → handled by Firebase in browser externally
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
        //   Firebase caches the auth session locally on the device.
        //   currentUser != null reads that local cache — no network needed.
        // ──────────────────────────────────────────────────────────────────────
        composable(route = Routes.SPLASH) {
            val viewModel: AuthViewModel = viewModel()

            // Single source of truth for routing — only this LaunchedEffect
            // decides where to go after the splash delay.
            LaunchedEffect(Unit) {
                delay(2_200)
                if (viewModel.isAlreadyLoggedIn()) {
                    // Scenarios 5 & 6 — valid session (online or cached offline)
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                } else {
                    // Scenarios 1, 2, 3, 4 — no session, land on SignUp
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
        // Primary unauthenticated landing screen.
        // Offline errors (Sc.2) and duplicate email (Sc.11) show inside the
        // screen's own error banner — no navigation change required for those.
        // ──────────────────────────────────────────────────────────────────────
        composable(route = Routes.SIGN_UP) {
            SignUpScreen(
                onSignUpSuccess = {
                    // Scenario 1 — new account created, go home, clear auth stack
                    // so Back on Home exits the app instead of returning here
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SIGN_UP) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    // Scenarios 3, 4 — returning user taps "Log in"
                    // SignUp stays in the back-stack so Back on Login returns here
                    navController.navigate(Routes.LOGIN)
                }
            )
        }

        // ── 3. LOGIN ───────────────────────────────────────────────────────────
        // Covers: Scenarios 3, 4, 7, 8, 12, 13
        //
        // Offline errors (Sc.4), OTP flow (Sc.12, 13) are self-contained inside
        // LoginScreen — no navigation change required for those cases.
        // ──────────────────────────────────────────────────────────────────────
        composable(route = Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    // Scenarios 3, 12 — authenticated successfully
                    // Clear entire auth stack (SignUp + Login) so Back exits app
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SIGN_UP) { inclusive = true }
                    }
                },
                onForgotPassword = {
                    // Scenario 7 — user needs a reset link
                    navController.navigate(Routes.FORGOT_PASSWORD)
                },
                onSignUp = {
                    // User wants to create an account instead
                    // Pop back to SignUp which is already below Login in the stack
                    navController.popBackStack()
                },
                onBack = {
                    // Back arrow — returns to SignUp
                    navController.popBackStack()
                }
            )
        }

        // ── 4. FORGOT PASSWORD ─────────────────────────────────────────────────
        // Covers: Scenarios 7, 8
        //
        // Offline errors (Sc.8) show inside the screen's own error banner.
        // On success the screen shows a non-dismissible dialog, then calls
        // onNavigateBackToLogin which fires the lambda below.
        // ──────────────────────────────────────────────────────────────────────
        composable(route = Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                onNavigateBackToLogin = {
                    // Scenario 7 — dialog OK tapped, or back arrow pressed
                    // Remove ForgotPassword from stack; Login sits below it
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.FORGOT_PASSWORD) { inclusive = true }
                    }
                }
            )
        }

        // ── 5. HOME ────────────────────────────────────────────────────────────
        // Covers: Scenarios 5, 6, 9, 10
        //
        // Scenario 10 (session expired / 401) will be handled here in Stage 5
        // when Retrofit is wired up. The AuthInterceptor will detect a 401,
        // call viewModel.signOut(), and trigger the same flow as Scenario 9.
        // ──────────────────────────────────────────────────────────────────────
        composable(route = Routes.HOME) {
            val viewModel: AuthViewModel = viewModel()

            HomeScreen(
                onLogOut = {
                    // Scenarios 9 & 10 — clear Firebase session first
                    viewModel.signOut()
                    // Land on SignUp and wipe the entire stack so Back exits app
                    navController.navigate(Routes.SIGN_UP) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }
    }
}