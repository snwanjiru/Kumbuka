package app.kumbuka.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import app.kumbuka.ui.components.KumbukaLogo
import app.kumbuka.ui.theme.KumbukaColors
import app.kumbuka.ui.theme.ManropeFamily
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// SplashScreen
//
// Shown on cold-start for 2 200 ms, then NavController decides where to go:
//   • Already logged in  → Home   (via onNavigateToHome)
//   • Not logged in      → SignUp (via onNavigateToSignUp)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SplashScreen(
    isSystemSplashVisible: Boolean = false,
    onNavigateToSignUp: () -> Unit,   // default path — new / unauthenticated user
    onNavigateToHome:   () -> Unit    // already-logged-in fast path
) {
    // ── Animation States ──────────────────────────────────────────────────────
    var startAnimation by remember { mutableStateOf(false) }

    // This Scale variable sets how much the splash screen logo bounces into place
    val animScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    // This alpha variable can be adjusted to make the logo appear faster or slower
    val animAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "alpha"
    )

    // ── Animation Trigger ─────────────────────────────────────────────────────
    // Only start the bouncy animation when the system splash screen (icon) is gone
    LaunchedEffect(isSystemSplashVisible) {
        if (!isSystemSplashVisible) {
            startAnimation = true
        }
    }

    // ── Timer — NavGraph decides which branch to take ─────────────────────────
    LaunchedEffect(Unit) {
        delay(2_200)
        onNavigateToSignUp()   // NavGraph will override with Home if needed
    }

    // ── Background: radial gradient white → #f8f9ff ───────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color.White, KumbukaColors.Background)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(Modifier.weight(1f))

            // ── Circular Logo Circle ─────────────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer {
                        scaleX = animScale
                        scaleY = animScale
                        alpha  = animAlpha
                    }
                    .shadow(elevation = 12.dp, shape = CircleShape, clip = false)
                    .background(Color.White, CircleShape)
            ) {
                KumbukaLogo(size = 100.dp)
            }

            Spacer(Modifier.height(24.dp))

            // ── "Gentle Accountability" tagline ──────────────────────────────
            Text(
                text       = "Gentle Accountability",
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Bold,
                fontSize   = 24.sp,
                lineHeight = (24 * 1.3).sp,
                color      = KumbukaColors.OnPrimaryContainer,
                textAlign  = TextAlign.Center,
                modifier   = Modifier.graphicsLayer {
                    alpha = animAlpha
                }
            )

            Spacer(Modifier.height(40.dp))

            // ── Animated pulsing dots ─────────────────────────────────────────
            SplashDots()

            Spacer(Modifier.weight(1f))
        }

        // ── "Informal Lending, Formalized" footer ─────────────────────────────
        Text(
            text      = "INFORMAL LENDING, FORMALIZED",
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.Medium,
            fontSize   = 12.sp,
            letterSpacing = 2.sp,
            color     = KumbukaColors.OnSurfaceVariant.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier  = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SplashDots — three circles with decreasing opacity, first dot pulses
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SplashDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 0.3f,
        animationSpec = infiniteRepeatable(
            animation  = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape)
            .background(KumbukaColors.Primary.copy(alpha = alpha)))
        Box(Modifier.size(6.dp).clip(CircleShape)
            .background(KumbukaColors.Primary.copy(alpha = 0.4f)))
        Box(Modifier.size(6.dp).clip(CircleShape)
            .background(KumbukaColors.Primary.copy(alpha = 0.2f)))
    }
}