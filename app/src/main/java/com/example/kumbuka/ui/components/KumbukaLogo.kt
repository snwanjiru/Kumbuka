package com.example.kumbuka.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.kumbuka.R
import com.example.kumbuka.ui.theme.KumbukaColors

// ─────────────────────────────────────────────────────────────────────────────
// KumbukaLogo — shared composable used by Splash, Login, and SignUp screens.
//
// WHY THIS EXISTS:
//   The original screens loaded the logo from a lh3.googleusercontent.com URL.
//   Those URLs are temporary and expire — that is why the logo disappeared.
//   All screens now import this one composable instead. When you update the
//   logo asset, you only change it here and every screen picks up the change.
//
// SETUP (do this once — see the guide for step-by-step instructions):
//   1. Save your logo image as a PNG file
//   2. Place it at:  app/src/main/res/drawable/ic_kumbuka_logo.png
//   3. The R.drawable.ic_kumbuka_logo reference below will resolve automatically
//
// SIZES USED ACROSS THE APP:
//   Splash screen  → size = 160.dp  (large centred hero logo)
//   Login screen   → size = 44.dp   (top bar, horizontal)
//   SignUp screen  → size = 64.dp   (small centred icon above the form)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun KumbukaLogo(
    size:                Dp               = 64.dp,
    contentDescription:  String           = "Kumbuka",
    modifier:            Modifier         = Modifier
) {
    Image(
        painter            = painterResource(id = R.drawable.ic_kumbuka_logo),
        contentDescription = contentDescription,
        contentScale       = ContentScale.Fit,
        modifier           = modifier.size(size)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// KumbukaLogoWithGlow — splash-screen variant with the soft radial halo
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun KumbukaLogoWithGlow(
    size:     Dp       = 192.dp,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier         = modifier.size(size)
    ) {
        // Soft ambient glow ring behind the logo
        Box(
            modifier = Modifier
                .size(size)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            KumbukaColors.Primary.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        // The actual logo, slightly smaller than the glow ring
        KumbukaLogo(
            size               = size * 0.80f,
            contentDescription = "Kumbuka logo"
        )
    }
}