package com.example.kumbuka.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kumbuka.ui.components.KumbukaLogo
import com.example.kumbuka.ui.theme.KumbukaColors
import com.example.kumbuka.ui.theme.ManropeFamily
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// Layout constants — same straddling pattern used across all screens
// ─────────────────────────────────────────────────────────────────────────────
private val TOP_BAR_HEIGHT = 64.dp
private val LOGO_CIRCLE    = 80.dp
private val LOGO_HALF      = LOGO_CIRCLE / 2   // 40.dp

// Orbit constants — tweak these to resize the circular feature layout
private val ORBIT_CONTAINER = 300.dp   // total canvas the orbit sits inside
private val ORBIT_RADIUS_DP = 108f     // distance from centre to each item centre
private val ITEM_SIZE       = 76.dp    // diameter of each feature circle

// ─────────────────────────────────────────────────────────────────────────────
// Feature data model
// ─────────────────────────────────────────────────────────────────────────────
private data class Feature(
    val icon:        ImageVector,
    val title:       String,
    val description: String
)

private val features = listOf(
    Feature(Icons.Outlined.Alarm,   "Forgetfulness",   "Automated tracking and smart reminders ensure no loan slips through."),
    Feature(Icons.Outlined.Groups,  "Social Friction",  "Gentler, third-party nudges mean you never have to be the bad guy."),
    Feature(Icons.Outlined.Balance, "Lack of Order",    "A centralized ledger both parties can verify anytime."),
    Feature(Icons.Outlined.Shield,  "Overextending",    "Smart tools to set lending boundaries and track your exposure.")
)

