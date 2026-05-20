package com.example.kumbuka.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.kumbuka.R

// ─────────────────────────────────────────────────────────────────────────────
// COLOR TOKENS — mirrored exactly from the Stitch tailwind.config
// ─────────────────────────────────────────────────────────────────────────────

object KumbukaColors {
    val Primary            = Color(0xFF000915)  // deep navy-black
    val OnPrimary          = Color(0xFFFFFFFF)
    val PrimaryContainer   = Color(0xFF122130)  // used as dark button bg
    val OnPrimaryContainer = Color(0xFF7A899B)

    val Secondary          = Color(0xFFB52614)  // red accent
    val OnSecondary        = Color(0xFFFFFFFF)
    val SecondaryContainer = Color(0xFFFF5C43)
    val OnSecondaryContainer = Color(0xFF5F0500)

    val Tertiary           = Color(0xFF050809)
    val OnTertiary         = Color(0xFFFFFFFF)
    val TertiaryContainer  = Color(0xFF1D2022)
    val OnTertiaryContainer = Color(0xFF858889)

    val Background         = Color(0xFFF8F9FF)  // near-white blue tint
    val OnBackground       = Color(0xFF0B1C30)

    val Surface            = Color(0xFFF8F9FF)
    val OnSurface          = Color(0xFF0B1C30)
    val SurfaceVariant     = Color(0xFFD3E4FE)
    val OnSurfaceVariant   = Color(0xFF44474C)

    val SurfaceContainerLowest  = Color(0xFFFFFFFF)
    val SurfaceContainerLow     = Color(0xFFEFF4FF)
    val SurfaceContainer        = Color(0xFFE5EEFF)
    val SurfaceContainerHigh    = Color(0xFFDCE9FF)
    val SurfaceContainerHighest = Color(0xFFD3E4FE)
    val SurfaceDim              = Color(0xFFCBDBF5)
    val SurfaceBright           = Color(0xFFF8F9FF)

    val Outline        = Color(0xFF74777D)
    val OutlineVariant = Color(0xFFC4C6CC)

    val Error          = Color(0xFFBA1A1A)
    val OnError        = Color(0xFFFFFFFF)
    val ErrorContainer = Color(0xFFFFDAD6)

    val InverseSurface   = Color(0xFF213145)
    val InverseOnSurface = Color(0xFFEAF1FF)
    val InversePrimary   = Color(0xFFB9C8DC)

    val PrimaryFixed        = Color(0xFFD5E4F9)
    val PrimaryFixedDim     = Color(0xFFB9C8DC)
    val OnPrimaryFixed      = Color(0xFF0D1D2B)
    val OnPrimaryFixedVariant = Color(0xFF394859)
}

// ─────────────────────────────────────────────────────────────────────────────
// FONT FAMILY — Manrope (add the font files to res/font/ and replace with
// FontFamily(Font(R.font.manrope_regular), ...) once assets are available;
// for now we rely on the system default which approximates the weight range)
// ─────────────────────────────────────────────────────────────────────────────

// When you add Manrope OTF/TTF files to res/font/, replace this with:
//   val ManropeFamily = FontFamily(
//       Font(R.font.manrope_regular,   FontWeight.Normal),
//       Font(R.font.manrope_medium,    FontWeight.Medium),
//       Font(R.font.manrope_semibold,  FontWeight.SemiBold),
//       Font(R.font.manrope_bold,      FontWeight.Bold),
//       Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
//   )
val ManropeFamily = FontFamily(
    Font(R.font.manrope_regular,    FontWeight.Normal),
    Font(R.font.manrope_medium,     FontWeight.Medium),
    Font(R.font.manrope_semibold,   FontWeight.SemiBold),
    Font(R.font.manrope_bold,       FontWeight.Bold),
    Font(R.font.manrope_extrabold,  FontWeight.ExtraBold),
)

// ─────────────────────────────────────────────────────────────────────────────
// TYPOGRAPHY — mapped from Stitch fontSize tokens
// ─────────────────────────────────────────────────────────────────────────────

val KumbukaTypography = Typography(
    // display-lg  → 48sp / ExtraBold / -0.02em tracking
    displayLarge = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 48.sp,
        lineHeight = (48 * 1.1).sp,
        letterSpacing = (-0.02 * 48).sp
    ),
    // headline-lg → 32sp / Bold / -0.01em
    headlineLarge = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = (32 * 1.2).sp,
        letterSpacing = (-0.01 * 32).sp
    ),
    // headline-md → 24sp / SemiBold
    headlineMedium = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = (24 * 1.3).sp
    ),
    // body-lg → 18sp / Normal
    bodyLarge = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = (18 * 1.6).sp
    ),
    // body-md → 16sp / Normal
    bodyMedium = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = (16 * 1.6).sp
    ),
    // label-md → 14sp / SemiBold / 0.01em
    labelLarge = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = (14 * 1.4).sp,
        letterSpacing = (0.01 * 14).sp
    ),
    // label-sm → 12sp / Medium
    labelSmall = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = (12 * 1.4).sp
    )
)

// ─────────────────────────────────────────────────────────────────────────────
// COLOR SCHEMES
// ─────────────────────────────────────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary             = KumbukaColors.Primary,
    onPrimary           = KumbukaColors.OnPrimary,
    primaryContainer    = KumbukaColors.PrimaryContainer,
    onPrimaryContainer  = KumbukaColors.OnPrimaryContainer,
    secondary           = KumbukaColors.Secondary,
    onSecondary         = KumbukaColors.OnSecondary,
    secondaryContainer  = KumbukaColors.SecondaryContainer,
    onSecondaryContainer = KumbukaColors.OnSecondaryContainer,
    tertiary            = KumbukaColors.Tertiary,
    onTertiary          = KumbukaColors.OnTertiary,
    tertiaryContainer   = KumbukaColors.TertiaryContainer,
    onTertiaryContainer = KumbukaColors.OnTertiaryContainer,
    background          = KumbukaColors.Background,
    onBackground        = KumbukaColors.OnBackground,
    surface             = KumbukaColors.Surface,
    onSurface           = KumbukaColors.OnSurface,
    surfaceVariant      = KumbukaColors.SurfaceVariant,
    onSurfaceVariant    = KumbukaColors.OnSurfaceVariant,
    outline             = KumbukaColors.Outline,
    outlineVariant      = KumbukaColors.OutlineVariant,
    error               = KumbukaColors.Error,
    onError             = KumbukaColors.OnError,
    errorContainer      = KumbukaColors.ErrorContainer,
    inverseSurface      = KumbukaColors.InverseSurface,
    inverseOnSurface    = KumbukaColors.InverseOnSurface,
    inversePrimary      = KumbukaColors.InversePrimary,
)

// ─────────────────────────────────────────────────────────────────────────────
// THEME COMPOSABLE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun KumbukaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // App currently uses light theme only (matching Stitch export class="light")
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography  = KumbukaTypography,
        content     = content
    )
}
