package com.example.kumbuka.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.kumbuka.ui.components.KumbukaLogo
import com.example.kumbuka.ui.theme.KumbukaColors
import com.example.kumbuka.ui.theme.ManropeFamily
import com.example.kumbuka.viewmodel.AuthState
import com.example.kumbuka.viewmodel.AuthViewModel

// ─────────────────────────────────────────────────────────────────────────────
// LoginScreen
// Tabs:
// Password -> viewModel.login(email, password)
// Passwordless -> step 1: viewModel.sendOTP(email) -> button shows "Get OTP"
//                 step 2: viewModel.verifyOtp (email, otpCode) -> button shows "Log In"
// ─────────────────────────────────────────────────────────────────────────────

private enum class LoginTab { Password, OTP }

@Composable
fun LoginScreen(
    onLoginSuccess:   () -> Unit,
    onForgotPassword: () -> Unit,
    onSignUp:         () -> Unit,
    onBack:           () -> Unit,

    // Production: NavGraph calls LoginScreen() - hiltViewModel() default.
    // Tests: pass a fake ViewModel directly.
    viewModel: AuthViewModel = hiltViewModel()
) {
    val focusManager = LocalFocusManager.current

    val authState by viewModel.authState.collectAsState()

    var activeTab       by remember { mutableStateOf(LoginTab.Password) }
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var otpSent         by remember { mutableStateOf(false) }
    var otpCode         by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val isLoading       = authState is AuthState.Loading
    var errorMessage    by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(authState) {
        when (val s = authState) {
            is AuthState.Success -> { viewModel.resetState(); onLoginSuccess() }
            is AuthState.OtpSent -> { viewModel.resetState(); otpSent = true }
            is AuthState.Error   -> errorMessage = s.message
            else                 -> {}
        }
    }

    LaunchedEffect(Unit) { viewModel.resetState() }

    fun isEmailValid(e: String) = android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches()

    fun validate(): Boolean {
        return when {
            !isEmailValid(email) -> {
                errorMessage = "Please enter a valid email address.";
                false
            }
            activeTab == LoginTab.Password && password.isBlank() -> {
                errorMessage = "Password cannot be empty.";
                false
            }
            activeTab == LoginTab.OTP && otpCode.length < 4 -> {
                errorMessage = "Please enter a valid one-time code.";
                false
            }
            else -> {
                errorMessage = null;
                true
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // PASSWORD TAB LOGIN -only called on the Password tab
    // ─────────────────────────────────────────────────────────────────────────────

    fun onLoginClicked() {
        focusManager.clearFocus()
        if (!validate()) return
        errorMessage = null
        viewModel.login(email, password)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // OTP Verification - separate function, calls verifyOtp not login
    // Previously onLoginClicked() was called for both tabs which meant the OTP tab
    // was calling viewModel.login(email, password) with an empty password.

    fun onVerifyOtpClicked() {
        focusManager.clearFocus()
        if (!validate()) return
        errorMessage = null
        viewModel.verifyOtp(email, otpCode) // was login(email, password)
    }


    Scaffold(
        containerColor = KumbukaColors.Surface,
        topBar = { LoginTopBar(onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(40.dp))

            // Heading
            Text("Welcome Back", fontFamily = ManropeFamily, fontWeight = FontWeight.Bold,
                fontSize = 32.sp, color = KumbukaColors.Primary,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            Text("Log in to manage your lending circles.",
                fontFamily = ManropeFamily, fontSize = 16.sp,
                color = KumbukaColors.OnSurfaceVariant, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(24.dp))

            // Form card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = KumbukaColors.SurfaceContainerLowest,
                tonalElevation = 1.dp, shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth().border(1.dp,
                    KumbukaColors.OutlineVariant, RoundedCornerShape(12.dp))
            ) {
                Column(Modifier.padding(20.dp)) {
                    LoginTabs(activeTab) { tab -> activeTab = tab; errorMessage = null; otpSent = false }
                    Spacer(Modifier.height(24.dp))

                    // Email field shared by both tabs
                    LoginTextField(
                        value = email, onValueChange = { email = it; errorMessage = null },
                        label = "Email Address", placeholder = "name@example.com",
                        keyboardType = KeyboardType.Email, imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                        isError = errorMessage != null && !isEmailValid(email) && email.isNotBlank()
                    )
                    Spacer(Modifier.height(16.dp))

                    // Tab-specific content

                    AnimatedContent(
                        targetState = activeTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "secondField"
                    ) { tab ->
                        when (tab) {

                            // Password tab

                            LoginTab.Password -> LoginPasswordField(
                                password         = password,
                                onPasswordChange = { password = it; errorMessage = null },
                                visible          = passwordVisible,
                                onToggleVisible  = { passwordVisible = !passwordVisible },
                                onForgotPassword = onForgotPassword,
                                onDone           = { onLoginClicked() },
                                isError          = errorMessage != null && password.isBlank()
                            )

                            // Passwordless (OTP) tab
                            LoginTab.OTP -> Column {
                                // ── OTP field — only shown and enabled after code is sent
                                AnimatedVisibility(visible = otpSent) {
                                    Column {
                                        Spacer(Modifier.height(16.dp))
                                        LoginTextField(
                                            value         = otpCode,
                                            onValueChange = { if (it.length <= 6) { otpCode = it; errorMessage = null } },
                                            label         = "One-Time Code",
                                            placeholder   = "Enter the 6-digit code",
                                            keyboardType  = KeyboardType.NumberPassword,
                                            imeAction     = ImeAction.Done,
                                            onImeAction   = { onLoginClicked() },
                                            isError       = errorMessage != null && otpCode.length < 4
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        // Hint + resend link
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Code sent to your email.",
                                                fontFamily = ManropeFamily,
                                                fontSize   = 12.sp,
                                                color      = KumbukaColors.OnSurfaceVariant
                                            )
                                            Text(
                                                "Resend",
                                                fontFamily     = ManropeFamily,
                                                fontWeight     = FontWeight.Medium,
                                                fontSize       = 12.sp,
                                                color          = KumbukaColors.Secondary,
                                                textDecoration = TextDecoration.Underline,
                                                modifier       = Modifier.clickable {
                                                    otpSent = false
                                                    otpCode = ""
                                                    errorMessage = null
                                                }
                                            )
                                        }
                                    }
                                }

                                // ── Hint shown before OTP is sent ─────────────────────────────
                                AnimatedVisibility(visible = !otpSent) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "We'll send a one-time code to your email address.",
                                        fontFamily = ManropeFamily,
                                        fontSize   = 13.sp,
                                        color      = KumbukaColors.OnSurfaceVariant,
                                        modifier   = Modifier.padding(start = 4.dp, top = 8.dp)
                                    )
                                }
                            }
                        }
                    }

// ── Error banner ──────────────────────────────────────────────────────────
                    AnimatedVisibility(visible = errorMessage != null) {
                        errorMessage?.let { msg ->
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(KumbukaColors.ErrorContainer)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.Warning, null,
                                    tint = KumbukaColors.Error, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(msg, fontFamily = ManropeFamily,
                                    fontSize = 12.sp, color = KumbukaColors.Error)
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

// ── Dynamic button ────────────────────────────────────────────────────────
// Password tab  → always "Log In"
// OTP tab, code not yet sent → "Get OTP" (sends code to email)
// OTP tab, code sent         → "Log In"  (verifies code)
                    val onOtpTabBeforeSend = activeTab == LoginTab.OTP && !otpSent

                    Button(
                        onClick = {
                            if (onOtpTabBeforeSend) {
                                // Step 1 — validate email then dispatch the code
                                focusManager.clearFocus()
                                if (!isEmailValid(email)) {
                                    errorMessage = "Please enter a valid email address."
                                } else {
                                    errorMessage = null
                                    viewModel.sendOtp(email)
                                }
                            } else {
                                // Step 2 (or Password tab) — normal login
                                onLoginClicked()
                            }
                        },
                        enabled = !isLoading,
                        colors  = ButtonDefaults.buttonColors(
                            containerColor         = KumbukaColors.PrimaryContainer,
                            contentColor           = KumbukaColors.OnPrimary,
                            disabledContainerColor = KumbukaColors.PrimaryContainer.copy(alpha = 0.6f)
                        ),
                        shape    = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(Modifier.size(20.dp),
                                color = KumbukaColors.OnPrimary, strokeWidth = 2.dp)
                        } else {
                            Text(
                                text       = if (onOtpTabBeforeSend) "Get OTP" else "Log In",
                                fontFamily = ManropeFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
            OrDivider()
            Spacer(Modifier.height(24.dp))

            // Sign Up prompt
            Row(horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()) {
                Text("Don't have an account? ", fontFamily = ManropeFamily,
                    fontSize = 16.sp, color = KumbukaColors.OnSurfaceVariant)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .border(1.dp, KumbukaColors.Secondary, RoundedCornerShape(50))
                        .clip(RoundedCornerShape(50))
                        .clickable { onSignUp() }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Sign Up", fontFamily = ManropeFamily,
                        fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                        color = KumbukaColors.Secondary)
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SUB-COMPOSABLES
// ─────────────────────────────────────────────────────────────────────────────

/** Top bar using KumbukaLogo — truly centred via Box overlay */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(KumbukaColors.SurfaceContainerLowest)
            .statusBarsPadding()
    ) {
        IconButton(
            onClick  = onBack,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                tint = KumbukaColors.OnSurface)
        }
        // Truly centred logo — not affected by the nav icon offset
        KumbukaLogo(
            size     = 56.dp,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun LoginTabs(activeTab: LoginTab, onTabChange: (LoginTab) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .border(1.dp, KumbukaColors.OutlineVariant, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
    ) {
        LoginTab.entries.forEach { tab ->
            val isActive = tab == activeTab
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f)
                    .background(if (isActive) KumbukaColors.PrimaryContainer else Color.Transparent)
                    .clickable(remember { MutableInteractionSource() }, null) { onTabChange(tab) }
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    text = if (tab == LoginTab.Password) "Password" else "Passwordless",
                    fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    color = if (isActive) KumbukaColors.OnPrimary else KumbukaColors.OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LoginTextField(
    value: String, onValueChange: (String) -> Unit,
    label: String, placeholder: String,
    keyboardType: KeyboardType, imeAction: ImeAction, onImeAction: () -> Unit,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column {
        Text(label, fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp, color = KumbukaColors.OnSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        OutlinedTextField(
            value = value, onValueChange = onValueChange,
            placeholder = { Text(placeholder, fontFamily = ManropeFamily,
                fontSize = 16.sp, color = KumbukaColors.Outline) },
            singleLine = true, isError = isError,
            visualTransformation = visualTransformation, trailingIcon = trailingIcon,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(onNext = { onImeAction() }, onDone = { onImeAction() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = KumbukaColors.PrimaryContainer,
                unfocusedBorderColor    = Color.Transparent,
                focusedContainerColor   = KumbukaColors.SurfaceContainerLow,
                unfocusedContainerColor = KumbukaColors.SurfaceContainerLow,
                errorContainerColor     = KumbukaColors.ErrorContainer,
                errorBorderColor        = KumbukaColors.Error
            ),
            shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LoginPasswordField(
    password: String, onPasswordChange: (String) -> Unit,
    visible: Boolean, onToggleVisible: () -> Unit,
    onForgotPassword: () -> Unit, onDone: () -> Unit, isError: Boolean
) {
    Column {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("Password", fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp, color = KumbukaColors.OnSurfaceVariant)
            Text("Forgot Password?", fontFamily = ManropeFamily, fontWeight = FontWeight.Medium,
                fontSize = 12.sp, color = KumbukaColors.Secondary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { onForgotPassword() })
        }
        OutlinedTextField(
            value = password, onValueChange = onPasswordChange,
            placeholder = { Text("••••••••", fontFamily = ManropeFamily,
                fontSize = 16.sp, color = KumbukaColors.Outline) },
            singleLine = true, isError = isError,
            visualTransformation = if (visible) VisualTransformation.None
            else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onToggleVisible) {
                    Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        if (visible) "Hide password" else "Show password",
                        tint = KumbukaColors.Outline)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = KumbukaColors.PrimaryContainer,
                unfocusedBorderColor    = Color.Transparent,
                focusedContainerColor   = KumbukaColors.SurfaceContainerLow,
                unfocusedContainerColor = KumbukaColors.SurfaceContainerLow,
                errorContainerColor     = KumbukaColors.ErrorContainer,
                errorBorderColor        = KumbukaColors.Error
            ),
            shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun OrDivider() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(Modifier.weight(1f), color = KumbukaColors.OutlineVariant, thickness = 1.dp)
        Text("OR", fontFamily = ManropeFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
            color = KumbukaColors.OutlineVariant, modifier = Modifier.padding(horizontal = 8.dp))
        HorizontalDivider(Modifier.weight(1f), color = KumbukaColors.OutlineVariant, thickness = 1.dp)
    }
}