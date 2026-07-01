package app.kumbuka.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel          // ← CHANGED from viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.kumbuka.ui.screens.ForgotPasswordScreen
import app.kumbuka.ui.screens.HomeScreen
import app.kumbuka.ui.screens.LoginScreen
import app.kumbuka.ui.screens.RecordTransactionScreen
import app.kumbuka.ui.screens.RemindersScreen
import app.kumbuka.ui.screens.SignUpScreen
import app.kumbuka.ui.screens.SplashScreen
import app.kumbuka.ui.screens.TransactionListScreen
import app.kumbuka.viewmodel.AuthViewModel
import app.kumbuka.viewmodel.HomeTab
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// ROUTES
// ─────────────────────────────────────────────────────────────────────────────

object Routes {
    const val SPLASH             = "splash"
    const val SIGN_UP            = "sign_up"
    const val LOGIN              = "login"
    const val FORGOT_PASSWORD    = "forgot_password"
    const val HOME               = "home?tab={tab}"
    const val REMINDERS          = "reminders"
    const val RECORD_TRANSACTION = "record_transaction/{type}?id={id}"
    const val TRANSACTION_LIST   = "transaction_list"
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
    navController: NavHostController = rememberNavController(),
    isSystemSplashVisible: Boolean = false
) {
    NavHost(
        navController    = navController,
        startDestination = Routes.SPLASH
    ) {

        // ── 1. SPLASH ──────────────────────────────────────────────────────────
        composable(route = Routes.SPLASH) {
            val viewModel: AuthViewModel = hiltViewModel()

            LaunchedEffect(Unit) {
                delay(2_200)
                if (viewModel.isAlreadyLoggedIn()) {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                } else {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            }

            SplashScreen(
                isSystemSplashVisible = isSystemSplashVisible,
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
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
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
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onForgotPassword = {
                    navController.navigate(Routes.FORGOT_PASSWORD)
                },
                onSignUp = {
                    navController.navigate(Routes.SIGN_UP)
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
        composable(route = Routes.HOME) { backStackEntry ->
            // Extract the initial tab from the URL parameter, default to Dashboard
            val tabParam = backStackEntry.arguments?.getString("tab")
            val initialTab = if (tabParam == "cash_flow") HomeTab.CashFlow else HomeTab.Dashboard

            // CHANGED: hiltViewModel() replaces viewModel()
            val viewModel: AuthViewModel = hiltViewModel()

            HomeScreen(
                onLogOut = {
                    // Clears JWT from DataStore, then NavGraph navigates away
                    viewModel.signOut()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
                onNavigateToRecordLent = { id ->
                    val route = if (id != null) "record_transaction/lent?id=$id" else "record_transaction/lent"
                    navController.navigate(route)
                },
                onNavigateToRecordBorrowed = { id ->
                    val route = if (id != null) "record_transaction/borrowed?id=$id" else "record_transaction/borrowed"
                    navController.navigate(route)
                },
                onNavigateToActivity = {
                    navController.navigate(Routes.REMINDERS)
                },
                initialTab = initialTab
            )
        }

        // ── 5a. REMINDERS ──────────────────────────────────────────────────────
        composable(route = Routes.REMINDERS) {
            RemindersScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── 6. RECORD TRANSACTION ─────────────────────────────────────────────
        composable(
            route = Routes.RECORD_TRANSACTION,
            arguments = listOf(
                navArgument("type") { type = NavType.StringType },
                navArgument("id") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "lent"
            val id = backStackEntry.arguments?.getString("id")?.toLongOrNull()
            RecordTransactionScreen(
                transactionType = type,
                transactionId = id,
                onNavigateBack = { 
                    navController.navigate("home?tab=cash_flow") {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
                onSaveSuccess = { 
                    navController.navigate("home?tab=cash_flow") {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }

        // ── 7. TRANSACTION LIST ───────────────────────────────────────────────
        composable(route = Routes.TRANSACTION_LIST) {
            TransactionListScreen(
                onNavigateToRecord = { type, id ->
                    val route = if (id != null) "record_transaction/$type?id=$id" else "record_transaction/$type"
                    navController.navigate(route)
                }
            )
        }
    }
}