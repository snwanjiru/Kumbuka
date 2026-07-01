package app.kumbuka

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import app.kumbuka.navigation.KumbukaNavGraph
import app.kumbuka.ui.theme.KumbukaColors
import app.kumbuka.ui.theme.KumbukaTheme
import dagger.hilt.android.AndroidEntryPoint

// ─────────────────────────────────────────────────────────────────────────────
// MainActivity
//
// Single-Activity architecture — every screen is a Compose destination wired
// through KumbukaNavGraph. No Fragments needed.
//
// @AndroidEntryPoint tells Hilt that this Activity is part of the DI graph.
// Without this annotation, hiltViewModel() calls inside KumbukaNavGraph would
// crash at runtime with "No @HiltViewModel-annotated class found".
// ─────────────────────────────────────────────────────────────────────────────

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // ── System Splash Screen (Icon Launcher) ─────────────────────────────
        // installSplashScreen() must be called BEFORE super.onCreate()
        val splashScreen = installSplashScreen()
        
        // This condition keeps the system splash screen visible.
        // Once keepSplashScreen becomes false, it transitions to our Compose UI.
        var keepSplashScreen by mutableStateOf(true)
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }

        // Start a simple timer to control the duration.
        // You can change 1000L (1 second) to whatever you prefer.
        window.decorView.postDelayed({
            keepSplashScreen = false
        }, 1000L)

        super.onCreate(savedInstanceState)

        // Draw content behind system bars so each screen can control its own
        // insets via statusBarsPadding() / navigationBarsPadding() modifiers.
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            KumbukaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = KumbukaColors.Background
                ) {
                    // Pass the splash state so the Compose animation knows when to start
                    KumbukaNavGraph(isSystemSplashVisible = keepSplashScreen)
                }
            }
        }
    }
}