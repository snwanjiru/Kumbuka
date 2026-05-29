package com.example.kumbuka

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// ─────────────────────────────────────────────────────────────────────────────
// KumbukaApplication
//
// The @HiltAndroidApp annotation triggers Hilt's code generation and creates
// the application-level dependency container. This MUST be the first thing
// that runs when the app starts — which is why it is an Application subclass.
//
// Without this:
//   • @HiltViewModel on AuthViewModel won't work
//   • @AndroidEntryPoint on MainActivity won't work
//   • hiltViewModel() in Compose screens won't work
//
// Registered in AndroidManifest.xml via:
//   <application android:name=".KumbukaApplication" ...>
// ─────────────────────────────────────────────────────────────────────────────

@HiltAndroidApp
class KumbukaApplication : Application()
// No code needed here — Hilt generates everything at compile time.
// Add app-level initialisation (logging, crash reporting, etc.) here later
// by overriding onCreate() if needed.