// ─────────────────────────────────────────────────────────────────────────────
// HomeScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    onLogOut:       () -> Unit,
    onGetStarted:   () -> Unit = {}    // placeholder — wire to CircleListScreen later
) {
    // ── Orbit rotation state ──────────────────────────────────────────────────
    // Currently static (0f). To animate in future, replace with:
    //   val infiniteTransition = rememberInfiniteTransition(label = "orbit")
    //   val orbitRotation by infiniteTransition.animateFloat(
    //       initialValue = 0f, targetValue = 360f,
    //       animationSpec = infiniteRepeatable(tween(12_000, easing = LinearEasing)),
    //       label = "orbitAngle"
    //   )
    val orbitRotation = 0f   // change to animated value when ready

    // ── Selected feature detail ───────────────────────────────────────────────
    var selectedFeature by remember { mutableStateOf<Feature?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KumbukaColors.Background)
    ) {
        // ── Layer 1: scrollable page content ─────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            // ── Top bar — same bg as screen for seamless logo straddle ────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TOP_BAR_HEIGHT)
                    .background(KumbukaColors.Background)
                    .statusBarsPadding()
            ) {
                // Log out — pinned to the right
                TextButton(
                    onClick  = onLogOut,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        "Log out",
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize   = 13.sp,
                        color      = KumbukaColors.Secondary
                    )
                }
                // Logo slot is empty here — it floats in Layer 2
            }

            // ── Dark hero section ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // Extra top padding so the hero doesn't hide behind the logo
                    .padding(top = LOGO_HALF + 8.dp)
                    .background(KumbukaColors.Primary)
                    .padding(horizontal = 24.dp, vertical = 36.dp)
            ) {
                Column {
                    Text(
                        text       = "Lend with clarity.",
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 30.sp,
                        color      = KumbukaColors.OnPrimaryContainer,
                        lineHeight = 38.sp
                    )
                    Text(
                        text       = "Preserve the bond.",
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 30.sp,
                        color      = Color.White,
                        lineHeight = 38.sp
                    )
                }
            }

            // ── Features section — light background ───────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KumbukaColors.SurfaceContainerLow)
                    .padding(horizontal = 24.dp, vertical = 36.dp)
            ) {
                Text(
                    text       = "Solving the Friction of Lending",
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 22.sp,
                    color      = KumbukaColors.Primary,
                    textAlign  = TextAlign.Center
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text       = "Informal lending shouldn't cost you a friendship. We handle the awkward parts so you can focus on the relationship.",
                    fontFamily = ManropeFamily,
                    fontSize   = 14.sp,
                    color      = KumbukaColors.OnSurfaceVariant,
                    textAlign  = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(Modifier.height(24.dp))

                // "Get Started" button
                Button(
                    onClick = onGetStarted,
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = KumbukaColors.Primary,
                        contentColor   = KumbukaColors.OnPrimary
                    ),
                    shape    = RoundedCornerShape(50),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        "Get Started",
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 14.sp
                    )
                }

                Spacer(Modifier.height(40.dp))

                // ── CIRCULAR ORBIT of 4 features ─────────────────────────────
                // Each feature is placed at 90° intervals around a centre hub.
                // orbitRotation shifts all items simultaneously — wire it to
                // an InfiniteTransition to make them orbit continuously.
                //
                // Tap any item to reveal its description below the orbit.
                //
                // Position maths:
                //   angleRad  = toRadians(orbitRotation + index*90 - 90)
                //   px offset = cos(angleRad) * ORBIT_RADIUS_DP   (in dp)
                //   py offset = sin(angleRad) * ORBIT_RADIUS_DP
                //   final x   = containerCentre + px - itemSize/2
                //   final y   = containerCentre + py - itemSize/2
                // ─────────────────────────────────────────────────────────────
                val containerPx   = with(androidx.compose.ui.platform.LocalDensity.current) { ORBIT_CONTAINER.toPx() }
                val itemSizePx    = with(androidx.compose.ui.platform.LocalDensity.current) { ITEM_SIZE.toPx() }
                val orbitRadiusPx = with(androidx.compose.ui.platform.LocalDensity.current) { ORBIT_RADIUS_DP.dp.toPx() }
                val centrePx      = containerPx / 2f

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(ORBIT_CONTAINER)
                ) {

                    // ── Dashed orbit ring (visual guide) ──────────────────────
                    Box(
                        modifier = Modifier
                            .size((ORBIT_RADIUS_DP * 2 + ITEM_SIZE.value).dp)
                            .clip(CircleShape)
                            .background(
                                color = KumbukaColors.SurfaceContainerHigh.copy(alpha = 0.6f)
                            )
                    )

                    // ── Inner ring (subtle) ───────────────────────────────────
                    Box(
                        modifier = Modifier
                            .size((ORBIT_RADIUS_DP * 0.8f).dp)
                            .clip(CircleShape)
                            .background(KumbukaColors.Background)
                    )

                    // ── Centre hub: Kumbuka logo ──────────────────────────────
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(60.dp)
                            .shadow(elevation = 3.dp, shape = CircleShape, clip = false)
                            .background(Color.White, CircleShape)
                    ) {
                        KumbukaLogo(size = 40.dp)
                    }

                    // ── The 4 orbiting feature items ──────────────────────────
                    features.forEachIndexed { index, feature ->
                        val angleRad = Math.toRadians(
                            (orbitRotation + index * 90f - 90f).toDouble()
                        )
                        val px = (cos(angleRad) * orbitRadiusPx).toFloat()
                        val py = (sin(angleRad) * orbitRadiusPx).toFloat()

                        val offsetX = (centrePx + px - itemSizePx / 2f).roundToInt()
                        val offsetY = (centrePx + py - itemSizePx / 2f).roundToInt()

                        val isSelected = selectedFeature == feature

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(ITEM_SIZE)
                                .offset { IntOffset(offsetX, offsetY) }
                                .shadow(
                                    elevation = if (isSelected) 8.dp else 3.dp,
                                    shape     = CircleShape,
                                    clip      = false
                                )
                                .background(
                                    color = if (isSelected) KumbukaColors.PrimaryContainer
                                    else Color.White,
                                    shape = CircleShape
                                )
                                .clip(CircleShape)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp)
                            ) {
                                Icon(
                                    imageVector        = feature.icon,
                                    contentDescription = feature.title,
                                    tint               = if (isSelected) Color.White
                                    else KumbukaColors.Primary,
                                    modifier           = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text       = feature.title,
                                    fontFamily = ManropeFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 9.sp,
                                    color      = if (isSelected) Color.White
                                    else KumbukaColors.Primary,
                                    textAlign  = TextAlign.Center,
                                    maxLines   = 2,
                                    lineHeight = 12.sp
                                )
                            }
                        }

                        // Invisible full-size tap target sitting on the item
                        // (prevents the offset Box clickable fighting with scroll)
                        Box(
                            modifier = Modifier
                                .size(ITEM_SIZE)
                                .offset { IntOffset(offsetX, offsetY) }
                                .clip(CircleShape)
                                .then(
                                    Modifier.noRippleClickable {
                                        selectedFeature =
                                            if (selectedFeature == feature) null else feature
                                    }
                                )
                        )
                    }
                }

                // ── Feature detail card — appears below orbit on tap ──────────
                selectedFeature?.let { f ->
                    Spacer(Modifier.height(20.dp))
                    Surface(
                        shape          = RoundedCornerShape(16.dp),
                        color          = KumbukaColors.SurfaceContainerLowest,
                        shadowElevation = 2.dp,
                        modifier       = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        KumbukaColors.SurfaceContainerHigh,
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    f.icon, f.title,
                                    tint     = KumbukaColors.Primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    f.title,
                                    fontFamily = ManropeFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 14.sp,
                                    color      = KumbukaColors.Primary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    f.description,
                                    fontFamily = ManropeFamily,
                                    fontSize   = 13.sp,
                                    color      = KumbukaColors.OnSurfaceVariant,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Tap hint
                Text(
                    "Tap any circle to learn more",
                    fontFamily = ManropeFamily,
                    fontSize   = 11.sp,
                    color      = KumbukaColors.OnSurfaceVariant.copy(alpha = 0.6f),
                    textAlign  = TextAlign.Center
                )
            }

            // ── Privacy & Security section ────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KumbukaColors.SurfaceContainerLowest)
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                Text(
                    "Private & Secure",
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp,
                    color      = KumbukaColors.Primary
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    "Your data is yours. We use bank-level encryption to ensure that your financial relationships stay private and secure. No one sees your ledger except for you and the person you're transacting with.",
                    fontFamily = ManropeFamily,
                    fontSize   = 14.sp,
                    color      = KumbukaColors.OnSurfaceVariant,
                    lineHeight = 22.sp
                )

                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(
                        Icons.Outlined.Shield to "Encrypted",
                    ).forEach { (icon, label) ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    KumbukaColors.SurfaceContainerHigh,
                                    CircleShape
                                )
                        ) {
                            Icon(icon, label,
                                tint     = KumbukaColors.Primary,
                                modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // ── Footer ────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KumbukaColors.Background)
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Text(
                    "Kumbuka",
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp,
                    color      = KumbukaColors.Primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "© 2026 Kumbuka. Built for gentle accountability.",
                    fontFamily = ManropeFamily,
                    fontSize   = 12.sp,
                    color      = KumbukaColors.OnSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    listOf("Privacy Policy", "Terms of Service", "Contact Support").forEach { label ->
                        Text(
                            label,
                            fontFamily = ManropeFamily,
                            fontSize   = 12.sp,
                            color      = KumbukaColors.OnSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        // ── Layer 2: straddling logo circle ───────────────────────────────────
        // Centre sits exactly on the top-bar bottom edge —
        // same technique as ForgotPasswordScreen.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = TOP_BAR_HEIGHT - LOGO_HALF)
                .size(LOGO_CIRCLE)
                .shadow(elevation = 4.dp, shape = CircleShape, clip = false)
                .background(Color.White, CircleShape)
        ) {
            KumbukaLogo(size = LOGO_CIRCLE * 0.68f)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Clickable extension — avoids importing the wrong clickable overload
// ─────────────────────────────────────────────────────────────────────────────
private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = composed {
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication        = null,
        onClick           = onClick
    )
}