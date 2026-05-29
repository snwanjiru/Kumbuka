package com.example.kumbuka

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.example.kumbuka.navigation.KumbukaNavGraph
import com.example.kumbuka.ui.theme.KumbukaColors
import com.example.kumbuka.ui.theme.KumbukaTheme
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
                    // All navigation and screen composition happens here.
                    // ViewModels are created via hiltViewModel() inside NavGraph
                    // — MainActivity has no knowledge of them.
                    KumbukaNavGraph()
                }
            }
        }
    }
}