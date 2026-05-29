package com.example.kumbuka

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

// ─────────────────────────────────────────────────────────────────────────────
// HiltTestRunner
//
// Replaces the default AndroidJUnitRunner for instrumented (UI) tests.
// Tells Hilt to use HiltTestApplication instead of KumbukaApplication during
// tests — this is a special test-only Application that Hilt generates which
// allows individual test modules to override real bindings with fakes.
//
// Registered in app/build.gradle.kts:
//   testInstrumentationRunner = "com.example.kumbuka.HiltTestRunner"
//
// Location: app/src/androidTest/java/com/example/kumbuka/HiltTestRunner.kt
// ─────────────────────────────────────────────────────────────────────────────

class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl:        ClassLoader?,
        className: String?,
        context:   Context?
    ): Application = super.newApplication(cl, HiltTestApplication::class.java.name, context)
}