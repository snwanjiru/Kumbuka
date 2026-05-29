package com.example.kumbuka.ui.screens

import com.example.kumbuka.ui.components.KumbukaLogo
import com.example.kumbuka.ui.theme.KumbukaColors
import com.example.kumbuka.ui.theme.ManropeFamily
import com.example.kumbuka.viewmodel.AuthState
import com.example.kumbuka.viewmodel.AuthViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
@Composable
fun ForgotPasswordScreen(
    onNavigateBackToLogin: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val viewModel: AuthViewModel = hiltViewModel()
    val authState by viewModel.authState.collectAsState()

    var email by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccess by remember { mutableStateOf(false) }
    val isLoading = authState is AuthState.Loading

    LaunchedEffect(authState) {
        when (val s = authState) {
            is AuthState.Success -> {
                viewModel.resetState()
                showSuccess = true
            }
            is AuthState.Error -> errorMessage = s.message
            else -> {}
        }
    }
    LaunchedEffect(Unit) { viewModel.resetState() }

    fun isEmailValid(e: String) = android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches()

    fun onSendClicked() {
        focusManager.clearFocus()
        if (!isEmailValid(email)) {
            errorMessage = "Please enter a valid email address."
            return
        }
        errorMessage = null
        viewModel.forgotPassword(email)
    }

    // Success dialog (unchanged)
    if (showSuccess) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = KumbukaColors.SurfaceContainerLowest,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(KumbukaColors.SurfaceContainerHigh)
                    ) {
                        Icon(
                            Icons.Outlined.MarkEmailRead, null,
                            tint = KumbukaColors.Primary, modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "Check Your Email",
                        fontFamily = ManropeFamily, fontWeight = FontWeight.Bold,
                        fontSize = 22.sp, color = KumbukaColors.Primary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "A password reset link has been sent to\n$email\n\n" +
                                "Open your email app and tap the link to choose a new password.",
                        fontFamily = ManropeFamily, fontSize = 14.sp, lineHeight = 22.sp,
                        color = KumbukaColors.OnSurfaceVariant, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(28.dp))
                    Button(
                        onClick = { showSuccess = false; onNavigateBackToLogin() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KumbukaColors.Primary,
                            contentColor = KumbukaColors.OnPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text(
                            "Back to Login", fontFamily = ManropeFamily,
                            fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    // --- Layout constants for the overlapping bar and logo ---
    val topBarHeight = 100.dp          // height of the coloured bar
    val logoDiameter = 96.dp
    val logoRadius = logoDiameter / 2   // 48.dp

    // Vertical offset for the logo: its top edge starts at (topBarHeight - logoRadius)
    // This makes the logo's centre align with the bottom edge of the bar.
    // The bar will therefore cover the top half of the logo, "cutting through" it.
    val logoTopOffset = topBarHeight - logoRadius   // = 52.dp

    // Total space from screen top to the bottom of the logo
    val bottomOfLogo = logoTopOffset + logoDiameter  // = 148.dp

    // Padding to apply on the scrollable content so it starts below the logo
    val contentTopPadding = bottomOfLogo + 24.dp      // = 172.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KumbukaColors.Background)
    ) {
        // --- Layer 1: Scrollable content (pushed down) ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = contentTopPadding)   // start below the overlapping logo
                .padding(horizontal = 28.dp)
        ) {
            // "Forgot Password?" heading
            Text(
                text = "Forgot Password?",
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = KumbukaColors.Primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // Subtitle
            Text(
                text = "Enter your email address and we'll\nsend you a link to reset your password.",
                fontFamily = ManropeFamily,
                fontSize = 16.sp,
                lineHeight = 26.sp,
                color = KumbukaColors.OnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(40.dp))

            // Email label
            Text(
                text = "Email Address",
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = KumbukaColors.OnSurfaceVariant,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, bottom = 6.dp)
            )

            // Email field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; errorMessage = null },
                placeholder = {
                    Text(
                        "name@example.com", fontFamily = ManropeFamily,
                        fontSize = 16.sp, color = KumbukaColors.Outline
                    )
                },
                singleLine = true,
                isError = errorMessage != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onSendClicked() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = KumbukaColors.Primary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = KumbukaColors.SurfaceContainerLow,
                    unfocusedContainerColor = KumbukaColors.SurfaceContainerLow,
                    errorContainerColor = KumbukaColors.ErrorContainer,
                    errorBorderColor = KumbukaColors.Error
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            )

            // Error banner
            AnimatedVisibility(visible = errorMessage != null) {
                errorMessage?.let { msg ->
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(KumbukaColors.ErrorContainer)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Warning, null,
                            tint = KumbukaColors.Error, modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            msg, fontFamily = ManropeFamily,
                            fontSize = 12.sp, color = KumbukaColors.Error
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // Send Reset Link button
            Button(
                onClick = { onSendClicked() },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = KumbukaColors.Primary,
                    contentColor = KumbukaColors.OnPrimary,
                    disabledContainerColor = KumbukaColors.Primary.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        Modifier.size(22.dp),
                        color = KumbukaColors.OnPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Send Reset Link", fontFamily = ManropeFamily,
                        fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Back to Log In text link
            TextButton(
                onClick = onNavigateBackToLogin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Back to Log In",
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = KumbukaColors.OnSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(48.dp))
        }

        // --- Layer 2: Top bar that cuts across the screen ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(topBarHeight)
                .background(KumbukaColors.PrimaryContainer)  // Use a distinct bar colour
                .statusBarsPadding()
        ) {
            // Back arrow button
            IconButton(
                onClick = onNavigateBackToLogin,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 4.dp, top = 8.dp)   // slight top padding for comfort
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = KumbukaColors.OnSurface
                )
            }
        }

        // --- Layer 3: Logo overlapping the bar (cutting through effect) ---
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = logoTopOffset)   // position so the bar covers the top half
                .size(logoDiameter)
                .shadow(elevation = 4.dp, shape = CircleShape, clip = false)
                .background(Color.White, CircleShape)
        ) {
            KumbukaLogo(size = logoDiameter * 0.68f)
        }
    }
}