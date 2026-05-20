package com.example.kumbuka

import android.os.Bundle
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.example.kumbuka.ui.theme.KumbukaColors
import com.example.kumbuka.ui.theme.KumbukaTheme
import com.example.kumbuka.navigation.KumbukaNavGraph
import com.example.kumbuka.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewModel
        authViewModel = AuthViewModel()

        // Draw content behind system bars
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Check if the app was opened via a deep link (email sign‑in link)
        handleDeepLink(intent)

        setContent {
            KumbukaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = KumbukaColors.Background
                ) {
                    KumbukaNavGraph()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle new deep links while the app is already open
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent) {
        val deepLink = intent.data?.toString()
        if (deepLink == null) return

        // Check if this is a Firebase email sign‑in link
        val auth = FirebaseAuth.getInstance()
        if (auth.isSignInWithEmailLink(deepLink)) {
            // Retrieve the stored email address
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val email = prefs.getString("email_for_sign_in", null)

            if (email != null) {
                // Complete sign‑in using ViewModel
                lifecycleScope.launch {
                    authViewModel.signInWithEmailLink(email, deepLink)
                    // After sign‑in, the NavGraph will observe authState and navigate accordingly.
                    // You can also close the current activity or show a success message.
                }
            } else {
                // Email not found – maybe the user cleared preferences.
                // Ask the user to re‑enter their email address.
                Log.e("MainActivity", "Email not found for sign‑in link")
                // Optionally show a dialog or redirect to login screen.
            }
        }
    }
}